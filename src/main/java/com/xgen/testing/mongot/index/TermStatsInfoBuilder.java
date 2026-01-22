package com.xgen.testing.mongot.index;

import com.xgen.mongot.index.TermStatsInfo;

public class TermStatsInfoBuilder {

  private long docFreq = 0;
  private long sumDocFreq = 0;
  private long sumTotalTermFreq = 0;
  private int docCount = 0;
  private double avgDocFreq = 0.0;

  public TermStatsInfoBuilder docFreq(long docFreq) {
    this.docFreq = docFreq;
    return this;
  }

  public TermStatsInfoBuilder sumDocFreq(long sumDocFreq) {
    this.sumDocFreq = sumDocFreq;
    return this;
  }

  public TermStatsInfoBuilder sumTotalTermFreq(long sumTotalTermFreq) {
    this.sumTotalTermFreq = sumTotalTermFreq;
    return this;
  }

  public TermStatsInfoBuilder docCount(int docCount) {
    this.docCount = docCount;
    return this;
  }

  public TermStatsInfoBuilder avgDocFreq(double avgDocFreq) {
    this.avgDocFreq = avgDocFreq;
    return this;
  }

  public TermStatsInfo build() {
    return new TermStatsInfo(docFreq, sumDocFreq, sumTotalTermFreq, docCount, avgDocFreq);
  }
}
