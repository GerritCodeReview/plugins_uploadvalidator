@PLUGIN@
========

Configuration
-------------

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
    rejectDuplicatePathnames = false
    rejectDuplicatePathnamesLocale = en
```

plugin.@PLUGIN@.blockedFileExtension
:	File extension to be blocked.

	The values for this check are case insensitive. You can define the
	blocked file extensions with or without a leading dot. This check
	only test if the filename ends with one of the defined values.

plugin.@PLUGIN@.requiredFooter
:	Footer that is required.

	This is the footer in the commit message.

plugin.@PLUGIN@.maxPathLength
:	Maximum allowed path length. '0' means no limit.

	Defaults to '0'.

plugin.@PLUGIN@.invalidFilenamePattern
:	Patterns for invalid filenames.

	This check is using `java.util.regex.Pattern` which is described
	[here][1].

plugin.@PLUGIN@.rejectWindowsLineEndings
:	Reject Windows line endings.

	This check looks for carriage return (CR) characters in pushed
	files. If the check finds a carriage return (CR) character
	the push will be rejected.

	This check does not run on [binary files][4]

	The default value is false. This means the check will not be executed.

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

plugin.@PLUGIN@.rejectSymlink
:	Reject symbolic links.

	This check looks for symbolic links in the set of pushed files. If
	the check finds a symbolic link the push will be rejected.

	The default value is false. This means the check will not be executed.

plugin.@PLUGIN@.rejectSubmodule
:	Reject submodules.

	This check looks for submodules in the set of pushed commits. If
	the check finds a submodule the push will be rejected.

	The default value is false. This means the check will not be executed.

plugin.@PLUGIN@.blockedKeywordPattern
:	Patterns for blocked keywords.

	This check looks for blocked keywords in files. If the check finds an
	blocked keyword the push will be rejected.

	To find a keyword it is possible to pass a regular expressions by
	blockedKeywordPattern.

	To detect blocked keywords, this check is using
	`java.util.regex.Pattern` which is described [here][1].

	This check does not run on [binary files][4]

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

plugin.@PLUGIN@.blockedContentTypeWhitelist
:	Blocked content type whitelist.

	If this option is checked, the entered content types are interpreted as
	a whitelist. Otherwise the entered content types are interpreted as a
	blacklist and commits that contains one of these content types will be
	rejected.

	There must be specified at least one blocked content type pattern,
	otherwise this option will be ignored.

	The default value is false. This means the entered content types are
	interpreted as a blacklist.

plugin.@PLUGIN@.rejectDuplicatePathnames
:	Reject duplicate pathnames.

	This check looks for duplicate pathnames which only differ in case
	in the tree of the commit as these can cause problems on case
	insensitive filesystems commonly used e.g. on Windows or Mac. If the
	check finds duplicate pathnames the push will be rejected.

	The default value is false. This means duplicate pathnames ignoring
	case are allowed.

plugin.@PLUGIN@.rejectDuplicatePathnamesLocale
:	Reject duplicate pathnames locale.

	When the validator checks for duplicate pathnames it will convert
	the pathnames to lower case. In some cases this leads to a [problem][5].

	To avoid these kind of problems, this option is used to specify a
	locale which is used when converting a pathname to lower case.

	Full list of supported locales can be found [here][6].

	The default value is "en" (English).

[5]: http://bugs.java.com/view_bug.do?bug_id=6208680
[6]: http://www.oracle.com/technetwork/java/javase/javase7locales-334809.html


UI Integration
--------------

This plugin provides a GUI to allow users to set @PLUGIN@ configurations.  It can
be access by selecting Projects -> List -> All-Projects -> General

Setting configurations with this UI is sticky, meaning that it will not be unset.
Default values will continue to be set when the fields are cleared and saved using
the UI. The consequence of this is that child projects may continue to override
its parent project's configurations with default settings.


Inheritance
-----------

The configuration parameters for this plugin are inheritable, meaning that
child projects can inherit settings from a parent project. Conversely child
projects can override settings in its' parent projects.

Since setting configurations using the UI is sticky users may need to ensure
that child projects do not contain specific @PLUGIN@ settings configured in
it's project.config

Example:

    Assume you have the project hierarchy 'All-Projects/products/widgetA'
    
    If you want widgetA project to inherit 'Reject Symbolic Links'
    configuration from its parent projects (All-Projects or products) then
    make sure widgetA's project.config does not contain the 'rejectSymlink'
    setting.  Add that setting to one of it's parent projects.


Additional information about project inheritance is in the Gerrit documentation.
