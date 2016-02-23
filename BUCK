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

java_test(
  name = 'uploadvalidator_tests',
  srcs = glob(['src/test/java/**/*.java']),
  labels = ['uploadvalidator'],
  source_under_test = [':uploadvalidator__plugin'],
  deps = [
    ':uploadvalidator__plugin',
    ':commons-io',
    '//gerrit-server:server',
    '//lib/jgit:jgit',
    '//lib:junit',
    '//lib/guice:guice',
    '//lib:mime-util',
  ],
)
