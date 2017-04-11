@PLUGIN@ Configuration
======================

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
	
	Note: If there is a NULL byte in the first 8k then the file will be considered
	binary regardless of this setting.

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

This @PLUGIN@ configuration is read from the project.config, editable from
the general project settings screen.
E.g. /admin/projects/myrepo for a project named 'myrepo'

Inheritance
-----------

The configuration parameters for this plugin are inheritable, meaning that
child projects can inherit settings from a parent project.
The mechanism for evaluating the combined parameters follows the standard
[Project's inheritance rules][7].

[7]: config-project-config.html#file-project_config

Ref-specific validations
---------------------------

By default, the validation will be enabled for all refs. However, it can
be limited to particular refs by setting `plugin.@PLUGIN@.ref`. The
refs may be configured using specific ref names, ref patterns, or regular
expressions. Multiple refs may be specified.

NOTE: ref needs to start with "ref/" for name and pattern or "^ref/" for regex
matching. When an invalid ref is specified in the Project config, the plugin
functionality is disabled, and an error is reported in the Gerrit log.

E.g. to limit the validation to the `master` branch and all stable
branches the following could be configured:

```
  [plugin "@PLUGIN@"]
    ref = refs/heads/master
    ref = ^refs/heads/stable-.*
```

Email-specific validations
---------------------------

By default, the validation will be enabled for all users. However, it can
be limited to users with particular emails by setting `plugin.@PLUGIN@.email`.
The emails may be configured using specific emails, patterns, or regular
expressions. Multiple emails may be specified.

E.g. to limit the validation to all users whose emails match `.*@example.com$`
the following could be configured:

```
  [plugin "@PLUGIN@"]
    email = .*@example.com$
```

Project-specific validations
---------------------------

By default, the validation will be enabled for all projects. However, it can
be limited to particular projects by setting `plugin.@PLUGIN@.project`. The
projects may be configured using specific project names, project patterns, or
regular expressions. Multiple projects may be specified.

E.g. to limit the validation to all projects that match `^platform/.*` the
following could be configured:

```
  [plugin "@PLUGIN@"]
    project = ^platform/.*
```

Permission to skip the rules
----------------------------

Some users may be allowed to skip some of the rules on a per project and
per repository basis by configuring the appropriate "skip" settings in the
project.config.

Skip of the rules is controlled by:

plugin.@PLUGIN@.skipGroup
:	Group names or UUIDs allowed to skip the rules.

	Groups that are allowed to skip the rules.

	Multiple values are supported.
	Default: nobody is allowed to skip the rules (empty).

	NOTE: When skipGroup isn't defined, all the other skip settings are ignored.

plugin.@PLUGIN@.skipRef
:	Ref name, pattern or regexp of the branch to skip.

	List of specific ref names, ref patterns, or regular expressions
	of the branches where Groups defined in skipGroup are allowed to
	skip the rules.

	Multiple values are supported.
	Default: skip validation on all branches for commits pushed by a member of
	a group listed in skipGroup

plugin.@PLUGIN@.skipValidation
:	Specific validation to be skipped.

	List of specific validation operations allowed to be skipped by
	the Groups defined in skipGroup on the branches defined in skipRef.

	Validations can be one of the following strings:

	- blockedContentType
	- blockedFileExtension
	- blockedKeyword
	- invalidFilename
	- maxPathLength
	- rejectDuplicatePathnames
	- rejectSubmodule
	- rejectSymlink
	- rejectWindowsLineEndings
	- requiredFooter

	Multiple values are supported.
	Default: groups defined at skipGroup can skip all the validation rules.

NOTE: Skip of the validations are inherited from parent projects. The definition
of the skip criteria on All-Projects automatically apply to every project.

The simplest configuration is to allow a specific group (e.g. Administrators)
to skip all the rules:

```
   [plugin "@PLUGIN@"]
     skipGroup = Administrators
```

A typical configuration would be to enable validation for a set of branches,
while excluding a few of them.
```
   [plugin "@PLUGIN@"]
       ref = ^refs/heads/stable-.*
       skipGroup = release-manager
       skipRef = refs/heads/stable-3.4
       skipRef = refs/heads/stable-5.6
```

A more complex configuration is to allow a set of groups from LDAP, the ReleaseManager
and GerritAdmins, to push any content to any file extension but only for the master branch:

```
  [plugin "@PLUGIN@"]
    skipValidation = blockedFileExtension
    skipValidation = blockedContentType
    skipGroup = ldap/ReleaseManagers
    skipGroup = ldap/GerritAdmins
    skipRef = refs/heads/master
```
