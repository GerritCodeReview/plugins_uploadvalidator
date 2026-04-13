load("@rules_java//java:defs.bzl", "java_library")
load(
    "@com_googlesource_gerrit_bazlets//:gerrit_plugin.bzl",
    "gerrit_plugin",
    "gerrit_plugin_tests",
)
load(
    "@com_googlesource_gerrit_bazlets//tools:in_gerrit_tree.bzl",
    "in_gerrit_tree_enabled",
)
load(
    "@com_googlesource_gerrit_bazlets//tools:runtime_jars_allowlist.bzl",
    "runtime_jars_allowlist_test",
)
load(
    "@com_googlesource_gerrit_bazlets//tools:runtime_jars_overlap.bzl",
    "runtime_jars_overlap_test",
)

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
        "@uploadvalidator_plugin_deps//:org_overviewproject_mime_types",
    ],
)

TEST_DEPS = [
    "@uploadvalidator_plugin_deps//:org_overviewproject_mime_types",
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
    deps = TEST_DEPS + [
        "//plugins:plugin-lib-neverlink",
        "//lib:junit",
        "//lib:jgit-junit",
        "//lib/mockito:mockito",
    ],
)

gerrit_plugin_tests(
    name = "uploadvalidator_tests",
    testonly = 1,
    srcs = glob(
        ["src/test/java/**/*Test.java"],
    ),
    tags = ["uploadvalidator"],
    deps = TEST_DEPS + [
        ":testutils",
    ],
)

gerrit_plugin_tests(
    name = "uploadvalidator_integration_tests",
    testonly = 1,
    srcs = glob(
        ["src/test/java/**/*IT.java"],
    ),
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

runtime_jars_allowlist_test(
    name = "check_uploadvalidator_third_party_runtime_jars",
    allowlist = ":uploadvalidator_third_party_runtime_jars.allowlist.txt",
    hint = "plugins/uploadvalidator:check_uploadvalidator_third_party_runtime_jars_manifest",
    target = ":uploadvalidator__plugin",
)

runtime_jars_overlap_test(
    name = "uploadvalidator_no_overlap_with_gerrit",
    against = "//:headless.war.jars.txt",
    hint = "Exclude overlaps via maven.install(excluded_artifacts=[...]) and re-run this test.",
    target = ":uploadvalidator__plugin",
    target_compatible_with = in_gerrit_tree_enabled(),
)
