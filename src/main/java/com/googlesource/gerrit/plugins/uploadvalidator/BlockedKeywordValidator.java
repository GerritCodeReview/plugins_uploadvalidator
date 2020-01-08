// Copyright (C) 2016 The Android Open Source Project
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

import static com.googlesource.gerrit.plugins.uploadvalidator.PatternCacheModule.CACHE_NAME;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Patch;
import com.google.gerrit.entities.Project;
import com.google.gerrit.entities.Project.NameKey;
import com.google.gerrit.extensions.annotations.Exports;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.api.projects.ProjectConfigEntryType;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.extensions.validators.CommentForValidation;
import com.google.gerrit.extensions.validators.CommentValidationContext;
import com.google.gerrit.extensions.validators.CommentValidationFailure;
import com.google.gerrit.extensions.validators.CommentValidator;
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
import com.google.inject.name.Named;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

/**
 * A validator for checking for use of blocked keywords in commit content and comments.
 *
 * <p>This class implements both {@link CommitValidationListener} and {@link CommentValidator}
 * validator classes to run its list of blocked keywords against commit content and comments.
 */
public class BlockedKeywordValidator implements CommitValidationListener, CommentValidator {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final String KEY_CHECK_BLOCKED_KEYWORD = "blockedKeyword";
  private static final String KEY_CHECK_COMMENT_BLOCKED_KEYWORD = "blockedKeywordComments";
  private static final String KEY_CHECK_BLOCKED_KEYWORD_PATTERN =
      KEY_CHECK_BLOCKED_KEYWORD + "Pattern";

  public static AbstractModule module() {
    return new AbstractModule() {
      @Override
      protected void configure() {
        DynamicSet.bind(binder(), CommitValidationListener.class).to(BlockedKeywordValidator.class);
        DynamicSet.bind(binder(), CommentValidator.class).to(BlockedKeywordValidator.class);
        bind(ProjectConfigEntry.class)
            .annotatedWith(Exports.named(KEY_CHECK_BLOCKED_KEYWORD_PATTERN))
            .toInstance(
                new ProjectConfigEntry(
                    "Blocked Keyword Pattern",
                    null,
                    ProjectConfigEntryType.ARRAY,
                    null,
                    false,
                    "Pushes of commits that contain files or commit messages with "
                        + "blocked keywords will be rejected."));
      }
    };
  }

  private final String pluginName;
  private final PluginConfigFactory cfgFactory;
  private final GitRepositoryManager repoManager;
  private final LoadingCache<String, Pattern> patternCache;
  private final ContentTypeUtil contentTypeUtil;
  private final ValidatorConfig validatorConfig;

  @Inject
  BlockedKeywordValidator(
      @PluginName String pluginName,
      ContentTypeUtil contentTypeUtil,
      @Named(CACHE_NAME) LoadingCache<String, Pattern> patternCache,
      PluginConfigFactory cfgFactory,
      GitRepositoryManager repoManager,
      ValidatorConfig validatorConfig) {
    this.pluginName = pluginName;
    this.patternCache = patternCache;
    this.cfgFactory = cfgFactory;
    this.repoManager = repoManager;
    this.contentTypeUtil = contentTypeUtil;
    this.validatorConfig = validatorConfig;
  }

  static boolean isActive(PluginConfig cfg) {
    return cfg.getStringList(KEY_CHECK_BLOCKED_KEYWORD_PATTERN).length > 0;
  }

  @Override
  public List<CommitValidationMessage> onCommitReceived(CommitReceivedEvent receiveEvent)
      throws CommitValidationException {
    try {
      PluginConfig cfg =
          cfgFactory.getFromProjectConfigWithInheritance(
              receiveEvent.project.getNameKey(), pluginName);
      if (isActive(cfg)
          && validatorConfig.isEnabled(
              receiveEvent.user,
              receiveEvent.getProjectNameKey(),
              receiveEvent.getRefName(),
              KEY_CHECK_BLOCKED_KEYWORD)) {
        ImmutableMap<String, Pattern> blockedKeywordPatterns =
            patternCache.getAll(
                Arrays.asList(cfg.getStringList(KEY_CHECK_BLOCKED_KEYWORD_PATTERN)));
        try (Repository repo = repoManager.openRepository(receiveEvent.project.getNameKey())) {
          List<CommitValidationMessage> messages =
              performValidation(
                  repo,
                  receiveEvent.commit,
                  receiveEvent.revWalk,
                  blockedKeywordPatterns.values(),
                  cfg);
          if (!messages.isEmpty()) {
            throw new CommitValidationException(
                "includes files containing blocked keywords", messages);
          }
        }
      }
    } catch (NoSuchProjectException | IOException | ExecutionException e) {
      throw new CommitValidationException("failed to check on blocked keywords", e);
    }
    return Collections.emptyList();
  }

