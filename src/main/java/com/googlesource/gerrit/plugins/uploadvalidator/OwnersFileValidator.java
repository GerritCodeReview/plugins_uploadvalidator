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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Multimap;
import com.google.gerrit.extensions.annotations.Exports;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.api.projects.ProjectConfigEntryType;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.account.Emails;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.config.ProjectConfigEntry;
import com.google.gerrit.server.events.CommitReceivedEvent;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.validators.CommitValidationException;
import com.google.gerrit.server.git.validators.CommitValidationListener;
import com.google.gerrit.server.git.validators.CommitValidationMessage;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

/** Check syntax of changed OWNERS files. */
public class OwnersFileValidator implements CommitValidationListener {

  public static final String FIND_OWNERS = "find-owners"; // the find-owners plugin name
  public static final String KEY_OWNERS_FILE_NAME = "ownersFileName";
  public static final String KEY_REJECT_ERROR_IN_OWNERS = "rejectErrorInOwners";
  public static final String OWNERS = "OWNERS"; // default OWNERS file name

  public static AbstractModule module() {
    return new AbstractModule() {
      @Override
      protected void configure() {
        DynamicSet.bind(binder(), CommitValidationListener.class).to(OwnersFileValidator.class);
        bind(ProjectConfigEntry.class)
            .annotatedWith(Exports.named(KEY_REJECT_ERROR_IN_OWNERS))
            .toInstance(
                new ProjectConfigEntry(
                    "Reject OWNERS Files With Errors",
                    null,
                    ProjectConfigEntryType.BOOLEAN,
                    null,
                    false,
                    "Pushes of commits with errors in OWNERS files will be rejected."));
      }
    };
  }

  private final String pluginName;
  private final PluginConfigFactory cfgFactory;
  private final GitRepositoryManager repoManager;
  private final ValidatorConfig validatorConfig;
  private final Emails emails;

  @Inject
  OwnersFileValidator(
      @PluginName String pluginName,
      PluginConfigFactory cfgFactory,
      GitRepositoryManager repoManager,
      Emails emails,
      ValidatorConfig validatorConfig) {
    this.pluginName = pluginName;
    this.cfgFactory = cfgFactory;
    this.repoManager = repoManager;
    this.emails = emails;
    this.validatorConfig = validatorConfig;
  }

  public static String getOwnersFileName(PluginConfig cfg) {
    return getOwnersFileName(cfg, OWNERS);
  }

  public static String getOwnersFileName(PluginConfig cfg, String defaultName) {
    return cfg.getString(KEY_OWNERS_FILE_NAME, defaultName);
  }

  public String getOwnersFileName(Project.NameKey project) {
    String name = getOwnersFileName(cfgFactory.getFromGerritConfig(FIND_OWNERS, true));
    try {
      return getOwnersFileName(
          cfgFactory.getFromProjectConfigWithInheritance(project, FIND_OWNERS), name);
    } catch (NoSuchProjectException e) {
      return name;
    }
  }

  @VisibleForTesting
  static boolean isActive(PluginConfig cfg) {
    return cfg.getBoolean(KEY_REJECT_ERROR_IN_OWNERS, false);
  }

  @Override
  public List<CommitValidationMessage> onCommitReceived(CommitReceivedEvent receiveEvent)
      throws CommitValidationException {
    List<CommitValidationMessage> messages = new LinkedList<>();
    try {
      Project.NameKey project = receiveEvent.project.getNameKey();
      PluginConfig cfg = cfgFactory.getFromProjectConfigWithInheritance(project, pluginName);
      if (isActive(cfg)
          && validatorConfig.isEnabledForRef(
              receiveEvent.user,
              receiveEvent.getProjectNameKey(),
              receiveEvent.getRefName(),
              KEY_REJECT_ERROR_IN_OWNERS)) {
        try (Repository repo = repoManager.openRepository(project)) {
          String name = getOwnersFileName(project);
          messages =
              performValidation(repo, receiveEvent.commit, receiveEvent.revWalk, name, false);
        }
      }
    } catch (NoSuchProjectException | IOException | ExecutionException e) {
      throw new CommitValidationException("failed to check owners files", e);
    }
    if (hasError(messages)) {
      throw new CommitValidationException("found invalid owners file", messages);
    }
    return messages;
  }

