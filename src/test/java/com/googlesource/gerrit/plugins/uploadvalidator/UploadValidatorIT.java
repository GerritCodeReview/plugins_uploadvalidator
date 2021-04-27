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
import com.google.gerrit.extensions.restapi.BadRequestException;
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
  public void testRulesNotEnforcedForNonGroupMembers() throws Exception {
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
  public void testRulesNotEnforcedForSkipPushOption() throws Exception {
    PushOneCommit push =
        pushFactory.create(
            admin.newIdent(),
            clone,
            "Subject",
            ImmutableMap.of("a.txt", "content\nline2\n", "A.TXT", "content"));
    push.setPushOptions(ImmutableList.of("uploadvalidator~skip"));
    push.to("refs/heads/master").assertOkStatus();
  }
}
