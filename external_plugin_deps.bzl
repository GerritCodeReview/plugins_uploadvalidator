load("//tools/bzl:maven_jar.bzl", "maven_jar")

def external_plugin_deps():
    maven_jar(
        name = "commons-io",
        artifact = "commons-io:commons-io:2.2",
        sha1 = "83b5b8a7ba1c08f9e8c8ff2373724e33d3c1e22a",
    )
    maven_jar(
        name = "mime-types",
        artifact = "org.overviewproject:mime-types:2.0.0",
        sha1 = "af05afd015df62cc6b949792e59dff97e403fcea",
    )
    maven_jar(
        name = "juniversalchardet",
        artifact = "com.github.albfernandez:juniversalchardet:2.5.0",
        sha1 = "423123a1ddfe458d07948bc09cfa0170037a9e3d",
    )
