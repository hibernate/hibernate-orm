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
public class EhcacheStatsImpl extends BaseEmitterBean implements EhcacheStats {
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
	 * @throws javax.management.NotCompliantMBeanException
	 */
	public EhcacheStatsImpl(CacheManager manager) throws NotCompliantMBeanException {
		super( EhcacheStats.class );
		this.sampledCacheManager = new SampledCacheManager( manager );
		this.cacheManager = manager;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isStatisticsEnabled() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public void clearStats() {
		sampledCacheManager.clearStatistics();
		statsSince = System.currentTimeMillis();
	}

	/**
	 * {@inheritDoc}
	 */
	public void disableStats() {
		setStatisticsEnabled( false );
	}

	/**
	 * {@inheritDoc}
	 */
	public void enableStats() {
		setStatisticsEnabled( true );
	}

	/**
	 * {@inheritDoc}
	 */
	public void flushRegionCache(String region) {
		Cache cache = this.cacheManager.getCache( region );
		if ( cache != null ) {
			cache.flush();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void flushRegionCaches() {
		for ( String name : cacheManager.getCacheNames() ) {
			Cache cache = this.cacheManager.getCache( name );
			if ( cache != null ) {
				cache.flush();
			}
		}

	}

	/**
	 * {@inheritDoc}
	 */
	public String generateActiveConfigDeclaration() {
		return this.cacheManager.getActiveConfigurationText();
	}

	/**
	 * {@inheritDoc}
	 */
	public String generateActiveConfigDeclaration(String region) {
		return this.cacheManager.getActiveConfigurationText( region );
	}

	/**
	 * {@inheritDoc}
	 */
	public long getCacheHitCount() {
		long count = 0;
		for ( String name : cacheManager.getCacheNames() ) {
			Cache cache = cacheManager.getCache( name );
			if ( cache != null ) {
				count += cache.getLiveCacheStatistics().getCacheHitCount();
			}
		}
		return count;
	}

	/**
	 * {@inheritDoc}
	 */
	public double getCacheHitRate() {
		long now = System.currentTimeMillis();
		double deltaSecs = (double) ( now - statsSince ) / MILLIS_PER_SECOND;
		return getCacheHitCount() / deltaSecs;
	}

	/**
	 * {@inheritDoc}
	 */
	public long getCacheHitSample() {
		long count = 0;
		for ( String name : cacheManager.getCacheNames() ) {
			Cache cache = cacheManager.getCache( name );
			if ( cache != null ) {
				count += cache.getSampledCacheStatistics().getCacheHitMostRecentSample();
			}
		}
		return count;
	}

	/**
	 * {@inheritDoc}
	 */
	public long getCacheMissCount() {
		long count = 0;
		for ( String name : cacheManager.getCacheNames() ) {
			Cache cache = cacheManager.getCache( name );
			if ( cache != null ) {
				count += cache.getLiveCacheStatistics().getCacheMissCount();
			}
		}
		return count;
	}

	/**
	 * {@inheritDoc}
	 */
	public double getCacheMissRate() {
		long now = System.currentTimeMillis();
		double deltaSecs = (double) ( now - statsSince ) / MILLIS_PER_SECOND;
		return getCacheMissCount() / deltaSecs;
	}

	/**
	 * {@inheritDoc}
	 */
	public long getCacheMissSample() {
		long count = 0;
		for ( String name : cacheManager.getCacheNames() ) {
			Cache cache = cacheManager.getCache( name );
			if ( cache != null ) {
				count += cache.getSampledCacheStatistics().getCacheMissMostRecentSample();
			}
		}
		return count;
	}

	/**
	 * {@inheritDoc}
	 */
	public long getCachePutCount() {
		long count = 0;
		for ( String name : cacheManager.getCacheNames() ) {
			Cache cache = cacheManager.getCache( name );
			if ( cache != null ) {
				count += cache.getLiveCacheStatistics().getPutCount();
			}
		}
		return count;
	}

	/**
	 * {@inheritDoc}
	 */
	public double getCachePutRate() {
		long now = System.currentTimeMillis();
		double deltaSecs = (double) ( now - statsSince ) / MILLIS_PER_SECOND;
		return getCachePutCount() / deltaSecs;
	}

	/**
	 * {@inheritDoc}
	 */
	public long getCachePutSample() {
		long count = 0;
		for ( String name : cacheManager.getCacheNames() ) {
			Cache cache = cacheManager.getCache( name );
			if ( cache != null ) {
				count += cache.getSampledCacheStatistics().getCacheElementPutMostRecentSample();
			}
		}
		return count;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getOriginalConfigDeclaration() {
		return this.cacheManager.getOriginalConfigurationText();
	}

	/**
	 * {@inheritDoc}
	 */
	public String getOriginalConfigDeclaration(String region) {
		return this.cacheManager.getOriginalConfigurationText( region );
	}

	/**
	 * {@inheritDoc}
	 */
	public Map<String, Map<String, Object>> getRegionCacheAttributes() {
		Map<String, Map<String, Object>> result = new HashMap<String, Map<String, Object>>();
		for ( String regionName : this.cacheManager.getCacheNames() ) {
			result.put( regionName, getRegionCacheAttributes( regionName ) );
		}
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	public Map<String, Object> getRegionCacheAttributes(String regionName) {
		Map<String, Object> result = new HashMap<String, Object>();
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

	/**
	 * {@inheritDoc}
	 */
	public int getRegionCacheMaxTTISeconds(String region) {
		Cache cache = cacheManager.getCache( region );
		if ( cache != null ) {
			return (int) cache.getCacheConfiguration().getTimeToIdleSeconds();
		}
		else {
			return -1;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public int getRegionCacheMaxTTLSeconds(String region) {
		Cache cache = cacheManager.getCache( region );
		if ( cache != null ) {
			return (int) cache.getCacheConfiguration().getTimeToLiveSeconds();
		}
		else {
			return -1;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public int getRegionCacheOrphanEvictionPeriod(String region) {
		Cache cache = this.cacheManager.getCache( region );
		if ( cache != null && cache.isTerracottaClustered() ) {
			return cache.getCacheConfiguration().getTerracottaConfiguration().getOrphanEvictionPeriod();
		}
		else {
			return -1;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Map<String, int[]> getRegionCacheSamples() {
		Map<String, int[]> rv = new HashMap<String, int[]>();
		for ( String name : cacheManager.getCacheNames() ) {
			Cache cache = cacheManager.getCache( name );
			if ( cache != null ) {
				rv.put(
						name, new int[] {
						(int) cache.getSampledCacheStatistics().getCacheHitMostRecentSample(),
						(int) ( cache.getSampledCacheStatistics().getCacheMissNotFoundMostRecentSample()
								+ cache.getSampledCacheStatistics().getCacheMissExpiredMostRecentSample() ),
						(int) cache.getSampledCacheStatistics().getCacheElementPutMostRecentSample(),
				}
				);
			}
		}
		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getRegionCacheTargetMaxInMemoryCount(String region) {
		Cache cache = cacheManager.getCache( region );
		if ( cache != null ) {
			return cache.getCacheConfiguration().getMaxElementsInMemory();
		}
		else {
			return -1;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public int getRegionCacheTargetMaxTotalCount(String region) {
		Cache cache = cacheManager.getCache( region );
		if ( cache != null ) {
			return cache.getCacheConfiguration().getMaxElementsOnDisk();
		}
		else {
			return -1;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public String[] getTerracottaHibernateCacheRegionNames() {
		ArrayList<String> rv = new ArrayList<String>();
		for ( String name : cacheManager.getCacheNames() ) {
			Cache cache = cacheManager.getCache( name );
			if ( cache != null ) {
				if ( cache.getCacheConfiguration().isTerracottaClustered() ) {
					rv.add( name );
				}
			}
		}
		return rv.toArray( new String[] { } );
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isRegionCacheEnabled(String region) {
		Cache cache = this.cacheManager.getCache( region );
		if ( cache != null ) {
			return !cache.isDisabled();
		}
		else {
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setRegionCacheEnabled(String region, boolean enabled) {
		Cache cache = this.cacheManager.getCache( region );
		if ( cache != null ) {
			cache.setDisabled( !enabled );
		}
		sendNotification( CACHE_REGION_CHANGED, getRegionCacheAttributes( region ), region );
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isRegionCachesEnabled() {
		for ( String name : this.cacheManager.getCacheNames() ) {
			Cache cache = this.cacheManager.getCache( name );
			if ( cache != null ) {
				if ( cache.isDisabled() ) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * @see EhcacheStats#setRegionCachesEnabled(boolean)
	 */
	public void setRegionCachesEnabled(final boolean flag) {
		for ( String name : this.cacheManager.getCacheNames() ) {
			Cache cache = this.cacheManager.getCache( name );
			if ( cache != null ) {
				cache.setDisabled( !flag );
			}
		}
		sendNotification( CACHE_ENABLED, flag );
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isRegionCacheLoggingEnabled(String region) {
		Cache cache = this.cacheManager.getCache( region );
		if ( cache != null ) {
			return cache.getCacheConfiguration().getLogging();
		}
		else {
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isRegionCacheOrphanEvictionEnabled(String region) {
		Cache cache = this.cacheManager.getCache( region );
		if ( cache != null && cache.isTerracottaClustered() ) {
			return cache.getCacheConfiguration().getTerracottaConfiguration().getOrphanEviction();
		}
		else {
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isTerracottaHibernateCache(String region) {
		Cache cache = cacheManager.getCache( region );
		if ( cache != null ) {
			return cache.getCacheConfiguration().isTerracottaClustered();
		}
		else {
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setRegionCacheLoggingEnabled(String region, boolean loggingEnabled) {
		Cache cache = this.cacheManager.getCache( region );
		if ( cache != null ) {
			cache.getCacheConfiguration().setLogging( loggingEnabled );
			sendNotification( CACHE_REGION_CHANGED, getRegionCacheAttributes( region ), region );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setRegionCacheMaxTTISeconds(String region, int maxTTISeconds) {
		Cache cache = this.cacheManager.getCache( region );
		if ( cache != null ) {
			cache.getCacheConfiguration().setTimeToIdleSeconds( maxTTISeconds );
			sendNotification( CACHE_REGION_CHANGED, getRegionCacheAttributes( region ), region );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setRegionCacheMaxTTLSeconds(String region, int maxTTLSeconds) {
		Cache cache = this.cacheManager.getCache( region );
		if ( cache != null ) {
			cache.getCacheConfiguration().setTimeToLiveSeconds( maxTTLSeconds );
			sendNotification( CACHE_REGION_CHANGED, getRegionCacheAttributes( region ), region );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setRegionCacheTargetMaxInMemoryCount(String region, int targetMaxInMemoryCount) {
		Cache cache = this.cacheManager.getCache( region );
		if ( cache != null ) {
			cache.getCacheConfiguration().setMaxElementsInMemory( targetMaxInMemoryCount );
			sendNotification( CACHE_REGION_CHANGED, getRegionCacheAttributes( region ), region );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setRegionCacheTargetMaxTotalCount(String region, int targetMaxTotalCount) {
		Cache cache = this.cacheManager.getCache( region );
		if ( cache != null ) {
			cache.getCacheConfiguration().setMaxElementsOnDisk( targetMaxTotalCount );
			sendNotification( CACHE_REGION_CHANGED, getRegionCacheAttributes( region ), region );
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see EhcacheStats#getNumberOfElementsInMemory(java.lang.String)
	 */
	public int getNumberOfElementsInMemory(String region) {
		Cache cache = this.cacheManager.getCache( region );
		if ( cache != null ) {
			return (int) cache.getMemoryStoreSize();
		}
		else {
			return -1;
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see EhcacheStats#getNumberOfElementsOffHeap(java.lang.String)
	 */
	public int getNumberOfElementsOffHeap(String region) {
		Cache cache = this.cacheManager.getCache( region );
		if ( cache != null ) {
			return (int) cache.getOffHeapStoreSize();
		}
		else {
			return -1;
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see EhcacheStats#getNumberOfElementsOnDisk(java.lang.String)
	 */
	public int getNumberOfElementsOnDisk(String region) {
		Cache cache = this.cacheManager.getCache( region );
		if ( cache != null ) {
			return cache.getDiskStoreSize();
		}
		else {
			return -1;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setStatisticsEnabled(boolean flag) {
		for ( String cacheName : cacheManager.getCacheNames() ) {
			Cache cache = cacheManager.getCache( cacheName );
			if ( cache != null ) {
				cache.setStatisticsEnabled( flag );
			}
		}
		if ( flag ) {
			clearStats();
		}
		sendNotification( CACHE_STATISTICS_ENABLED, flag );
	}

	/**
	 * {@inheritDoc}
	 */
	public long getMaxGetTimeMillis() {
		long rv = 0;
		for ( String cacheName : cacheManager.getCacheNames() ) {
			Cache cache = cacheManager.getCache( cacheName );
			if ( cache != null ) {
				rv = Math.max( rv, cache.getLiveCacheStatistics().getMaxGetTimeMillis() );
			}
		}
		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public long getMinGetTimeMillis() {
		long rv = 0;
		for ( String cacheName : cacheManager.getCacheNames() ) {
			Cache cache = cacheManager.getCache( cacheName );
			if ( cache != null ) {
				rv = Math.max( rv, cache.getLiveCacheStatistics().getMinGetTimeMillis() );
			}
		}
		return rv;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see EhcacheStats#getMaxGetTimeMillis(java.lang.String)
	 */
	public long getMaxGetTimeMillis(String cacheName) {
		Cache cache = cacheManager.getCache( cacheName );
		if ( cache != null ) {
			return cache.getLiveCacheStatistics().getMaxGetTimeMillis();
		}
		else {
			return 0;
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see EhcacheStats#getMinGetTimeMillis(java.lang.String)
	 */
	public long getMinGetTimeMillis(String cacheName) {
		Cache cache = cacheManager.getCache( cacheName );
		if ( cache != null ) {
			return cache.getLiveCacheStatistics().getMinGetTimeMillis();
		}
		else {
			return 0;
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see EhcacheStats#getAverageGetTimeMillis(java.lang.String)
	 */
	public float getAverageGetTimeMillis(String region) {
		Cache cache = this.cacheManager.getCache( region );
		if ( cache != null ) {
			return cache.getLiveCacheStatistics().getAverageGetTimeMillis();
		}
		else {
			return -1f;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doDispose() {
		// no-op
	}

	/**
	 * @see BaseEmitterBean#getNotificationInfo()
	 */
	@Override
	public MBeanNotificationInfo[] getNotificationInfo() {
		return new MBeanNotificationInfo[] { NOTIFICATION_INFO };
	}
}
