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

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.gerrit.extensions.annotations.Exports;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.api.projects.ProjectConfigEntryType;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.config.ProjectConfigEntry;
import com.google.gerrit.server.events.CommitReceivedEvent;
import com.google.gerrit.server.git.validators.CommitValidationException;
import com.google.gerrit.server.git.validators.CommitValidationListener;
import com.google.gerrit.server.git.validators.CommitValidationMessage;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;

import org.eclipse.jgit.revwalk.FooterLine;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FooterValidator implements CommitValidationListener {

  public static AbstractModule module() {
    return new AbstractModule() {

      @Override
      protected void configure() {
        DynamicSet.bind(binder(), CommitValidationListener.class)
            .to(FooterValidator.class);
        bind(ProjectConfigEntry.class)
            .annotatedWith(Exports.named(KEY_REQUIRED_FOOTER))
            .toInstance(new ProjectConfigEntry("Required Footers", null,
                ProjectConfigEntryType.ARRAY, null, false,
                "Required footers. Pushes of commits that miss any"
                    + " of the footers will be rejected."));
      }
    };
  }

  public static final String KEY_REQUIRED_FOOTER = "requiredFooter";

  private final String pluginName;
  private final PluginConfigFactory cfgFactory;
  private final ValidatorConfig validatorConfig;

  @Inject
  FooterValidator(@PluginName String pluginName, PluginConfigFactory cfgFactory,
      ValidatorConfig validatorConfig) {
    this.pluginName = pluginName;
    this.cfgFactory = cfgFactory;
    this.validatorConfig = validatorConfig;
  }

  @Override
  public List<CommitValidationMessage> onCommitReceived(
      CommitReceivedEvent receiveEvent) throws CommitValidationException {
    try {
      PluginConfig cfg =
          cfgFactory.getFromProjectConfigWithInheritance(
              receiveEvent.project.getNameKey(), pluginName);
      String[] requiredFooters =
          cfg.getStringList(KEY_REQUIRED_FOOTER);
      if (requiredFooters.length > 0
          && validatorConfig.isEnabledForRef(receiveEvent.user,
              receiveEvent.getProjectNameKey(), receiveEvent.getRefName(),
              KEY_REQUIRED_FOOTER)) {
        List<CommitValidationMessage> messages = new LinkedList<>();
        Set<String> footers = FluentIterable.from(receiveEvent.commit.getFooterLines())
            .transform(new Function<FooterLine, String>() {
                @Override
                public String apply(FooterLine f) {
                    return f.getKey().toLowerCase(Locale.US);
                }
            })
            .toSet();
        for (int i = 0; i < requiredFooters.length; i++) {
          if (!footers.contains(requiredFooters[i].toLowerCase(Locale.US))) {
            messages.add(new CommitValidationMessage(
                "missing required footer: " + requiredFooters[i], true));
          }
        }
        if (!messages.isEmpty()) {
          throw new CommitValidationException(
              "missing required footers in commit message", messages);
        }
      }
    } catch (NoSuchProjectException e) {
      throw new CommitValidationException("failed to check for required footers", e);
    }

    return Collections.emptyList();
  }
}
