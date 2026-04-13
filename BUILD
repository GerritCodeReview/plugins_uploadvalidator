load(
    "@com_googlesource_gerrit_bazlets//:gerrit_plugin.bzl",
    "gerrit_plugin",
    "gerrit_plugin_dependency_tests",
    "gerrit_plugin_test_util",
    "gerrit_plugin_tests",
)
load("@rules_java//java:defs.bzl", "java_library")

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

gerrit_plugin_test_util(
    name = "testutils",
    srcs = glob(
        ["src/test/java/**/*.java"],
        exclude = TEST_SRCS,
    ),
    deps = TEST_DEPS,
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

gerrit_plugin_dependency_tests(plugin = "uploadvalidator")
