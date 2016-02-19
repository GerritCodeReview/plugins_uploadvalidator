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

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class ContentValidator implements CommitValidationListener {

  protected Map<ObjectId, String> getContent(Repository repo, RevCommit c)
      throws IOException {
    Map<ObjectId, String> content = new HashMap<>();

    try (TreeWalk tw = new TreeWalk(repo)) {
      tw.setRecursive(true);
      tw.setFilter(TreeFilter.ANY_DIFF);
      tw.addTree(c.getTree());
      if (c.getParentCount() > 0) {
        for (RevCommit p : c.getParents()) {
          tw.addTree(p.getTree());
        }
        while (tw.next()) {
          boolean diff = true;
          if (c.getParentCount() > 1) {
            for (int p = 1; p <= c.getParentCount(); p++) {
              if (tw.getObjectId(0).equals(tw.getObjectId(p))) {
                diff = false;
                break;
              }
            }
          }
          if (diff) {
            content.put(tw.getObjectId(0), tw.getPathString());
          }
        }
      } else {
        while(tw.next()) {
          content.put(tw.getObjectId(0), tw.getPathString());
        }
      }
    }
    return content;
  }
}
