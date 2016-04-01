Configuration
=============

The configuration of the @PLUGIN@ plugin is done on project level in
the `project.config` file of the project.

Project owners can do the configuration in the Gerrit web UI from
project info screen.

```
  [plugin "@PLUGIN@"]
    blockedFileExtension = jar
    blockedFileExtension = .zip
    blockedFileExtension = tar.gz
    blockedFileExtension = .core.tmp
    blockedKeywordPattern = myp4ssw0rd
    blockedKeywordPattern = foobar
    blockedKeywordPattern = \\$(Id|Header):[^$]*\\$
    invalidFilenamePattern = \\[|\\]|\\*|#
    invalidFilenamePattern = [%:@]
    rejectWindowsLineEndings = false
    binaryTypes = text/*
    binaryTypes = ^application/(pdf|xml)
    binaryTypes = application/zip
    requiredFooter = Bug
    maxPathLength = 200
    rejectSymlink = false
    rejectSubmodule = false
```

plugin.@PLUGIN@.blockedFileExtension
:	File extension to be blocked.

	The values for this check are case insensitive. You can define the
	blocked file extensions with or without a leading dot. This check
	only test if the filename ends with one of the defined values.

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

plugin.@PLUGIN@.rejectWindowsLineEndings
:	Reject Windows line endings.

	This check looks for carriage return (CR) characters in pushed
	files. If the check finds a carriage return (CR) character
	the push will be rejected.

	This check does not run on binary files.

	The default value is false. This means the check will not be executed.

	This option is *not* inherited by child projects.

plugin.@PLUGIN@.binaryTypes
:	Binary types.

	At the moment, there is no ideal solution to detect binary files. But
	some checks shouldn't run on binary files (e. g. InvalidLineEndingCheck).
	Because of that you can enter content types to avoid that these checks
	run on files with one of the entered content types.

	This check is using Apache Tika [2] to detect content types.

	This option is *not* inherited by child projects.

plugin.@PLUGIN@.rejectSymlink
:	Reject symbolic links.

	This check looks for symbolic links in the set of pushed files. If
	the check finds a symbolic link the push will be rejected.

	The default value is false. This means the check will not be executed.

	This option is *not* inherited by child projects.

plugin.@PLUGIN@.rejectSubmodule
:	Reject submodules.

	This check looks for submodules in the set of pushed commits. If
	the check finds a submodule the push will be rejected.

	The default value is false. This means the check will not be executed.

	This option is *not* inherited by child projects.

plugin.@PLUGIN@.blockedKeywordPattern
:	Patterns for blocked keywords.

	This check looks for blocked keywords in files. If the check finds an
	blocked keyword the push will be rejected.

	To find a keyword it is possible to pass a regular expressions by
	blockedKeywordPattern.

	To detect blocked keywords, this check is using
	`java.util.regex.Pattern` which is described [here][1].

	This check does not run on binary files.

	Defined patterns are *not* inherited by child projects.

[1]: https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html
[2]: https://tika.apache.org/
