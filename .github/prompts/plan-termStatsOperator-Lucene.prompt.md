# Plan: Implement `termStats` SEARCH_META Operator

Add a new `termStats` operator to SEARCH_META that returns Lucene term statistics (document frequency, total term frequency, collection statistics) for specified fields. This enables users to analyze term distribution and rarity across their search corpus.

## Implementation Status

### ✅ Phase 1: BSON Protocol & Data Structures (COMPLETED)

The foundation is complete with full BSON serialization/deserialization and test infrastructure.

### 🚧 Phase 2: Lucene Execution Layer (IN PROGRESS)

This phase implements the actual term statistics collection from Lucene indexes.

### ⏳ Phase 3: Sharded Search Support (NOT STARTED)

This phase adds distributed query support for sharded collections.

---

## Detailed Steps

### ✅ Step 1: Create `TermStatsCollector` and supporting classes (COMPLETED)

**Created Files:**
- [src/main/java/com/xgen/mongot/index/TermStatsInfo.java](src/main/java/com/xgen/mongot/index/TermStatsInfo.java) - Record containing term statistics with fields:
  - `docFreq` - Documents containing any term in the field
  - `sumDocFreq` - Sum of document frequencies for all terms
  - `sumTotalTermFreq` - Total term occurrences across all documents
  - `docCount` - Documents with at least one term for this field
  - `avgDocFreq` - Average document frequency per term
  - Includes `merge(List<TermStatsInfo>)` static method for combining shard results

- [src/main/java/com/xgen/mongot/index/query/collectors/TermStatsDefinition.java](src/main/java/com/xgen/mongot/index/query/collectors/TermStatsDefinition.java) - Defines which fields to analyze
  - `fields` - List of field names (required, must not be empty)

- [src/main/java/com/xgen/mongot/index/query/collectors/TermStatsCollector.java](src/main/java/com/xgen/mongot/index/query/collectors/TermStatsCollector.java) - Collector implementation
  - `definition` - The TermStatsDefinition specifying fields
  - Implements `Collector` interface with `getType()` returning `TERM_STATS`

**Updated Files:**
- [src/main/java/com/xgen/mongot/index/query/collectors/Collector.java](src/main/java/com/xgen/mongot/index/query/collectors/Collector.java)
  - Added `TERM_STATS("termStats")` to Type enum
  - Added `TERM_STATS` field to Fields class
  - Updated `permits` clause to include `TermStatsCollector`
  - Updated `toBson()` switch to handle TermStatsCollector case
  - Updated `atMostOneFromBson()` to parse termStats field

### ✅ Step 2: Add parsing and serialization for `termStats` BSON protocol (COMPLETED)

**Updated Files:**
- [src/main/java/com/xgen/mongot/index/MetaResults.java](src/main/java/com/xgen/mongot/index/MetaResults.java)
  - Added `termStats` parameter: `Optional<Map<String, TermStatsInfo>>`
  - Added `TERM_STATS_RESULTS` field for parsing/serialization
  - Updated all constructors to include termStats parameter
  - Updated `EMPTY` constant to include empty termStats
  - Updated `mergeCountResult()` to validate termStats is empty
  - Updated `fromBson()` and `toBson()` methods

**BUILD Files Updated:**
- [src/main/java/com/xgen/mongot/index/BUILD](src/main/java/com/xgen/mongot/index/BUILD) - Added TermStatsInfo.java
- [src/main/java/com/xgen/mongot/index/query/collectors/BUILD](src/main/java/com/xgen/mongot/index/query/collectors/BUILD) - Added TermStatsCollector.java and TermStatsDefinition.java

### ✅ Step 6 (Partial): Add comprehensive testing (COMPLETED - Serialization tests)

**Created Test Files:**
- [src/test/unit/java/com/xgen/mongot/index/query/collectors/TermStatsCollectorTest.java](src/test/unit/java/com/xgen/mongot/index/query/collectors/TermStatsCollectorTest.java)
  - Parameterized tests using BsonDeserializationTestSuite
  - Tests for single field, multiple fields, and invalid cases
  - Tests for collector type, toBson(), and collectorToBson()

- [src/test/unit/java/com/xgen/mongot/index/TermStatsInfoTest.java](src/test/unit/java/com/xgen/mongot/index/TermStatsInfoTest.java)
  - Tests for BSON serialization/deserialization
  - Tests for merging (empty, single, multiple TermStatsInfo objects)
  - Validates avgDocFreq recalculation during merge

