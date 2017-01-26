load("//tools/bzl:maven_jar.bzl", "maven_jar")

def external_plugin_deps():
  maven_jar(
      name = 'tika_core',
      artifact = 'org.apache.tika:tika-core:1.12',
      sha1 = '5ab95580d22fe1dee79cffbcd98bb509a32da09b',
  )