  @VisibleForTesting
  List<CommitValidationMessage> performValidation(
      Repository repo, RevCommit c, RevWalk revWalk, String ownersFileName, boolean verbose)
      throws IOException, ExecutionException {
    // Collect all messages from all files.
    List<CommitValidationMessage> messages = new LinkedList<>();
    // Collect all email addresses from all files and check each address only once.
    Map<String, Set<String>> email2lines = new HashMap<>();
    Map<String, ObjectId> content = CommitUtils.getChangedContent(repo, c, revWalk);
    for (String path : content.keySet()) {
      // skip non-OWNERS files
      if (!ownersFileName.equals(Paths.get(path).getFileName().toString())) {
        continue;
      }
      ObjectLoader ol = revWalk.getObjectReader().open(content.get(path));
      try (InputStream in = ol.openStream()) {
        if (RawText.isBinary(in)) {
          add(messages, path + " is a binary file", true); // OWNERS files cannot be binary
          continue;
        }
      }
      checkFile(messages, email2lines, path, ol, verbose);
    }
    checkEmails(messages, emails, email2lines, verbose);
    return messages;
  }

  private static void checkEmails(
      List<CommitValidationMessage> messages,
      Emails emails,
      Map<String, Set<String>> email2lines,
      boolean verbose) {
    List<String> owners = new ArrayList<>(email2lines.keySet());
    if (verbose) {
      for (String owner : owners) {
        add(messages, "owner: " + owner, false);
      }
    }
    if (emails == null || owners.isEmpty()) {
      return;
    }
    String[] ownerEmailsAsArray = new String[owners.size()];
    owners.toArray(ownerEmailsAsArray);
    try {
      Multimap<String, Account.Id> email2ids = emails.getAccountsFor(ownerEmailsAsArray);
      for (String owner : ownerEmailsAsArray) {
        boolean wrongEmail = (email2ids == null);
        if (!wrongEmail) {
          try {
            Collection<Account.Id> ids = email2ids.get(owner);
            wrongEmail = (ids == null || ids.size() != 1);
          } catch (Exception e) {
            wrongEmail = true;
          }
        }
        if (wrongEmail) {
          String locations = String.join(" ", email2lines.get(owner));
          add(messages, "unknown: " + owner + " at " + locations, true);
        }
      }
    } catch (Exception e) {
      add(messages, "accounts.byEmails failed.", true);
    }
  }

  private static void checkFile(
      List<CommitValidationMessage> messages,
      Map<String, Set<String>> email2lines,
      String path,
      ObjectLoader ol,
      boolean verbose)
      throws IOException {
    if (verbose) {
      add(messages, "validate: " + path, false);
    }
    try (BufferedReader br =
        new BufferedReader(new InputStreamReader(ol.openStream(), StandardCharsets.UTF_8))) {
      int line = 0;
      for (String l = br.readLine(); l != null; l = br.readLine()) {
        line++;
        checkLine(messages, email2lines, path, line, l, verbose);
      }
    }
  }

  // Line patterns accepted by Parser.java in the find-owners plugin.
  static final Pattern patComment = Pattern.compile("^ *(#.*)?$");
  static final Pattern patEmail = // email address or a "*"
      Pattern.compile("^ *([^ <>@]+@[^ <>@#]+|\\*) *(#.*)?$");
  static final Pattern patFile = Pattern.compile("^ *file:.*$");
  static final Pattern patNoParent = Pattern.compile("^ *set +noparent *(#.*)?$");
  static final Pattern patPerFileNoParent =
      Pattern.compile("^ *per-file +([^= ]+) *= *set +noparent *(#.*)?$");
  static final Pattern patPerFileEmail =
      Pattern.compile("^ *per-file +([^= ]+) *= *([^ <>@]+@[^ <>@#]+|\\*) *(#.*)?$");

  private static void collectEmail(
      Map<String, Set<String>> map, String email, String file, int lineNumber) {
    if (!email.equals("*")) {
      if (map.get(email) == null) {
        map.put(email, new HashSet<>());
      }
      map.get(email).add(file + ":" + lineNumber);
    }
  }

  private static boolean hasError(List<CommitValidationMessage> messages) {
    for (CommitValidationMessage m : messages) {
      if (m.isError()) {
        return true;
      }
    }
    return false;
  }

  private static void add(List<CommitValidationMessage> messages, String msg, boolean error) {
    messages.add(new CommitValidationMessage(msg, error));
  }

  private static void checkLine(
      List<CommitValidationMessage> messages,
      Map<String, Set<String>> email2lines,
      String path,
      int lineNumber,
      String line,
      boolean verbose) {
    Matcher m;
    if (patComment.matcher(line).find()
        || patNoParent.matcher(line).find()
        || patPerFileNoParent.matcher(line).find()) {
      return;
    } else if ((m = patEmail.matcher(line)).find()) {
      collectEmail(email2lines, m.group(1), path, lineNumber);
    } else if ((m = patPerFileEmail.matcher(line)).find()) {
      collectEmail(email2lines, m.group(2).trim(), path, lineNumber);
    } else {
      String prefix = patFile.matcher(line).find() ? "ignored" : "syntax";
      add(messages, prefix + ": " + path + ":" + lineNumber + ": " + line, true);
    }
  }
}
