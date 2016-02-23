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
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.Locale;

public class CaseInsensitiveFilter extends TreeFilter {

  private final Locale locale;
  private final Collection<String> searchPaths;

  public CaseInsensitiveFilter(Collection<String> searchPaths, Locale locale) {
    this.locale = locale;
    this.searchPaths = searchPaths;
  }

  @Override
  public boolean include(TreeWalk tw)
      throws MissingObjectException, IncorrectObjectTypeException, IOException {
    if (isDeleted(tw)) {
      // If this object is deleted in current commit, it is not necessary to
      // check it
      return false;
    }

    String current = tw.getPathString();
    for (String s : searchPaths) {
      if (s.length() >= current.length()
          && equalsIgnoreCase(current, s.substring(0, current.length()))) {
          return true;
      }
    }
    return false;
  }

  private boolean equalsIgnoreCase(String s1, String s2) {
    return s1.toLowerCase(locale).equals(s2.toLowerCase(locale));
  }

  private static boolean isDeleted(TreeWalk tw) {
    return FileMode.MISSING.equals(tw.getRawMode(0));
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
