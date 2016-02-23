// Copyright (C) 2014 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.uploadvalidator;

import static com.googlesource.gerrit.plugins.uploadvalidator.DuplicateFilenameValidator.KEY_ALLOW_DUPLICATE_FILENAMES;
import static com.googlesource.gerrit.plugins.uploadvalidator.FileExtensionValidator.KEY_BLOCKED_FILE_EXTENSION;
import static com.googlesource.gerrit.plugins.uploadvalidator.FooterValidator.KEY_REQUIRED_FOOTER;
import static com.googlesource.gerrit.plugins.uploadvalidator.InvalidFilenameValidator.KEY_INVALID_FILENAME_PATTERN;
import static com.googlesource.gerrit.plugins.uploadvalidator.InvalidLineEndingValidator.KEY_CHECK_RECJECT_WINDOWS_LINE_ENDINGS;
import static com.googlesource.gerrit.plugins.uploadvalidator.KeywordValidator.KEY_CHECK_BLOCKED_KEYWORD_PATTERN;
import static com.googlesource.gerrit.plugins.uploadvalidator.MaxPathLengthValidator.KEY_MAX_PATH_LENGTH;
import static com.googlesource.gerrit.plugins.uploadvalidator.MimeTypeValidator.KEY_BLOCKED_MIME_TYPE;
import static com.googlesource.gerrit.plugins.uploadvalidator.SubmoduleValidator.KEY_CHECK_SUBMODULE;
import static com.googlesource.gerrit.plugins.uploadvalidator.SymlinkValidator.KEY_CHECK_SYMLINK;

import com.google.gerrit.extensions.annotations.Exports;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.config.ProjectConfigEntry;
import com.google.gerrit.server.git.validators.CommitValidationListener;
import com.google.inject.AbstractModule;

class Module extends AbstractModule {
  @Override
  protected void configure() {
    DynamicSet.bind(binder(), CommitValidationListener.class)
        .to(FileExtensionValidator.class);
    bind(ProjectConfigEntry.class)
        .annotatedWith(Exports.named(KEY_BLOCKED_FILE_EXTENSION))
        .toInstance(
            new ProjectConfigEntry("Blocked File Extensions", null,
                ProjectConfigEntry.Type.ARRAY, null, false,
                "Forbidden file extensions. Pushes of commits that "
                    + "contain files with these extensions will be rejected."));

    DynamicSet.bind(binder(), CommitValidationListener.class)
        .to(FooterValidator.class);
    bind(ProjectConfigEntry.class)
        .annotatedWith(Exports.named(KEY_REQUIRED_FOOTER))
        .toInstance(
            new ProjectConfigEntry("Required Footers", null,
                ProjectConfigEntry.Type.ARRAY, null, false,
                "Required footers. Pushes of commits that miss any"
                    + " of the footers will be rejected."));

    DynamicSet.bind(binder(), CommitValidationListener.class)
        .to(InvalidFilenameValidator.class);
    bind(ProjectConfigEntry.class)
        .annotatedWith(Exports.named(KEY_INVALID_FILENAME_PATTERN))
        .toInstance(
            new ProjectConfigEntry("Invalid Filename Pattern", null,
                ProjectConfigEntry.Type.ARRAY, null, false,
                "Invalid filenames. Pushes of commits that "
                    + "contain filenames which match one of these patterns "
                    + "will be rejected."));

    DynamicSet.bind(binder(), CommitValidationListener.class)
        .to(InvalidLineEndingValidator.class);
    bind(ProjectConfigEntry.class)
        .annotatedWith(Exports.named(KEY_CHECK_RECJECT_WINDOWS_LINE_ENDINGS))
        .toInstance(
            new ProjectConfigEntry("Reject Windows Line Endings", "false",
                ProjectConfigEntry.Type.BOOLEAN, null, false, "Windows line "
                    + "endings. Pushes of commits that include files "
                    + "containing carriage return (CR) characters will be "
                    + "rejected."));

    DynamicSet.bind(binder(), CommitValidationListener.class)
        .to(DuplicateFilenameValidator.class);
    bind(ProjectConfigEntry.class)
        .annotatedWith(Exports.named(KEY_ALLOW_DUPLICATE_FILENAMES))
        .toInstance(
            new ProjectConfigEntry("Allow Duplicate Filenames", null,
                ProjectConfigEntry.Type.BOOLEAN, null, false,
                "Pushes of commits that contain duplicate filenames will be "
                    + "rejected. If y.equalsIgnoreCase(z) == true, then 'y' "
                    + "and 'z' are duplicates."));

    DynamicSet.bind(binder(), CommitValidationListener.class)
        .to(SymlinkValidator.class);
    bind(ProjectConfigEntry.class)
        .annotatedWith(Exports.named(KEY_CHECK_SYMLINK))
        .toInstance(
            new ProjectConfigEntry("Reject Symbolic Links", "false",
                ProjectConfigEntry.Type.BOOLEAN, null, false, "Symbolic Links. "
                    + "Pushes of commits that include symbolic links will be "
                    + "rejected."));

    DynamicSet.bind(binder(), CommitValidationListener.class)
        .to(SubmoduleValidator.class);
    bind(ProjectConfigEntry.class)
        .annotatedWith(Exports.named(KEY_CHECK_SUBMODULE))
        .toInstance(
            new ProjectConfigEntry("Reject Submodules", "false",
                ProjectConfigEntry.Type.BOOLEAN, null, false, "Pushes of "
                    + "commits that include submodules will be rejected."));

    DynamicSet.bind(binder(), CommitValidationListener.class)
        .to(MaxPathLengthValidator.class);
    bind(ProjectConfigEntry.class)
        .annotatedWith(Exports.named(KEY_MAX_PATH_LENGTH))
        .toInstance(
            new ProjectConfigEntry("Max Path Length", 0, false,
                "Maximum path length. Pushes of commits that "
                    + "contain files with longer paths will be rejected. "
                    + "'0' means no limit."));

    DynamicSet.bind(binder(), CommitValidationListener.class)
        .to(MimeTypeValidator.class);
    bind(ProjectConfigEntry.class)
        .annotatedWith(Exports.named(KEY_BLOCKED_MIME_TYPE))
        .toInstance(
            new ProjectConfigEntry("Blocked Mime Type", null,
                ProjectConfigEntry.Type.ARRAY, null, false,
                "Pushes of commits that contain files with blocked mime types "
                    + "will be rejected."));

    DynamicSet.bind(binder(), CommitValidationListener.class)
        .to(KeywordValidator.class);
    bind(ProjectConfigEntry.class)
        .annotatedWith(Exports.named(KEY_CHECK_BLOCKED_KEYWORD_PATTERN))
        .toInstance(
            new ProjectConfigEntry("Blocked Keyword Pattern", null,
                ProjectConfigEntry.Type.ARRAY, null, false,
                "Pushes of commits that contain files with blocked keywords "
                    + "will be rejected."));
  }
}
