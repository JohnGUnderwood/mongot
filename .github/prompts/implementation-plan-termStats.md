# Implementation Plan: termStats Feature

## Overview

The `termStats` feature provides term statistics (document frequency, term frequency, etc.) from Lucene's term dictionary for specified fields. It works as a query-level metadata field similar to `count` and `highlight`, and can be combined with any operator to get statistics for a filtered set of documents, or used without an operator for index-wide statistics.

## Architecture Decision

**Key Design Choice:** `termStats` is implemented as a **query-level field**, NOT as a Collector type.

### Why Not a Collector?

Unlike `facet` (which is a Collector containing its own operator), `termStats` follows the same pattern as `count` and `highlight`:
- It's an optional metadata request that can be added to any operator query
- It doesn't change the fundamental query execution model
- It works naturally with the existing operator → results flow

### Query Structure

```json
{
  "text": {
    "query": "search terms",
    "path": "content"
  },
  "count": { "type": "total" },
  "termStats": {
    "fields": ["fieldName1", "fieldName2"]
  }
}
```

Or for index-wide statistics (no operator):
```json
{
  "count": { "type": "total" },
  "termStats": {
    "fields": ["fieldName"]
  }
}
```

When no operator is present, the query parsing defaults to `AllDocumentsOperator`.

## Data Structures

### 1. TermStatsRequest (Query Level)

**Location:** `src/main/java/com/xgen/mongot/index/query/TermStatsRequest.java`

```java
public record TermStatsRequest(TermStatsDefinition definition) implements DocumentEncodable
```

- Simple wrapper around `TermStatsDefinition`
- Parsed from the `termStats` field in the query
- Added as an `Optional<TermStatsRequest>` field to both `OperatorQuery` and `CollectorQuery`

### 2. TermStatsDefinition

**Location:** `src/main/java/com/xgen/mongot/index/query/collectors/TermStatsDefinition.java`

```java
public record TermStatsDefinition(List<String> fields)
```

- Specifies which fields to collect statistics for
- Validates that fields exist and are indexed
- Can be extended later to support additional parameters (e.g., term filtering, min/max frequencies)

### 3. TermStatsInfo (Response)

**Location:** `src/main/java/com/xgen/mongot/index/TermStatsInfo.java`

```java
public record TermStatsInfo(
    long docFreq,           // Number of documents containing any term in this field
    long sumDocFreq,        // Sum of document frequencies for all terms
    long sumTotalTermFreq,  // Total term occurrences across all documents
    int docCount,           // Number of documents with this field
    double avgDocFreq       // Average document frequency per term
)
```

- Contains aggregated statistics for a single field
- Includes a `merge()` method for combining stats from multiple index partitions
- Uses `@Var` annotations for mutable variables in the merge method

## Query Integration

### SearchQuery Interface Changes

**File:** `src/main/java/com/xgen/mongot/index/query/SearchQuery.java`

1. Add field definition:
```java
public static final Field.Optional<TermStatsRequest> TERM_STATS =
    Field.builder("termStats")
        .classField(TermStatsRequest::fromBson)
        .disallowUnknownFields()
        .optional()
        .noDefault();
```

2. Add method to interface:
```java
Optional<TermStatsRequest> termStats();
```

### OperatorQuery Changes

**File:** `src/main/java/com/xgen/mongot/index/query/OperatorQuery.java`

Add `termStats` as the last parameter:
```java
public record OperatorQuery(
    Operator operator,
    String index,
    Count count,
    Optional<UnresolvedHighlight> highlight,
    Optional<Pagination> pagination,
    boolean returnStoredSource,
    boolean scoreDetails,
    boolean concurrent,
    Optional<SortSpec> rawSortSpec,
    Optional<Tracking> tracking,
    Optional<ReturnScope> returnScope,
    Optional<TermStatsRequest> termStats)  // <-- Add this
```

### CollectorQuery Changes

**File:** `src/main/java/com/xgen/mongot/index/query/CollectorQuery.java`

Add `termStats` as the last parameter (same as OperatorQuery) to maintain consistency.

### Query Parsing

**File:** `src/main/java/com/xgen/mongot/index/query/SearchQuery.java` - `fromBson()` method

When constructing both `OperatorQuery` and `CollectorQuery`, pass the termStats field:
```java
parser.getField(Fields.TERM_STATS).unwrap()
```

## Response Structure

### MetaResults Enhancement

**File:** `src/main/java/com/xgen/mongot/index/MetaResults.java`

The existing constructor already supports termStats:
```java
public record MetaResults(
    CountResult count,
    Optional<Map<String, FacetInfo>> facet,
    Optional<Map<String, TermStatsInfo>> termStats)  // <-- Already exists
```

