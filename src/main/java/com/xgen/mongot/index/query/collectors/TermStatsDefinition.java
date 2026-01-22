package com.xgen.mongot.index.query.collectors;

import com.xgen.mongot.util.bson.parser.BsonDocumentBuilder;
import com.xgen.mongot.util.bson.parser.BsonParseException;
import com.xgen.mongot.util.bson.parser.DocumentEncodable;
import com.xgen.mongot.util.bson.parser.DocumentParser;
import com.xgen.mongot.util.bson.parser.Field;
import java.util.List;
import org.bson.BsonDocument;

/**
 * Defines which fields to collect term statistics for.
 *
 * @param fields List of field names to collect term statistics from
 */
public record TermStatsDefinition(List<String> fields) implements DocumentEncodable {

  private static class Fields {
    private static final Field.Required<List<String>> FIELDS =
        Field.builder("fields").stringField().asList().mustNotBeEmpty().required();
  }

  /** Deserializes definition from BSON. */
  public static TermStatsDefinition fromBson(DocumentParser parser) throws BsonParseException {
    return new TermStatsDefinition(parser.getField(Fields.FIELDS).unwrap());
  }

  @Override
  public BsonDocument toBson() {
    return BsonDocumentBuilder.builder().field(Fields.FIELDS, this.fields).build();
  }
}
