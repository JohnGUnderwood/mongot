package com.xgen.mongot.index.query;

import com.xgen.mongot.index.query.counts.Count;
import com.xgen.mongot.index.query.highlights.UnresolvedHighlight;
import com.xgen.mongot.index.query.operators.Operator;
import com.xgen.mongot.index.query.sort.SortSpec;
import com.xgen.mongot.util.Check;
import java.util.Optional;
import org.bson.BsonDocument;

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
    Optional<TermStatsRequest> termStats)
    implements SearchQuery {

  public OperatorQuery {
    Check.argNotEmpty(index, "index");
    Check.argNotNull(operator, "operator");
  }

  @Override
  public BsonDocument queryToBson() {
    return this.operator.toBson();
  }
}
