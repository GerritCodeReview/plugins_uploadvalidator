// Copyright (C) 2014 The Android Open Source Project
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
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class PathValidator implements CommitValidationListener {

  protected List<String> getFiles(Repository repo, RevCommit c)
      throws IOException, GitAPIException {
    List<String> files = new ArrayList<>();

    if (c.getParentCount() > 0) {
      Git git = new Git(repo);
      List<DiffEntry> diffEntries =
          git.diff().setOldTree(getTreeIterator(repo, c.getName() + "^"))
              .setNewTree(getTreeIterator(repo, c.getName())).call();
      for (DiffEntry e : diffEntries) {
        if (e.getNewPath() != null) {
          files.add(e.getNewPath());
        }
      }
    } else {
      TreeWalk tw = new TreeWalk(repo);
      tw.addTree(c.getTree());
      tw.setRecursive(true);
      while (tw.next()) {
        files.add(tw.getPathString());
      }
    }

    return files;
  }

  private AbstractTreeIterator getTreeIterator(Repository repo, String name)
      throws IOException {
    CanonicalTreeParser p = new CanonicalTreeParser();
    ObjectReader or = repo.newObjectReader();
    try {
      p.reset(or, new RevWalk(repo).parseTree(repo.resolve(name)));
      return p;
    } finally {
      or.close();
    }
  }
}
