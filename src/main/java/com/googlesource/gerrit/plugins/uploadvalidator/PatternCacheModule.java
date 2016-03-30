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

import com.google.common.cache.CacheLoader;
import com.google.gerrit.server.cache.CacheModule;

import java.util.regex.Pattern;

public class PatternCacheModule extends CacheModule {
  public static final String CACHE_NAME = "patternCache";

  @Override
  protected void configure() {
    cache(CACHE_NAME, String.class, Pattern.class).loader(Loader.class);
  }

  static class Loader extends CacheLoader<String, Pattern> {
    @Override
    public Pattern load(String regex) throws Exception {
      return Pattern.compile(regex);
    }
  }

}
