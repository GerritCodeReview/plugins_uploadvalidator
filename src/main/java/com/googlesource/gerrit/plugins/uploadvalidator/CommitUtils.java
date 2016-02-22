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

import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CommitUtils {

  /**
   * This method spots all files which differ between the passed commit and its
   * parents. The paths of the spotted files will be returned as a Set.
   *
   * @param repo The repository
   * @param c The commit
   * @return A Set containing the paths of all files which differ between the
   *     passed commit and its parents.
   * @throws IOException
   */
  public static Set<String> getChangedPaths(Repository repo, RevCommit c)
      throws IOException {
    Map<String, ObjectId> content = getChangedContent(repo, c);
    return content.keySet();
  }

  /**
   * This method spots all files which differ between the passed commit and its
   * parents. The spotted files will be returned as a Map. The structure of the
   * returned map looks like this:
   * <p>
   * <ul>
   * <li> Key: Path to the changed file.</li>
   * <li> Value: ObjectId of the changed file.</li>
   * <ul>
   * @param repo The repository
   * @param c The commit
   * @return A Map containing all files which differ between the passed commit
   *     and its parents.
   * @throws IOException
   */
  public static Map<String, ObjectId> getChangedContent(Repository repo,
      RevCommit c) throws IOException {
    final Map<String, ObjectId> content = new HashMap<>();

    runOnChangedTreeEntry(repo, c, new TreeWalkListener() {
      @Override
      public void onEnterEntry(TreeWalk tw) {
        if (isFile(tw)) {
          content.put(tw.getPathString(), tw.getObjectId(0));
        }
      }
    });
    return content;
  }

  private static boolean isFile(TreeWalk tw) {
    return FileMode.EXECUTABLE_FILE.equals(tw.getRawMode(0))
        || FileMode.REGULAR_FILE.equals(tw.getRawMode(0));
  }

  /**
   * This method spots all TreeWalk entries which differ between the passed
   * commit and its parents. If a TreeWalk entry is found this method calls the
   * onEnterEntry() method of the class TreeWalkListener.
   *
   * @param repo The repository
   * @param c The commit
   * @param listener A TreeWalkListener with the desired action
   * @throws IOException
   */
  public static void runOnChangedTreeEntry(Repository repo, RevCommit c,
      TreeWalkListener listener) throws IOException {
    try (TreeWalk tw = new TreeWalk(repo)) {
      tw.setRecursive(true);
      tw.setFilter(TreeFilter.ANY_DIFF);
      tw.addTree(c.getTree());
      if (c.getParentCount() > 0) {
        for (RevCommit p : c.getParents()) {
          tw.addTree(p.getTree());
        }
        while (tw.next()) {
          if (isDifferentToAllParents(c, tw)) {
            listener.onEnterEntry(tw);
          }
        }
      } else {
        while (tw.next()) {
          listener.onEnterEntry(tw);
        }
      }
    }
  }

  private static boolean isDifferentToAllParents(RevCommit c, TreeWalk tw) {
    if (c.getParentCount() > 1) {
      for (int p = 1; p <= c.getParentCount(); p++) {
        if (tw.getObjectId(0).equals(tw.getObjectId(p))) {
          return false;
        }
      }
    }
    return true;
  }
}
