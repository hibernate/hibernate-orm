package org.hibernate.cache;

import org.hibernate.cache.access.AccessType;
import org.hibernate.cfg.Settings;

import java.util.Properties;

/**
 * @author Alex Snaps
 */
public class DelegatingRegionFactory implements RegionFactory {

    private final RegionFactory regionFactory;

    DelegatingRegionFactory(final RegionFactory regionFactory) {
        this.regionFactory = regionFactory;
    }

    public final void start(final Settings settings, final Properties properties) throws CacheException {
        regionFactory.start(settings, properties);
    }

    public final void stop() {
        regionFactory.stop();
    }

    public final boolean isMinimalPutsEnabledByDefault() {
        return regionFactory.isMinimalPutsEnabledByDefault();
    }

    public final AccessType getDefaultAccessType() {
        return regionFactory.getDefaultAccessType();
    }

    public final long nextTimestamp() {
        return regionFactory.nextTimestamp();
    }

    public final EntityRegion buildEntityRegion(final String regionName, final Properties properties,
                                                final CacheDataDescription metadata) throws CacheException {
        return regionFactory.buildEntityRegion(regionName, properties, metadata);
    }

    public final CollectionRegion buildCollectionRegion(final String regionName, final Properties properties,
                                                        final CacheDataDescription metadata) throws CacheException {
        return regionFactory.buildCollectionRegion(regionName, properties, metadata);
    }

    public final QueryResultsRegion buildQueryResultsRegion(final String regionName, final Properties properties) throws CacheException {
        return regionFactory.buildQueryResultsRegion(regionName, properties);
    }

    public final TimestampsRegion buildTimestampsRegion(final String regionName, final Properties properties) throws CacheException {
        return regionFactory.buildTimestampsRegion(regionName, properties);
    }
}