The response structure:
```json
{
  "meta": {
    "count": { "total": 1000 },
    "termStats": {
      "fieldName": {
        "docFreq": 500,
        "sumDocFreq": 1200,
        "sumTotalTermFreq": 5000,
        "docCount": 500,
        "avgDocFreq": 2.4
      }
    }
  },
  "docs": [...]
}
```

## Lucene Implementation

### Field Type Validation ✓ IMPLEMENTED

**Important Design Decision**: `termStats` only works with text-indexed fields that have traditional inverted indexes with term dictionaries.

**Supported Field Types:**
- `TOKEN` - Tokenized text fields
- `STRING` - String fields with optional case normalization  
- `AUTOCOMPLETE` - Autocomplete fields

**Unsupported Field Types:**
- Numeric fields (`NUMBER_INT64`, `NUMBER_DOUBLE`, etc.) - Use BKD point trees, not term dictionaries
- Date fields (`DATE`, `DATE_V2`, etc.) - Use BKD point trees, not term dictionaries
- Vector fields (`KNN_VECTOR`, `KNN_BYTE`, etc.) - Use vector indexes, not term dictionaries
- Boolean, ObjectID, UUID fields - May have doc values but not useful term statistics

**Behavior for Unsupported Fields:**
- Returns `TermStatsInfo(0, 0, 0, 0, 0.0)` to indicate no term statistics are available
- Does not throw errors - gracefully indicates the field type doesn't support this operation
- Users can distinguish between "field doesn't exist" and "field exists but has no term stats"

### Collection Strategy

The implementation should collect term statistics during or after query execution:

1. **For Filtered Queries** (with operator):
   - Execute the query to get matching documents
   - For each requested field, iterate through the terms
   - Collect statistics only from documents matching the query
   - This requires accessing the term vectors or postings lists filtered by the DocIdSet

2. **For Index-Wide Stats** (no operator / AllDocumentsOperator):
   - Use `IndexReader.terms(field)` to get `Terms` object
   - Call `getSumDocFreq()`, `getSumTotalTermFreq()`, `getDocCount()`, `size()`
   - Much faster since no document filtering is needed

### Key Lucene APIs

- `IndexReader.terms(String field)` - Get Terms for a field
- `Terms.getDocCount()` - Number of documents with at least one term
- `Terms.getSumDocFreq()` - Sum of document frequencies
- `Terms.getSumTotalTermFreq()` - Sum of term frequencies
- `Terms.size()` - Number of unique terms (for calculating averages)

### Integration Points

The actual Lucene statistics collection will need to be implemented in:

**File:** `src/main/java/com/xgen/mongot/index/lucene/LuceneSearchIndexReader.java`

After executing the query and collecting results, check if `query.termStats().isPresent()`:
- If present, collect statistics for the specified fields
- Package them into `Map<String, TermStatsInfo>`
- Include in the `MetaResults` returned with the search results

### Considerations for Filtered Statistics

For filtered queries (non-AllDocuments operators), collecting statistics is more complex:

**Option 1: Post-filter approach**
- Get all term statistics from Lucene
- Use the query's DocIdSet to filter which documents contribute
- Iterate through postings lists, checking if each doc is in the result set

**Option 2: Document-based approach**
- Iterate through matching documents
- For each document, get its term vectors
- Aggregate statistics from the matched document set
- More accurate but potentially slower

**Recommendation:** Start with Option 2 for correctness, optimize later if needed.

## Collector.java Changes

**File:** `src/main/java/com/xgen/mongot/index/query/collectors/Collector.java`

Remove all TermStatsCollector references:
- Remove from `sealed interface permits` clause (keep only `FacetCollector`)
- Remove `TERM_STATS` field from `Fields` inner class
- Remove `TERM_STATS("termStats")` from `Type` enum

This keeps the Collector interface focused on its original purpose (facets).

## File Organization

### New Files
- `src/main/java/com/xgen/mongot/index/query/TermStatsRequest.java`
- Implementation files in Lucene package (to be created)

### Modified Files
- `src/main/java/com/xgen/mongot/index/query/SearchQuery.java`
- `src/main/java/com/xgen/mongot/index/query/OperatorQuery.java`
- `src/main/java/com/xgen/mongot/index/query/CollectorQuery.java`
- `src/main/java/com/xgen/mongot/index/query/collectors/Collector.java`
- `src/main/java/com/xgen/mongot/index/MetaResults.java` (already has support)
- Lucene implementation files (LuceneSearchIndexReader, etc.)

### Existing Files (No Changes Needed)
- `src/main/java/com/xgen/mongot/index/TermStatsInfo.java` ✓
- `src/main/java/com/xgen/mongot/index/query/collectors/TermStatsDefinition.java` ✓

## Implementation Steps

