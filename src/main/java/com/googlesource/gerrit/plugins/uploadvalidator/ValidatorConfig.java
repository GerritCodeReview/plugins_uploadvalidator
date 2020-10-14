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

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.gerrit.common.Nullable;
import com.google.gerrit.entities.AccessSection;
import com.google.gerrit.entities.AccountGroup;
import com.google.gerrit.entities.AccountGroup.UUID;
import com.google.gerrit.entities.Project;
import com.google.gerrit.exceptions.StorageException;
import com.google.gerrit.extensions.annotations.Exports;
import com.google.gerrit.extensions.api.projects.ProjectConfigEntryType;
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

  /**
   * Checks whether the provided params match with the plugin configuration to verify it is enabled.
   *
   * @param user A Nullable field identifying the user defined on the ref. Passing null will ignore
   *     user checks.
   * @param projectName Identifier for the project name on the ref.
   * @param refName Identifier for the ref name.
   * @param validatorOp The name of the validator operation. Can be used in skip validation config.
   * @return boolean indicating if the ref is enabled for validation.
   */
  public boolean isEnabled(
      @Nullable IdentifiedUser user,
      Project.NameKey projectName,
      String refName,
      String validatorOp) {
    PluginConfig conf = configFactory.get(projectName);
    if (conf == null) {
      // Not enabled if no config is retrieved.
      return false;
    }
    if (!isValidConfig(conf, projectName)) {
      // Not enabled if config is not determined to be valid.
      return false;
    }
    if (isDisabledValidatorOp(conf, validatorOp)) {
      // Not enabled if it is an explicitly disabled validator.
      return false;
    }
    if (isDisabledForProject(conf, projectName.get())) {
      // Not enabled if project is a disabled project.
      return false;
    }
    if (!activeForRef(conf, refName)) {
      return false;
    }
    if (!activeForProject(conf, projectName.get())) {
      return false;
    }
    if (user != null && !activeForEmail(conf, user.getAccount().preferredEmail())) {
      return false;
    }
    if (hasCriteria(conf, "skipGroup")
        && canSkipGroup(conf, user)
        && canSkipValidation(conf, validatorOp)
        && canSkipRef(conf, refName)) {
      return false;
    }
    return true;
  }


  /**
   * Checks whether the provided filepath is disabled.
   *
   * @param config The config file to check the filename against.
   * @param filepath The filepath to check against the plugin config.
   * @return boolean indicating if the filepath is disabled for the provided plugin config.
   */
  public boolean isDisabledForFilepath(PluginConfig config, String filepath) {
    return hasCriteria(config, "disabledFilepath")
        && matchCriteria(config, "disabledFilepath", filepath, true, false);
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

  private boolean isDisabledValidatorOp(PluginConfig config, String validatorOp) {
    String[] c = config.getStringList("disabledValidation");
    return Arrays.asList(c).contains(validatorOp);
  }

  private boolean activeForProject(PluginConfig config, String project) {
    return matchCriteria(config, "project", project, true, false);
  }

  private boolean isDisabledForProject(PluginConfig config, String project) {
    return hasCriteria(config, "disabledProject")
        && matchCriteria(config, "disabledProject", project, true, false);
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

  private boolean canSkipGroup(PluginConfig conf, @Nullable IdentifiedUser user) {
    if (user == null || !user.isIdentifiedUser()) {
      return false;
    }

    ImmutableList<UUID> skipGroups =
        Arrays.stream(conf.getStringList("skipGroup"))
            .map(this::groupUUID)
            .collect(toImmutableList());
    return user.asIdentifiedUser().getEffectiveGroups().containsAnyOf(skipGroups);
  }

  private AccountGroup.UUID groupUUID(String groupNameOrUUID) {
    Optional<InternalGroup> group = groupByNameFinder.get(AccountGroup.nameKey(groupNameOrUUID));
    return group.map(InternalGroup::getGroupUUID).orElse(AccountGroup.uuid(groupNameOrUUID));
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
