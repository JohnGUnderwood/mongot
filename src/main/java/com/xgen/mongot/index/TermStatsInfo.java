package com.xgen.mongot.index;

import com.google.errorprone.annotations.Var;
import com.xgen.mongot.util.bson.parser.BsonDocumentBuilder;
import com.xgen.mongot.util.bson.parser.BsonParseException;
import com.xgen.mongot.util.bson.parser.DocumentEncodable;
import com.xgen.mongot.util.bson.parser.DocumentParser;
import com.xgen.mongot.util.bson.parser.Field;
import org.bson.BsonDocument;

/**
 * Contains term statistics for a field from Lucene's term dictionary.
 *
 * @param docFreq The number of documents that contain at least one occurrence of any term in this
 *     field
 * @param sumDocFreq The sum of document frequencies for all terms in this field (may be -1 if not
 *     available)
 * @param sumTotalTermFreq The sum of total term frequencies for all terms in this field (total
 *     occurrences across all documents)
 * @param docCount The number of documents that have at least one term for this field
 * @param avgDocFreq The average document frequency per term (docFreq / number of unique terms)
 */
public record TermStatsInfo(
    long docFreq, long sumDocFreq, long sumTotalTermFreq, int docCount, double avgDocFreq)
    implements DocumentEncodable {

  private static class Fields {
    static final Field.Required<Long> DOC_FREQ =
        Field.builder("docFreq").longField().required();

    static final Field.Required<Long> SUM_DOC_FREQ =
        Field.builder("sumDocFreq").longField().required();

    static final Field.Required<Long> SUM_TOTAL_TERM_FREQ =
        Field.builder("sumTotalTermFreq").longField().required();

    static final Field.Required<Integer> DOC_COUNT =
        Field.builder("docCount").intField().required();

    static final Field.Required<Double> AVG_DOC_FREQ =
        Field.builder("avgDocFreq").doubleField().required();
  }

  public static TermStatsInfo fromBson(DocumentParser parser) throws BsonParseException {
    return new TermStatsInfo(
        parser.getField(Fields.DOC_FREQ).unwrap(),
        parser.getField(Fields.SUM_DOC_FREQ).unwrap(),
        parser.getField(Fields.SUM_TOTAL_TERM_FREQ).unwrap(),
        parser.getField(Fields.DOC_COUNT).unwrap(),
        parser.getField(Fields.AVG_DOC_FREQ).unwrap());
  }

  @Override
  public BsonDocument toBson() {
    return BsonDocumentBuilder.builder()
        .field(Fields.DOC_FREQ, this.docFreq)
        .field(Fields.SUM_DOC_FREQ, this.sumDocFreq)
        .field(Fields.SUM_TOTAL_TERM_FREQ, this.sumTotalTermFreq)
        .field(Fields.DOC_COUNT, this.docCount)
        .field(Fields.AVG_DOC_FREQ, this.avgDocFreq)
        .build();
  }

  /**
   * Merges multiple TermStatsInfo objects by summing their statistics.
   *
   * @param infos List of TermStatsInfo objects to merge
   * @return Merged TermStatsInfo
   */
  public static TermStatsInfo merge(java.util.List<TermStatsInfo> infos) {
    if (infos.isEmpty()) {
      return new TermStatsInfo(0, 0, 0, 0, 0.0);
    }

    @Var long totalDocFreq = 0;
    @Var long totalSumDocFreq = 0;
    @Var long totalSumTotalTermFreq = 0;
    @Var int totalDocCount = 0;

    for (TermStatsInfo info : infos) {
      totalDocFreq += info.docFreq;
      totalSumDocFreq += info.sumDocFreq;
      totalSumTotalTermFreq += info.sumTotalTermFreq;
      totalDocCount += info.docCount;
    }

    // Calculate average doc frequency across all terms
    double avgDocFreq = totalSumDocFreq > 0 ? (double) totalDocFreq / totalSumDocFreq : 0.0;

    return new TermStatsInfo(
        totalDocFreq, totalSumDocFreq, totalSumTotalTermFreq, totalDocCount, avgDocFreq);
  }
}
