# Search Query Tool

A command-line tool to send search queries directly to mongot via gRPC, bypassing mongod's query parser.

## Why Use This?

When developing new search operators in mongot, you need to test them before implementing the corresponding parser in mongod. This tool lets you:

- Test **NEW operators** that mongod doesn't know about yet
- Test **new parameters** on existing operators
- Iterate on mongot features without touching mongod
- Validate mongot behavior independently

## Quick Start

### 1. Generate a Query File

Use the helper script to create a properly formatted query:

```bash
python scripts/generate_search_query.py \
  --database sample_mflix \
  --collection movies \
  --output /tmp/query.json
```

This creates a query file with the correct format including the collection UUID.

### 2. Run the Query Tool

```bash
make tools.search-query QUERY_FILE=/tmp/query.json
```

Or directly with bazelisk:

```bash
./scripts/tools/bazelisk/run.sh run //src/main/java/com/xgen/mongot/tools:search_query_tool -- \
  --query-file /tmp/query.json
```

## Testing Custom Operators

### Example: Test a New "fuzzyMatch" Operator

1. **Create a custom query file** (`/tmp/custom_query.json`):

```json
{
  "search": "movies",
  "$db": "sample_mflix",
  "collectionUUID": {"$binary": {"base64": "K30RUisYTRSMXczkKPzs7A==", "subType": "04"}},
  "query": {
    "myNewFuzzyOperator": {
      "query": "basebal",
      "path": "plot",
      "maxEdits": 2,
      "customParameter": "testValue"
    }
  }
}
```

2. **Run it**:

```bash
make tools.search-query QUERY_FILE=/tmp/custom_query.json
```

If mongot doesn't implement your operator yet, you'll see an error - that's your signal to implement it!

### Example: Test New Parameters on Existing Operators

```json
{
  "search": "movies",
  "$db": "sample_mflix",
  "collectionUUID": {"$binary": {"base64": "K30RUisYTRSMXczkKPzs7A==", "subType": "04"}},
  "query": {
    "text": {
      "query": "baseball",
      "path": "plot",
      "experimentalBoost": 2.5,
      "futureParameter": true
    }
  }
}
```

## Query File Format

The query file must be a complete internal `search` command in JSON format:

```json
{
  "search": "collection_name",
  "$db": "database_name",
  "collectionUUID": {
    "$binary": {
      "base64": "base64_encoded_uuid",
      "subType": "04"
    }
  },
  "query": {
    // Your search query here - this is where you test new operators!
  },
  "explain": false,
  "cursorOptions": {
    "batchSize": 10
  }
}
```

## Helper Script Options

```bash
python scripts/generate_search_query.py --help
```

### Generate with Custom Query

```bash
python scripts/generate_search_query.py \
  --database sample_mflix \
  --collection movies \
  --query '{"compound": {"must": [{"text": {"query": "baseball", "path": "plot"}}]}}' \
  --output /tmp/compound_query.json
```

## Development Workflow

1. **Implement your new operator in mongot** (e.g., in `src/main/java/com/xgen/mongot/index/query/operators/`)
2. **Rebuild mongot**:
   ```bash
   make docker.up MODE=local
   ```
3. **Generate a test query**:
   ```bash
   python scripts/generate_search_query.py -d sample_mflix -c movies -o /tmp/test.json
   ```
4. **Edit the query file** to use your new operator
5. **Run the test**:
   ```bash
   bazel run //src/main/java/com/xgen/mongot/tools:search_query_tool -- --query-file /tmp/test.json
   make tools.search-query QUERY_FILE=
6. **Iterate** until it works!
7. **Then** implement the operator in mongod's `$search` parser

## Troubleshooting

### Connection Refused

Make sure mongot is running:
```bash
docker compose --project-directory community-quick-start ps
```

### Invalid Collection UUID

The UUID must be in base64-encoded format. Use the `generate_search_query.py` script to get the correct format.

### Query Validation Errors

If you see validation errors, mongot is processing your query! The error tells you what's wrong with your operator implementation.

## See Also

- [GrpcStreamingClient.java](../src/main/java/com/xgen/testing/mongot/server/grpc/GrpcStreamingClient.java) - The underlying gRPC client
- [SearchCommand.java](../src/main/java/com/xgen/mongot/server/command/search/SearchCommand.java) - How mongot processes search commands
