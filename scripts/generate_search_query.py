#!/usr/bin/env python3
"""
Helper script to get collection UUID and generate a search query file for SearchQueryTool.

Usage:
    python scripts/generate_search_query.py --collection movies --database sample_mflix --output /tmp/query.json

Then run the query tool:
    bazel run //src/main/java/com/xgen/mongot/tools:search_query_tool -- --query-file /tmp/query.json
"""

from pymongo import MongoClient
import json
import base64
import argparse

def get_collection_uuid(db_name, collection_name, mongod_uri="mongodb://localhost:27017/?directConnection=true"):
    """Get the collection UUID from mongod."""
    client = MongoClient(mongod_uri)
    result = client[db_name].command("listCollections", filter={"name": collection_name})
    for collection in result["cursor"]["firstBatch"]:
        if collection["name"] == collection_name:
            uuid = collection["info"]["uuid"]
            client.close()
            return uuid
    client.close()
    raise ValueError(f"Collection {collection_name} not found in database {db_name}")

def uuid_to_base64(uuid_bytes):
    """Convert UUID bytes to base64 string for BSON."""
    return base64.b64encode(uuid_bytes).decode('utf-8')

def generate_query_file(db_name, collection_name, query, output_file, mongod_uri, explain_verbosity=None):
    """Generate a complete search query JSON file."""
    
    # Get collection UUID
    print(f"Fetching collection UUID for {db_name}.{collection_name}...")
    collection_uuid = get_collection_uuid(db_name, collection_name, mongod_uri)
    uuid_base64 = uuid_to_base64(collection_uuid)
    print(f"Collection UUID: {collection_uuid.hex()}")
    print(f"Base64 encoded: {uuid_base64}")
    
    # Build the complete search command
    search_command = {
        "search": collection_name,
        "$db": db_name,
        "collectionUUID": {
            "$binary": {
                "base64": uuid_base64,
                "subType": "04"  # UUID subtype
            }
        },
        "query": query,
        "cursorOptions": {
            "batchSize": 10
        }
    }
    
    # Add explain if requested
    if explain_verbosity:
        search_command["explain"] = {
            "verbosity": explain_verbosity
        }
    
    # Write to file
    with open(output_file, 'w') as f:
        json.dump(search_command, f, indent=2)
    
    print(f"\nQuery file written to: {output_file}")
    print("\nYou can now run:")
    print(f"  make tools.search-query QUERY_FILE={output_file}")
    
    return search_command

def main():
    parser = argparse.ArgumentParser(description="Generate search query files for SearchQueryTool")
    parser.add_argument("--database", "-d", required=True, help="Database name")
    parser.add_argument("--collection", "-c", required=True, help="Collection name")
    parser.add_argument("--search-index", "-s", help="Search index name (not used in this script)")
    parser.add_argument("--stored-source", help="returStoredSource true or false")
    parser.add_argument("--output", "-o", default="/tmp/search_query.json", help="Output file path")
    parser.add_argument("--mongod-uri", default="mongodb://localhost:27017/?directConnection=true", 
                       help="MongoDB connection URI")
    parser.add_argument("--query", type=json.loads, 
                       help="Custom query JSON (default: simple text search)")
    parser.add_argument("--explain", choices=["queryPlanner", "executionStats", "allPlansExecution"],
                       help="Enable explain with specified verbosity level")
    
    args = parser.parse_args()
    
    # Default query if none provided
    if args.query is None:
        args.query = {
            "text": {
                "query": "baseball",
                "path": "plot"
            }
        }
        print("Using default text search query. Use --query to provide custom query.")
    
    if args.search_index:
        args.query["index"] = args.search_index
    
    if args.stored_source:
        args.query["returnStoredSource"] = args.stored_source.lower() == "true"

    try:
        generate_query_file(
            args.database,
            args.collection,
            args.query,
            args.output,
            args.mongod_uri,
            args.explain
        )
    except Exception as e:
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()
        return 1
    
    return 0

if __name__ == "__main__":
    exit(main())
