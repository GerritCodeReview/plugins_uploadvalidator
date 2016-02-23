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

import static com.google.common.truth.Truth.assertThat;
import static com.googlesource.gerrit.plugins.uploadvalidator.TestUtils.EMPTY_PLUGIN_CONFIG;

import com.google.common.collect.ImmutableSet;
import com.google.gerrit.server.git.validators.CommitValidationMessage;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DuplicatePathnameValidatorTest extends ValidatorTestCase {
  private Set<File> getInitialFiles() {
    Set<File> files = new HashSet<>();
    files.add(TestUtils.createEmptyFile("foobar", repo));
    files.add(TestUtils.createEmptyFile("folder1/foo", repo));
    files.add(TestUtils.createEmptyFile("folder1/bar", repo));
    files.add(TestUtils.createEmptyFile("folder1/f2/foo", repo));
    files.add(TestUtils.createEmptyFile("folder1/f2/bar", repo));
    return files;
  }

  @Test
  public void testCheckInsideOfCommit() throws Exception {
    Set<File> files = getInitialFiles();
    // add files with duplicate names
    files.add(TestUtils.createEmptyFile("fooBar", repo));
    files.add(TestUtils.createEmptyFile("folder1/bAr", repo));
    files.add(TestUtils.createEmptyFile("folDer1/f2/foo", repo));
    files.add(TestUtils.createEmptyFile("folder1/F2/bar", repo));
    RevCommit c =
        TestUtils.makeCommit(repo, "Commit with empty test files.", files);
    List<CommitValidationMessage> m =
        DuplicatePathnameValidator.performValidation(repo, c, Locale.ENGLISH);
    // During checking inside of the commit it's unknown which file is checked
    // first, because of that, both capabilities must be checked.
    assertThat(TestUtils.transformMessages(m)).containsAnyOf(
        "ERROR: fooBar: pathname collides with foobar",
        "ERROR: foobar: pathname collides with fooBar");

    assertThat(TestUtils.transformMessages(m)).containsAnyOf(
        "ERROR: folder1/bAr: pathname collides with folder1/bar",
        "ERROR: folder1/bar: pathname collides with folder1/bAr");

    assertThat(TestUtils.transformMessages(m)).containsAnyOf(
        "ERROR: folDer1/f2/foo: pathname collides with folder1/f2/foo",
        "ERROR: folder1/f2/foo: pathname collides with folDer1/f2/foo");

    assertThat(TestUtils.transformMessages(m)).containsAnyOf(
        "ERROR: folder1/F2/bar: pathname collides with folder1/f2/bar",
        "ERROR: folder1/f2/bar: pathname collides with folder1/F2/bar");
  }

  private RevCommit makeCommitWithDuplicates()
      throws NoFilepatternException, IOException, GitAPIException {
    Map<File, byte[]> files = new HashMap<>();
    // content of file has changed
    files.put(TestUtils.createEmptyFile("foobar", repo),
        "Test".getBytes(StandardCharsets.UTF_8));
    // duplicate pathnames
    files.put(TestUtils.createEmptyFile("folder1/Foo", repo), null);
    files.put(TestUtils.createEmptyFile("foldeR1/bar", repo), null);
    files.put(TestUtils.createEmptyFile("folder1/F2/foo", repo), null);
    files.put(TestUtils.createEmptyFile("folder1/f2/bAr", repo), null);
    // valid new pathnames
    files.put(TestUtils.createEmptyFile("folder1/f2/valid", repo), null);
    files.put(TestUtils.createEmptyFile("valid", repo), null);
    files.put(TestUtils.createEmptyFile("folder1/valid/foo", repo), null);
    return TestUtils.makeCommit(repo, "Commit with empty test files.", files);
  }

  @Test
  public void testCheckAgainstWholeTree() throws Exception {
    TestUtils.makeCommit(repo, "Commit with empty test files.", getInitialFiles());
    RevCommit c = makeCommitWithDuplicates();
    TestUtils.parseHeadersOfParentCommits(repo, c);
    List<CommitValidationMessage> m =
        DuplicatePathnameValidator.performValidation(repo, c, Locale.ENGLISH);
    Set<String> expected = ImmutableSet.of(
        "ERROR: folder1/Foo: pathname collides with folder1/foo",
        "ERROR: foldeR1/bar: pathname collides with folder1/bar",
        "ERROR: folder1/F2/foo: pathname collides with folder1/f2/foo",
        "ERROR: folder1/f2/bAr: pathname collides with folder1/f2/bar");
    assertThat(TestUtils.transformMessages(m)).containsExactlyElementsIn(expected);
  }

  private RevCommit doRename()
      throws NoFilepatternException, GitAPIException, IOException {
    Set<File> files = new HashSet<>();
    try (Git git = new Git(repo)) {
      files.add(TestUtils.createEmptyFile("Foo", repo));
      TestUtils.removeFiles(git, files);
      files.clear();
      files.add(TestUtils.createEmptyFile("foo", repo));
      TestUtils.addEmptyFiles(git, files);
      return git.commit().setMessage("Renaming").call();
    }
  }

  @Test
  public void testCheckRenaming() throws Exception {
    Set<File> files = new HashSet<>();
    files.add(TestUtils.createEmptyFile("Foo", repo));
    TestUtils.makeCommit(repo, "Add one file", files);
    RevCommit c = doRename();
    TestUtils.parseHeadersOfParentCommits(repo, c);
    List<CommitValidationMessage> m =
        DuplicatePathnameValidator.performValidation(repo, c, Locale.ENGLISH);
    assertThat(m).isEmpty();
  }

  @Test
  public void validatorInactiveWhenConfigEmpty() {
    assertThat(DuplicatePathnameValidator.isActive(EMPTY_PLUGIN_CONFIG)).isFalse();
  }

  @Test
  public void defaultLocale() {
    assertThat(DuplicatePathnameValidator.getLocale(EMPTY_PLUGIN_CONFIG))
        .isEqualTo(Locale.ENGLISH);
  }
}
