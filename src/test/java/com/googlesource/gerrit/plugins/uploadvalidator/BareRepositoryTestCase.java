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

import com.google.common.base.Charsets;

import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.junit.Before;

import java.io.IOException;
import java.util.List;

public class BareRepositoryTestCase extends RepositoryTestCase {
  protected static final byte[] CONTENT_EMPTY = "".getBytes(Charsets.UTF_8);

  protected TestRepository<Repository> bareRepo;

  @Before
  public void setUp() throws IOException {
    bareRepo = new TestRepository<>(repo);
  }

  protected RevCommit makeCommit(DirCacheEntry[] entries) throws Exception {
    return makeCommit(entries, (RevCommit[]) null);
  }

  protected RevCommit makeCommit(DirCacheEntry[] entries, RevCommit... parents)
      throws Exception {
    final RevTree ta = bareRepo.tree(entries);
    RevCommit c =
        (parents == null) ? bareRepo.commit(ta) : bareRepo.commit(ta, parents);
    bareRepo.parseBody(c);
    return c;
  }

  protected DirCacheEntry createDirCacheEntry(String pathname, byte[] content)
      throws Exception {
    return bareRepo.file(pathname, bareRepo.blob(content));
  }

  protected DirCacheEntry[] createEmptyDirCacheEntries(List<String> filenames)
      throws Exception {
    DirCacheEntry[] entries = new DirCacheEntry[filenames.size()];
    for (int x = 0; x < filenames.size(); x++) {
      entries[x] = createDirCacheEntry(filenames.get(x), CONTENT_EMPTY);
    }
    return entries;
  }
}
