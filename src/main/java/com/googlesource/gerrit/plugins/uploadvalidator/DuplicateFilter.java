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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DuplicateFilter extends TreeFilter {
  private final Locale locale;
  private final List<String> changedPaths;
  private final Set<String> folders;

  public static Map<String, String> getAllParentFolders(
      Collection<String> pathnames, Locale locale) {
    Map<String, String> folders = new HashMap<>();
    for (String p : pathnames) {
      for (String folder : getAllParentFolders(p)) {
        String folderLC = folder.toLowerCase(locale);
        if (!folders.containsKey(folderLC)) {
          folders.put(folderLC, folder);
        }
      }
    }
    return folders;
  }

  private static List<String> getAllParentFolders(String path) {
    List<String> folders = new ArrayList<>();
    if (!path.contains("/")) {
      return Collections.<String> emptyList();
    }
    String[] tmpFolders = path.split("/");
    for (int n = 0; n < tmpFolders.length; n++) {
      StringBuilder sb = new StringBuilder();
      sb.append(tmpFolders[0]);
      for (int y = 1; y < n; y++) {
        sb.append("/" + tmpFolders[y]);
      }
      folders.add(sb.toString());
    }
    return folders;
  }

  private static boolean isDeleted(TreeWalk tw) {
    return FileMode.MISSING.equals(tw.getRawMode(0));
  }

  public DuplicateFilter(Collection<String> searchPaths,
      Collection<String> folders, Locale locale) {
    this.locale = locale;
    this.changedPaths = Lists.newArrayList(searchPaths);
    this.folders = Sets.newHashSet(folders);
  }

  @Override
  public boolean include(TreeWalk tw)
      throws MissingObjectException, IncorrectObjectTypeException, IOException {
    if (isDeleted(tw)) {
      // If this object is deleted in current commit, it is not necessary to
      // check it
      return false;
    }
    Iterator<String> iterator = changedPaths.iterator();
    String currentPath = tw.getPathString();
    while (iterator.hasNext()) {
      String changedPath = iterator.next();
      if (currentPath.length() <= changedPath.length()) {
        String changedPathprefix =
            changedPath.substring(0, currentPath.length());
        if (equalsIgnoreCase(currentPath, changedPathprefix)
            && !changedPath.equals(currentPath)) {
          if (tw.isSubtree()) {
            if (folders.contains(currentPath)) {
              tw.enterSubtree();
              return false;
            } else {
              // Folder conflict
              iterator.remove();
              return true;
            }
          } else if (changedPath.length() == currentPath.length()) {
            // If there are no duplicates in the tree, then there exist at most
            // one duplicate for each changedPath. If we found one, this
            // changedPath is not longer relevant
            // The length of the paths must be equal, otherwise it cannot be a
            // duplicate
            iterator.remove();
            return true;
          }
        }
      }
    }
    return false;
  }

  private boolean equalsIgnoreCase(String s1, String s2) {
    return s1.toLowerCase(locale).equals(s2.toLowerCase(locale));
  }

  @Override
  public boolean shouldBeRecursive() {
    return false;
  }

  @Override
  public TreeFilter clone() {
    return this;
  }
}