### Phase 1: Core Data Structures ✓
1. ✓ Create/verify `TermStatsInfo.java` with merge logic
2. ✓ Create/verify `TermStatsDefinition.java`
3. ✓ Create `TermStatsRequest.java` as query-level wrapper

### Phase 2: Query Integration ✓
1. ✓ Add `TERM_STATS` field to `SearchQuery.Fields`
2. ✓ Add `termStats()` method to `SearchQuery` interface
3. ✓ Add `termStats` parameter to `OperatorQuery` constructor
4. ✓ Add `termStats` parameter to `CollectorQuery` constructor
5. ✓ Update `SearchQuery.fromBson()` to parse and pass termStats
6. ✓ Remove TermStatsCollector from `Collector.java`

### Phase 3: Response Integration ✓
1. ✓ Verify `MetaResults` has termStats support
2. ✓ Update all `MetaResults` constructor calls to pass `Optional.empty()` for termStats
3. ✓ Update `MetaResultsBuilder` test utility to support termStats

### Phase 4: Lucene Implementation ✓ WITH FIELD TYPE VALIDATION
1. ✓ Implement statistics collection in `LuceneMetaResultsBuilder`
2. ✓ Add field type validation to only process text-indexed fields (TOKEN, STRING, AUTOCOMPLETE)
3. ✓ Return zero statistics for non-text fields (numeric, date, vector fields)
4. TODO: Add index-wide statistics collection (AllDocumentsOperator case)
5. TODO: Add filtered statistics collection (other operators)
6. TODO: Handle multi-partition statistics merging
7. TODO: Add proper error handling and validation

### Phase 5: Testing (TODO)
1. Unit tests for `TermStatsRequest` parsing
2. Unit tests for `TermStatsInfo.merge()`
3. Integration tests for index-wide statistics
4. Integration tests for filtered statistics
5. Performance tests for large indexes

## Edge Cases and Considerations

### 1. Non-existent Fields
- Validate field names against index definition
- Return zero statistics if field doesn't exist
- This indicates the field is not present in the index

### 2. Non-Text-Indexed Fields ✓ IMPLEMENTED
- **Only TOKEN, STRING, and AUTOCOMPLETE fields support term statistics**
- Numeric fields (DATE, NUMBER_INT64, NUMBER_DOUBLE, etc.) use point trees without term dictionaries
- Vector fields (KNN_VECTOR, KNN_BYTE, etc.) don't have term-based indexes
- **Behavior**: Return zero statistics for non-text fields to indicate no term statistics are available
- **Rationale**: This is honest about what data is available and prevents returning misleading statistics
- Clear distinction: zeros mean "no data" rather than "field doesn't exist"

### 3. Empty Result Sets
- When no documents match the query
- Return zero values for all statistics
- Don't return error, just empty stats

### 4. Vector Fields
- Vector fields don't have traditional term statistics
- Handled by the text-indexed field validation (see #2)
- Returns zeros as they are not text-indexed

### 5. Multi-valued Fields
- Lucene handles multi-valued fields naturally
- Statistics reflect all values across all occurrences
- Only applies to text-indexed fields (TOKEN, STRING, AUTOCOMPLETE)

### 6. Performance
- Large indexes with many unique terms
- Consider caching frequently requested field statistics
- Add sampling or limits if needed for very large fields

## Testing Strategy

### Unit Tests
- `TermStatsRequest` BSON serialization/deserialization
- `TermStatsInfo.merge()` with various input combinations
- Query parsing with and without termStats field
- Validation of field names

### Integration Tests
- End-to-end query with termStats request
- Verify statistics accuracy against known test data
- Test with various operators (text, range, compound, etc.)
- Test with AllDocumentsOperator (no operator specified)

### Performance Tests
- Benchmark statistics collection on large indexes
- Compare filtered vs. index-wide statistics performance
- Test with high-cardinality fields

## Future Enhancements

### Possible Extensions
1. **Term-level filtering**: Request statistics for specific terms only
2. **Top terms**: Return most frequent N terms with their statistics  
3. **Percentiles**: Add percentile calculations for term frequencies
4. **Field type statistics**: Different stats for different field types
5. **Sampling**: Option to sample large term dictionaries for faster results
6. **Caching**: Cache frequently requested field statistics

### API Evolution
The current API is extensible. `TermStatsDefinition` can be enhanced with additional parameters without breaking existing queries:

```java
public record TermStatsDefinition(
    List<String> fields,
    Optional<Integer> topTerms,      // Future: return top N terms
    Optional<List<String>> terms     // Future: filter to specific terms
)
```

## Summary

This implementation makes `termStats` a first-class query metadata field, consistent with how `count` and `highlight` work. It's simpler, more intuitive, and more flexible than the Collector-based approach, allowing any operator to be combined with term statistics collection naturally.

The key insight: **termStats is metadata about the result set, not a different type of collection strategy.**
