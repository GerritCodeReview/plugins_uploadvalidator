workspace(name = "uploadvalidator")

load("//:bazlets.bzl", "load_bazlets")

load_bazlets(
    commit = "fbe2b2fd07c95d752dced6b8624c9d5a08e8c6c6",
    #local_path = "/home/<user>/projects/bazlets",
)

load(
    "@com_googlesource_gerrit_bazlets//:gerrit_api.bzl",
    "gerrit_api",
)

gerrit_api(version = "3.3.0-SNAPSHOT")

load("//:external_plugin_deps.bzl", "external_plugin_deps")

external_plugin_deps()
