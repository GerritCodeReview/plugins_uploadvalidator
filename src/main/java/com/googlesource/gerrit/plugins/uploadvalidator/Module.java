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

import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.git.receive.PluginPushOption;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class Module extends AbstractModule {

  @Override
  protected void configure() {
    install(new PatternCacheModule());
    install(ContentTypeUtil.module());

    install(FooterValidator.module());
    install(MaxPathLengthValidator.module());
    install(FileExtensionValidator.module());
    install(ChangeEmailValidator.module());
    install(InvalidFilenameValidator.module());
    install(SubmoduleValidator.module());
    install(SymlinkValidator.module());
    install(BlockedKeywordValidator.module());
    install(InvalidLineEndingValidator.module());
    install(ContentTypeValidator.module());
    install(DuplicatePathnameValidator.module());
    install(ValidatorConfig.module());

    bind(ConfigFactory.class).to(PluginConfigWithInheritanceFactory.class).in(Scopes.SINGLETON);

    DynamicSet.bind(binder(), PluginPushOption.class).to(SkipValidationPushOption.class);
  }
}
