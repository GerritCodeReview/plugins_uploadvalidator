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

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class CommitUtilsTest extends BareRepositoryTestCase {
  private static final List<String> INITIAL_PATHNAMES =
      ImmutableList.of("a", "b", "c", "d");
  private static final List<String> MORE_PATHNAMES =
      ImmutableList.of("e", "f", "g", "h");
  private static final byte[] CONTENT_NOT_EMPTY =
      "notEmpty".getBytes(Charsets.UTF_8);

  @Test
  public void testChangedPathsWithSingleCommit() throws Exception {
    RevCommit c = makeCommit(createEmptyDirCacheEntries(INITIAL_PATHNAMES));
    Set<String> changedPaths = CommitUtils.getChangedPaths(repo, c);
    assertThat(changedPaths).containsExactlyElementsIn(INITIAL_PATHNAMES);
  }

  @Test
  public void testChangedPathWithParentCommit() throws Exception {
    RevCommit c = makeCommit(createEmptyDirCacheEntries(INITIAL_PATHNAMES));
    List<String> tempPaths = Lists.newArrayList(INITIAL_PATHNAMES);
    tempPaths.addAll(MORE_PATHNAMES);
    RevCommit c1 = makeCommit(createEmptyDirCacheEntries(tempPaths), c);
    Set<String> changedPaths = CommitUtils.getChangedPaths(repo, c1);
    assertThat(changedPaths).containsExactlyElementsIn(MORE_PATHNAMES);
  }

  @Test
  public void testChangedPathWithDeletedFiles() throws Exception {
    RevCommit c = makeCommit(createEmptyDirCacheEntries(INITIAL_PATHNAMES));
    // Delete "c" and "d"
    RevCommit c1 = makeCommit(
        createEmptyDirCacheEntries(INITIAL_PATHNAMES.subList(0, 2)), c);
    Set<String> changedPaths = CommitUtils.getChangedPaths(repo, c1);
    assertThat(changedPaths).isEmpty();
  }

  @Test
  public void testChangedContentWithParentCommit() throws Exception {
    RevCommit c = makeCommit(createEmptyDirCacheEntries(INITIAL_PATHNAMES));
    DirCacheEntry[] entries = new DirCacheEntry[INITIAL_PATHNAMES.size()];
    for (int x = 0; x < INITIAL_PATHNAMES.size(); x++) {
      byte[] content = (x < 2) ? CONTENT_EMPTY : CONTENT_NOT_EMPTY;
      entries[x] = createDirCacheEntry(INITIAL_PATHNAMES.get(x), content);
    }
    RevCommit c1 = makeCommit(entries, c);
    Map<String, ObjectId> changedContent =
        CommitUtils.getChangedContent(repo, c1);
    assertThat(changedContent.keySet()).containsExactly("c", "d");
    for (ObjectId oid : changedContent.values()) {
      assertThat(repo.open(oid).getBytes()).isEqualTo(CONTENT_NOT_EMPTY);
    }
  }
}
