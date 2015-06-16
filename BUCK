include_defs('//bucklets/gerrit_plugin.bucklet')

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
)

# this is required for bucklets/tools/eclipse/project.py to work
java_library(
  name = 'classpath',
  deps = [':uploadvalidator__plugin'],
)

