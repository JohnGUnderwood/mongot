package com.xgen.testing.mongot.index.query;

import static com.xgen.mongot.util.Check.checkArg;

import com.xgen.mongot.index.query.OperatorQuery;
import com.xgen.mongot.index.query.Pagination;
import com.xgen.mongot.index.query.Query;
import com.xgen.mongot.index.query.ReturnScope;
import com.xgen.mongot.index.query.SearchQuery;
import com.xgen.mongot.index.query.Tracking;
import com.xgen.mongot.index.query.counts.Count;
import com.xgen.mongot.index.query.highlights.UnresolvedHighlight;
import com.xgen.mongot.index.query.operators.Operator;
import com.xgen.mongot.index.query.sort.SequenceToken;
import com.xgen.mongot.index.query.sort.SortBetaV1;
import com.xgen.mongot.index.query.sort.SortSpec;
import com.xgen.mongot.util.Check;
import java.util.Optional;

public class OperatorQueryBuilder {

  private Optional<String> index = Optional.empty();
  private Optional<Operator> operator = Optional.empty();
  private Optional<Count> count = Optional.empty();
  private Optional<UnresolvedHighlight> highlight = Optional.empty();
  private Optional<Boolean> returnStoredSource = Optional.empty();
  private Optional<Boolean> scoreDetails = Optional.empty();
  private Optional<SortSpec> sortBetaV1 = Optional.empty();
  private Optional<SortSpec> sort = Optional.empty();
  private Optional<Tracking> tracking = Optional.empty();
  private Optional<Boolean> concurrent = Optional.empty();
  private Optional<ReturnScope> returnScope = Optional.empty();

  private Optional<SequenceToken> searchBefore = Optional.empty();

  private Optional<SequenceToken> searchAfter = Optional.empty();

  public static OperatorQueryBuilder builder() {
    return new OperatorQueryBuilder();
  }

  public static OperatorQueryBuilder builder(OperatorQuery query) {

    var builder =
        new OperatorQueryBuilder()
            .index(query.index())
            .operator(query.operator())
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

  public OperatorQueryBuilder index(String index) {
    this.index = Optional.of(index);
    return this;
  }

  public OperatorQueryBuilder count(Count count) {
    this.count = Optional.of(count);
    return this;
  }

  public OperatorQueryBuilder operator(Operator operator) {
    this.operator = Optional.of(operator);
    return this;
  }

  public OperatorQueryBuilder highlight(UnresolvedHighlight unresolvedHighlight) {
    this.highlight = Optional.of(unresolvedHighlight);
    return this;
  }

  public OperatorQueryBuilder returnStoredSource(boolean returnStoredSource) {
    this.returnStoredSource = Optional.of(returnStoredSource);
    return this;
  }

  public OperatorQueryBuilder returnScope(ReturnScope returnScope) {
    this.returnScope = Optional.of(returnScope);
    return this;
  }

  public OperatorQueryBuilder scoreDetails(boolean scoreDetails) {
    this.scoreDetails = Optional.of(scoreDetails);
    return this;
  }

  public OperatorQueryBuilder sortBetaV1(SortBetaV1 sortBetaV1) {
    this.sortBetaV1 = Optional.of(sortBetaV1);
    return this;
  }

  public OperatorQueryBuilder sort(SortSpec sort) {
    this.sort = Optional.of(sort);
    return this;
  }

  public OperatorQueryBuilder concurrent(boolean concurrent) {
    this.concurrent = Optional.of(concurrent);
    return this;
  }

  public OperatorQueryBuilder tracking(Tracking tracking) {
    this.tracking = Optional.of(tracking);
    return this;
  }

  public OperatorQueryBuilder searchBefore(SequenceToken sequenceToken) {
    this.searchBefore = Optional.of(sequenceToken);
    return this;
  }

  public OperatorQueryBuilder searchAfter(SequenceToken sequenceToken) {
    this.searchAfter = Optional.of(sequenceToken);
    return this;
  }

  public OperatorQuery build() {
    Check.isPresent(this.operator, "operator");
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

    return new OperatorQuery(
        this.operator.get(),
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