- [src/test/unit/resources/index/query/collectors/termStats.json](src/test/unit/resources/index/query/collectors/termStats.json)
  - Test fixtures with valid and invalid cases

**Created Test Builders:**
- [src/main/java/com/xgen/testing/mongot/index/TermStatsInfoBuilder.java](src/main/java/com/xgen/testing/mongot/index/TermStatsInfoBuilder.java)
- [src/main/java/com/xgen/testing/mongot/index/query/collectors/TermStatsDefinitionBuilder.java](src/main/java/com/xgen/testing/mongot/index/query/collectors/TermStatsDefinitionBuilder.java)
- [src/main/java/com/xgen/testing/mongot/index/query/collectors/TermStatsCollectorBuilder.java](src/main/java/com/xgen/testing/mongot/index/query/collectors/TermStatsCollectorBuilder.java)
- Updated [src/main/java/com/xgen/testing/mongot/index/query/collectors/CollectorBuilder.java](src/main/java/com/xgen/testing/mongot/index/query/collectors/CollectorBuilder.java) with `termStats()` factory method

**BUILD Files Updated:**
- [src/main/java/com/xgen/testing/mongot/index/BUILD](src/main/java/com/xgen/testing/mongot/index/BUILD) - Added TermStatsInfoBuilder.java
- [src/main/java/com/xgen/testing/mongot/index/query/collectors/BUILD](src/main/java/com/xgen/testing/mongot/index/query/collectors/BUILD) - Added builders
- [src/test/unit/java/com/xgen/mongot/index/query/collectors/BUILD](src/test/unit/java/com/xgen/mongot/index/query/collectors/BUILD) - Added TermStatsCollectorTest.java

---

## Next Steps: Lucene Execution Layer

### 🚧 Step 3: Implement Lucene term statistics collection

**Pattern to follow:** See [LuceneFacetCollectorSearchManager.java](src/main/java/com/xgen/mongot/index/lucene/LuceneFacetCollectorSearchManager.java) and related facet classes.

#### 3.1 Create `LuceneTermStatsCollectorSearchManager`

Create [src/main/java/com/xgen/mongot/index/lucene/LuceneTermStatsCollectorSearchManager.java](src/main/java/com/xgen/mongot/index/lucene/LuceneTermStatsCollectorSearchManager.java):

```java
class LuceneTermStatsCollectorSearchManager 
    extends AbstractLuceneSearchManager<LuceneTermStatsCollectorSearchManager.TermStatsCollectorQueryInfo> {
  
  static class TermStatsCollectorQueryInfo extends QueryInfo {
    // No special collector needed - just TopDocs for count
    TermStatsCollectorQueryInfo(TopDocs topDocs, boolean luceneExhausted) { ... }
  }
  
  // Override initialSearch() to run query without special collectors
  // Term stats are collected separately from the documents
}
```

#### 3.2 Add term statistics collection logic to `LuceneMetaResultsBuilder`

Update [src/main/java/com/xgen/mongot/index/lucene/LuceneMetaResultsBuilder.java](src/main/java/com/xgen/mongot/index/lucene/LuceneMetaResultsBuilder.java):

