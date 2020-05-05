load("//tools/bzl:maven_jar.bzl", "maven_jar")

def external_plugin_deps():
    maven_jar(
        name = "commons-io",
        artifact = "commons-io:commons-io:1.4",
        sha1 = "a8762d07e76cfde2395257a5da47ba7c1dbd3dce",
    )
    maven_jar(
        name = "tika-core",
        artifact = "org.apache.tika:tika-core:1.12",
        sha1 = "5ab95580d22fe1dee79cffbcd98bb509a32da09b",
    )
