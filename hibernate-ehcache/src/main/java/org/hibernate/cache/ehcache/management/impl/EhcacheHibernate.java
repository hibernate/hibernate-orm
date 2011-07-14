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

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.openmbean.TabularData;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.hibernate.management.api.EhcacheHibernateMBean;
import net.sf.ehcache.hibernate.management.api.EhcacheStats;
import net.sf.ehcache.hibernate.management.api.HibernateStats;

import org.hibernate.SessionFactory;

/**
 * Implementation of the {@link EhcacheHibernateMBean}
 * <p/>
 * <p/>
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 */
public class EhcacheHibernate extends BaseEmitterBean implements EhcacheHibernateMBean {
	private static final MBeanNotificationInfo NOTIFICATION_INFO;

	private final AtomicBoolean statsEnabled = new AtomicBoolean( true );
	private EhcacheStats ehcacheStats;
	private volatile HibernateStats hibernateStats = NullHibernateStats.INSTANCE;

	static {
		final String[] notifTypes = new String[] { };
		final String name = Notification.class.getName();
		final String description = "Ehcache Hibernate Statistics Event";
		NOTIFICATION_INFO = new MBeanNotificationInfo( notifTypes, name, description );
	}

	/**
	 * Constructor accepting the backing {@link CacheManager}
	 *
	 * @param manager the backing {@link CacheManager}
	 *
	 * @throws NotCompliantMBeanException
	 */
	public EhcacheHibernate(CacheManager manager) throws NotCompliantMBeanException {
		super( EhcacheHibernateMBean.class );
		ehcacheStats = new EhcacheStatsImpl( manager );
	}

