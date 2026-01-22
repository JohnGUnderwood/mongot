load("@rules_java//java:defs.bzl", "java_binary")
load("//bazel/java:package.bzl", "java_library")

java_library(
    name = "search_query_tool_lib",
    srcs = ["SearchQueryTool.java"],
    testonly = True,
    visibility = ["//visibility:public"],
    deps = [
        "//src/main/java/com/xgen/mongot/config/util",
        "//src/main/java/com/xgen/testing/mongot/server/grpc",
        "//src/proto/searchenvoy:searchenvoy_java_proto",
        "@maven//:org_mongodb_bson",
    ],
)

java_binary(
    name = "search_query_tool",
    main_class = "com.xgen.mongot.tools.SearchQueryTool",
    testonly = True,
    visibility = ["//visibility:public"],
    data = ["logback.xml"],
    jvm_flags = [
        "-Dlogback.configurationFile=src/main/java/com/xgen/mongot/tools/logback.xml",
    ],
    runtime_deps = [":search_query_tool_lib"],
)
