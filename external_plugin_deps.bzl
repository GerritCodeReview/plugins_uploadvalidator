load("//tools/bzl:maven_jar.bzl", "maven_jar")

def external_plugin_deps():
    maven_jar(
        name = "commons-io",
            artifact = "commons-io:commons-io:2.2",
        sha1 = "83b5b8a7ba1c08f9e8c8ff2373724e33d3c1e22a",
    )
    maven_jar(
        name = "tika-core",
        artifact = "org.apache.tika:tika-core:1.24.1",
        sha1 = "703e65fb300d1425d4ad7b68c21c7795bb7a95c3",
    )
