load("@rules_java//java:defs.bzl", "java_library")
load("//tools/bzl:junit.bzl", "junit_tests")
load("//tools/bzl:plugin.bzl", "PLUGIN_DEPS", "PLUGIN_TEST_DEPS", "gerrit_plugin")

gerrit_plugin(
    name = "uploadvalidator",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: uploadvalidator",
        "Gerrit-ApiVersion: 2.16",
        "Gerrit-Module: com.googlesource.gerrit.plugins.uploadvalidator.Module",
        "Gerrit-HttpModule: com.googlesource.gerrit.plugins.uploadvalidator.HttpModule",
    ],
    resources = glob(["src/main/resources/**/*"]),
    deps = [
        "@tika-core//jar",
    ],
)

TEST_SRCS = "src/test/java/**/*Test.java"

TEST_DEPS = PLUGIN_DEPS + PLUGIN_TEST_DEPS + [
    "@commons-io//jar",
    "@tika-core//jar",
    ":uploadvalidator__plugin",
]

java_library(
    name = "testutils",
    testonly = 1,
    srcs = glob(
        ["src/test/java/**/*.java"],
        exclude = [TEST_SRCS],
    ),
    deps = TEST_DEPS,
)

junit_tests(
    name = "uploadvalidator_tests",
    testonly = 1,
    srcs = glob([TEST_SRCS]),
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
