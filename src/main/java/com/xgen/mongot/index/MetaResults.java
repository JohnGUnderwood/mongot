package com.xgen.mongot.index;

import com.xgen.mongot.util.Check;
import com.xgen.mongot.util.bson.parser.BsonDocumentBuilder;
import com.xgen.mongot.util.bson.parser.BsonDocumentParser;
import com.xgen.mongot.util.bson.parser.BsonParseException;
import com.xgen.mongot.util.bson.parser.DocumentEncodable;
import com.xgen.mongot.util.bson.parser.DocumentParser;
import com.xgen.mongot.util.bson.parser.Field;
import com.xgen.mongot.util.bson.parser.Value;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.bson.BsonDocument;

/**
 * Contains meta info for the query.
 *
 * @param facet Keyed by facet name.
 * @param termStats Keyed by field name.
 */
public record MetaResults(
    CountResult count,
    Optional<Map<String, FacetInfo>> facet,
    Optional<Map<String, TermStatsInfo>> termStats)
    implements DocumentEncodable {

  private static class Fields {
    static final Field.Required<CountResult> COUNT_RESULT =
        Field.builder("count").classField(CountResult::fromBson).disallowUnknownFields().required();

    static final Field.Optional<Map<String, FacetInfo>> FACET_RESULTS =
        Field.builder("facet")
            .mapOf(
                Value.builder().classValue(FacetInfo::fromBson).disallowUnknownFields().required())
            .optional()
            .noDefault();

    static final Field.Optional<Map<String, TermStatsInfo>> TERM_STATS_RESULTS =
        Field.builder("termStats")
            .mapOf(
                Value.builder()
                    .classValue(TermStatsInfo::fromBson)
                    .disallowUnknownFields()
                    .required())
            .optional()
            .noDefault();
  }

  public static final MetaResults EMPTY =
      new MetaResults(CountResult.totalCount(0), Optional.empty(), Optional.empty());

  public MetaResults(CountResult count) {
    this(count, Optional.empty(), Optional.empty());
  }

  /**
   * This method merges the count field only, and intentionally rejects to merge facet fields. This
   * is because string facet merging requires every facet value instead of top facet values from the
   * merge sources, while this class usually prunes to the top N string facet values.
   */
  public static MetaResults mergeCountResult(List<MetaResults> metaResultsList) {
    for (var metaResults : metaResultsList) {
      Check.isEmpty(metaResults.facet(), "metaResults.getFacet().");
      Check.isEmpty(metaResults.termStats(), "metaResults.getTermStats().");
    }
    List<CountResult> countResults =
        metaResultsList.stream().map(MetaResults::count).collect(Collectors.toList());
    return new MetaResults(CountResult.merge(countResults));
  }

  public static MetaResults fromBson(BsonDocument document) throws BsonParseException {
    try (var parser = BsonDocumentParser.fromRoot(document).build()) {
      return fromBson(parser);
    }
  }

  public static MetaResults fromBson(DocumentParser parser) throws BsonParseException {
    return new MetaResults(
        parser.getField(Fields.COUNT_RESULT).unwrap(),
        parser.getField(Fields.FACET_RESULTS).unwrap(),
        parser.getField(Fields.TERM_STATS_RESULTS).unwrap());
  }

  @Override
  public BsonDocument toBson() {
    return BsonDocumentBuilder.builder()
        .field(Fields.COUNT_RESULT, this.count)
        .field(Fields.FACET_RESULTS, this.facet)
        .field(Fields.TERM_STATS_RESULTS, this.termStats)
        .build();
  }
}
