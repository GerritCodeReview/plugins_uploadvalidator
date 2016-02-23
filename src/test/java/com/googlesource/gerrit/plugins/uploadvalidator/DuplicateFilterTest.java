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
import static com.googlesource.gerrit.plugins.uploadvalidator.DuplicateFilter.getAllParentFolders;
import static com.googlesource.gerrit.plugins.uploadvalidator.TestUtils.createEmptyDirCacheEntries;
import static com.googlesource.gerrit.plugins.uploadvalidator.TestUtils.makeCommit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DuplicateFilterTest extends ValidatorTestCase {
  private static final Locale LOCALE = Locale.ENGLISH;
  private static final List<String> INITIAL_PATHNAMES = ImmutableList.of(
      "a" , "ab",
      "Folder1/a", "Folder1/ab",
      "Folder2/a", "Folder2/ab", "Folder2/subFolder1/a", "Folder2/subFolder1/ab");

  private TestRepository<Repository> testRepo;
  private List<String> vistedPaths = Lists.newArrayList();
  private List<String> duplicates = Lists.newArrayList();
  private List<String> changedPaths;

  @Override
  @Before
  public void init() throws IOException {
    super.init();
    testRepo = new TestRepository<>(repo);
  }

  private void runFilter(List<String> existingTreePaths, List<String> testPaths,
      List<String> duplicates, List<String> visitedPaths) throws Exception {
    RevCommit c = makeCommit(
        createEmptyDirCacheEntries(existingTreePaths, testRepo), testRepo);
    try (TreeWalk tw = new TreeWalk(repo)) {
      Map<String, String> folders = getAllParentFolders(testPaths, LOCALE);
      tw.setFilter(
          AndTreeFilter.create(new ListVisitedPathsFilter(visitedPaths),
              new DuplicateFilter(testPaths, folders.values(), LOCALE)));
      tw.setRecursive(false);
      tw.addTree(c.getTree());
      while (tw.next()) {
        duplicates.add(tw.getPathString());
      }
    }
  }

  @Test
  public void testSkipSubTreesWithImproperPrefix() throws Exception {
    changedPaths = ImmutableList.of("Folder1/A");
    runFilter(INITIAL_PATHNAMES, changedPaths, duplicates, vistedPaths);
    assertThat(duplicates).containsExactly("Folder1/a");
    assertThat(vistedPaths).containsExactlyElementsIn(ImmutableList.of("a",
        "ab", "Folder1", "Folder1/a", "Folder1/ab", "Folder2"));
  }

  @Test
  public void testFindConflictingSubtree() throws Exception {
    changedPaths = ImmutableList.of("folder1/a");
    runFilter(INITIAL_PATHNAMES, changedPaths, duplicates, vistedPaths);
    assertThat(duplicates).containsExactly("Folder1");
    assertThat(vistedPaths).containsExactlyElementsIn(
        ImmutableList.of("a", "ab", "Folder1", "Folder2"));
  }

  @Test
  public void testFindConflictingSubtree2() throws Exception {
    changedPaths = ImmutableList.of("Folder2/subfolder1", "folder2/a");
    runFilter(INITIAL_PATHNAMES, changedPaths, duplicates, vistedPaths);
    assertThat(duplicates).containsExactly("Folder2/a", "Folder2/subFolder1");
    assertThat(vistedPaths).containsExactlyElementsIn(
        ImmutableList.of("a", "ab", "Folder1", "Folder2", "Folder2/a",
            "Folder2/ab", "Folder2/subFolder1"));
  }

  @Test
  public void testFindDuplicates() throws Exception {
    changedPaths = ImmutableList.of("AB", "Folder1/A", "Folder2/Ab");
    runFilter(INITIAL_PATHNAMES, changedPaths, duplicates, vistedPaths);
    assertThat(duplicates).containsExactlyElementsIn(
        ImmutableList.of("ab", "Folder1/a", "Folder2/ab"));
    assertThat(vistedPaths).containsExactlyElementsIn(
        ImmutableList.of("a", "ab", "Folder1", "Folder1/a", "Folder1/ab",
            "Folder2", "Folder2/a", "Folder2/ab", "Folder2/subFolder1"));
  }

  @Test
  public void testFindNoDuplicates() throws Exception {
    changedPaths = ImmutableList.of("a", "ab", "Folder1/ab");
    runFilter(INITIAL_PATHNAMES, changedPaths, duplicates, vistedPaths);
    assertThat(duplicates).isEmpty();
    assertThat(vistedPaths).containsExactlyElementsIn(ImmutableList.of("a",
        "ab", "Folder1", "Folder1/a", "Folder1/ab", "Folder2"));
  }

  @Test
  public void testGetParentFolder() {
    assertThat(getAllParentFolders(INITIAL_PATHNAMES, LOCALE))
        .containsExactlyEntriesIn(ImmutableMap.of("folder1", "Folder1",
            "folder2", "Folder2", "folder2/subfolder1", "Folder2/subFolder1"));
  }
}
