// Copyright (C) 2021 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;
import static com.google.gerrit.acceptance.testsuite.project.TestProjectUpdate.allow;
import static com.google.gerrit.testing.GerritJUnit.assertThrows;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
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
import com.google.gerrit.extensions.client.ChangeStatus;
import com.google.gerrit.extensions.common.ChangeInput;
import com.google.gerrit.extensions.common.MergeInput;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.server.group.SystemGroupBackend;
import com.google.inject.Inject;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Before;
import org.junit.Test;

@TestPlugin(
    name = "uploadvalidator",
    sysModule = "com.googlesource.gerrit.plugins.uploadvalidator.Module")
public class UploadValidatorIT extends LightweightPluginDaemonTest {

  @Inject ProjectOperations projectOperations;

  TestRepository<InMemoryRepository> clone;

  void pushConfig(String config) throws Exception {
    TestRepository<InMemoryRepository> repo = cloneProject(project, admin);
    GitUtil.fetch(repo, RefNames.REFS_CONFIG + ":config");
    repo.reset("config");
    PushOneCommit push =
        pushFactory.create(admin.newIdent(), repo, "Subject", "project.config", config);
    PushOneCommit.Result res = push.to(RefNames.REFS_CONFIG);
    res.assertOkStatus();
  }

  @Before
  public void setup() throws Exception {
    projectOperations
        .project(allProjects)
        .forUpdate()
        .add(allow(Permission.READ).ref("refs/*").group(SystemGroupBackend.REGISTERED_USERS))
        .add(allow(Permission.CREATE).ref("refs/*").group(SystemGroupBackend.REGISTERED_USERS))
        .add(allow(Permission.PUSH).ref("refs/*").group(SystemGroupBackend.REGISTERED_USERS))
        .update();

    clone = GitUtil.cloneProject(project, registerRepoConnection(project, admin));
  }

  @Test
  public void fileExtension() throws Exception {
    pushConfig(
        Joiner.on("\n")
            .join(
                "[plugin \"uploadvalidator\"]",
                "    blockedFileExtension = jar",
                "    blockedFileExtension = .zip"));
    RevCommit head = getHead(testRepo.getRepository(), "HEAD");
    pushFactory
        .create(admin.newIdent(), clone, "Subject", "file.jar", "content")
        .to("refs/heads/master")
        .assertErrorStatus("blocked file extensions");

    clone.reset(head);
    pushFactory
        .create(admin.newIdent(), clone, "Subject", "file.zip", "content")
        .to("refs/heads/master")
        .assertErrorStatus("blocked file extensions");

    clone.reset(head);
    pushFactory
        .create(admin.newIdent(), clone, "Subject", "file.txt", "content")
        .to("refs/heads/master")
        .assertOkStatus();
  }

  @Test
  public void keywordInComment() throws Exception {
    pushConfig(
        Joiner.on("\n").join("[plugin \"uploadvalidator\"]", "    blockedKeywordPattern = secr3t"));

    PushOneCommit.Result r1 = createChange("Subject", "file.txt", "content");
    DraftInput in = new DraftInput();
    in.message = "the password is secr3t ! ";
    in.path = "file.txt";
    gApi.changes().id(r1.getChangeId()).revision("current").createDraft(in);

    ReviewInput reviewIn = new ReviewInput();
    reviewIn.drafts = DraftHandling.PUBLISH;
    BadRequestException e =
        assertThrows(
            BadRequestException.class,
            () -> gApi.changes().id(r1.getChangeId()).revision("current").review(reviewIn));
    assertThat(e.getMessage()).contains("banned words");
  }

  @Test
  public void keywordInNewFile() throws Exception {
    pushConfig(
        Joiner.on("\n").join("[plugin \"uploadvalidator\"]", "    blockedKeywordPattern = secr3t"));

    pushFactory
        .create(admin.newIdent(), clone, "Subject", "file.txt", "blah secr3t blah")
        .to("refs/heads/master")
        .assertErrorStatus("blocked keywords");
  }

