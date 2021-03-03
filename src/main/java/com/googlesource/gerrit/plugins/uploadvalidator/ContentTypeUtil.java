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

import static com.googlesource.gerrit.plugins.uploadvalidator.PatternCacheModule.CACHE_NAME;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.LoadingCache;
import com.google.gerrit.extensions.annotations.Exports;
import com.google.gerrit.extensions.api.projects.ProjectConfigEntryType;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.ProjectConfigEntry;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import org.eclipse.jgit.lib.ObjectLoader;
import org.overviewproject.mime_types.GetBytesException;
import org.overviewproject.mime_types.MimeTypeDetector;

public class ContentTypeUtil {
  private static final String KEY_BINARY_TYPES = "binaryTypes";

  public static AbstractModule module() {
    return new AbstractModule() {

      @Override
      protected void configure() {
        bind(ContentTypeUtil.class);
        bind(ProjectConfigEntry.class)
            .annotatedWith(Exports.named(KEY_BINARY_TYPES))
            .toInstance(
                new ProjectConfigEntry(
                    "Binary Types",
                    null,
                    ProjectConfigEntryType.ARRAY,
                    null,
                    false,
                    "At the moment, there is no ideal solution to detect binary "
                        + "files. But some checks shouldn't run on binary files "
                        + "(e. g. InvalidLineEndingCheck). Because of that you can "
                        + "enter content types to avoid that these checks run on "
                        + "files with one of the entered content types."));
      }
    };
  }

  @VisibleForTesting
  static String[] getBinaryTypes(PluginConfig cfg) {
    return cfg.getStringList(KEY_BINARY_TYPES);
  }

  private final LoadingCache<String, Pattern> patternCache;
  private final MimeTypeDetector detector = new MimeTypeDetector();

  @Inject
  ContentTypeUtil(@Named(CACHE_NAME) LoadingCache<String, Pattern> patternCache) {
    this.patternCache = patternCache;
  }

  public boolean isForbiddenBinaryContentType(ObjectLoader ol, String pathname, PluginConfig cfg)
      throws IOException, ExecutionException {
    try (InputStream is = ol.openStream()) {
      return matchesAny(getContentType(is, pathname), getBinaryTypes(cfg));
    }
  }

  public String getContentType(InputStream is, String pathname) throws IOException {
    try {
      return detector.detectMimeType(pathname, is);
    } catch (GetBytesException e) {
      throw new IOException(e);
    }
  }

  @VisibleForTesting
  boolean matchesAny(String s, String[] patterns) throws ExecutionException {
    for (String p : patterns) {
      if (p.startsWith("^") && patternCache.get(p).matcher(s).matches()) {
        return true;
      } else if (p.endsWith("*") && s.startsWith(p.substring(0, p.length() - 1))) {
        return true;
      } else {
        if (p.equals(s)) {
          return true;
        }
      }
    }
    return false;
  }
}
