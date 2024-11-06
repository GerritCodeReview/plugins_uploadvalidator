package com.googlesource.gerrit.plugins.uploadvalidator;

import static com.google.common.truth.Truth.assertThat;
import static com.google.gerrit.acceptance.testsuite.project.TestProjectUpdate.allow;
import static com.googlesource.gerrit.plugins.uploadvalidator.TestUtils.EMPTY_PLUGIN_CONFIG;

import com.google.gerrit.acceptance.GitUtil;
import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.PushOneCommit;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.acceptance.testsuite.project.ProjectOperations;
import com.google.gerrit.entities.Permission;
import com.google.gerrit.entities.RefNames;
import com.google.gerrit.server.group.SystemGroupBackend;
import com.google.inject.Inject;
import java.util.regex.Pattern;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.junit.TestRepository;
import org.junit.Before;
import org.junit.Test;

@TestPlugin(
    name = "uploadvalidator",
    sysModule = "com.googlesource.gerrit.plugins.uploadvalidator.Module")
public class OverriddenCommitMessageValidatorIT extends LightweightPluginDaemonTest {
  private static final Pattern SUCCESS_WITH_NO_WARNING_MESSAGES =
      Pattern.compile("Processing changes: refs: 1, (new|updated): 1, done\\s*SUCCESS");
  @Inject ProjectOperations projectOperations;

  @Before
  public void setup() throws Exception {
    projectOperations
        .project(allProjects)
        .forUpdate()
        .add(allow(Permission.READ).ref("refs/*").group(SystemGroupBackend.REGISTERED_USERS))
        .add(allow(Permission.CREATE).ref("refs/*").group(SystemGroupBackend.REGISTERED_USERS))
        .add(allow(Permission.PUSH).ref("refs/*").group(SystemGroupBackend.REGISTERED_USERS))
        .update();
    // Enable the validator
    pushConfig("[plugin \"uploadvalidator\"]\n" + "overriddenCommitMessage = true");
  }

  @Test
  public void messageIsOverridden_warn() throws Exception {
    String changeId = createChange("original message", "file", "content").getChangeId();
    amendChange(changeId, "revised message", "file2", "content");
    PushOneCommit.Result res = amendChange(changeId, "original message", "file3", "content");
    res.assertOkStatus();
    res.assertMessage("will override the latest commit message");
  }

  void pushConfig(String config) throws Exception {
    TestRepository<InMemoryRepository> repo = cloneProject(project, admin);
    GitUtil.fetch(repo, RefNames.REFS_CONFIG + ":config");
    repo.reset("config");
    PushOneCommit push =
        pushFactory.create(admin.newIdent(), repo, "Push config", "project.config", config);
    PushOneCommit.Result res = push.to(RefNames.REFS_CONFIG);
    res.assertOkStatus();
  }

  @Test
  public void differentMessages_doNothing() throws Exception {
    String changeId = createChange("original message", "file", "content").getChangeId();
    amendChange(changeId, "revised message", "file2", "content");
    PushOneCommit.Result res = amendChange(changeId, "yet another message", "file3", "content");
    assertNoWarnings(res);
  }

  @Test
  public void firstCommit_doNothing() throws Exception {
    PushOneCommit.Result res = createChange("original message", "file", "content");
    assertNoWarnings(res);
  }

  @Test
  public void secondCommit_doNothing() throws Exception {
    String changeId = createChange("original message", "file", "content").getChangeId();
    PushOneCommit.Result res = amendChange(changeId, "original message", "file2", "content");
    assertNoWarnings(res);
  }

  private void assertNoWarnings(PushOneCommit.Result res) {
    res.assertOkStatus();
    assertThat(res.getMessage()).containsMatch(SUCCESS_WITH_NO_WARNING_MESSAGES);
  }

  @Test
  public void emptyConfig_inactive() {
    assertThat(OverriddenCommitMessageValidator.isActive(EMPTY_PLUGIN_CONFIG)).isFalse();
  }
}
