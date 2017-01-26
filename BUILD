load("//tools/bzl:plugin.bzl", "gerrit_plugin", "PLUGIN_DEPS", "PLUGIN_TEST_DEPS")

gerrit_plugin(
    name = "uploadvalidator",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: uploadvalidator",
        "Gerrit-ApiVersion: 2.14-SNAPSHOT",
        "Gerrit-Module: com.googlesource.gerrit.plugins.uploadvalidator.Module",
    ],
    resources = glob(["src/main/resources/**/*"]),
    deps = [
        "@commons_io_commons_io//jar",
        "@org_apache_tika_tika_core//jar",
    ],
)

java_test(
    name = "uploadvalidator_tests",
    testonly = 1,
    srcs = glob(["src/test/java/**/*.java"]),
    deps = PLUGIN_DEPS + PLUGIN_TEST_DEPS + [
        "@commons_io_commons_io//jar",
        ":uploadvalidator__plugin",
    ],
)
