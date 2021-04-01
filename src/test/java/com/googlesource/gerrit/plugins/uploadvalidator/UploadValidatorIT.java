package com.googlesource.gerrit.plugins.uploadvalidator;

import static com.google.common.truth.Truth.assertThat;
import static com.google.gerrit.acceptance.testsuite.project.TestProjectUpdate.allow;
import static com.google.gerrit.testing.GerritJUnit.assertThrows;

import com.google.common.collect.ImmutableMap;
import com.google.gerrit.acceptance.GitUtil;
import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.PushOneCommit;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.acceptance.testsuite.project.ProjectOperations;
import com.google.gerrit.entities.Permission;
import com.google.gerrit.entities.RefNames;
import com.google.gerrit.extensions.api.changes.DraftInput;
import com.google.gerrit.extensions.api.changes.ReviewInput;
import com.google.gerrit.extensions.api.changes.ReviewInput.DraftHandling;
import com.google.gerrit.extensions.common.ChangeInput;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.server.group.SystemGroupBackend;
import com.google.inject.Inject;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.junit.TestRepository;
import org.junit.Before;
import org.junit.Test;

@TestPlugin(
    name = "uploadvalidator",
    sysModule = "com.googlesource.gerrit.plugins.uploadvalidator.Module")
public class UploadValidatorIT extends LightweightPluginDaemonTest {

  @Inject
  ProjectOperations projectOperations;

  TestRepository<InMemoryRepository> clone;

  void pushConfig(String config) throws Exception {
    // This will trigger a configuration reload.
    TestRepository<InMemoryRepository> allProjectRepo = cloneProject(allProjects, admin);
    GitUtil.fetch(allProjectRepo, RefNames.REFS_CONFIG + ":config");
    allProjectRepo.reset("config");
    PushOneCommit push =
        pushFactory.create(
            admin.newIdent(), allProjectRepo, "Subject", "project.config", config);
    PushOneCommit.Result res = push.to("refs/meta/config");
    res.assertOkStatus();
  }

  @Before
  public void setup() throws Exception {
    pushConfig("[plugin \"uploadvalidator\"]\n" +
        "    blockedFileExtension = jar\n" +
        "    blockedFileExtension = .zip\n" +
        "    blockedKeywordPattern = secr3t\n" +
        "    invalidFilenamePattern = [%:@]\n" +
        "    rejectWindowsLineEndings = true\n" +
        "    maxPathLength = 20\n" +
        "    rejectDuplicatePathnames = true\n" +

        "");

    projectOperations.project(allProjects).forUpdate()
        .add(allow(Permission.READ).ref("refs/*").group(SystemGroupBackend.REGISTERED_USERS))
        .add(allow(Permission.CREATE).ref("refs/*").group(SystemGroupBackend.REGISTERED_USERS))
        .add(allow(Permission.PUSH).ref("refs/*").group(SystemGroupBackend.REGISTERED_USERS))
        .update();

    clone =
        GitUtil.cloneProject(project, registerRepoConnection(project, admin));
  }

  @Test
  public void testFileExtension() throws Exception {
    pushFactory
        .create(admin.newIdent(), clone, "Subject", "file.jar", "content")
        .to("refs/heads/master")
        .assertErrorStatus("blocked file extensions");

    pushFactory
        .create(admin.newIdent(), clone, "Subject", "file.zip", "content")
        .to("refs/heads/master")
        .assertErrorStatus("blocked file extensions");
  }

  @Test
  public void testKeywordComment() throws Exception {
    PushOneCommit.Result r1 = createChange("Subject", "file.txt", "content");
    DraftInput in = new DraftInput();
    in.message = "the password is secr3t ! ";
    in.path = "file.txt";
    gApi.changes().id(r1.getChangeId()).revision("current").createDraft(in);

    ReviewInput reviewIn = new ReviewInput();
    reviewIn.drafts = DraftHandling.PUBLISH;
    BadRequestException e = assertThrows(
        BadRequestException.class,
        () -> gApi.changes().id(r1.getChangeId()).revision("current").review(reviewIn)
    );
    assertThat(e.getMessage()).contains("banned words");
  }

  @Test
  public void testKeyword() throws Exception {
    pushFactory
        .create(admin.newIdent(), clone, "Subject", "file.txt", "blah secr3t blah")
        .to("refs/heads/master")
        .assertErrorStatus("blocked keywords");
  }
  @Test
  public void testFilenamePattern() throws Exception {
    pushFactory
        .create(admin.newIdent(), clone, "Subject", "f:le.txt", "content")
        .to("refs/heads/master")
        .assertErrorStatus("invalid filename");
  }

  @Test
  public void testWindowsLineEndings() throws Exception {
    pushFactory
        .create(admin.newIdent(), clone, "Subject", "win.ini",
            "content\r\nline2\r\n")
        .to("refs/heads/master")
        .assertErrorStatus("Windows line ending");
  }

  @Test
  public void testPathLength() throws Exception {
    pushFactory
        .create(admin.newIdent(), clone, "Subject",
            "123456789012345678901234567890.txt",
            "content\nline2\n")
        .to("refs/heads/master")
        .assertErrorStatus("too long paths");
  }

  @Test
  public void testUniqueName() throws Exception {
    pushFactory
        .create(admin.newIdent(), clone, "Subject",
            ImmutableMap.of("a.txt",
            "content\nline2\n",
            "A.TXT",
            "content")
        )
        .to("refs/heads/master")
        .assertErrorStatus("duplicate pathnames");
  }

}
