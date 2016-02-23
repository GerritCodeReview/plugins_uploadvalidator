This plugin allows to configure upload validations per project.

Project owners can configure:

- blocked file extensions
- invalid filenames
- blocked keywords
- blocked content types
- reject Windows line endings
- symbolic links
- reject submodules
- required footers
- maximum allowed path length

Pushes of commits that violate these settings are rejected by Gerrit.
