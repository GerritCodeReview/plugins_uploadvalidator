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

import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

import java.io.IOException;
import java.util.List;

public class ListVisitedPathsFilter extends TreeFilter {
  private List<String> visitedPaths = null;

  public ListVisitedPathsFilter(List<String> visitedPaths) {
    super();
    this.visitedPaths = visitedPaths;
  }

  @Override
  public boolean include(TreeWalk walker)
      throws MissingObjectException, IncorrectObjectTypeException, IOException {
    visitedPaths.add(walker.getPathString());
    return true;
  }

  @Override
  public boolean shouldBeRecursive() {
    return false;
  }

  @Override
  public TreeFilter clone() {
    return this;
  }

  public List<String> getVisitedPaths() {
    return visitedPaths;
  }
}
