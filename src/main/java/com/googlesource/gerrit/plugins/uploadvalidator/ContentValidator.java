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

import com.google.gerrit.server.git.validators.CommitValidationListener;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ContentValidator implements CommitValidationListener {

  protected Map<ObjectId, String> getContent(Repository repo, RevCommit c)
      throws IOException, GitAPIException {
    Map<ObjectId, String> content = new HashMap<>();

    if (c.getParentCount() > 0) {
      List<DiffEntry> diffEntries;
      try (Git git = new Git(repo)) {
        diffEntries = git.diff()
            .setOldTree(getTreeIterator(repo, c.getName() + "^"))
            .setNewTree(getTreeIterator(repo, c.getName())).call();
      }
      for (DiffEntry e : diffEntries) {
        if (e.getNewPath() != null) {
          content.put(e.getNewId().toObjectId(), e.getNewPath());
        }
      }
    } else {
      try (TreeWalk tw = new TreeWalk(repo)) {
        tw.addTree(c.getTree());
        tw.setRecursive(true);
        while (tw.next()) {
          content.put(tw.getObjectId(0), tw.getPathString());
        }
      }
    }

    return content;
  }

  private AbstractTreeIterator getTreeIterator(Repository repo, String name)
      throws IOException {
    CanonicalTreeParser p = new CanonicalTreeParser();
    try (ObjectReader or = repo.newObjectReader();
        RevWalk rw = new RevWalk(repo)) {
      p.reset(or, rw.parseTree(repo.resolve(name)));
      return p;
    }
  }
}