Add method:
```java
public MetaResults buildTermStatsMetaResults(
    LuceneIndexSearcherReference searcherReference,
    TopDocs topDocs,
    CollectorQuery collectorQuery) 
    throws IOException, InvalidQueryException {
  
  TermStatsCollector termStatsCollector = (TermStatsCollector) collectorQuery.collector();
  Map<String, TermStatsInfo> termStatsResults = new HashMap<>();
  
  for (String userFieldName : termStatsCollector.definition().fields()) {
    // Resolve user field to Lucene field(s) using FieldName
    List<FieldName> luceneFields = this.facetContext.resolveFieldNames(userFieldName);
    
    // Collect stats across all matching Lucene fields
    TermStatsInfo fieldStats = collectTermStatsForField(
        searcherReference, 
        luceneFields,
        collectorQuery.returnScope());
    
    termStatsResults.put(userFieldName, fieldStats);
  }
  
  return new MetaResults(
      LuceneFacetResultUtil.getCount(topDocs.totalHits.value, Count.Type.TOTAL),
      Optional.empty(), // No facets
      Optional.of(termStatsResults));
}

private TermStatsInfo collectTermStatsForField(
    LuceneIndexSearcherReference searcherReference,
    List<FieldName> luceneFields,
    Optional<ReturnScope> returnScope) throws IOException {
  
  long totalDocFreq = 0;
  long totalSumDocFreq = 0;
  long totalSumTotalTermFreq = 0;
  int totalDocCount = 0;
  
  IndexSearcher searcher = searcherReference.get();
  
  for (FieldName fieldName : luceneFields) {
    String luceneFieldName = fieldName.getLuceneFieldName();
    
    // Iterate over all leaf readers
    for (LeafReaderContext context : searcher.getIndexReader().leaves()) {
      LeafReader leafReader = context.reader();
      
      // Get terms for this field
      Terms terms = leafReader.terms(luceneFieldName);
      if (terms == null) continue;
      
      // Get collection statistics
      CollectionStatistics collStats = 
          searcher.collectionStatistics(luceneFieldName);
      if (collStats != null) {
        totalDocCount += collStats.docCount();
        totalSumDocFreq += collStats.sumDocFreq();
        totalSumTotalTermFreq += collStats.sumTotalTermFreq();
      }
      
      // Count unique terms with docs
      TermsEnum termsEnum = terms.iterator();
      while (termsEnum.next() != null) {
        if (termsEnum.docFreq() > 0) {
          totalDocFreq++;
        }
      }
    }
  }
  
  double avgDocFreq = totalSumDocFreq > 0 
      ? (double) totalDocFreq / totalSumDocFreq 
      : 0.0;
  
  return new TermStatsInfo(
      totalDocFreq, 
      totalSumDocFreq, 
      totalSumTotalTermFreq, 
      totalDocCount, 
      avgDocFreq);
}
```

#### 3.3 Update `LuceneSearchIndexReader.collectorQuery()`

Update [src/main/java/com/xgen/mongot/index/lucene/LuceneSearchIndexReader.java](src/main/java/com/xgen/mongot/index/lucene/LuceneSearchIndexReader.java):

In the `collectorQuery()` method, add a case for TermStatsCollector:

```java
private SearchProducerAndMetaResults collectorQuery(...) {
  return switch (query.collector()) {
    case FacetCollector facetCollector -> {
      // existing facet logic...
    }
    case TermStatsCollector termStatsCollector -> {
      // Create search manager
      var termStatsSearchManager = 
          this.luceneSearchManagerFactory.newTermStatsCollectorManager(
              luceneQuery, luceneSort, searchAfter);
      
      var collectorQueryInfo = termStatsSearchManager.initialSearch(
          searcherReference, 
          batchSizeStrategy.adviseNextBatchSize());
      
      yield new SearchProducerAndMetaResults(
          EmptyBatchProducer.INSTANCE, // No search results for meta-only queries
          this.metaResultsBuilder.buildTermStatsMetaResults(
              searcherReference,
              collectorQueryInfo.topDocs,
              query));
    }
  };
}
```

#### 3.4 Add factory method in `LuceneSearchManagerFactory`

Update [src/main/java/com/xgen/mongot/index/lucene/LuceneSearchManagerFactory.java](src/main/java/com/xgen/mongot/index/lucene/LuceneSearchManagerFactory.java):

```java
public LuceneTermStatsCollectorSearchManager newTermStatsCollectorManager(
    Query luceneQuery, 
    Sort luceneSort,
    Optional<FieldDoc> searchAfter) {
  return new LuceneTermStatsCollectorSearchManager(
      luceneQuery, luceneSort, searchAfter);
}
```

#### 3.5 Add execution tests

Create [src/test/unit/java/com/xgen/mongot/index/lucene/LuceneTermStatsCollectorTest.java](src/test/unit/java/com/xgen/mongot/index/lucene/LuceneTermStatsCollectorTest.java):

- Test term stats collection on simple index with known terms
- Test multiple fields
- Test fields with different term distributions
- Test empty fields
- Test with return scope filtering

### 🚧 Step 4: Create batch producer for intermediate results (for sharded queries)

**Pattern to follow:** See [LuceneFacetCollectorMetaBatchProducer.java](src/main/java/com/xgen/mongot/index/lucene/LuceneFacetCollectorMetaBatchProducer.java) and [LuceneFacetCollectorMetaBatchProducerFactory.java](src/main/java/com/xgen/mongot/index/lucene/LuceneFacetCollectorMetaBatchProducerFactory.java).

#### 4.1 Create `LuceneTermStatsCollectorMetaBatchProducer`

