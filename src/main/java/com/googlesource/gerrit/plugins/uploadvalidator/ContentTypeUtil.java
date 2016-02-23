package com.googlesource.gerrit.plugins.uploadvalidator;

import static com.googlesource.gerrit.plugins.uploadvalidator.PatternCacheModule.CACHE_NAME;

import com.google.common.cache.LoadingCache;
import com.google.gerrit.extensions.annotations.Exports;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.ProjectConfigEntry;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.eclipse.jgit.lib.ObjectLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

public class ContentTypeUtil {
  private static String KEY_BINARY_TYPES = "binaryTypes";

  public static AbstractModule module() {
    return new AbstractModule() {

      @Override
      protected void configure() {
        bind(ContentTypeUtil.class);
        bind(ProjectConfigEntry.class)
            .annotatedWith(Exports.named(KEY_BINARY_TYPES))
            .toInstance(new ProjectConfigEntry("Binary Types", null,
                ProjectConfigEntry.Type.ARRAY, null, false,
                "At the moment, there is no ideal solution to detect binary "
                    + "files. But some checks shouldn't run on binary files "
                    + "(e. g. InvalidLineEndingCheck). Because of that you can "
                    + "enter content types to avoid that these checks run on "
                    + "files with one of the entered content types."));
      }
    };
  }

  private final LoadingCache<String, Pattern> patternCache;
  private final Tika tika = new Tika(TikaConfig.getDefaultConfig());

  @Inject
  ContentTypeUtil(
      @Named(CACHE_NAME) LoadingCache<String, Pattern> patternCache) {
    this.patternCache = patternCache;
  }

  static String[] getBinaryTypes(PluginConfig cfg) {
    return cfg.getStringList(KEY_BINARY_TYPES);
  }

  public String getContentType(InputStream is, String pathname)
      throws IOException {
    Metadata metadata = new Metadata();
    metadata.set(Metadata.RESOURCE_NAME_KEY, pathname);
    return tika.detect(TikaInputStream.get(is), metadata);
  }

  public boolean isBinary(ObjectLoader ol, String pathname, PluginConfig cfg)
      throws IOException, ExecutionException {
    try (InputStream is = ol.openStream()) {
      return doesTypeMatch(getContentType(is, pathname), getBinaryTypes(cfg));
    }
  }

  public boolean doesTypeMatch(String currentType, String[] listOfTypes)
      throws ExecutionException {
    for (String blockedType : listOfTypes) {
      if (blockedType.startsWith("^")
          && patternCache.get(blockedType).matcher(currentType).matches()) {
        return true;
      } else if (blockedType.endsWith("*") && currentType
          .startsWith(blockedType.substring(0, blockedType.length() - 1))) {
        return true;
      } else {
        if (blockedType.equals(currentType)) {
          return true;
        }
      }
    }
    return false;
  }
}