  @Override
  public ImmutableList<CommentValidationFailure> validateComments(
      CommentValidationContext ctx, ImmutableList<CommentForValidation> comments) {
    try {
      NameKey projectNameKey = Project.nameKey(ctx.getProject());
      PluginConfig cfg = cfgFactory.getFromProjectConfigWithInheritance(projectNameKey, pluginName);
      if (isActive(cfg)
          && validatorConfig.isEnabled(
              null, projectNameKey, "", KEY_CHECK_COMMENT_BLOCKED_KEYWORD)) {
        ImmutableMap<String, Pattern> blockedKeywordPatterns =
            patternCache.getAll(
                Arrays.asList(cfg.getStringList(KEY_CHECK_BLOCKED_KEYWORD_PATTERN)));
        return comments.stream()
            .map(comment -> validateComment(blockedKeywordPatterns, comment))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(ImmutableList.toImmutableList());
      }
    } catch (NoSuchProjectException | ExecutionException e) {
      throw new IllegalStateException("Exception validating comments in uploadvalidator", e);
    }
    return ImmutableList.of();
  }

  @VisibleForTesting
  List<CommitValidationMessage> performValidation(
      Repository repo,
      RevCommit c,
      RevWalk revWalk,
      ImmutableCollection<Pattern> blockedKeywordPartterns,
      PluginConfig cfg)
      throws IOException, ExecutionException {
    List<CommitValidationMessage> messages = new LinkedList<>();
    checkCommitMessageForBlockedKeywords(blockedKeywordPartterns, messages, c.getFullMessage());
    Map<String, ObjectId> content = CommitUtils.getChangedContent(repo, c, revWalk);
    for (String path : content.keySet()) {
      ObjectLoader ol = revWalk.getObjectReader().open(content.get(path));
      try (InputStream in = ol.openStream()) {
        if (RawText.isBinary(in) || contentTypeUtil.isBlacklistedBinaryContentType(ol, path, cfg)) {
          continue;
        }
      }
      checkFileForBlockedKeywords(blockedKeywordPartterns, messages, path, ol);
    }
    return messages;
  }

  private static Optional<CommentValidationFailure> validateComment(
      ImmutableMap blockedKeywordPatterns, CommentForValidation comment) {
    Set<String> findings =
        new HashSet<String>(
            findBlockedKeywordsInString(blockedKeywordPatterns.values(), comment.getText()));
    if (findings.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(comment.failValidation(
        String.format("banned words found in your comment (%s)", Iterables.toString(findings))));
  }

  private static void checkCommitMessageForBlockedKeywords(
      ImmutableCollection<Pattern> blockedKeywordPatterns,
      List<CommitValidationMessage> messages,
      String commitMessage) {
    int line = 0;
    for (String l : commitMessage.split("[\r\n]+")) {
      line++;
      checkLineForBlockedKeywords(blockedKeywordPatterns, messages, Patch.COMMIT_MSG, line, l);
    }
  }

  private static void checkFileForBlockedKeywords(
      ImmutableCollection<Pattern> blockedKeywordPartterns,
      List<CommitValidationMessage> messages,
      String path,
      ObjectLoader ol)
      throws IOException {
    try (BufferedReader br =
        new BufferedReader(new InputStreamReader(ol.openStream(), StandardCharsets.UTF_8))) {
      int line = 0;
      for (String l = br.readLine(); l != null; l = br.readLine()) {
        line++;
        checkLineForBlockedKeywords(blockedKeywordPartterns, messages, path, line, l);
      }
    }
  }

  private static List<String> findBlockedKeywordsInString(
      ImmutableCollection<Pattern> blockedKeywordPartterns, String text) {
    List<String> found = new ArrayList<>();
    for (Pattern p : blockedKeywordPartterns) {
      Matcher matcher = p.matcher(text);
      while (matcher.find()) {
        found.add(matcher.group());
      }
    }
    return found;
  }

  private static void checkLineForBlockedKeywords(
      ImmutableCollection<Pattern> blockedKeywordPartterns,
      List<CommitValidationMessage> messages,
      String path,
      int lineNumber,
      String line) {
    List<String> found = findBlockedKeywordsInString(blockedKeywordPartterns, line);
    if (!found.isEmpty()) {
      messages.add(
          new CommitValidationMessage(
              MessageFormat.format(
                  "blocked keyword(s) found in: {0} (Line: {1}) (found: {2})",
                  path, lineNumber, Joiner.on(", ").join(found)),
              true));
    }
  }
}
