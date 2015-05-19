/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.ehcache.management.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.hibernate.management.api.EhcacheStats;
import net.sf.ehcache.management.sampled.SampledCacheManager;

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

	private final SampledCacheManager sampledCacheManager;
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
	 * @throws javax.management.NotCompliantMBeanException should registering the MBean fail
	 */
	public EhcacheStatsImpl(CacheManager manager) throws NotCompliantMBeanException {
		super( EhcacheStats.class );
		this.sampledCacheManager = new SampledCacheManager( manager );
		this.cacheManager = manager;
	}

	@Override
	public boolean isStatisticsEnabled() {
		return false;
	}

	@Override
	public void clearStats() {
		sampledCacheManager.clearStatistics();
		statsSince = System.currentTimeMillis();
	}

	@Override
	public void disableStats() {
		setStatisticsEnabled( false );
	}

	@Override
	public void enableStats() {
		setStatisticsEnabled( true );
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
				count += cache.getLiveCacheStatistics().getCacheHitCount();
			}
		}
		return count;
	}

	@Override
	public double getCacheHitRate() {
		final long now = System.currentTimeMillis();
		final double deltaSecs = (double) (now - statsSince) / MILLIS_PER_SECOND;
		return getCacheHitCount() / deltaSecs;
	}

	@Override
	public long getCacheHitSample() {
		long count = 0;
		for ( String name : cacheManager.getCacheNames() ) {
			final Cache cache = cacheManager.getCache( name );
			if ( cache != null ) {
				count += cache.getSampledCacheStatistics().getCacheHitMostRecentSample();
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
				count += cache.getLiveCacheStatistics().getCacheMissCount();
			}
		}
		return count;
	}

	@Override
	public double getCacheMissRate() {
		final long now = System.currentTimeMillis();
		final double deltaSecs = (double) (now - statsSince) / MILLIS_PER_SECOND;
		return getCacheMissCount() / deltaSecs;
	}

	@Override
	public long getCacheMissSample() {
		long count = 0;
		for ( String name : cacheManager.getCacheNames() ) {
			final Cache cache = cacheManager.getCache( name );
			if ( cache != null ) {
				count += cache.getSampledCacheStatistics().getCacheMissMostRecentSample();
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
				count += cache.getLiveCacheStatistics().getPutCount();
			}
		}
		return count;
	}

	@Override
	public double getCachePutRate() {
		final long now = System.currentTimeMillis();
		final double deltaSecs = (double) (now - statsSince) / MILLIS_PER_SECOND;
		return getCachePutCount() / deltaSecs;
	}

	@Override
	public long getCachePutSample() {
		long count = 0;
		for ( String name : cacheManager.getCacheNames() ) {
			final Cache cache = cacheManager.getCache( name );
			if ( cache != null ) {
				count += cache.getSampledCacheStatistics().getCacheElementPutMostRecentSample();
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
				rv.put(
						name, new int[] {
						(int) cache.getSampledCacheStatistics().getCacheHitMostRecentSample(),
						(int) (cache.getSampledCacheStatistics().getCacheMissNotFoundMostRecentSample()
								+ cache.getSampledCacheStatistics().getCacheMissExpiredMostRecentSample()),
						(int) cache.getSampledCacheStatistics().getCacheElementPutMostRecentSample(),
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
		return rv.toArray( new String[ rv.size() ] );
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
	public void setStatisticsEnabled(boolean flag) {
		for ( String cacheName : cacheManager.getCacheNames() ) {
			final Cache cache = cacheManager.getCache( cacheName );
			if ( cache != null ) {
				cache.setStatisticsEnabled( flag );
			}
		}
		if ( flag ) {
			clearStats();
		}
		sendNotification( CACHE_STATISTICS_ENABLED, flag );
	}

	@Override
	public long getMaxGetTimeMillis() {
		long rv = 0;
		for ( String cacheName : cacheManager.getCacheNames() ) {
			final Cache cache = cacheManager.getCache( cacheName );
			if ( cache != null ) {
				rv = Math.max( rv, cache.getLiveCacheStatistics().getMaxGetTimeMillis() );
			}
		}
		return rv;
	}

	@Override
	public long getMinGetTimeMillis() {
		long rv = 0;
		for ( String cacheName : cacheManager.getCacheNames() ) {
			final Cache cache = cacheManager.getCache( cacheName );
			if ( cache != null ) {
				rv = Math.max( rv, cache.getLiveCacheStatistics().getMinGetTimeMillis() );
			}
		}
		return rv;
	}

	@Override
	public long getMaxGetTimeMillis(String cacheName) {
		final Cache cache = cacheManager.getCache( cacheName );
		if ( cache != null ) {
			return cache.getLiveCacheStatistics().getMaxGetTimeMillis();
		}
		else {
			return 0;
		}
	}

	@Override
	public long getMinGetTimeMillis(String cacheName) {
		final Cache cache = cacheManager.getCache( cacheName );
		if ( cache != null ) {
			return cache.getLiveCacheStatistics().getMinGetTimeMillis();
		}
		else {
			return 0;
		}
	}

	@Override
	public float getAverageGetTimeMillis(String region) {
		final Cache cache = this.cacheManager.getCache( region );
		if ( cache != null ) {
			return cache.getLiveCacheStatistics().getAverageGetTimeMillis();
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
		return new MBeanNotificationInfo[] {NOTIFICATION_INFO};
	}
}
