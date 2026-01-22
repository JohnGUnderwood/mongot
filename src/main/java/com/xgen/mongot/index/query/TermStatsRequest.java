package com.xgen.mongot.index.query;

import com.xgen.mongot.index.query.collectors.TermStatsDefinition;
import com.xgen.mongot.util.bson.parser.BsonParseException;
import com.xgen.mongot.util.bson.parser.DocumentEncodable;
import com.xgen.mongot.util.bson.parser.DocumentParser;
import org.bson.BsonDocument;

/**
 * Request for term statistics on specified fields.
 *
 * @param definition Defines which fields to collect term statistics from
 */
public record TermStatsRequest(TermStatsDefinition definition) implements DocumentEncodable {

  /** Deserializes termStats request from BSON. */
  public static TermStatsRequest fromBson(DocumentParser parser) throws BsonParseException {
    // Parse the TermStatsDefinition directly from the current parser context
    return new TermStatsRequest(TermStatsDefinition.fromBson(parser));
  }

  @Override
  public BsonDocument toBson() {
    return this.definition.toBson();
  }
}
