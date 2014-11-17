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
    maxPathLength = 200
```

plugin.@PLUGIN@.blockedFileExtension.enabled
:	Enable or disable this validation type.

	Defaults to 'true'.

plugin.@PLUGIN@.requiredFooter.enabled
:	Enable or disable this validation type.

	Defaults to 'true'.

plugin.@PLUGIN@.maxPathLength.enabled
:	Enable or disable this validation type.

	Defaults to 'true'.

plugin.@PLUGIN@.charSetValidator.enabled
:	Enable or disable this validation type.

	Defaults to 'true'.

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


Global configuration values
===========================

These options are currently only available in the gerrit master
configuration file.

plugin.@PLUGIN@.charSetValidator.referenceRegex
:	Enforce branch naming conventions using regexp.

	Defaults to '^[a-z0-9_\\-/]+$'.

	The default pattern enforces lowercase US-ASCII branch names.

	In a hetrogenous environment you can have problems with machines
	that use case preserving filesystems. If one of these machines
	receives a branch (path, really) with a different case, it will
	simply rename the 'directory' and cause all kinds of problems for
	the user.

plugin.@PLUGIN@.charSetValidator.validateUTF8
:	Validate the UTF-8 encoding on commits.

	Defaults to 'fast'.

	Verify that the content of a commit message is UTF-8 valid.

	This value can be set to 'none', 'full' or 'fast'.

	Notes about 'fast' mode: 
	This check will handle US-ASCII and the basic encoding of UTF-8.
	It will not validate charset ranges or look closely at the data.

	It could potentially do the wrong thing (i.e. be too relaxed) but
	it should be fast and use less resources than a full validation.

plugin.@PLUGIN@.charSetValidator.referenceRejectReason
:	If a branch fails the validation, what should we tell the user.

	Defaults to 'Sorry, your branch is not valid.'.

	This is a good place to put links to internal or external documentation
	about the possible issues that you could have encountered and what the
	user should do to work around the issue.

plugin.@PLUGIN@.charSetValidator.charsetRejectReason
:	If a commit is not UTF-8 legal, what do we tell the user.

	Defaults to 'Sorry, your commit has non UTF-8 content.'.

	This is a good place to put links to internal or external documentation
	about the possible issues that you could have encountered and what the
	user should do to workaround the issue.

plugin.@PLUGIN@.charSetValidator.internalErrorMessage
:	If there is a internal error in the check, what should we tell the user.

	Defaults to 'CharSetValidator failed to validate your commit.'.

	This is a good place to put links to where the user can get support/help and
	report the issue.
