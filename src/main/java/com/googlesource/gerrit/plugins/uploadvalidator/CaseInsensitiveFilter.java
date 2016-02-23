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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

public class CaseInsensitiveFilter extends TreeFilter {
  /**
   * key = searchPath, value = depth of the searchPath
   */
  private final Map<String, Integer> searchEntity = new HashMap<>();

  public CaseInsensitiveFilter(Set<String> searchPaths) {
    for (String path : searchPaths) {
      this.searchEntity.put(path, StringUtils.countMatches(path, "/"));
    }
  }

  public CaseInsensitiveFilter(String searchPath) {
    this(new HashSet<>(Arrays.asList(searchPath)));
  }

  @Override
  public boolean include(TreeWalk walker)
      throws MissingObjectException, IncorrectObjectTypeException, IOException {
    String currentPath = walker.getPathString();
    int currentPathDepth = StringUtils.countMatches(currentPath, "/");
    String currentPathPart = extractPathPart(currentPath, currentPathDepth);
    if (walker.isSubtree()) {
      for (String searchPath : searchEntity.keySet()) {
        if (currentPathDepth <= searchEntity.get(searchPath)) {
          String searchPathPart = extractPathPart(searchPath, currentPathDepth);
          if (searchPathPart.length() == currentPathPart.length()
              && searchPathPart.equalsIgnoreCase(currentPathPart)) {
            return true;
          }
        }
      }
    } else {
      for (String searchPath : searchEntity.keySet()) {
        if (currentPathDepth == searchEntity.get(searchPath)
            && searchPath.length() == currentPath.length()
            && searchPath.equalsIgnoreCase(currentPath)) {
          return true;
        }
      }
    }
    return false;
  }

  private String extractPathPart(String path, int depth) {
    if (depth == 0) {
      if (path.contains("/")) {
        return path.substring(0, path.indexOf("/"));
      } else {
        return path;
      }
    } else {
      return extractPathPart(path.substring(path.indexOf("/") + 1), depth - 1);
    }
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
