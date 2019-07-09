package com.googlesource.gerrit.plugins.uploadvalidator;

import static com.googlesource.gerrit.plugins.uploadvalidator.PatternCacheModule.CACHE_NAME;

import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.reviewdb.client.Project.NameKey;
import com.google.gerrit.server.config.AllProjectsName;
import com.google.gerrit.server.config.PluginConfig;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class BlockedKeywordConfigLoader {

  private static final Logger log = LoggerFactory.getLogger(BlockedKeywordConfigLoader.class);

  private final AllProjectsName allProjectsName;
  private final String pluginName;
  private final ConfigFactory cfgFactory;
  private final LoadingCache<String, Pattern> patternCache;

  @Inject
  public BlockedKeywordConfigLoader(
      AllProjectsName allProjectsName,
      @PluginName String pluginName,
      @Named(CACHE_NAME) LoadingCache<String, Pattern> patternCache,
      ConfigFactory cfgFactory) {
    this.allProjectsName = allProjectsName;
    this.patternCache = patternCache;
    this.pluginName = pluginName;
    this.cfgFactory = cfgFactory;
  }

  private Config getPluginConfig() throws ConfigInvalidException {
    return cfgFactory.get(allProjectsName);
  }

  private PluginConfig getProjectConfig(NameKey projectName) throws ConfigInvalidException {
    return cfgFactory.getFromProjectConfig(projectName);
  }

  public List<BlockedKeywordMatcher> getBlockedKeywordMatchers(NameKey projectName) throws ConfigInvalidException {
    List<BlockedKeywordMatcher> matchers = new ArrayList<>();
    matchers.addAll(getPluginBlockedKeywordMatchers());
    matchers.addAll(getProjectConfigBlockedKeywordMatchers(projectName));
    return matchers;
  }

  public List<BlockedKeywordMatcher> getPluginBlockedKeywordMatchers() throws ConfigInvalidException {
    Config cfg = getPluginConfig();
    Set<String> blockedKeywordSubsections = cfg.getSubsections("blockedKeyword");
    List<BlockedKeywordMatcher> matchers = new ArrayList<>();
    for (String blockedKeywordString : blockedKeywordSubsections) {
      try {
        Pattern blockedWordPattern = patternCache.get(blockedKeywordString);
        List<Pattern> skipProjectPatterns = getPaternsForAttribute(cfg, blockedKeywordString, "skipProject");
        List<String> skipRefPatterns = Arrays.asList(cfg.getStringList("blockedKeyword", blockedKeywordString, "skipRef"));
        List<Pattern> skipFilePatterns = getPaternsForAttribute(cfg, blockedKeywordString, "skipFile");
        List<Pattern> skipEmailPatterns = getPaternsForAttribute(cfg, blockedKeywordString, "skipEmail");
        matchers.add(new BlockedKeywordMatcher(
            blockedWordPattern,
            skipProjectPatterns,
            skipRefPatterns,
            skipFilePatterns,
            skipEmailPatterns));
      } catch (ExecutionException e) {
        throw new ConfigInvalidException("Config contains invalid regex", e);
      }
    }
    return matchers;
  }

  public List<BlockedKeywordMatcher> getProjectConfigBlockedKeywordMatchers(NameKey projectName) throws ConfigInvalidException {
    PluginConfig cfg = getProjectConfig(projectName);
    List<BlockedKeywordMatcher> matchers = new ArrayList<>();
    try {
      ImmutableMap<String, Pattern> blockedKeywordPatterns =
          patternCache.getAll(
              Arrays.asList(cfg.getStringList("blockedKeywordPattern")));
      for (Pattern blockedWordPattern : blockedKeywordPatterns.values()) {
        matchers.add(new BlockedKeywordMatcher(blockedWordPattern));
      }
    } catch (ExecutionException e) {
      throw new ConfigInvalidException("Config contains invalid regex", e);
    }
    return matchers;
  }

  private List<Pattern> getPaternsForAttribute(Config cfg, String blockedKeywordSubsection, String attribute) throws ExecutionException {
    List<Pattern> res = new ArrayList<>();
    List<String> rawPatterns = Arrays.asList(cfg.getStringList("blockedKeyword", blockedKeywordSubsection, attribute));
    for (String rawPattern : rawPatterns) {
      res.add(patternCache.get(rawPattern));
    }
    return res;
  }
}
