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

import static com.google.common.truth.Truth.assertThat;
import static com.googlesource.gerrit.plugins.uploadvalidator.TestUtils.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.gerrit.server.git.validators.CommitValidationMessage;

import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DuplicatePathnameValidatorTest extends ValidatorTestCase {
  private static final List<String> INITIAL_PATHNAMES = ImmutableList.of(
      "foobar",
      "folder1/foo",
      "folder1/bar",
      "folder1/f2/foo",
      "folder1/f2/bar");

  private static final List<String> MORE_PATHNAMES = ImmutableList.of(
      // conflicting pathnames
      "folder1/Foo",
      "foldeR1/bar",
      "folder1/F2/foo",
      "folder1/f2/bAr",
      // non-conflicting new pathnames
      "folder1/f2/valid",
      "valid",
      "folder1/valid/foo"
      );

  private TestRepository<Repository> testRepo;

  @Override
  @Before
  public void init() throws IOException {
    super.init();
    testRepo = new TestRepository<>(repo);
  }

  @Test
  public void testCheckInsideOfCommit() throws Exception {
    List<String> filenames = Lists.newArrayList(INITIAL_PATHNAMES);
    // add files with conflicting pathnames
    filenames.add("fooBar");
    filenames.add("folder1/bAr");
    filenames.add("folDer1/f2/foo");
    filenames.add("folder1/F2/bar");
    RevCommit c = makeCommit(
        createEmptyDirCacheEntries(filenames, testRepo), testRepo);
    List<CommitValidationMessage> m =
        DuplicatePathnameValidator.performValidation(repo, c, Locale.ENGLISH);
    // During checking inside of the commit it's unknown which file is checked
    // first, because of that, both capabilities must be checked.
    assertThat(TestUtils.transformMessages(m)).containsAnyOf(
        "ERROR: fooBar: pathname collides with foobar",
        "ERROR: foobar: pathname collides with fooBar");

    assertThat(TestUtils.transformMessages(m)).containsAnyOf(
        "ERROR: folder1/bAr: pathname collides with folder1/bar",
        "ERROR: folder1/bar: pathname collides with folder1/bAr");

    assertThat(TestUtils.transformMessages(m)).containsAnyOf(
        "ERROR: folDer1/f2/foo: pathname collides with folder1/f2/foo",
        "ERROR: folder1/f2/foo: pathname collides with folDer1/f2/foo");

    assertThat(TestUtils.transformMessages(m)).containsAnyOf(
        "ERROR: folder1/F2/bar: pathname collides with folder1/f2/bar",
        "ERROR: folder1/f2/bar: pathname collides with folder1/F2/bar");
  }

  @Test
  public void testCheckAgainstWholeTree() throws Exception {
    RevCommit c = makeCommit(
        createEmptyDirCacheEntries(INITIAL_PATHNAMES, testRepo), testRepo);
    List<String> tempPaths = Lists.newArrayList(INITIAL_PATHNAMES);
    tempPaths.addAll(MORE_PATHNAMES);
    RevCommit c1 = makeCommit(createEmptyDirCacheEntries(tempPaths, testRepo),
        testRepo, c);
    List<CommitValidationMessage> m =
        DuplicatePathnameValidator.performValidation(this.repo, c1, Locale.ENGLISH);
    Set<String> expected = ImmutableSet.of(
        "ERROR: folder1/Foo: pathname collides with folder1/foo",
        "ERROR: foldeR1/bar: pathname collides with folder1/bar",
        "ERROR: folder1/F2/foo: pathname collides with folder1/f2/foo",
        "ERROR: folder1/f2/bAr: pathname collides with folder1/f2/bar");
    assertThat(TestUtils.transformMessages(m)).containsExactlyElementsIn(expected);
  }

  @Test
  public void testCheckRenaming() throws Exception {
    RevCommit c = makeCommit(
        createEmptyDirCacheEntries(INITIAL_PATHNAMES, testRepo), testRepo);
    DirCacheEntry[] entries = new DirCacheEntry[INITIAL_PATHNAMES.size()];
    for (int x = 0; x < INITIAL_PATHNAMES.size(); x++) {
      //Rename files
      entries[x] = createDirCacheEntry(INITIAL_PATHNAMES.get(x).toUpperCase(),
          EMPTY_CONTENT, testRepo);
    }
    RevCommit c1 = makeCommit(entries, testRepo, c);
    List<CommitValidationMessage> m =
        DuplicatePathnameValidator.performValidation(repo, c1, Locale.ENGLISH);
    assertThat(m).isEmpty();
  }

  @Test
  public void validatorInactiveWhenConfigEmpty() {
    assertThat(DuplicatePathnameValidator.isActive(EMPTY_PLUGIN_CONFIG)).isFalse();
  }

  @Test
  public void defaultLocale() {
    assertThat(DuplicatePathnameValidator.getLocale(EMPTY_PLUGIN_CONFIG))
        .isEqualTo(Locale.ENGLISH);
  }
}
