Build
=====

This plugin is built with Bazel, and two build modes are supported:

* Standalone
* In Gerrit tree

Standalone build mode is recommended, as this mode doesn't require local Gerrit
tree to exist. Moreover, there are additional manual steps required when building
in Gerrit tree mode (see corresponding section).

## Build standalone

To build the plugin, issue the following command:

```
  bazel build @PLUGIN@
```

The output is created in

```
  bazel-bin/@PLUGIN@.jar
```

To package the plugin sources run:

```
  bazel build lib@PLUGIN@__plugin-src.jar
```

The output is created in:

```
  bazel-bin/lib@PLUGIN@__plugin-src.jar
```

To execute the tests run:

```
  bazel test //...
```

This project can be imported into the Eclipse IDE:

```
  ./tools/eclipse/project.sh
```

## Build in Gerrit tree

Clone (or link) this plugin to the `plugins` directory of Gerrit's source
tree.

```
  git clone https://gerrit.googlesource.com/gerrit
  git clone https://gerrit.googlesource.com/plugins/@PLUGIN@
  cd gerrit/plugins
  ln -s ../../@PLUGIN@ .
```

Put the external dependency Bazel build file into the Gerrit /plugins
directory, replacing the existing empty one.

```
  cd gerrit/plugins
  rm external_plugin_deps.bzl
  ln -s @PLUGIN@/external_plugin_deps.bzl .
```

From Gerrit source tree issue the command:

```
  bazel build plugins/@PLUGIN@
```

The output is created in

```
  bazel-bin/plugins/@PLUGIN@/@PLUGIN@.jar
```

To execute the tests run:

```
  bazel test plugins/@PLUGIN@:@PLUGIN@_tests
```

or filtering using the comma separated tags:

````
  bazel test --test_tag_filters=@PLUGIN@ //...
````

This project can be imported into the Eclipse IDE.
Add the plugin name to the `CUSTOM_PLUGINS` set in
Gerrit core in `tools/bzl/plugins.bzl`, and execute:

```
  ./tools/eclipse/project.py
```