  @Test
  public void filenamePattern() throws Exception {
    pushConfig(
        Joiner.on("\n").join("[plugin \"uploadvalidator\"]", "    invalidFilenamePattern = [%:@]"));

    RevCommit head = getHead(testRepo.getRepository(), "HEAD");
    pushFactory
        .create(admin.newIdent(), clone, "Subject", "f:le.txt", "content")
        .to("refs/heads/master")
        .assertErrorStatus("invalid filename");

    clone.reset(head);
    pushFactory
        .create(admin.newIdent(), clone, "Subject", "file.txt", "content")
        .to("refs/heads/master")
        .assertOkStatus();
  }

  @Test
  public void windowsLineEndings() throws Exception {
    pushConfig(
        Joiner.on("\n")
            .join("[plugin \"uploadvalidator\"]", "    rejectWindowsLineEndings = true"));

    RevCommit head = getHead(testRepo.getRepository(), "HEAD");
    pushFactory
        .create(admin.newIdent(), clone, "Subject", "win.ini", "content\r\nline2\r\n")
        .to("refs/heads/master")
        .assertErrorStatus("Windows line ending");

    clone.reset(head);
    pushFactory
        .create(admin.newIdent(), clone, "Subject", "file.txt", "content\nline2\n")
        .to("refs/heads/master")
        .assertOkStatus();
  }

  @Test
  public void pathLength() throws Exception {
    pushConfig(Joiner.on("\n").join("[plugin \"uploadvalidator\"]", "    maxPathLength = 20"));

    RevCommit head = getHead(testRepo.getRepository(), "HEAD");
    pushFactory
        .create(
            admin.newIdent(),
            clone,
            "Subject",
            "123456789012345678901234567890.txt",
            "content\nline2\n")
        .to("refs/heads/master")
        .assertErrorStatus("too long paths");

    clone.reset(head);
    pushFactory
        .create(admin.newIdent(), clone, "Subject", "file.txt", "content\nline2\n")
        .to("refs/heads/master")
        .assertOkStatus();
  }

  @Test
  public void uniqueName() throws Exception {
    pushConfig(
        Joiner.on("\n")
            .join("[plugin \"uploadvalidator\"]", "    rejectDuplicatePathnames = true"));

    pushFactory
        .create(
            admin.newIdent(),
            clone,
            "Subject",
            ImmutableMap.of("a.txt", "content\nline2\n", "A.TXT", "content"))
        .to("refs/heads/master")
        .assertErrorStatus("duplicate pathnames");
  }

  @Test
  public void rulesNotEnforcedForNonGroupMembers() throws Exception {
    pushConfig(
        Joiner.on("\n")
            .join(
                "[plugin \"uploadvalidator\"]",
                "    group = " + adminGroupUuid(),
                "    rejectDuplicatePathnames = true"));

    TestRepository<InMemoryRepository> userClone =
        GitUtil.cloneProject(project, registerRepoConnection(project, user));
    pushFactory
        .create(
            user.newIdent(),
            userClone,
            "Subject",
            ImmutableMap.of("a.txt", "content\nline2\n", "A.TXT", "content"))
        .to("refs/heads/master")
        .assertOkStatus();
  }

  @Test
  public void rulesNotEnforcedForSkipPushOption() throws Exception {
    pushConfig(
        Joiner.on("\n")
            .join(
                "[plugin \"uploadvalidator\"]",
                "    skipViaPushOption = true",
                "    rejectDuplicatePathnames = true"));

    PushOneCommit push =
        pushFactory.create(
            admin.newIdent(),
            clone,
            "Subject",
            ImmutableMap.of("a.txt", "content\nline2\n", "A.TXT", "content"));
    push.setPushOptions(ImmutableList.of("uploadvalidator~skip"));
    push.to("refs/heads/master").assertOkStatus();
  }

