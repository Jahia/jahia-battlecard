package org.jahiacommunity.modules.battlecard.edp;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.cache.CacheHelper;
import org.jahia.services.cache.ModuleClassLoaderAwareCacheEntry;
import org.jahia.services.cache.ehcache.EhCacheProvider;
import org.jahiacommunity.modules.battlecard.service.NodeValue;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import java.util.List;
import java.util.Map;

@Component(service = BattlecardCacheManager.class, immediate = true)
public class BattlecardCacheManager {
    private static final String CACHE_KEY = "battlecard-cache";

    private Ehcache cache;
    private String outputPath;

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    @Activate
    private void onActivate() {
        CacheManager cacheManager = ((EhCacheProvider) SpringContextSingleton.getBean("ehCacheProvider")).getCacheManager();
        cache = cacheManager.getCache(CACHE_KEY);
        if (cache == null) {
            CacheConfiguration cacheConfiguration = cacheManager.getConfiguration().getDefaultCacheConfiguration().clone();
            cacheConfiguration.setName(CACHE_KEY);
            // Create a new cache with the configuration
            cache = new Cache(cacheConfiguration);
            cache.setName(CACHE_KEY);
            // Cache name has been set now we can initialize it by putting it in the manager.
            // Only Cache manager is initializing caches.
            cacheManager.addCacheIfAbsent(cache);
        } else {
            cache.removeAll();
        }
    }

    @Deactivate
    private void onDeactivate() {
        flush();
    }

    public void flush() {
        if (cache != null) {
            CacheHelper.flushEhcacheByName(CACHE_KEY, true);
        }
        if (outputPath != null) {
            CacheHelper.flushOutputCachesForPath(outputPath, true);
            CacheHelper.sendCacheFlushCommandToCluster(CacheHelper.CMD_FLUSH_PATH, outputPath);
        }
    }

    public void putSheets(String cacheKey, List<NodeValue> sheets) {
        cache.put(new Element(cacheKey, new ModuleClassLoaderAwareCacheEntry(sheets, CACHE_KEY)));
    }

    public List<NodeValue> getSheets(String cacheKey) {
        return (List<NodeValue>) CacheHelper.getObjectValue(cache, cacheKey);
    }

    public void putSheetData(String sheet, Map<NodeValue, Map<NodeValue, String>> data) {
        cache.put(new Element(sheet, new ModuleClassLoaderAwareCacheEntry(data, CACHE_KEY)));
    }

    public Map<NodeValue, Map<NodeValue, String>> getSheetData(String cacheKey) {
        return (Map<NodeValue, Map<NodeValue, String>>) CacheHelper.getObjectValue(cache, cacheKey);
    }
}
