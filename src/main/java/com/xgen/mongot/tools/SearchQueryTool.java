package com.xgen.mongot.tools;

import com.xgen.mongot.config.util.TlsMode;
import com.xgen.testing.mongot.server.grpc.GrpcStreamingClient;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import org.bson.BsonDocument;
import org.bson.RawBsonDocument;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;

/**
 * A command-line tool to send search queries directly to mongot via gRPC.
 *
 * <p>This bypasses mongod's query parser, allowing you to test new operators
 * that mongod doesn't know about yet.
 *
 * <p>Usage:
 * <pre>
 * bazel run //src/main/java/com/xgen/mongot/tools:search_query_tool -- \
 *   --host localhost \
 *   --port 27028 \
 *   --query-file query.json
 * </pre>
 */
public class SearchQueryTool {

  public static void main(String[] args) {
    try {
      var options = parseArgs(args);
      sendSearchQuery(options);
    } catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static void sendSearchQuery(Options options) throws IOException {
    // Read the query from file
    String queryJson = Files.readString(Paths.get(options.queryFile));
    BsonDocument queryDoc = BsonDocument.parse(queryJson);

    System.out.println("Connecting to mongot at " + options.host + ":" + options.port);
    System.out.println("Sending query:");
    System.out.println(queryDoc.toJson(JsonWriterSettings.builder()
        .outputMode(JsonMode.RELAXED)
        .indent(true)
        .build()));
    System.out.println();

    // Create gRPC client
    try (GrpcStreamingClient client =
        GrpcStreamingClient.create(
            new InetSocketAddress(options.host, options.port),
            Optional.empty(), // No search envoy metadata needed for testing
            TlsMode.DISABLED,
            Optional.empty())) {

      // Start a search command stream
      try (var stream = client.startBsonSearchCommandStream()) {
        // Send the query as a RawBsonDocument
        RawBsonDocument request = new RawBsonDocument(queryDoc, new BsonDocumentCodec());
        RawBsonDocument response = stream.handleMessage(request);

        // Print the response
        System.out.println("Response from mongot:");
        System.out.println(response.toJson(JsonWriterSettings.builder()
            .outputMode(JsonMode.RELAXED)
            .indent(true)
            .build()));

        // Check if successful
        if (response.getInt32("ok").getValue() == 1) {
          System.out.println("\n✓ Success!");
        } else {
          System.out.println("\n✗ Query failed - see error above");
          System.exit(1);
        }
      }
    }
  }

  private static Options parseArgs(String[] args) {
    Options options = new Options();

    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--host":
          options.host = args[++i];
          break;
        case "--port":
          options.port = Integer.parseInt(args[++i]);
          break;
        case "--query-file":
          options.queryFile = args[++i];
          break;
        case "--help":
          printHelp();
          System.exit(0);
          break;
        default:
          throw new IllegalArgumentException("Unknown argument: " + args[i]);
      }
    }

    if (options.queryFile == null) {
      throw new IllegalArgumentException("--query-file is required");
    }

    return options;
  }

  private static void printHelp() {
    System.out.println("SearchQueryTool - Send queries directly to mongot via gRPC");
    System.out.println();
    System.out.println("Usage:");
    System.out.println("  bazel run //src/main/java/com/xgen/mongot/tools:search_query_tool -- \\");
    System.out.println("    [--host HOST] \\");
    System.out.println("    [--port PORT] \\");
    System.out.println("    --query-file QUERY_FILE");
    System.out.println();
    System.out.println("Options:");
    System.out.println("  --host HOST         Mongot host (default: localhost)");
    System.out.println("  --port PORT         Mongot gRPC port (default: 27028)");
    System.out.println("  --query-file FILE   JSON file containing the search command");
    System.out.println("  --help              Show this help message");
    System.out.println();
    System.out.println("Query file format:");
    System.out.println("  The query file should contain a complete search command in JSON format:");
    System.out.println("  {");
    System.out.println("    \"search\": \"collection_name\",");
    System.out.println("    \"$db\": \"database_name\",");
    System.out.println("    \"collectionUUID\": {\"$binary\": {\"base64\": \"...\", \"subType\": \"04\"}},");
    System.out.println("    \"query\": {");
    System.out.println("      \"text\": {");
    System.out.println("        \"query\": \"search term\",");
    System.out.println("        \"path\": \"field_name\"");
    System.out.println("      }");
    System.out.println("    }");
    System.out.println("  }");
    System.out.println();
    System.out.println("Examples:");
    System.out.println("  # Create a query file");
    System.out.println("  cat > /tmp/query.json << 'EOF'");
    System.out.println("  {");
    System.out.println("    \"search\": \"movies\",");
    System.out.println("    \"$db\": \"sample_mflix\",");
    System.out.println("    \"collectionUUID\": {\"$binary\": {\"base64\": \"K30RUiO4TRSMXczkKPzs7A==\", \"subType\": \"04\"}},");
    System.out.println("    \"query\": {");
    System.out.println("      \"text\": {");
    System.out.println("        \"query\": \"baseball\",");
    System.out.println("        \"path\": \"plot\"");
    System.out.println("      }");
    System.out.println("    }");
    System.out.println("  }");
    System.out.println("  EOF");
    System.out.println();
    System.out.println("  # Run the query");
    System.out.println("  bazel run //src/main/java/com/xgen/mongot/tools:search_query_tool -- \\");
    System.out.println("    --query-file /tmp/query.json");
  }

  private static class Options {
    String host = "localhost";
    int port = 27028;
    String queryFile;
  }
}