	/**
	 * Enable hibernate statistics with the input session factory
	 */
	public void enableHibernateStatistics(SessionFactory sessionFactory) {
		try {
			hibernateStats = new HibernateStatsImpl( sessionFactory );
		}
		catch ( Exception e ) {
			throw new RuntimeException( e );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isHibernateStatisticsSupported() {
		return hibernateStats instanceof HibernateStatsImpl;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setStatisticsEnabled(boolean flag) {
		if ( flag ) {
			ehcacheStats.enableStats();
			hibernateStats.enableStats();
		}
		else {
			ehcacheStats.disableStats();
			hibernateStats.disableStats();
		}
		statsEnabled.set( flag );
		sendNotification( HibernateStats.CACHE_STATISTICS_ENABLED, flag );
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isStatisticsEnabled() {
		return statsEnabled.get();
	}

	/**
	 * {@inheritDoc}
	 */
	public void clearStats() {
		ehcacheStats.clearStats();
		hibernateStats.clearStats();
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
		ehcacheStats.flushRegionCache( region );
		sendNotification( HibernateStats.CACHE_REGION_FLUSHED, region );
	}

	/**
	 * {@inheritDoc}
	 */
	public void flushRegionCaches() {
		ehcacheStats.flushRegionCaches();
		sendNotification( HibernateStats.CACHE_FLUSHED );
	}

	/**
	 * {@inheritDoc}
	 */
	public String generateActiveConfigDeclaration() {
		return ehcacheStats.generateActiveConfigDeclaration();
	}

	/**
	 * {@inheritDoc}
	 */
	public String generateActiveConfigDeclaration(String region) {
		return ehcacheStats.generateActiveConfigDeclaration( region );
	}

	/**
	 * {@inheritDoc}
	 */
	public long getCacheHitCount() {
		return ehcacheStats.getCacheHitCount();
	}

	/**
	 * {@inheritDoc}
	 */
	public double getCacheHitRate() {
		return ehcacheStats.getCacheHitRate();
	}

	/**
	 * {@inheritDoc}
	 */
	public long getCacheHitSample() {
		return ehcacheStats.getCacheHitSample();
	}

	/**
	 * {@inheritDoc}
	 */
	public long getCacheMissCount() {
		return ehcacheStats.getCacheMissCount();
	}

	/**
	 * {@inheritDoc}
	 */
	public double getCacheMissRate() {
		return ehcacheStats.getCacheMissRate();
	}

	/**
	 * {@inheritDoc}
	 */
	public long getCacheMissSample() {
		return ehcacheStats.getCacheMissSample();
	}

	/**
	 * {@inheritDoc}
	 */
	public long getCachePutCount() {
		return ehcacheStats.getCachePutCount();
	}

	/**
	 * {@inheritDoc}
	 */
	public double getCachePutRate() {
		return ehcacheStats.getCachePutRate();
	}

	/**
	 * {@inheritDoc}
	 */
	public long getCachePutSample() {
		return ehcacheStats.getCachePutSample();
	}

	/**
	 * {@inheritDoc}
	 */
	public TabularData getCacheRegionStats() {
		return hibernateStats.getCacheRegionStats();
	}

	/**
	 * {@inheritDoc}
	 */
	public long getCloseStatementCount() {
		return hibernateStats.getCloseStatementCount();
	}

	/**
	 * {@inheritDoc}
	 */
	public TabularData getCollectionStats() {
		return hibernateStats.getCollectionStats();
	}

	/**
	 * {@inheritDoc}
	 */
	public long getConnectCount() {
		return hibernateStats.getConnectCount();
	}

	/**
	 * {@inheritDoc}
	 */
	public TabularData getEntityStats() {
		return hibernateStats.getEntityStats();
	}

	/**
	 * {@inheritDoc}
	 */
	public long getFlushCount() {
		return hibernateStats.getFlushCount();
	}

	/**
	 * {@inheritDoc}
	 */
	public long getOptimisticFailureCount() {
		return hibernateStats.getOptimisticFailureCount();
	}

	/**
	 * {@inheritDoc}
	 */
	public String getOriginalConfigDeclaration() {
		return ehcacheStats.getOriginalConfigDeclaration();
	}

	/**
	 * {@inheritDoc}
	 */
	public String getOriginalConfigDeclaration(String region) {
		return ehcacheStats.getOriginalConfigDeclaration( region );
	}

	/**
	 * {@inheritDoc}
	 */
	public long getPrepareStatementCount() {
		return hibernateStats.getPrepareStatementCount();
	}

	/**
	 * {@inheritDoc}
	 */
	public long getQueryExecutionCount() {
		return hibernateStats.getQueryExecutionCount();
	}

	/**
	 * {@inheritDoc}
	 */
	public double getQueryExecutionRate() {
		return hibernateStats.getQueryExecutionRate();
	}

	/**
	 * {@inheritDoc}
	 */
	public long getQueryExecutionSample() {
		return hibernateStats.getQueryExecutionSample();
	}

	/**
	 * {@inheritDoc}
	 */
	public TabularData getQueryStats() {
		return hibernateStats.getQueryStats();
	}

	/**
	 * {@inheritDoc}
	 */
	public Map<String, Map<String, Object>> getRegionCacheAttributes() {
		return ehcacheStats.getRegionCacheAttributes();
	}

	/**
	 * {@inheritDoc}
	 */
	public Map<String, Object> getRegionCacheAttributes(String regionName) {
		return ehcacheStats.getRegionCacheAttributes( regionName );
	}

	/**
	 * {@inheritDoc}
	 */
	public int getRegionCacheMaxTTISeconds(String region) {
		return ehcacheStats.getRegionCacheMaxTTISeconds( region );
	}

	/**
	 * {@inheritDoc}
	 */
	public int getRegionCacheMaxTTLSeconds(String region) {
		return ehcacheStats.getRegionCacheMaxTTLSeconds( region );
	}

	/**
	 * {@inheritDoc}
	 */
	public int getRegionCacheOrphanEvictionPeriod(String region) {
		return ehcacheStats.getRegionCacheOrphanEvictionPeriod( region );
	}

	/**
	 * {@inheritDoc}
	 */
	public Map<String, int[]> getRegionCacheSamples() {
		return ehcacheStats.getRegionCacheSamples();
	}

	/**
	 * {@inheritDoc}
	 */
	public int getRegionCacheTargetMaxInMemoryCount(String region) {
		return ehcacheStats.getRegionCacheTargetMaxInMemoryCount( region );
	}

	/**
	 * {@inheritDoc}
	 */
	public int getRegionCacheTargetMaxTotalCount(String region) {
		return ehcacheStats.getRegionCacheTargetMaxTotalCount( region );
	}

	/**
	 * {@inheritDoc}
	 */
	public long getSessionCloseCount() {
		return hibernateStats.getSessionCloseCount();
	}

	/**
	 * {@inheritDoc}
	 */
	public long getSessionOpenCount() {
		return hibernateStats.getSessionOpenCount();
	}

	/**
	 * {@inheritDoc}
	 */
	public long getSuccessfulTransactionCount() {
		return hibernateStats.getSuccessfulTransactionCount();
	}

	/**
	 * {@inheritDoc}
	 */
	public String[] getTerracottaHibernateCacheRegionNames() {
		return ehcacheStats.getTerracottaHibernateCacheRegionNames();
	}

	/**
	 * {@inheritDoc}
	 */
	public long getTransactionCount() {
		return hibernateStats.getTransactionCount();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isRegionCacheEnabled(String region) {
		return ehcacheStats.isRegionCacheEnabled( region );
	}

	/**
	 * {@inheritDoc}
	 */
	public void setRegionCachesEnabled(boolean enabled) {
		ehcacheStats.setRegionCachesEnabled( enabled );
		sendNotification( HibernateStats.CACHE_ENABLED, enabled );
	}

	/**
	 * {@inheritDoc}
	 */
	public void setRegionCacheEnabled(String region, boolean enabled) {
		ehcacheStats.setRegionCacheEnabled( region, enabled );
		sendNotification( HibernateStats.CACHE_REGION_CHANGED, getRegionCacheAttributes( region ), region );
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isRegionCacheLoggingEnabled(String region) {
		return ehcacheStats.isRegionCacheLoggingEnabled( region );
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isRegionCacheOrphanEvictionEnabled(String region) {
		return ehcacheStats.isRegionCacheOrphanEvictionEnabled( region );
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isRegionCachesEnabled() {
		return ehcacheStats.isRegionCachesEnabled();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isTerracottaHibernateCache(String region) {
		return ehcacheStats.isTerracottaHibernateCache( region );
	}

	/**
	 * {@inheritDoc}
	 */
	public void setRegionCacheLoggingEnabled(String region, boolean loggingEnabled) {
		ehcacheStats.setRegionCacheLoggingEnabled( region, loggingEnabled );
		sendNotification( HibernateStats.CACHE_REGION_CHANGED, getRegionCacheAttributes( region ), region );
	}

	/**
	 * {@inheritDoc}
	 */
	public void setRegionCacheMaxTTISeconds(String region, int maxTTISeconds) {
		ehcacheStats.setRegionCacheMaxTTISeconds( region, maxTTISeconds );
		sendNotification( HibernateStats.CACHE_REGION_CHANGED, getRegionCacheAttributes( region ), region );
	}

	/**
	 * {@inheritDoc}
	 */
	public void setRegionCacheMaxTTLSeconds(String region, int maxTTLSeconds) {
		ehcacheStats.setRegionCacheMaxTTLSeconds( region, maxTTLSeconds );
		sendNotification( HibernateStats.CACHE_REGION_CHANGED, getRegionCacheAttributes( region ), region );
	}

	/**
	 * {@inheritDoc}
	 */
	public void setRegionCacheTargetMaxInMemoryCount(String region, int targetMaxInMemoryCount) {
		ehcacheStats.setRegionCacheTargetMaxInMemoryCount( region, targetMaxInMemoryCount );
		sendNotification( HibernateStats.CACHE_REGION_CHANGED, getRegionCacheAttributes( region ), region );
	}

	/**
	 * {@inheritDoc}
	 */
	public void setRegionCacheTargetMaxTotalCount(String region, int targetMaxTotalCount) {
		ehcacheStats.setRegionCacheTargetMaxTotalCount( region, targetMaxTotalCount );
		sendNotification( HibernateStats.CACHE_REGION_CHANGED, getRegionCacheAttributes( region ), region );
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see EhcacheStats#getNumberOfElementsInMemory(java.lang.String)
	 */
	public int getNumberOfElementsInMemory(String region) {
		return ehcacheStats.getNumberOfElementsInMemory( region );
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see EhcacheStats#getNumberOfElementsInMemory(java.lang.String)
	 */
	public int getNumberOfElementsOffHeap(String region) {
		return ehcacheStats.getNumberOfElementsOffHeap( region );
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see EhcacheStats#getNumberOfElementsOnDisk(java.lang.String)
	 */
	public int getNumberOfElementsOnDisk(String region) {
		return ehcacheStats.getNumberOfElementsOnDisk( region );
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see EhcacheStats#getMaxGetTimeMillis()
	 */
	public long getMaxGetTimeMillis() {
		return ehcacheStats.getMaxGetTimeMillis();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see EhcacheStats#getMaxGetTimeMillis(java.lang.String)
	 */
	public long getMaxGetTimeMillis(String cacheName) {
		return ehcacheStats.getMaxGetTimeMillis( cacheName );
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see EhcacheStats#getMinGetTimeMillis()
	 */
	public long getMinGetTimeMillis() {
		return ehcacheStats.getMinGetTimeMillis();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see EhcacheStats#getMinGetTimeMillis(java.lang.String)
	 */
	public long getMinGetTimeMillis(String cacheName) {
		return ehcacheStats.getMinGetTimeMillis( cacheName );
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see EhcacheStats#getAverageGetTimeMillis(java.lang.String)
	 */
	public float getAverageGetTimeMillis(String region) {
		return ehcacheStats.getAverageGetTimeMillis( region );
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
