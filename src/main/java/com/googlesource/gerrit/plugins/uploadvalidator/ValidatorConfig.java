// Copyright (C) 2017 The Android Open Source Project
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

import com.google.gerrit.common.Nullable;
import com.google.gerrit.common.data.AccessSection;
import com.google.gerrit.exceptions.StorageException;
import com.google.gerrit.extensions.annotations.Exports;
import com.google.gerrit.extensions.api.projects.ProjectConfigEntryType;
import com.google.gerrit.reviewdb.client.AccountGroup;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.ProjectConfigEntry;
import com.google.gerrit.server.group.InternalGroup;
import com.google.gerrit.server.project.RefPatternMatcher;
import com.google.gerrit.server.query.group.InternalGroupQuery;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidatorConfig {
  private static final Logger log = LoggerFactory.getLogger(ValidatorConfig.class);
  private static final String KEY_PROJECT = "project";
  private static final String KEY_REF = "ref";
  private final ConfigFactory configFactory;
  private final GroupByNameFinder groupByNameFinder;

  public static AbstractModule module() {
    return new AbstractModule() {
      @Override
      protected void configure() {
        bind(ProjectConfigEntry.class)
            .annotatedWith(Exports.named(KEY_PROJECT))
            .toInstance(
                new ProjectConfigEntry(
                    "Projects",
                    null,
                    ProjectConfigEntryType.ARRAY,
                    null,
                    false,
                    "Only projects that match this regex will be validated."));
        bind(ProjectConfigEntry.class)
            .annotatedWith(Exports.named(KEY_REF))
            .toInstance(
                new ProjectConfigEntry(
                    "Refs",
                    null,
                    ProjectConfigEntryType.ARRAY,
                    null,
                    false,
                    "Only refs that match this regex will be validated."));
        bind(GroupByNameFinder.class).to(GroupByNameFromIndexFinder.class);
      }
    };
  }

  @Inject
  public ValidatorConfig(ConfigFactory configFactory, GroupByNameFinder groupByNameFinder) {
    this.configFactory = configFactory;
    this.groupByNameFinder = groupByNameFinder;
  }

  public boolean isEnabledForRef(
      IdentifiedUser user, Project.NameKey projectName, String refName, String validatorOp) {
    PluginConfig conf = configFactory.get(projectName);

    return conf != null
        && isValidConfig(conf, projectName)
        && (activeForRef(conf, refName))
        && (activeForEmail(conf, user.getAccount().getPreferredEmail()))
        && (activeForProject(conf, projectName.get()))
        && (!hasCriteria(conf, "skipGroup")
            || !canSkipValidation(conf, validatorOp)
            || !canSkipRef(conf, refName)
            || !canSkipGroup(conf, user));
  }

  private boolean isValidConfig(PluginConfig config, Project.NameKey projectName) {
    return hasValidConfigRef(config, "ref", projectName)
        && hasValidConfigRef(config, "skipRef", projectName);
  }

  private boolean hasValidConfigRef(
      PluginConfig config, String refKey, Project.NameKey projectName) {
    boolean valid = true;
    for (String refPattern : config.getStringList(refKey)) {
      if (!AccessSection.isValidRefSectionName(refPattern)) {
        log.error(
            "Invalid {} name/pattern/regex '{}' in {} project's plugin config",
            refKey,
            refPattern,
            projectName.get());
        valid = false;
      }
    }
    return valid;
  }

  private boolean hasCriteria(PluginConfig config, String criteria) {
    return config.getStringList(criteria).length > 0;
  }

  private boolean activeForProject(PluginConfig config, String project) {
    return matchCriteria(config, "project", project, true, false);
  }

  private boolean activeForRef(PluginConfig config, String ref) {
    return matchCriteria(config, "ref", ref, true, true);
  }

  private boolean activeForEmail(PluginConfig config, @Nullable String email) {
    return matchCriteria(config, "email", email, true, false);
  }

  private boolean canSkipValidation(PluginConfig config, String validatorOp) {
    return matchCriteria(config, "skipValidation", validatorOp, false, false);
  }

  private boolean canSkipRef(PluginConfig config, String ref) {
    return matchCriteria(config, "skipRef", ref, true, true);
  }

  private boolean matchCriteria(
      PluginConfig config,
      String criteria,
      @Nullable String value,
      boolean allowRegex,
      boolean refMatcher) {
    String[] c = config.getStringList(criteria);
    if (c.length == 0) {
      return true;
    }
    if (value == null) {
      return false;
    }
    if (allowRegex) {
      return Arrays.stream(c).anyMatch(s -> match(value, s, refMatcher));
    }
    return Arrays.asList(c).contains(value);
  }

  private static boolean match(String value, String pattern, boolean refMatcher) {
    if (refMatcher) {
      return RefPatternMatcher.getMatcher(pattern).match(value, null);
    }
    return Pattern.matches(pattern, value);
  }

  private boolean canSkipGroup(PluginConfig conf, IdentifiedUser user) {
    if (!user.isIdentifiedUser()) {
      return false;
    }

    Stream<AccountGroup.UUID> skipGroups =
        Arrays.stream(conf.getStringList("skipGroup")).map(this::groupUUID);
    return user.asIdentifiedUser().getEffectiveGroups().containsAnyOf(skipGroups::iterator);
  }

  private AccountGroup.UUID groupUUID(String groupNameOrUUID) {
    Optional<InternalGroup> group =
        groupByNameFinder.get(AccountGroup.nameKey(groupNameOrUUID));
    return group.map(InternalGroup::getGroupUUID).orElse(new AccountGroup.UUID(groupNameOrUUID));
  }

  interface GroupByNameFinder {
    Optional<InternalGroup> get(AccountGroup.NameKey groupName);
  }

  static class GroupByNameFromIndexFinder implements GroupByNameFinder {

    private final Provider<InternalGroupQuery> groupQueryProvider;

    @Inject
    GroupByNameFromIndexFinder(Provider<InternalGroupQuery> groupQueryProvider) {
      this.groupQueryProvider = groupQueryProvider;
    }

    @Override
    public Optional<InternalGroup> get(AccountGroup.NameKey groupName) {
      try {
        return groupQueryProvider.get().byName(groupName);
      } catch (StorageException e) {
        log.warn(String.format("Cannot lookup group %s by name", groupName.get()), e);
      }
      return Optional.empty();
    }
  }
}
