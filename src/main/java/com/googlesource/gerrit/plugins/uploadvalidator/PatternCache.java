package com.googlesource.gerrit.plugins.uploadvalidator;

import com.google.common.cache.CacheLoader;
import com.google.gerrit.server.cache.CacheModule;

import java.util.regex.Pattern;

public class PatternCache extends CacheModule {
  public static final String CACHE_NAME = "uvp_PatternCache";

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
