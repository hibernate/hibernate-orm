/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.cache.ehcache.management.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.hibernate.management.api.EhcacheStats;

/**
 * Implementation of {@link EhcacheStats}
 * <p/>
 * <p/>
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 */
public class EhcacheStatsImpl extends AbstractEmitterBean implements EhcacheStats {
	private static final long MILLIS_PER_SECOND = 1000;
	private static final MBeanNotificationInfo NOTIFICATION_INFO;

	private final CacheManager cacheManager;
	private long statsSince = System.currentTimeMillis();

	static {
		final String[] notifTypes = new String[] {
				CACHE_ENABLED, CACHE_REGION_CHANGED, CACHE_FLUSHED, CACHE_REGION_FLUSHED,
				CACHE_STATISTICS_ENABLED, CACHE_STATISTICS_RESET,
		};
		final String name = Notification.class.getName();
		final String description = "Ehcache Hibernate Statistics Event";
		NOTIFICATION_INFO = new MBeanNotificationInfo( notifTypes, name, description );
	}

	/**
	 * Constructor accepting the backing {@link CacheManager}
	 *
	 * @param manager The {@link CacheManager} to expose stats for
	 *
	 * @throws javax.management.NotCompliantMBeanException should registering the MBean fail
	 */
	public EhcacheStatsImpl(CacheManager manager) throws NotCompliantMBeanException {
		super( EhcacheStats.class );
		this.cacheManager = manager;
	}

	@Override
	public void flushRegionCache(String region) {
		final Cache cache = this.cacheManager.getCache( region );
		if ( cache != null ) {
			cache.flush();
		}
	}

	@Override
	public void flushRegionCaches() {
		for ( String name : cacheManager.getCacheNames() ) {
			final Cache cache = this.cacheManager.getCache( name );
			if ( cache != null ) {
				cache.flush();
			}
		}
	}

	@Override
	public String generateActiveConfigDeclaration() {
		return this.cacheManager.getActiveConfigurationText();
	}

	@Override
	public String generateActiveConfigDeclaration(String region) {
		return this.cacheManager.getActiveConfigurationText( region );
	}

	@Override
	public long getCacheHitCount() {
		long count = 0;
		for ( String name : cacheManager.getCacheNames() ) {
			final Cache cache = cacheManager.getCache( name );
			if ( cache != null ) {
				count += cache.getStatistics().cacheHitCount();
			}
		}
		return count;
	}

	@Override
	public double getCacheHitRate() {
		final long now = System.currentTimeMillis();
		final double deltaSecs = (double) ( now - statsSince ) / MILLIS_PER_SECOND;
		return getCacheHitCount() / deltaSecs;
	}

	@Override
	public long getCacheHitSample() {
		long count = 0;
		for ( String name : cacheManager.getCacheNames() ) {
			final Cache cache = cacheManager.getCache( name );
			if ( cache != null ) {
				count += cache.getStatistics().cacheHitOperation().rate().value().longValue();
			}
		}
		return count;
	}

	@Override
	public long getCacheMissCount() {
		long count = 0;
		for ( String name : cacheManager.getCacheNames() ) {
			final Cache cache = cacheManager.getCache( name );
			if ( cache != null ) {
				count += cache.getStatistics().cacheMissCount();
			}
		}
		return count;
	}

	@Override
	public double getCacheMissRate() {
		final long now = System.currentTimeMillis();
		final double deltaSecs = (double) ( now - statsSince ) / MILLIS_PER_SECOND;
		return getCacheMissCount() / deltaSecs;
	}

	@Override
	public long getCacheMissSample() {
		long count = 0;
		for ( String name : cacheManager.getCacheNames() ) {
			final Cache cache = cacheManager.getCache( name );
			if ( cache != null ) {
				count += cache.getStatistics().cacheMissOperation().rate().value().longValue();
			}
		}
		return count;
	}

	@Override
	public long getCachePutCount() {
		long count = 0;
		for ( String name : cacheManager.getCacheNames() ) {
			final Cache cache = cacheManager.getCache( name );
			if ( cache != null ) {
				count += cache.getStatistics().cachePutCount();
			}
		}
		return count;
	}