Create [src/main/java/com/xgen/mongot/index/lucene/LuceneTermStatsCollectorMetaBatchProducer.java](src/main/java/com/xgen/mongot/index/lucene/LuceneTermStatsCollectorMetaBatchProducer.java):

Similar structure to `LuceneFacetCollectorMetaBatchProducer`:
- Outputs count document first
- Then outputs intermediate term stats documents for each field
- Format: `{ "type": "termStats", "field": "fieldName", "stats": { docFreq: ..., sumDocFreq: ..., ... } }`

#### 4.2 Create `LuceneTermStatsCollectorMetaBatchProducerFactory`

Create [src/main/java/com/xgen/mongot/index/lucene/LuceneTermStatsCollectorMetaBatchProducerFactory.java](src/main/java/com/xgen/mongot/index/lucene/LuceneTermStatsCollectorMetaBatchProducerFactory.java):

Factory to create batch producers, following the pattern in the facet factory.

#### 4.3 Update `LuceneSearchIndexReader.intermediateCollectorQuery()`

Add routing for TermStatsCollector to create the appropriate batch producer for intermediate queries (used in sharded scenarios).

---

## Next Steps: Sharded Search Support

### ⏳ Step 5: Add sharded search support and merging logic

**Pattern to follow:** See [FacetMergingBatchProducer.java](src/main/java/com/xgen/mongot/index/lucene/FacetMergingBatchProducer.java) and [ShardedSearchPlanner.java](src/main/java/com/xgen/mongot/server/command/search/ShardedSearchPlanner.java).

#### 5.1 Create `TermStatsMergingBatchProducer`

Create similar to `FacetMergingBatchProducer` but for term stats:
- Takes multiple `LuceneTermStatsCollectorMetaBatchProducer` instances (one per shard)
- Merges term stats by summing statistics across shards
- Uses `TermStatsInfo.merge()` method

#### 5.2 Update `ShardedSearchPlanner`

Update [src/main/java/com/xgen/mongot/server/command/search/ShardedSearchPlanner.java](src/main/java/com/xgen/mongot/server/command/search/ShardedSearchPlanner.java):

In `getMetaStageIfPresent()`, add handling for TermStatsCollector:
- Create aggregation pipeline stages to merge term stats from shards
- Sum docFreq, sumDocFreq, sumTotalTermFreq, docCount
- Recalculate avgDocFreq after merging

---

## Remaining Testing

### Step 6 (Continuation): Integration and execution tests

#### 6.1 Create execution tests
- [src/test/unit/java/com/xgen/mongot/index/lucene/LuceneTermStatsMetaBatchProducerTest.java](src/test/unit/java/com/xgen/mongot/index/lucene/LuceneTermStatsMetaBatchProducerTest.java)
- Test batch production and intermediate result format

#### 6.2 Create integration tests
- [src/test/integration/java/com/xgen/mongot/index/TermStatsIntegrationTest.java](src/test/integration/java/com/xgen/mongot/index/TermStatsIntegrationTest.java)
- End-to-end tests with real Lucene indexes
- Test with different field types (STRING, TOKEN)
- Test with sharded collections

#### 6.3 Create merging tests
- Test `TermStatsMergingBatchProducer` with multiple shards
- Validate correct aggregation of statistics

---

## Further Considerations

1. **Field type support** - Current implementation focuses on STRING/TOKEN fields. Consider supporting or explicitly rejecting NUMBER, DATE, etc. Add validation in `TermStatsDefinition` or collection logic.

2. **Performance limits** - Large fields could have millions of unique terms. Consider adding:
   - `maxTerms` parameter to limit term enumeration
   - `samplingRate` to collect stats from subset of terms
   - Timeout mechanism for expensive operations

3. **Output format** - Current design provides aggregate statistics per field. Future enhancement could include:
   - Per-term statistics: `{ "term": "word", "docFreq": 10, "termFreq": 50 }`
   - Top-N most/least frequent terms
   - Histogram of term frequency distribution

4. **Compatibility** - Current design makes termStats a standalone collector (like facet). This means it cannot be combined with a search operator in the same query. This is consistent with facet behavior but could be enhanced in future to allow combined queries.

5. **Field resolution** - Pay attention to how `FieldName` resolution works with different field types and returnScope. Test edge cases like:
   - Fields that don't exist in the index
   - Fields with multiple Lucene representations (analyzed vs non-analyzed)
   - Return scope filtering
