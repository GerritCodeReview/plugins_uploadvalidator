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

import static org.eclipse.jgit.diff.DiffEntry.Side.NEW;
import static org.eclipse.jgit.diff.DiffEntry.Side.OLD;
import static org.eclipse.jgit.lib.FileMode.GITLINK;

import org.eclipse.jgit.diff.ContentSource;
import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.diff.ContentSource.Pair;
import org.eclipse.jgit.diff.DiffAlgorithm.SupportedAlgorithm;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.errors.LargeObjectException;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CommitUtils {
  private static final byte[] EMPTY = new byte[] {};
  private static final byte[] BINARY = new byte[] {};

  private static DiffAlgorithm diffAlgorithm = DiffAlgorithm
      .getAlgorithm(SupportedAlgorithm.MYERS);
  private static RawTextComparator comparator = RawTextComparator.DEFAULT;
  private static int binaryFileThreshold = 50 * 1024 * 1024;

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

    visitChangedEntries(repo, c, new TreeWalkVisitor() {
      @Override
      public void onVisit(TreeWalk tw) {
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
   * onVisit() method of the class TreeWalkVisitor.
   *
   * @param repo The repository
   * @param c The commit
   * @param visitor A TreeWalkVisitor with the desired action
   * @throws IOException
   */
  public static void visitChangedEntries(Repository repo, RevCommit c,
      TreeWalkVisitor visitor) throws IOException {
    try (TreeWalk tw = new TreeWalk(repo)) {
      tw.setRecursive(true);
      tw.setFilter(TreeFilter.ANY_DIFF);
      tw.addTree(c.getTree());
      if (c.getParentCount() > 0) {
        @SuppressWarnings("resource")
        RevWalk rw = null;
        try {
          for (RevCommit p : c.getParents()) {
            if (p.getTree() == null) {
              if (rw == null) {
                rw = new RevWalk(repo);
              }
              rw.parseHeaders(p);
            }
            tw.addTree(p.getTree());
          }
        } finally {
          if (rw != null) {
            rw.close();
          }
        }
        while (tw.next()) {
          if (isDifferentToAllParents(c, tw)) {
            visitor.onVisit(tw);
          }
        }
      } else {
        while (tw.next()) {
          visitor.onVisit(tw);
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

  static Map<String, EditList> getFilesEditList(Repository repo, RevCommit c)
      throws IOException {
    Map<String, EditList> changesMap = new HashMap<>();
    try (ByteArrayOutputStream out = new ByteArrayOutputStream();
        DiffFormatter diffFmt = new DiffFormatter(out)) {
      diffFmt.setRepository(repo);
      diffFmt
          .scan(c.getParentCount() > 0 ? c.getParent(0).getId() : null,
              c.getId())
          .stream()
          .filter(
              diff -> diff.getChangeType() == ChangeType.ADD
                  || diff.getChangeType() == ChangeType.MODIFY)
          .forEach(
              diff -> addToEdits(changesMap, diff.getNewPath(),
                  getEditList(repo, diff)));
      return changesMap;
    }
  }

  private static void addToEdits(Map<String, EditList> changesMap, String newPath,
      EditList editList) {
    EditList currEditList = changesMap.get(changesMap);
    if (currEditList == null) {
      currEditList = editList;
    } else {
      currEditList.addAll(editList);
    }
    changesMap.put(newPath, editList);
  }

  private static EditList getEditList(Repository repo, DiffEntry ent) {
    if (ent.getOldId() == null || ent.getNewId() == null) {
      return null;
    }
    byte[] aRaw, bRaw;

    if (ent.getOldMode() == GITLINK || ent.getNewMode() == GITLINK) {
      return null;
    }

    try {
      aRaw = open(repo, OLD, ent);
      bRaw = open(repo, NEW, ent);
    } catch (IOException e) {
      return null;
    }

    if (aRaw == BINARY || bRaw == BINARY //
        || RawText.isBinary(aRaw) || RawText.isBinary(bRaw)) {
      return null;
    }

    RawText resA = new RawText(aRaw);
    RawText resB = new RawText(bRaw);
    return diffAlgorithm.diff(comparator, resA, resB);
  }

  private static byte[] open(Repository repo, DiffEntry.Side side, DiffEntry entry)
      throws IOException {
    if (entry.getMode(side) == FileMode.MISSING) return EMPTY;

    if (entry.getMode(side).getObjectType() != Constants.OBJ_BLOB)
      return EMPTY;

    AbbreviatedObjectId id = entry.getId(side);
    ObjectReader reader = repo.newObjectReader();

    try {
      ContentSource cs = ContentSource.create(reader);
      Pair source = new ContentSource.Pair(cs, cs);
      ObjectLoader ldr = source.open(side, entry);
      return ldr.getBytes(binaryFileThreshold);

    } catch (LargeObjectException.ExceedsLimit overLimit) {
      return BINARY;

    } catch (LargeObjectException.ExceedsByteArrayLimit overLimit) {
      return BINARY;

    } catch (LargeObjectException.OutOfMemory tooBig) {
      return BINARY;

    } catch (LargeObjectException tooBig) {
      tooBig.setObjectId(id.toObjectId());
      throw tooBig;
    }
  }
}
