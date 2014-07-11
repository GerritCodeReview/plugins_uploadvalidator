Configuration
=============

The configuration of the @PLUGIN@ plugin is done on project level in
the `project.config` file of the project.

Project owners can do the configuration in the Gerrit web UI from
project info screen.

```
  [plugin "@PLUGIN@"]
    blockedFileExtension = jar
    blockedFileExtension = zip
    blockedFileExtension = war
    blockedFileExtension = exe
    requiredFooter = Bug
```

plugin.@PLUGIN@.blockedFileExtension
:	File extension to be blocked.

	Blocked file extensions are *not* inherited by child projects.

plugin.@PLUGIN@.requiredFooter
:	Footer that is required.

	Required footers are *not* inherited by child projects.