	@Override
	public double getCachePutRate() {
		final long now = System.currentTimeMillis();
		final double deltaSecs = (double) ( now - statsSince ) / MILLIS_PER_SECOND;
		return getCachePutCount() / deltaSecs;
	}

	@Override
	public long getCachePutSample() {
		long count = 0;
		for ( String name : cacheManager.getCacheNames() ) {
			final Cache cache = cacheManager.getCache( name );
			if ( cache != null ) {
				count += cache.getStatistics().cachePutOperation().rate().value().longValue();
			}
		}
		return count;
	}

	@Override
	public String getOriginalConfigDeclaration() {
		return this.cacheManager.getOriginalConfigurationText();
	}

	@Override
	public String getOriginalConfigDeclaration(String region) {
		return this.cacheManager.getOriginalConfigurationText( region );
	}

	@Override
	public Map<String, Map<String, Object>> getRegionCacheAttributes() {
		final Map<String, Map<String, Object>> result = new HashMap<String, Map<String, Object>>();
		for ( String regionName : this.cacheManager.getCacheNames() ) {
			result.put( regionName, getRegionCacheAttributes( regionName ) );
		}
		return result;
	}

	@Override
	public Map<String, Object> getRegionCacheAttributes(String regionName) {
		final Map<String, Object> result = new HashMap<String, Object>();
		result.put( "Enabled", isRegionCacheEnabled( regionName ) );
		result.put( "LoggingEnabled", isRegionCacheLoggingEnabled( regionName ) );
		result.put( "MaxTTISeconds", getRegionCacheMaxTTISeconds( regionName ) );
		result.put( "MaxTTLSeconds", getRegionCacheMaxTTLSeconds( regionName ) );
		result.put( "TargetMaxInMemoryCount", getRegionCacheTargetMaxInMemoryCount( regionName ) );
		result.put( "TargetMaxTotalCount", getRegionCacheTargetMaxTotalCount( regionName ) );
		result.put( "OrphanEvictionEnabled", isRegionCacheOrphanEvictionEnabled( regionName ) );
		result.put( "OrphanEvictionPeriod", getRegionCacheOrphanEvictionPeriod( regionName ) );
		return result;
	}

	@Override
	public int getRegionCacheMaxTTISeconds(String region) {
		final Cache cache = cacheManager.getCache( region );
		if ( cache != null ) {
			return (int) cache.getCacheConfiguration().getTimeToIdleSeconds();
		}
		else {
			return -1;
		}
	}

	@Override
	public int getRegionCacheMaxTTLSeconds(String region) {
		final Cache cache = cacheManager.getCache( region );
		if ( cache != null ) {
			return (int) cache.getCacheConfiguration().getTimeToLiveSeconds();
		}
		else {
			return -1;
		}
	}

	@Override
	public int getRegionCacheOrphanEvictionPeriod(String region) {
		final Cache cache = this.cacheManager.getCache( region );
		if ( cache != null && cache.isTerracottaClustered() ) {
			return cache.getCacheConfiguration().getTerracottaConfiguration().getOrphanEvictionPeriod();
		}
		else {
			return -1;
		}
	}

	@Override
	public Map<String, int[]> getRegionCacheSamples() {
		final Map<String, int[]> rv = new HashMap<String, int[]>();
		for ( String name : cacheManager.getCacheNames() ) {
			final Cache cache = cacheManager.getCache( name );
			if ( cache != null ) {
				final Double hits = cache.getStatistics().cacheHitOperation().rate().value();
				final Double misses = cache.getStatistics().cacheMissNotFoundOperation().rate().value();
				final Double expired = cache.getStatistics().cacheMissExpiredOperation().rate().value();
				final Double puts = cache.getStatistics().cachePutOperation().rate().value();
				rv.put(
						name, new int[] {
						hits.intValue(),
						misses.intValue(),
						expired.intValue(),
						puts.intValue()
				}
				);
			}
		}
		return rv;
	}

	@Override
	public int getRegionCacheTargetMaxInMemoryCount(String region) {
		final Cache cache = cacheManager.getCache( region );
		if ( cache != null ) {
			return cache.getCacheConfiguration().getMaxElementsInMemory();
		}
		else {
			return -1;
		}
	}