  @Test
  public void keywordExistsInFileButNotInDiff() throws Exception {
    pushFactory
        .create(
            admin.newIdent(),
            clone,
            "Subject",
            "file.txt",
            "" + "blah \n" + "secr3t\n" + "blah" + "")
        .to("refs/heads/master")
        .assertOkStatus();
    pushConfig(
        Joiner.on("\n").join("[plugin \"uploadvalidator\"]", "    blockedKeywordPattern = secr3t"));
    pushFactory
        .create(
            admin.newIdent(),
            clone,
            "Subject",
            "file.txt",
            "" + "blah \n" + "secr3t\n" + "foo" + "")
        .to("refs/heads/master")
        .assertOkStatus();
  }

  @Test
  public void keywordExistsInNewFile() throws Exception {
    pushConfig(
        Joiner.on("\n").join("[plugin \"uploadvalidator\"]", "    blockedKeywordPattern = secr3t"));
    pushFactory
        .create(
            admin.newIdent(),
            clone,
            "Subject",
            "file.txt",
            "" + "blah \n" + "secr3t\n" + "foo\n" + "secr3t")
        .to("refs/heads/master")
        .assertErrorStatus("blocked keywords");
  }

  @Test
  public void keywordExistsInFileAndInDiff() throws Exception {
    pushFactory
        .create(
            admin.newIdent(),
            clone,
            "Subject",
            "file.txt",
            "" + "blah \n" + "secr3t\n" + "blah" + "")
        .to("refs/heads/master")
        .assertOkStatus();
    pushConfig(
        Joiner.on("\n").join("[plugin \"uploadvalidator\"]", "    blockedKeywordPattern = secr3t"));
    pushFactory
        .create(
            admin.newIdent(),
            clone,
            "Subject",
            "file.txt",
            "" + "blah \n" + "secr3t\n" + "blah\n" + "secr3t")
        .to("refs/heads/master")
        .assertErrorStatus("blocked keywords");
  }

  @Test
  public void keywordExistsInFileAndIsRemoved() throws Exception {
    pushFactory
        .create(
            admin.newIdent(), clone, "Subject", "file.txt", "" + "blah \n" + "secr3t\n" + "blah")
        .to("refs/heads/master")
        .assertOkStatus();
    pushConfig(
        Joiner.on("\n").join("[plugin \"uploadvalidator\"]", "    blockedKeywordPattern = secr3t"));
    pushFactory
        .create(admin.newIdent(), clone, "Subject", "file.txt", "" + "blah \n" + "blah\n")
        .to("refs/heads/master")
        .assertOkStatus();
  }

  @Test
  public void keywordExistsInFileAndSameLineIsModified() throws Exception {
    pushFactory
        .create(
            admin.newIdent(),
            clone,
            "Subject",
            "file.txt",
            "" + "blah \n" + "secr3t\n" + "blah" + "")
        .to("refs/heads/master")
        .assertOkStatus();
    pushConfig(
        Joiner.on("\n").join("[plugin \"uploadvalidator\"]", "    blockedKeywordPattern = secr3t"));
    // This could be further improved using intra-line diffs
    pushFactory
        .create(
            admin.newIdent(),
            clone,
            "Subject",
            "file.txt",
            "" + "blah \n" + "secr3t foobar\n" + "blah" + "")
        .to("refs/heads/master")
        .assertErrorStatus("blocked keywords");
  }

  @Test
  public void keywordExistsInOldAndFileIsDeleted() throws Exception {
    pushFactory
        .create(
            admin.newIdent(),
            clone,
            "Subject",
            "file.txt",
            "" + "blah \n" + "secr3t\n" + "blah" + "")
        .to("refs/heads/master")
        .assertOkStatus();
    pushConfig(
        Joiner.on("\n").join("[plugin \"uploadvalidator\"]", "    blockedKeywordPattern = secr3t"));
    pushFactory
        .create(admin.newIdent(), clone, "Subject", "foo.txt", "blah")
        .rmFile("file.txt")
        .to("refs/heads/master")
        .assertOkStatus();
  }

