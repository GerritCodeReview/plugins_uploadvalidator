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
import static com.googlesource.gerrit.plugins.uploadvalidator.DuplicatePathnameValidator.conflict;
import static com.googlesource.gerrit.plugins.uploadvalidator.TestUtils.EMPTY_CONTENT;
import static com.googlesource.gerrit.plugins.uploadvalidator.TestUtils.EMPTY_PLUGIN_CONFIG;
import static com.googlesource.gerrit.plugins.uploadvalidator.TestUtils.createDirCacheEntry;
import static com.googlesource.gerrit.plugins.uploadvalidator.TestUtils.createEmptyDirCacheEntries;
import static com.googlesource.gerrit.plugins.uploadvalidator.TestUtils.makeCommit;
import static com.googlesource.gerrit.plugins.uploadvalidator.TestUtils.transformMessage;
import static com.googlesource.gerrit.plugins.uploadvalidator.TestUtils.transformMessages;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gerrit.server.git.validators.CommitValidationMessage;

import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class DuplicatePathnameValidatorTest extends ValidatorTestCase {
  private static final List<String> INITIAL_PATHNAMES = ImmutableList.of(
      "a" , "ab",
      "folder1/a", "folder1/ab",
      "folder2/a", "folder2/ab", "folder2/subFolder1/a", "folder2/subFolder1/ab");

  private TestRepository<Repository> testRepo;
  private List<String> vistedPaths = Lists.newArrayList();
  private List<CommitValidationMessage> messages = Lists.newArrayList();
  private List<String> changedPaths;
  private DuplicatePathnameValidator validator;

  private void runCheck(List<String> existingTreePaths, List<String> testPaths,
      List<CommitValidationMessage> messages, List<String> visitedPaths)
          throws Exception {
    RevCommit c = makeCommit(
        createEmptyDirCacheEntries(existingTreePaths, testRepo), testRepo);
    try (TreeWalk tw = new TreeWalk(repo)) {
      tw.setRecursive(false);
      tw.addTree(c.getTree());
      tw.setFilter(new ListVisitedPathsFilter(visitedPaths));
      validator.checkForDuplicatesAgainstTheWholeTree(tw, testPaths, messages);
    }
  }

  @Override
  @Before
  public void init() throws IOException {
    super.init();
    testRepo = new TestRepository<>(repo);
    validator = new DuplicatePathnameValidator(null, null, null);
    validator.setLocale(Locale.ENGLISH);
  }

  @Test
  public void testSkipSubTreesWithImproperPrefix() throws Exception {
    changedPaths = ImmutableList.of("folder1/A");
    runCheck(INITIAL_PATHNAMES, changedPaths, messages, vistedPaths);
    assertThat(transformMessages(messages))
        .containsExactly(transformMessage(conflict("folder1/A", "folder1/a")));
    assertThat(vistedPaths).containsExactlyElementsIn(ImmutableList.of("a",
        "ab", "folder1", "folder1/a", "folder1/ab", "folder2"));
  }

  @Test
  public void testFindConflictingSubtree() throws Exception {
    changedPaths = ImmutableList.of("Folder1/a");
    runCheck(INITIAL_PATHNAMES, changedPaths, messages, vistedPaths);
    assertThat(transformMessages(messages))
        .containsExactly(transformMessage(conflict("Folder1", "folder1")));
    assertThat(vistedPaths).containsExactlyElementsIn(
        ImmutableList.of("a", "ab", "folder1", "folder2"));
  }

  @Test
  public void testFindConflictingSubtree2() throws Exception {
    changedPaths = ImmutableList.of("folder2/subfolder1", "Folder2/a");
    runCheck(INITIAL_PATHNAMES, changedPaths, messages, vistedPaths);
    assertThat(transformMessages(messages)).containsExactly(
        transformMessage(conflict("Folder2/a", "folder2/a")),
        transformMessage(conflict("folder2/subfolder1", "folder2/subFolder1")));
    assertThat(vistedPaths)
        .containsExactlyElementsIn(ImmutableList.of("a", "ab", "folder1",
            "folder2", "folder2/a", "folder2/ab", "folder2/subFolder1"));
  }

  @Test
  public void testFindDuplicates() throws Exception {
    changedPaths = ImmutableList.of("AB", "folder1/A", "folder2/Ab");
    runCheck(INITIAL_PATHNAMES, changedPaths, messages, vistedPaths);
    assertThat(transformMessages(messages)).containsExactly(
        transformMessage(conflict("AB", "ab")),
        transformMessage(conflict("folder1/A", "folder1/a")),
        transformMessage(conflict("folder2/Ab", "folder2/ab")));
    assertThat(vistedPaths).containsExactlyElementsIn(
        ImmutableList.of("a", "ab", "folder1", "folder1/a", "folder1/ab",
            "folder2", "folder2/a", "folder2/ab", "folder2/subFolder1"));
  }

  @Test
  public void testFindNoDuplicates() throws Exception {
    changedPaths = ImmutableList.of("a", "ab", "folder1/ab");
    runCheck(INITIAL_PATHNAMES, changedPaths, messages, vistedPaths);
    assertThat(messages).isEmpty();
    assertThat(vistedPaths).containsExactlyElementsIn(ImmutableList.of("a",
        "ab", "folder1", "folder1/a", "folder1/ab", "folder2"));
  }

  @Test
  public void testCheckInsideOfCommit() throws Exception {
    List<String> filenames = Lists.newArrayList(INITIAL_PATHNAMES);
    // add files with conflicting pathnames
    filenames.add("A");
    filenames.add("Folder1/ab");
    filenames.add("folder2/subFolder1/aB");
    RevCommit c =
        makeCommit(createEmptyDirCacheEntries(filenames, testRepo), testRepo);
    List<CommitValidationMessage> m = validator.performValidation(repo, c);
    assertThat(m).hasSize(4);
    // During checking inside of the commit it's unknown which file is checked
    // first, because of that, both capabilities must be checked.
    assertThat(transformMessages(m)).containsAnyOf(
        transformMessage(conflict("A", "a")),
        transformMessage(conflict("a", "A")));

    assertThat(transformMessages(m)).containsAnyOf(
        transformMessage(conflict("Folder1", "folder1")),
        transformMessage(conflict("folder1", "Folder1")));

    assertThat(transformMessages(m)).containsAnyOf(
        transformMessage(conflict("Folder1/ab", "folder1/ab")),
        transformMessage(conflict("folder1/ab", "Folder1/ab")));

    assertThat(transformMessages(m)).containsAnyOf(
        transformMessage(
            conflict("folder2/subFolder1/aB", "folder2/subFolder1/ab")),
        transformMessage(
            conflict("folder2/subFolder1/ab", "folder2/subFolder1/aB")));
  }

  @Test
  public void testCheckRenaming() throws Exception {
    RevCommit c = makeCommit(
        createEmptyDirCacheEntries(INITIAL_PATHNAMES, testRepo), testRepo);
    DirCacheEntry[] entries = new DirCacheEntry[INITIAL_PATHNAMES.size()];
    for (int x = 0; x < INITIAL_PATHNAMES.size(); x++) {
      // Rename files
      entries[x] = createDirCacheEntry(INITIAL_PATHNAMES.get(x).toUpperCase(),
          EMPTY_CONTENT, testRepo);
    }
    RevCommit c1 = makeCommit(entries, testRepo, c);
    List<CommitValidationMessage> m = validator.performValidation(repo, c1);
    assertThat(m).isEmpty();
  }

  @Test
  public void validatorInactiveWhenConfigEmpty() {
    assertThat(DuplicatePathnameValidator.isActive(EMPTY_PLUGIN_CONFIG))
        .isFalse();
  }

  @Test
  public void defaultLocale() {
    assertThat(DuplicatePathnameValidator.getLocale(EMPTY_PLUGIN_CONFIG))
        .isEqualTo(Locale.ENGLISH);
  }

  @Test
  public void testGetParentFolder() {
    assertThat(DuplicatePathnameValidator.allParentFolders(INITIAL_PATHNAMES))
        .containsExactlyElementsIn(
            ImmutableList.of("folder1", "folder2", "folder2/subFolder1"));
  }
}
