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
    blockedContentType = application/x-object
    blockedContentType = application/*
    blockedContentType = ^text/(html|xml)
    blockedContentTypeWhitelist = false
    rejectWindowsLineEndings = false
    binaryType = application/*
    binaryType = ^application/(pdf|xml)
    binaryType = application/zip
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

	This check does not run on [binary files][4]

	The default value is false. This means the check will not be executed.

	This option is *not* inherited by child projects.

<a name="binary_type">
plugin.@PLUGIN@.binaryType
:	Binary types.

	Some checks should not run on binary files (e. g. InvalidLineEndingCheck).
	Using this option it is possible to configure which content types are
	considered binary types.

	To detect content types [Apache Tika library][2] is used.

	Content type can be specified as a string, wildcard or a regular expression,
	for example:

	- application/zip
	- application/*
	- ^application/(pdf|xml)

	As usual, the '^' prefix is used to denote that the value is a regular
	expression.

	Full list of supported content types can be found [here][3].

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

	This check does not run on [binary files][4]

	Defined patterns are *not* inherited by child projects.

[1]: https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html
[2]: https://tika.apache.org/
[3]: https://tika.apache.org/1.12/formats.html#Full_list_of_Supported_Formats
[4]: #binary_type

plugin.@PLUGIN@.blockedContentType
:	Blocked content type.

	This check looks for blocked content types. If the check finds a
	blocked content type the push will be rejected.

	To detect content types [Apache Tika library][2] is used.

	Content type can be specified as a string, wildcard or a regular expression,
	for example:

	- application/zip
	- application/*
	- ^application/(pdf|xml)

	As usual, the '^' prefix is used to denote that the value is a regular
	expression.

	Full list of supported content types can be found [here][3].

	Defined patterns are *not* inherited by child projects.

plugin.@PLUGIN@.blockedContentTypeWhitelist
:	Blocked content type whitelist.

	If this option is checked, the entered content types are interpreted as
	a whitelist. Otherwise the entered content types are interpreted as a
	blacklist and commits that contains one of these content types will be
	rejected.

	The default value is false. This means the entered content types are
	interpreted as a blacklist.

	Defined patterns are *not* inherited by child projects.
