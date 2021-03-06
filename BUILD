load("@rules_java//java:defs.bzl", "java_library")
load("//tools/bzl:junit.bzl", "junit_tests")
load("//tools/bzl:plugin.bzl", "PLUGIN_DEPS", "PLUGIN_TEST_DEPS", "gerrit_plugin")

gerrit_plugin(
    name = "uploadvalidator",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: uploadvalidator",
        "Gerrit-ApiVersion: 3.0-SNAPSHOT",
        "Gerrit-Module: com.googlesource.gerrit.plugins.uploadvalidator.Module",
    ],
    resources = glob(["src/main/resources/**/*"]),
    deps = [
        "@mime-types//jar",
    ],
)

TEST_DEPS = PLUGIN_DEPS + PLUGIN_TEST_DEPS + [
    "@commons-io//jar",
    "@mime-types//jar",
    ":uploadvalidator__plugin",
]

TEST_SRCS = [
    "src/test/java/**/*Test.java",
    "src/test/java/**/*IT.java",
]

java_library(
    name = "testutils",
    testonly = 1,
    srcs = glob(
        ["src/test/java/**/*.java"],
        exclude = TEST_SRCS,
    ),
    deps = TEST_DEPS,
)

junit_tests(
    name = "uploadvalidator_tests",
    testonly = 1,
    srcs = glob(
        ["src/test/java/**/*Test.java"]),
    tags = ["uploadvalidator"],
    deps = TEST_DEPS + [
        ":testutils",
    ],
)

junit_tests(
    name = "uploadvalidator_integration_tests",
    testonly = 1,
    srcs = glob(
        ["src/test/java/**/*IT.java"]),
    tags = ["uploadvalidator"],
    deps = TEST_DEPS + [
        ":testutils",
    ],
)

java_library(
    name = "uploadvalidator_classpath_deps",
    testonly = 1,
    visibility = ["//visibility:public"],
    exports = TEST_DEPS + [
        ":testutils",
    ],
)