	@Override
	public int getRegionCacheTargetMaxTotalCount(String region) {
		final Cache cache = cacheManager.getCache( region );
		if ( cache != null ) {
			return cache.getCacheConfiguration().getMaxElementsOnDisk();
		}
		else {
			return -1;
		}
	}

	@Override
	public String[] getTerracottaHibernateCacheRegionNames() {
		final ArrayList<String> rv = new ArrayList<String>();
		for ( String name : cacheManager.getCacheNames() ) {
			final Cache cache = cacheManager.getCache( name );
			if ( cache != null ) {
				if ( cache.getCacheConfiguration().isTerracottaClustered() ) {
					rv.add( name );
				}
			}
		}
		return rv.toArray( new String[rv.size()] );
	}

	@Override
	public boolean isRegionCacheEnabled(String region) {
		final Cache cache = this.cacheManager.getCache( region );
		return cache != null && !cache.isDisabled();
	}

	@Override
	public void setRegionCacheEnabled(String region, boolean enabled) {
		final Cache cache = this.cacheManager.getCache( region );
		if ( cache != null ) {
			cache.setDisabled( !enabled );
		}
		sendNotification( CACHE_REGION_CHANGED, getRegionCacheAttributes( region ), region );
	}

	@Override
	public boolean isRegionCachesEnabled() {
		for ( String name : this.cacheManager.getCacheNames() ) {
			final Cache cache = this.cacheManager.getCache( name );
			if ( cache != null ) {
				if ( cache.isDisabled() ) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public void setRegionCachesEnabled(final boolean flag) {
		for ( String name : this.cacheManager.getCacheNames() ) {
			final Cache cache = this.cacheManager.getCache( name );
			if ( cache != null ) {
				cache.setDisabled( !flag );
			}
		}
		sendNotification( CACHE_ENABLED, flag );
	}

	@Override
	public boolean isRegionCacheLoggingEnabled(String region) {
		final Cache cache = this.cacheManager.getCache( region );
		return cache != null && cache.getCacheConfiguration().getLogging();
	}

	@Override
	public boolean isRegionCacheOrphanEvictionEnabled(String region) {
		final Cache cache = this.cacheManager.getCache( region );
		return cache != null && cache.isTerracottaClustered() && cache.getCacheConfiguration()
				.getTerracottaConfiguration()
				.getOrphanEviction();
	}

	@Override
	public boolean isTerracottaHibernateCache(String region) {
		final Cache cache = cacheManager.getCache( region );
		return cache != null && cache.getCacheConfiguration().isTerracottaClustered();
	}

	@Override
	public void setRegionCacheLoggingEnabled(String region, boolean loggingEnabled) {
		final Cache cache = this.cacheManager.getCache( region );
		if ( cache != null ) {
			cache.getCacheConfiguration().setLogging( loggingEnabled );
			sendNotification( CACHE_REGION_CHANGED, getRegionCacheAttributes( region ), region );
		}
	}

	@Override
	public void setRegionCacheMaxTTISeconds(String region, int maxTTISeconds) {
		final Cache cache = this.cacheManager.getCache( region );
		if ( cache != null ) {
			cache.getCacheConfiguration().setTimeToIdleSeconds( maxTTISeconds );
			sendNotification( CACHE_REGION_CHANGED, getRegionCacheAttributes( region ), region );
		}
	}

	@Override
	public void setRegionCacheMaxTTLSeconds(String region, int maxTTLSeconds) {
		final Cache cache = this.cacheManager.getCache( region );
		if ( cache != null ) {
			cache.getCacheConfiguration().setTimeToLiveSeconds( maxTTLSeconds );
			sendNotification( CACHE_REGION_CHANGED, getRegionCacheAttributes( region ), region );
		}
	}

	@Override
	public void setRegionCacheTargetMaxInMemoryCount(String region, int targetMaxInMemoryCount) {
		final Cache cache = this.cacheManager.getCache( region );
		if ( cache != null ) {
			cache.getCacheConfiguration().setMaxElementsInMemory( targetMaxInMemoryCount );
			sendNotification( CACHE_REGION_CHANGED, getRegionCacheAttributes( region ), region );
		}
	}

	@Override
	public void setRegionCacheTargetMaxTotalCount(String region, int targetMaxTotalCount) {
		final Cache cache = this.cacheManager.getCache( region );
		if ( cache != null ) {
			cache.getCacheConfiguration().setMaxElementsOnDisk( targetMaxTotalCount );
			sendNotification( CACHE_REGION_CHANGED, getRegionCacheAttributes( region ), region );
		}
	}

	@Override
	public int getNumberOfElementsInMemory(String region) {
		final Cache cache = this.cacheManager.getCache( region );
		if ( cache != null ) {
			return (int) cache.getMemoryStoreSize();
		}
		else {
			return -1;
		}
	}

	@Override
	public int getNumberOfElementsOffHeap(String region) {
		final Cache cache = this.cacheManager.getCache( region );
		if ( cache != null ) {
			return (int) cache.getOffHeapStoreSize();
		}
		else {
			return -1;
		}
	}

	@Override
	public int getNumberOfElementsOnDisk(String region) {
		final Cache cache = this.cacheManager.getCache( region );
		if ( cache != null ) {
			return cache.getDiskStoreSize();
		}
		else {
			return -1;
		}
	}

	@Override
	public long getMaxGetTimeMillis() {
		long rv = 0;
		for ( String cacheName : cacheManager.getCacheNames() ) {
			final Cache cache = cacheManager.getCache( cacheName );
			if ( cache != null ) {
				final Long maximum = cache.getStatistics()
						.cacheGetOperation().latency().maximum().value();
				if ( maximum != null ) {
					rv = Math.max(
							rv, TimeUnit.MILLISECONDS.convert(
							maximum,
							TimeUnit.NANOSECONDS
					)
					);
				}
			}
		}
		return rv;
	}

	@Override
	public long getMinGetTimeMillis() {
		long rv = Long.MAX_VALUE;
		for ( String cacheName : cacheManager.getCacheNames() ) {
			final Cache cache = cacheManager.getCache( cacheName );
			if ( cache != null ) {
				final Long minimum = cache.getStatistics()
						.cacheGetOperation()
						.latency()
						.minimum().value();
				if ( minimum != null ) {
					rv = Math.min(
							rv, TimeUnit.MILLISECONDS.convert(
							minimum,
							TimeUnit.NANOSECONDS
					)
					);
				}
			}
		}
		return rv == Long.MAX_VALUE ? 0 : rv;
	}

	@Override
	public long getMaxGetTimeMillis(String cacheName) {
		final Cache cache = cacheManager.getCache( cacheName );
		if ( cache != null ) {
			final Long maximum = cache.getStatistics()
					.cacheGetOperation()
					.latency()
					.maximum().value();
			return maximum == null ? 0 : TimeUnit.MILLISECONDS.convert(
					maximum,
					TimeUnit.NANOSECONDS
			);
		}
		else {
			return 0;
		}
	}

	@Override
	public long getMinGetTimeMillis(String cacheName) {
		final Cache cache = cacheManager.getCache( cacheName );
		if ( cache != null ) {
			final Long minimum = cache.getStatistics()
					.cacheGetOperation()
					.latency()
					.minimum().value();
			return minimum == null ? 0 : TimeUnit.MILLISECONDS.convert(
					minimum,
					TimeUnit.NANOSECONDS
			);
		}
		else {
			return 0;
		}
	}

	@Override
	public float getAverageGetTimeMillis(String region) {
		final Cache cache = this.cacheManager.getCache( region );
		if ( cache != null ) {
			final Double avg = cache.getStatistics()
					.cacheGetOperation()
					.latency()
					.average().value();
			return TimeUnit.MILLISECONDS.convert(
					avg.longValue(),
					TimeUnit.NANOSECONDS
			);
		}
		else {
			return -1f;
		}
	}

	@Override
	protected void doDispose() {
		// no-op
	}

	@Override
	public MBeanNotificationInfo[] getNotificationInfo() {
		return new MBeanNotificationInfo[] { NOTIFICATION_INFO };
	}
}
