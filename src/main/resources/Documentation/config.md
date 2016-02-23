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
    blockedKeywordPattern = myp4ssw0rd
    blockedKeywordPattern = foobar
    blockedKeywordPattern = \\$(Id|Header):[^$]*\\$
    invalidFilenamePattern = [@:]
    invalidFilenamePattern = [#%*]
    rejectWindowsLineEndings = false
    ignoreFilesWhenCheckLineEndings = jpeg
    ignoreFilesWhenCheckLineEndings = pdf
    ignoreFilesWhenCheckLineEndings = exe
    ignoreFilesWhenCheckLineEndings = iso
    requiredFooter = Bug
    maxPathLength = 200
    rejectSymlink = false
    rejectSubmodule = false
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

plugin.@PLUGIN@.rejectWindowsLineEndings
:	Reject Windows line endings.

	This check looks for carriage return (CR) characters in pushed
	files. If the check finds a carriage return (CR) character
	the push will be rejected.

	The default value is false. This means the check will not be executed.

	This option is *not* inherited by child projects.

plugin.@PLUGIN@.ignoreFilesWhenCheckLineEndings
:	Ignore files during Windows line endings check.

	At the moment, there is no ideal solution to detect binary files.
	Because of that you can define file extensions, to prevent that
	this check validate this files.

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

	You can pass simple keywords or complex regular expressions. Each
	of them will be put into a capture group, so that [Matcher.find()][2]
	is able to find every proper subsequence.

	To detect blocked keywords, this check is using
	`java.util.regex.Pattern` which is described [here][1].

	Defined patterns are *not* inherited by child projects.

[1]: https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html
[2]: https://docs.oracle.com/javase/7/docs/api/java/util/regex/Matcher.html#find()

plugin.@PLUGIN@.allowDuplicateFilenames
:	Allow duplicate filenames.

	This check looks for duplicate filenames in the same folder in the
	tree of the commit as these can cause problems on Windows. If the
	check finds duplicate filenames the push will be rejected.

	This check compares filenames without caring about case sensitivity.

	The default value is true. This means duplicate filenames are allowed.

	This option is *not* inherited by child projects.