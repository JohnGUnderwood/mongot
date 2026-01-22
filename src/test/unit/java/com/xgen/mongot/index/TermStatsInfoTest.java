package com.xgen.mongot.index;

import static com.google.common.truth.Truth.assertThat;

import com.xgen.testing.mongot.index.TermStatsInfoBuilder;
import java.util.List;
import org.junit.Test;

/** Tests for TermStatsInfo serialization, deserialization, and merging. */
public class TermStatsInfoTest {

  @Test
  public void testToBson() {
    TermStatsInfo info =
        new TermStatsInfoBuilder()
            .docFreq(100)
            .sumDocFreq(500)
            .sumTotalTermFreq(1000)
            .docCount(50)
            .avgDocFreq(5.0)
            .build();

    var bson = info.toBson();
    assertThat(bson.getInt64("docFreq").getValue()).isEqualTo(100);
    assertThat(bson.getInt64("sumDocFreq").getValue()).isEqualTo(500);
    assertThat(bson.getInt64("sumTotalTermFreq").getValue()).isEqualTo(1000);
    assertThat(bson.getInt32("docCount").getValue()).isEqualTo(50);
    assertThat(bson.getDouble("avgDocFreq").getValue()).isEqualTo(5.0);
  }

  @Test
  public void testFromBson() throws Exception {
    TermStatsInfo original =
        new TermStatsInfoBuilder()
            .docFreq(100)
            .sumDocFreq(500)
            .sumTotalTermFreq(1000)
            .docCount(50)
            .avgDocFreq(5.0)
            .build();

    var bson = original.toBson();
    TermStatsInfo deserialized =
        TermStatsInfo.fromBson(
            com.xgen.mongot.util.bson.parser.BsonDocumentParser.fromRoot(bson).build());

    assertThat(deserialized).isEqualTo(original);
  }

  @Test
  public void testMergeEmpty() {
    TermStatsInfo merged = TermStatsInfo.merge(List.of());
    assertThat(merged.docFreq()).isEqualTo(0);
    assertThat(merged.sumDocFreq()).isEqualTo(0);
    assertThat(merged.sumTotalTermFreq()).isEqualTo(0);
    assertThat(merged.docCount()).isEqualTo(0);
    assertThat(merged.avgDocFreq()).isEqualTo(0.0);
  }

  @Test
  public void testMergeSingle() {
    TermStatsInfo info =
        new TermStatsInfoBuilder()
            .docFreq(100)
            .sumDocFreq(500)
            .sumTotalTermFreq(1000)
            .docCount(50)
            .avgDocFreq(5.0)
            .build();

    TermStatsInfo merged = TermStatsInfo.merge(List.of(info));
    assertThat(merged.docFreq()).isEqualTo(100);
    assertThat(merged.sumDocFreq()).isEqualTo(500);
    assertThat(merged.sumTotalTermFreq()).isEqualTo(1000);
    assertThat(merged.docCount()).isEqualTo(50);
    // avgDocFreq is recalculated: 100 / 500 = 0.2
    assertThat(merged.avgDocFreq()).isWithin(0.001).of(0.2);
  }

  @Test
  public void testMergeMultiple() {
    TermStatsInfo info1 =
        new TermStatsInfoBuilder()
            .docFreq(100)
            .sumDocFreq(500)
            .sumTotalTermFreq(1000)
            .docCount(50)
            .avgDocFreq(5.0)
            .build();

    TermStatsInfo info2 =
        new TermStatsInfoBuilder()
            .docFreq(200)
            .sumDocFreq(600)
            .sumTotalTermFreq(1500)
            .docCount(75)
            .avgDocFreq(3.0)
            .build();

    TermStatsInfo merged = TermStatsInfo.merge(List.of(info1, info2));
    assertThat(merged.docFreq()).isEqualTo(300);
    assertThat(merged.sumDocFreq()).isEqualTo(1100);
    assertThat(merged.sumTotalTermFreq()).isEqualTo(2500);
    assertThat(merged.docCount()).isEqualTo(125);
    // avgDocFreq is recalculated: 300 / 1100 ≈ 0.273
    assertThat(merged.avgDocFreq()).isWithin(0.001).of(0.273);
  }
}
