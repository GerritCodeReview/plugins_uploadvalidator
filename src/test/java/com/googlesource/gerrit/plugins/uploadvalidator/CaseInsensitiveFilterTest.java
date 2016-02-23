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
import static com.googlesource.gerrit.plugins.uploadvalidator.TestUtils.createEmptyDirCacheEntries;
import static com.googlesource.gerrit.plugins.uploadvalidator.TestUtils.makeCommit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CaseInsensitiveFilterTest extends ValidatorTestCase {
  private TestRepository<Repository> testRepo;

  @Override
  @Before
  public void init() throws IOException {
    super.init();
    testRepo = new TestRepository<>(repo);
  }

  private List<String> getVisitedTreePaths(List<String> existingTreePaths,
      String testPath) throws Exception {
    return getVisitedTreePaths(existingTreePaths, Sets.newHashSet(testPath));
  }

  private List<String> getVisitedTreePaths(List<String> existingTreePaths,
      Set<String> testPaths) throws Exception {
    List<String> found = new ArrayList<>();
    RevCommit c = makeCommit(
        createEmptyDirCacheEntries(existingTreePaths, testRepo), testRepo);
    try (TreeWalk tw = new TreeWalk(repo)) {
      CaseInsensitiveFilter f =
          new CaseInsensitiveFilter(testPaths, Locale.ENGLISH);
      tw.setFilter(f);
      tw.setRecursive(f.shouldBeRecursive());
      tw.addTree(c.getTree());
      while (tw.next()) {
        if (tw.isSubtree()) {
          tw.enterSubtree();
        }
        found.add(tw.getPathString());
      }
    }
    return found;
  }

  @Test
  public void testVisitOnlyPathsWithMatchingPrefix() throws Exception {
    List<String> INITIAL_PATHNAMES = ImmutableList.of(
        "a", "b",
        "folder1/a", "folder1/A",
        "Folder1/A", "Folder1/B",
        "Folder2/a");

    String testPath = "folder1/a";

    List<String> expected = ImmutableList.of(
        "folder1", "folder1/a", "folder1/A",
        "Folder1", "Folder1/A");

    List<String> result =
        getVisitedTreePaths(INITIAL_PATHNAMES, testPath);
    assertThat(result).containsExactlyElementsIn(expected);
  }

  @Test
  public void testVisitOnlyPathsWithMatchingPrefixAndDepth() throws Exception {
    List<String> INITIAL_PATHNAMES = ImmutableList.of(
        "folder1/a",
        "folder1/A",
        "Folder1/a/a");

    String testPath = "folder1/a";

    // It's important that "Folder1/a" is not included in results.
    // Folder1/a matches the prefix, but has a different depth.
    List<String> expected = ImmutableList.of(
        "folder1", "folder1/a", "folder1/A",
        "Folder1");

    List<String> result =
        getVisitedTreePaths(INITIAL_PATHNAMES, testPath);
    assertThat(result).containsExactlyElementsIn(expected);
  }
}
