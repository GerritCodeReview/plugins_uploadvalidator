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

import static com.googlesource.gerrit.plugins.uploadvalidator.FileExtensionValidator.KEY_BLOCKED_FILE_EXTENSION;
import static com.googlesource.gerrit.plugins.uploadvalidator.FooterValidator.KEY_REQUIRED_FOOTER;
import static com.googlesource.gerrit.plugins.uploadvalidator.MaxPathLengthValidator.KEY_MAX_PATH_LENGTH;

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
        .to(MaxPathLengthValidator.class);
    bind(ProjectConfigEntry.class)
        .annotatedWith(Exports.named(KEY_MAX_PATH_LENGTH))
        .toInstance(
            new ProjectConfigEntry("Max Path Length", 0, false,
                "Maximal path length. Pushes of commits that "
                    + "contain files with longer paths will be rejected. "
                    + "'0' means no limit."));
  }
}
