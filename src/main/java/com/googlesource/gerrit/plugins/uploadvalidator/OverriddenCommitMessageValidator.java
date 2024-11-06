package com.googlesource.gerrit.plugins.uploadvalidator;

import static com.google.gerrit.server.util.CommitMessageUtil.getChangeIdFromCommitMessageFooter;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Change;
import com.google.gerrit.entities.PatchSet;
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
import com.google.gerrit.server.git.validators.ValidationMessage;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.gerrit.server.query.change.ChangeData;
import com.google.gerrit.server.query.change.InternalChangeQuery;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javax.inject.Provider;
import org.eclipse.jgit.revwalk.RevWalk;

/**
 * An upload validator that warns if the commit-message to-be-uploaded reverts the commit-message to
 * an older revision.
 *
 * This plugin is enabled by default. However, it only generates (non-blocking) warnings, and all of
 * its failures should be quietly ignored.
 */
public class OverriddenCommitMessageValidator implements CommitValidationListener {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  public static AbstractModule module() {
    return new AbstractModule() {

      @Override
      protected void configure() {
        DynamicSet.bind(binder(), CommitValidationListener.class)
            .to(OverriddenCommitMessageValidator.class);
        bind(ProjectConfigEntry.class)
            .annotatedWith(Exports.named(KEY_CHECK_OVERRIDDEN_COMMIT_MESSAGE))
            .toInstance(
                new ProjectConfigEntry(
                    "Validate message overrides",
                    "true",
                    ProjectConfigEntryType.BOOLEAN,
                    null,
                    true,
                    "Warn whether a commit to-be-uploaded reverts the commit-message to an older"
                        + " revision."));
      }
    };
  }

  public static final String KEY_CHECK_OVERRIDDEN_COMMIT_MESSAGE = "overriddenCommitMessage";

  private final Provider<InternalChangeQuery> queryProvider;
  private final String pluginName;
  private final PluginConfigFactory cfgFactory;
  private final ValidatorConfig validatorConfig;

  @Inject
  OverriddenCommitMessageValidator(
      Provider<InternalChangeQuery> queryProvider,
      @PluginName String pluginName,
      PluginConfigFactory cfgFactory,
      ValidatorConfig validatorConfig) {
    this.queryProvider = queryProvider;
    this.pluginName = pluginName;
    this.cfgFactory = cfgFactory;
    this.validatorConfig = validatorConfig;
  }

  @Override
  public List<CommitValidationMessage> onCommitReceived(CommitReceivedEvent receiveEvent)
      throws CommitValidationException {
    try {
      if (!isValidatorEnabled(receiveEvent)) {
        return ImmutableList.of();
      }
    } catch (NoSuchProjectException e) {
      throw new CommitValidationException(
          "failed to check for OverriddenCommitMessageValidator enablement", e);
    }

    Optional<ChangeData> destChange = getDestChange(receiveEvent);
    if (destChange.isEmpty()) {
      // Cannot validate message overriding without a valid dest change.
      return ImmutableList.of();
    }

    int patchsetIdToUpload = destChange.get().currentPatchSet().number() + 1;
    if (patchsetIdToUpload <= 2) {
      // Overriding the message to a previous version requires at least 2 previous revisions.
      return ImmutableList.of();
    }

    String messageToUpload = receiveEvent.commit.getFullMessage();
    Optional<String> prevCommitMessage =
        getMessageForPatchset(receiveEvent.revWalk, destChange.get(), patchsetIdToUpload - 1);
    if (prevCommitMessage.isEmpty()) {
      // Could not parse message for the prev patchset. As this validator is not validating the
      // change integrity, fails quietly.
      return ImmutableList.of();
    }
    if (messageToUpload.equals(prevCommitMessage.get())) {
      // The message was not changed.
      return ImmutableList.of();
    }
    for (int oldPsId = patchsetIdToUpload - 2; oldPsId > 0; oldPsId--) {
      Optional<String> oldCommitMessage =
          getMessageForPatchset(receiveEvent.revWalk, destChange.get(), oldPsId);
      if (oldCommitMessage.isEmpty()) {
        // Cannot compare with this message, but maybe older messages are overridden to.
        continue;
      }
      if (messageToUpload.equals(oldCommitMessage.get())) {
        return ImmutableList.of(
            new CommitValidationMessage(
                String.format(
                    "This command will override the latest commit message (patchset [%d]) with"
                        + " the commit message of patchset [%d]. Did you update the message"
                        + " somewhere else and forgot to copy it over?",
                    patchsetIdToUpload - 1, oldPsId),
                ValidationMessage.Type.WARNING));
      }
    }

    // No previous patchset with identical message found.
    return ImmutableList.of();
  }

  static boolean isActive(PluginConfig cfg) {
    return cfg.getBoolean(KEY_CHECK_OVERRIDDEN_COMMIT_MESSAGE, false);
  }

  private boolean isValidatorEnabled(CommitReceivedEvent receiveEvent)
      throws NoSuchProjectException {
    PluginConfig cfg =
        cfgFactory.getFromProjectConfigWithInheritance(
            receiveEvent.project.getNameKey(), pluginName);
    return isActive(cfg)
        && validatorConfig.isEnabled(
            receiveEvent.user,
            receiveEvent.getProjectNameKey(),
            receiveEvent.getRefName(),
            KEY_CHECK_OVERRIDDEN_COMMIT_MESSAGE,
            receiveEvent.pushOptions);
  }

  private Optional<ChangeData> getDestChange(CommitReceivedEvent receiveEvent) {
    Optional<String> changeId =
        getChangeIdFromCommitMessageFooter(receiveEvent.commit.getFullMessage());
    if (changeId.isEmpty()) {
      // The only way in which we can determine the dest change is the commit message footer. When
      // unavailable, this validator cannot work. Do not warn the user as Change-Id footer is not a
      // requirement in all projects.
      return Optional.empty();
    }
    List<ChangeData> destChangesList =
        queryProvider
            .get()
            .setLimit(2)
            .byBranchKey(receiveEvent.getBranchNameKey(), Change.key(changeId.get()));
    if (destChangesList.size() != 1) {
      // Can't determine the dest change.
      return Optional.empty();
    }
    return Optional.of(destChangesList.get(0));
  }

  private Optional<String> getMessageForPatchset(
      RevWalk revWalk, ChangeData destChange, int patchSetId) {
    try {
      return Optional.of(
          revWalk
              .parseCommit(
                  destChange.patchSet(PatchSet.id(destChange.getId(), patchSetId)).commitId())
              .getFullMessage());
    } catch (IOException e) {
      logger.atWarning().withCause(e).log(
          "Could not parse commit for change: %d, patchset: %d",
          destChange.getId().get(), patchSetId);
    }
    return Optional.empty();
  }

  @Override
  public boolean shouldValidateAllCommits() {
    return false;
  }
}
