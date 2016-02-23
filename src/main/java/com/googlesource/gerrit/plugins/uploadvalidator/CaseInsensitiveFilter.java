package com.googlesource.gerrit.plugins.uploadvalidator;

import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

import java.io.IOException;

public class CaseInsensitiveFilter extends TreeFilter {
  private final String path;

  public CaseInsensitiveFilter(String path) {
    this.path = path;
  }

  @Override
  public boolean include(TreeWalk walker)
      throws MissingObjectException, IncorrectObjectTypeException, IOException {
    int n = walker.getTreeCount();



    return false;
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
