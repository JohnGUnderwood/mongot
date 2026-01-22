package com.xgen.testing.mongot.index.query;

import static com.xgen.mongot.util.Check.checkArg;

import com.xgen.mongot.index.query.CollectorQuery;
import com.xgen.mongot.index.query.Pagination;
import com.xgen.mongot.index.query.Query;
import com.xgen.mongot.index.query.ReturnScope;
import com.xgen.mongot.index.query.SearchQuery;
import com.xgen.mongot.index.query.Tracking;
import com.xgen.mongot.index.query.collectors.Collector;
import com.xgen.mongot.index.query.counts.Count;
import com.xgen.mongot.index.query.highlights.UnresolvedHighlight;
import com.xgen.mongot.index.query.sort.SequenceToken;
import com.xgen.mongot.index.query.sort.SortBetaV1;
import com.xgen.mongot.index.query.sort.SortSpec;
import com.xgen.mongot.util.Check;
import java.util.Optional;

public class CollectorQueryBuilder {

  private Optional<String> index = Optional.empty();
  private Optional<Collector> collector = Optional.empty();
  private Optional<Count> count = Optional.empty();
  private Optional<UnresolvedHighlight> highlight = Optional.empty();
  private Optional<Boolean> returnStoredSource = Optional.empty();
  private Optional<Boolean> scoreDetails = Optional.empty();
  private Optional<Boolean> concurrent = Optional.empty();
  private Optional<SortSpec> sortBetaV1 = Optional.empty();
  private Optional<SortSpec> sort = Optional.empty();
  private Optional<Tracking> tracking = Optional.empty();
  private Optional<ReturnScope> returnScope = Optional.empty();

  private Optional<SequenceToken> searchBefore = Optional.empty();

  private Optional<SequenceToken> searchAfter = Optional.empty();

  public static CollectorQueryBuilder builder() {
    return new CollectorQueryBuilder();
  }

  public static CollectorQueryBuilder builder(CollectorQuery query) {

    var builder =
        new CollectorQueryBuilder()
            .index(query.index())
            .collector(query.collector())
            .count(query.count())
            .returnStoredSource(query.returnStoredSource())
            .scoreDetails(query.scoreDetails())
            .concurrent(query.concurrent());

    query
        .pagination()
        .ifPresent(
            pagination -> {
              var token = pagination.sequenceToken();
              if (Pagination.Type.SEARCH_BEFORE == pagination.type()) {
                builder.searchBefore(token);
              } else {
                builder.searchAfter(token);
              }
            });

    query.highlight().ifPresent(builder::highlight);
    query.rawSortSpec().ifPresent(builder::sort);
    query.tracking().ifPresent(builder::tracking);
    query.returnScope().ifPresent(builder::returnScope);

    return builder;
  }

  public CollectorQueryBuilder index(String index) {
    this.index = Optional.of(index);
    return this;
  }

  public CollectorQueryBuilder count(Count count) {
    this.count = Optional.of(count);
    return this;
  }

  public CollectorQueryBuilder collector(Collector collector) {
    this.collector = Optional.of(collector);
    return this;
  }

  public CollectorQueryBuilder highlight(UnresolvedHighlight unresolvedHighlight) {
    this.highlight = Optional.of(unresolvedHighlight);
    return this;
  }

  public CollectorQueryBuilder returnStoredSource(boolean returnStoredSource) {
    this.returnStoredSource = Optional.of(returnStoredSource);
    return this;
  }

  public CollectorQueryBuilder returnScope(ReturnScope returnScope) {
    this.returnScope = Optional.of(returnScope);
    return this;
  }

  public CollectorQueryBuilder scoreDetails(boolean scoreDetails) {
    this.scoreDetails = Optional.of(scoreDetails);
    return this;
  }

  public CollectorQueryBuilder concurrent(boolean concurrent) {
    this.concurrent = Optional.of(concurrent);
    return this;
  }

  public CollectorQueryBuilder sortBetaV1(SortBetaV1 sortBetaV1) {
    this.sortBetaV1 = Optional.of(sortBetaV1);
    return this;
  }

  public CollectorQueryBuilder sort(SortSpec sort) {
    this.sort = Optional.of(sort);
    return this;
  }

  public CollectorQueryBuilder searchBefore(SequenceToken sequenceToken) {
    this.searchBefore = Optional.of(sequenceToken);
    return this;
  }

  public CollectorQueryBuilder searchAfter(SequenceToken sequenceToken) {
    this.searchAfter = Optional.of(sequenceToken);
    return this;
  }

  public CollectorQueryBuilder tracking(Tracking tracking) {
    this.tracking = Optional.of(tracking);
    return this;
  }

  public CollectorQuery build() {
    Check.isPresent(this.collector, "collector");
    Check.isPresent(this.returnStoredSource, "returnStoredSource");
    checkArg(
        !(this.sortBetaV1.isPresent() && this.sort.isPresent()),
        "Both sort and sortBetaV1 cannot be defined");
    checkArg(
        !(this.searchBefore.isPresent() && this.searchAfter.isPresent()),
        "Both searchBefore and searchAfter cannot be defined");
    Optional<SortSpec> sortSpec = this.sort.isPresent() ? this.sort : this.sortBetaV1;

    var pagination =
        this.searchBefore
            .map(token -> new Pagination(token, Pagination.Type.SEARCH_BEFORE))
            .or(
                () ->
                    this.searchAfter.map(
                        token -> new Pagination(token, Pagination.Type.SEARCH_AFTER)));

    return new CollectorQuery(
        this.collector.get(),
        this.index.orElse(Query.Fields.INDEX.getDefaultValue()),
        this.count.orElse(Count.DEFAULT),
        this.highlight,
        pagination,
        this.returnStoredSource.get(),
        this.scoreDetails.orElse(SearchQuery.Fields.SCORE_DETAILS.getDefaultValue()),
        this.concurrent.orElse(false),
        sortSpec,
        this.tracking,
        this.returnScope,
        Optional.empty());
  }
}