  @Test
  public void createChangeSucceedsWhenKeywordDoesNotExistInFileAndDiff() throws Exception {
    RevCommit head = getHead(testRepo.getRepository(), "HEAD");
    pushConfig(
        Joiner.on("\n").join("[plugin \"uploadvalidator\"]", "    blockedKeywordPattern = secr3t"));
    pushFactory
        .create(admin.newIdent(), clone, "Subject", "file.txt", "" + "blah \n")
        .to("refs/heads/master")
        .assertOkStatus();
    clone.reset(head);
    pushFactory
        .create(admin.newIdent(), clone, "Subject", "foo.txt", "" + "blah \n")
        .to("refs/heads/stable")
        .assertOkStatus();
    // Create a change that merges the other branch into master. This defaults back to
    // full-file validation. If it doesn't, the create change call below would fail with
    // a MissingObjectException.
    ChangeInput changeInput = new ChangeInput();
    changeInput.project = project.get();
    changeInput.branch = "master";
    changeInput.subject = "A change";
    changeInput.status = ChangeStatus.NEW;
    MergeInput mergeInput = new MergeInput();
    mergeInput.source = gApi.projects().name(project.get()).branch("stable").get().revision;
    changeInput.merge = mergeInput;
    gApi.changes().create(changeInput);
  }

  @Test
  public void createChangeSucceedsWhenKeywordExistsInFileButNotInDiff() throws Exception {
    RevCommit head = getHead(testRepo.getRepository(), "HEAD");
    pushFactory
        .create(admin.newIdent(), clone, "Subject", "file.txt", "" + "secr3t \n")
        .to("refs/heads/master")
        .assertOkStatus();
    pushConfig(
        Joiner.on("\n").join("[plugin \"uploadvalidator\"]", "    blockedKeywordPattern = secr3t"));
    clone.reset(head);
    pushFactory
        .create(admin.newIdent(), clone, "Subject", "foo.txt", "" + "blah \n")
        .to("refs/heads/stable")
        .assertOkStatus();
    // Create a change that merges the other branch into master. This defaults back to
    // full-file validation. If it doesn't, the create change call below would fail with
    // a MissingObjectException.
    ChangeInput changeInput = new ChangeInput();
    changeInput.project = project.get();
    changeInput.branch = "master";
    changeInput.subject = "A change";
    changeInput.status = ChangeStatus.NEW;
    MergeInput mergeInput = new MergeInput();
    mergeInput.source = gApi.projects().name(project.get()).branch("stable").get().revision;
    changeInput.merge = mergeInput;
    gApi.changes().create(changeInput);
  }

  @Test
  public void createChangeWithKeywordInMessageFails() throws Exception {
    RevCommit head = getHead(testRepo.getRepository(), "HEAD");
    pushConfig(
        Joiner.on("\n").join("[plugin \"uploadvalidator\"]", "    blockedKeywordPattern = secr3t"));
    pushFactory
        .create(admin.newIdent(), clone, "Subject", "file.txt", "" + "blah \n")
        .to("refs/heads/master")
        .assertOkStatus();
    clone.reset(head);
    pushFactory
        .create(admin.newIdent(), clone, "Subject", "foo.txt", "" + "blah \n")
        .to("refs/heads/stable")
        .assertOkStatus();
    // Create a change that merges the other branch into master. This defaults back to
    // full-file validation. If it doesn't, the create change call below would fail with
    // a MissingObjectException.
    ChangeInput changeInput = new ChangeInput();
    changeInput.project = project.get();
    changeInput.branch = "master";
    changeInput.subject = "A secr3t change";
    changeInput.status = ChangeStatus.NEW;
    MergeInput mergeInput = new MergeInput();
    mergeInput.source = gApi.projects().name(project.get()).branch("stable").get().revision;
    changeInput.merge = mergeInput;
    ResourceConflictException e =
        assertThrows(ResourceConflictException.class, () -> gApi.changes().create(changeInput));
    assertThat(e).hasMessageThat().contains("blocked keyword(s) found");
  }
}
