include_defs('//bucklets/gerrit_plugin.bucklet')
include_defs('//lib/maven.defs')

gerrit_plugin(
  name = 'uploadvalidator',
  srcs = glob(['src/main/java/**/*.java']),
  resources = glob(['src/main/resources/**/*']),
  manifest_entries = [
    'Gerrit-PluginName: uploadvalidator',
    'Gerrit-ApiType: plugin',
    'Gerrit-ApiVersion: 2.12-SNAPSHOT',
    'Gerrit-Module: com.googlesource.gerrit.plugins.uploadvalidator.Module',
  ],
  deps = [
    ':commons-io',
    ':tika-core',
  ],
)

# this is required for bucklets/tools/eclipse/project.py to work
java_library(
  name = 'classpath',
  deps = [':uploadvalidator__plugin'],
)

maven_jar(
  name = 'commons-io',
  id = 'commons-io:commons-io:1.4',
  sha1 = 'a8762d07e76cfde2395257a5da47ba7c1dbd3dce',
  license = 'Apache2.0',
)

maven_jar(
  name = 'tika-core',
  id = 'org.apache.tika:tika-core:1.12',
  sha1 = '5ab95580d22fe1dee79cffbcd98bb509a32da09b',
  license = 'Apache2.0',
)

java_test(
  name = 'uploadvalidator_tests',
  srcs = glob(['src/test/java/**/*.java']),
  labels = ['uploadvalidator'],
  source_under_test = [':uploadvalidator__plugin'],
  deps = GERRIT_PLUGIN_API + GERRIT_TESTS + [
    ':commons-io',
    ':uploadvalidator__plugin',
  ],
)
