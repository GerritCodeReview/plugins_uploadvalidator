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
    blockedMimeType = application/x-object
    blockedMimeType = application/msword
    blockedMimeType = application/pdf
    invalidFilenamePattern = [@:]
    invalidFilenamePattern = [#%*]
    rejectWindowsLineEndings = false
    requiredFooter = Bug
    maxPathLength = 200
    rejectSymlink = false
    allowDuplicateFilenames = false;
```

plugin.@PLUGIN@.blockedFileExtension
:	File extension to be blocked.

	Blocked file extensions are *not* inherited by child projects.

plugin.@PLUGIN@.requiredFooter
:	Footer that is required.

	Required footers are *not* inherited by child projects.

plugin.@PLUGIN@.maxPathLength
:	Maximum allowed path length. '0' means no limit.

	Defaults to '0'.

	The maximum allowed path length is *not* inherited by child
	projects.

plugin.@PLUGIN@.invalidFilenamePattern
:	Patterns for invalid filenames.

	This check is using `java.util.regex.Pattern` which is described
	[here][1].

	Defined patterns are *not* inherited by child projects.

[1]: https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html

plugin.@PLUGIN@.rejectWindowsLineEndings
:	Reject Windows line endings.

	This check looks for carriage return (CR) characters in pushed
	files. If the check finds a carriage return (CR) character
	the push will be rejected.

	The default value is false. This means the check will not be executed.

	This option is *not* inherited by child projects.

plugin.@PLUGIN@.rejectSymlink
:	Reject symbolic links.

	This check looks for symbolic links in pushed files. If the check
	finds a symbolic link the push will be rejected.

	The default value is false. This means the check will not be executed.

	This option is *not* inherited by child projects.

plugin.@PLUGIN@.rejectSubmodule
:	Reject submodules.

	This check looks for submodules in pushed files. If the check
	finds a submodule the push will be rejected.

	The default value is false. This means the check will not be executed.

	This option is *not* inherited by child projects.

plugin.@PLUGIN@.allowDuplicateFilenames
:	Allow duplicate filenames.

	This check looks for duplicate filenames in the tree of the commit
	as these can cause problems on Windows. If the check finds
	duplicate filenames the push will be rejected.

	This check compares filenames without caring about case sensitivity.

	The default value is false. This means duplicate filenames are not
	allowed.

	This option is *not* inherited by child projects.

plugin.@PLUGIN@.blockedMimeType
:	Blocked mime types.

	This check looks for blocked mime types. If the check finds an
	blocked mime type the push will be rejected.

	To detect mime types, this check is using the library
	`eu.medsea.mimeutil.MimeUtil2`.

	Defined patterns are *not* inherited by child projects.
