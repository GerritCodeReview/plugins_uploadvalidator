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

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.git.validators.CommitValidationMessage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

public class TestUtils {
  public static final PluginConfig EMPTY_PLUGIN_CONFIG =
      PluginConfig.create("", new Config(), null);

  protected static final byte[] EMPTY_CONTENT = "".getBytes(Charsets.UTF_8);

  private static final Function<CommitValidationMessage, String> MESSAGE_TRANSFORMER =
      new Function<CommitValidationMessage, String>() {
        @Override
        public String apply(CommitValidationMessage input) {
          String pre = (input.isError()) ? "ERROR: " : "MSG: ";
          return pre + input.getMessage();
        }
      };

  public static final LoadingCache<String, Pattern> PATTERN_CACHE =
      CacheBuilder.newBuilder().build(new PatternCacheModule.Loader());

  public static Repository createNewRepository(File repoFolder) throws IOException {
    Repository repository = FileRepositoryBuilder.create(new File(repoFolder, ".git"));
    repository.create();
    return repository;
  }

  public static RevCommit makeCommit(RevWalk rw, Repository repo, String message, Set<File> files)
      throws IOException, GitAPIException {
    Map<File, byte[]> tmp = new HashMap<>();
    for (File f : files) {
      tmp.put(f, null);
    }
    return makeCommit(rw, repo, message, tmp);
  }

  public static RevCommit makeCommit(
      RevWalk rw, Repository repo, String message, Map<File, byte[]> files)
      throws IOException, GitAPIException {
    try (Git git = new Git(repo)) {
      if (files != null) {
        addFiles(git, files);
      }
      return rw.parseCommit(git.commit().setMessage(message).call());
    }
  }

  private static String generateFilePattern(File f, Git git) {
    return f.getAbsolutePath()
        .replace(git.getRepository().getWorkTree().getAbsolutePath(), "")
        .substring(1);
  }

  public static void removeFiles(Git git, Set<File> files) throws GitAPIException {
    RmCommand rmc = git.rm();
    for (File f : files) {
      rmc.addFilepattern(generateFilePattern(f, git));
    }
    rmc.call();
  }

  private static void addFiles(Git git, Map<File, byte[]> files)
      throws IOException, GitAPIException {
    AddCommand ac = git.add();
    for (File f : files.keySet()) {
      if (!f.exists()) {
        FileUtils.touch(f);
      }
      if (files.get(f) != null) {
        FileUtils.writeByteArrayToFile(f, files.get(f));
      }
      ac = ac.addFilepattern(generateFilePattern(f, git));
    }
    ac.call();
  }

  public static File createEmptyFile(String name, Repository repo) {
    return new File(repo.getDirectory().getParent(), name);
  }

  public static String transformMessage(CommitValidationMessage messages) {
    return MESSAGE_TRANSFORMER.apply(messages);
  }

  public static List<String> transformMessages(List<CommitValidationMessage> messages) {
    return Lists.transform(messages, MESSAGE_TRANSFORMER);
  }

  public static DirCacheEntry[] createEmptyDirCacheEntries(
      List<String> filenames, TestRepository<Repository> repo) throws Exception {
    DirCacheEntry[] entries = new DirCacheEntry[filenames.size()];
    for (int x = 0; x < filenames.size(); x++) {
      entries[x] = createDirCacheEntry(filenames.get(x), EMPTY_CONTENT, repo);
    }
    return entries;
  }

  public static DirCacheEntry createDirCacheEntry(
      String pathname, byte[] content, TestRepository<Repository> repo) throws Exception {
    return repo.file(pathname, repo.blob(content));
  }

  public static RevCommit makeCommit(
      RevWalk rw, DirCacheEntry[] entries, TestRepository<Repository> repo) throws Exception {
    return makeCommit(rw, entries, repo, (RevCommit[]) null);
  }

  public static RevCommit makeCommit(
      RevWalk rw, DirCacheEntry[] entries, TestRepository<Repository> repo, RevCommit... parents)
      throws Exception {
    final RevTree ta = repo.tree(entries);
    RevCommit c = (parents == null) ? repo.commit(ta) : repo.commit(ta, parents);
    repo.parseBody(c);
    return rw.parseCommit(c);
  }
}
