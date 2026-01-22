# Plan: Implement `termStats` SEARCH_META Operator

Add a new `termStats` operator to SEARCH_META that returns Lucene term statistics (document frequency, total term frequency, collection statistics) for specified fields. This enables users to analyze term distribution and rarity across their search corpus.

## Steps

### 1. Create `TermStatsCollector` and supporting classes

- Add `TermStatsCollector` record to [src/main/java/com/xgen/mongot/index/query/Collector.java](src/main/java/com/xgen/mongot/index/query/Collector.java)
- Create `TermStatsDefinition` in [src/main/java/com/xgen/mongot/index/query/facet](src/main/java/com/xgen/mongot/index/query/facet) defining which fields to analyze
- Create `TermStatsInfo` in [src/main/java/com/xgen/mongot/index/meta](src/main/java/com/xgen/mongot/index/meta) containing term stats results (docFreq, totalTermFreq, etc.)

### 2. Add parsing and serialization for `termStats` BSON protocol

- Update `Collector.atMostOneFromBson()` to parse `termStats` field
- Add `TermStatsCollector.fromBson()` with field parsers following `Field.Required`/`Field.Optional` patterns
- Implement `toBson()` for serialization
- Update `MetaResults` to include `Optional<Map<String, TermStatsInfo>> termStats()`

### 3. Implement `LuceneTermStatsCollectorSearchManager` for execution

- Create search manager in [src/main/java/com/xgen/mongot/index/lucene](src/main/java/com/xgen/mongot/index/lucene) extending `LuceneSearchManager`
- Use Lucene `Terms` API: `leafReader.terms(fieldName)` → `TermsEnum` to iterate terms
- Collect `TermStatistics` (docFreq, totalTermFreq) and `CollectionStatistics` per field
- Handle field resolution via `FieldName` to map user fields to Lucene field names
- Build `TermStatsInfo` results for each requested field

### 4. Create `TermStatsMetaBatchProducer` and update result building

- Implement `BatchProducer` for streaming term stats results
- Update `LuceneMetaResultsBuilder` with `getTermStatsResult()` method
- Add `TermStatsMetaBatchProducerFactory` in [src/main/java/com/xgen/mongot/index/lucene/batch](src/main/java/com/xgen/mongot/index/lucene/batch)
- Update `LuceneSearchIndexReader.collectorQuery()` to route `TermStatsCollector` queries

### 5. Add sharded search support and merging logic

- Update `ShardedSearchPlanner.getMetaStageIfPresent()` to handle term stats merging
- Create aggregation pipeline stage to merge term stats from multiple shards (sum docFreqs, totalTermFreqs)
- Handle `FacetMergingBatchProducer` pattern for distributed results

### 6. Add comprehensive testing

- Create `TermStatsCollectorTest` in [src/test/unit/java/com/xgen/mongot/index/query](src/test/unit/java/com/xgen/mongot/index/query) with BSON serialization test suite
- Add `TermStatsMetaBatchProducerTest` for execution testing
- Create `TermStatsInfoBuilder` and `TermStatsCollectorBuilder` in [src/main/java/com/xgen/testing/mongot](src/main/java/com/xgen/testing/mongot)
- Add integration tests in [src/test/integration/java/com/xgen/mongot/index](src/test/integration/java/com/xgen/mongot/index) for end-to-end functionality

## Further Considerations

1. **Field type support** - Should term stats work for all field types (STRING, TOKEN, NUMBER, etc.) or only text fields? Recommend starting with STRING/TOKEN fields only.

2. **Performance limits** - Large fields could have millions of unique terms. Should we add limits like `maxTerms` or `termFilter` to prevent expensive operations?

3. **Output format** - Should results include per-term statistics or aggregate statistics per field? Consider: `{ "fieldName": { "numTerms": 1000, "avgDocFreq": 5.2, "terms": [...] } }`

4. **Compatibility with existing operators** - Can `termStats` be combined with a regular search operator (like facets work), or should it be standalone?
