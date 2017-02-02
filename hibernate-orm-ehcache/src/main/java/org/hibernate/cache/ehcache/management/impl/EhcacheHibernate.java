/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
public class EhcacheHibernate extends AbstractEmitterBean implements EhcacheHibernateMBean {
	private static final MBeanNotificationInfo NOTIFICATION_INFO;

	private final AtomicBoolean statsEnabled = new AtomicBoolean( true );
	private EhcacheStats ehcacheStats;
	private volatile HibernateStats hibernateStats = NullHibernateStats.INSTANCE;

	static {
		final String[] notifTypes = new String[] {};
		final String name = Notification.class.getName();
		final String description = "Ehcache Hibernate Statistics Event";
		NOTIFICATION_INFO = new MBeanNotificationInfo( notifTypes, name, description );
	}

	/**
	 * Constructor accepting the backing {@link CacheManager}
	 *
	 * @param manager the backing {@link CacheManager}
	 *
	 * @throws NotCompliantMBeanException if bean doesn't comply
	 */
	public EhcacheHibernate(CacheManager manager) throws NotCompliantMBeanException {
		super( EhcacheHibernateMBean.class );
		ehcacheStats = new EhcacheStatsImpl( manager );
	}

	/**
	 * Enable hibernate statistics with the input session factory
	 * @param sessionFactory the session factory to enable stats for
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
	@Override
	public boolean isHibernateStatisticsSupported() {
		return hibernateStats instanceof HibernateStatsImpl;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setStatisticsEnabled(boolean flag) {
		if ( flag ) {
			hibernateStats.enableStats();
		}
		else {
			hibernateStats.disableStats();
		}
		statsEnabled.set( flag );
		sendNotification( HibernateStats.CACHE_STATISTICS_ENABLED, flag );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isStatisticsEnabled() {
		return statsEnabled.get();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clearStats() {
		hibernateStats.clearStats();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void disableStats() {
		setStatisticsEnabled( false );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void enableStats() {
		setStatisticsEnabled( true );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void flushRegionCache(String region) {
		ehcacheStats.flushRegionCache( region );
		sendNotification( HibernateStats.CACHE_REGION_FLUSHED, region );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void flushRegionCaches() {
		ehcacheStats.flushRegionCaches();
		sendNotification( HibernateStats.CACHE_FLUSHED );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateActiveConfigDeclaration() {
		return ehcacheStats.generateActiveConfigDeclaration();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateActiveConfigDeclaration(String region) {
		return ehcacheStats.generateActiveConfigDeclaration( region );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getCacheHitCount() {
		return ehcacheStats.getCacheHitCount();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double getCacheHitRate() {
		return ehcacheStats.getCacheHitRate();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getCacheHitSample() {
		return ehcacheStats.getCacheHitSample();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getCacheMissCount() {
		return ehcacheStats.getCacheMissCount();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double getCacheMissRate() {
		return ehcacheStats.getCacheMissRate();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getCacheMissSample() {
		return ehcacheStats.getCacheMissSample();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getCachePutCount() {
		return ehcacheStats.getCachePutCount();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double getCachePutRate() {
		return ehcacheStats.getCachePutRate();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getCachePutSample() {
		return ehcacheStats.getCachePutSample();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TabularData getCacheRegionStats() {
		return hibernateStats.getCacheRegionStats();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getCloseStatementCount() {
		return hibernateStats.getCloseStatementCount();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TabularData getCollectionStats() {
		return hibernateStats.getCollectionStats();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getConnectCount() {
		return hibernateStats.getConnectCount();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TabularData getEntityStats() {
		return hibernateStats.getEntityStats();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getFlushCount() {
		return hibernateStats.getFlushCount();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getOptimisticFailureCount() {
		return hibernateStats.getOptimisticFailureCount();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getOriginalConfigDeclaration() {
		return ehcacheStats.getOriginalConfigDeclaration();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getOriginalConfigDeclaration(String region) {
		return ehcacheStats.getOriginalConfigDeclaration( region );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getPrepareStatementCount() {
		return hibernateStats.getPrepareStatementCount();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getQueryExecutionCount() {
		return hibernateStats.getQueryExecutionCount();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double getQueryExecutionRate() {
		return hibernateStats.getQueryExecutionRate();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getQueryExecutionSample() {
		return hibernateStats.getQueryExecutionSample();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TabularData getQueryStats() {
		return hibernateStats.getQueryStats();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, Map<String, Object>> getRegionCacheAttributes() {
		return ehcacheStats.getRegionCacheAttributes();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, Object> getRegionCacheAttributes(String regionName) {
		return ehcacheStats.getRegionCacheAttributes( regionName );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getRegionCacheMaxTTISeconds(String region) {
		return ehcacheStats.getRegionCacheMaxTTISeconds( region );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getRegionCacheMaxTTLSeconds(String region) {
		return ehcacheStats.getRegionCacheMaxTTLSeconds( region );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getRegionCacheOrphanEvictionPeriod(String region) {
		return ehcacheStats.getRegionCacheOrphanEvictionPeriod( region );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, int[]> getRegionCacheSamples() {
		return ehcacheStats.getRegionCacheSamples();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getRegionCacheTargetMaxInMemoryCount(String region) {
		return ehcacheStats.getRegionCacheTargetMaxInMemoryCount( region );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getRegionCacheTargetMaxTotalCount(String region) {
		return ehcacheStats.getRegionCacheTargetMaxTotalCount( region );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getSessionCloseCount() {
		return hibernateStats.getSessionCloseCount();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getSessionOpenCount() {
		return hibernateStats.getSessionOpenCount();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getSuccessfulTransactionCount() {
		return hibernateStats.getSuccessfulTransactionCount();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getTerracottaHibernateCacheRegionNames() {
		return ehcacheStats.getTerracottaHibernateCacheRegionNames();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getTransactionCount() {
		return hibernateStats.getTransactionCount();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isRegionCacheEnabled(String region) {
		return ehcacheStats.isRegionCacheEnabled( region );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setRegionCachesEnabled(boolean enabled) {
		ehcacheStats.setRegionCachesEnabled( enabled );
		sendNotification( HibernateStats.CACHE_ENABLED, enabled );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setRegionCacheEnabled(String region, boolean enabled) {
		ehcacheStats.setRegionCacheEnabled( region, enabled );
		sendNotification( HibernateStats.CACHE_REGION_CHANGED, getRegionCacheAttributes( region ), region );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isRegionCacheLoggingEnabled(String region) {
		return ehcacheStats.isRegionCacheLoggingEnabled( region );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isRegionCacheOrphanEvictionEnabled(String region) {
		return ehcacheStats.isRegionCacheOrphanEvictionEnabled( region );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isRegionCachesEnabled() {
		return ehcacheStats.isRegionCachesEnabled();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isTerracottaHibernateCache(String region) {
		return ehcacheStats.isTerracottaHibernateCache( region );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setRegionCacheLoggingEnabled(String region, boolean loggingEnabled) {
		ehcacheStats.setRegionCacheLoggingEnabled( region, loggingEnabled );
		sendNotification( HibernateStats.CACHE_REGION_CHANGED, getRegionCacheAttributes( region ), region );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setRegionCacheMaxTTISeconds(String region, int maxTTISeconds) {
		ehcacheStats.setRegionCacheMaxTTISeconds( region, maxTTISeconds );
		sendNotification( HibernateStats.CACHE_REGION_CHANGED, getRegionCacheAttributes( region ), region );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setRegionCacheMaxTTLSeconds(String region, int maxTTLSeconds) {
		ehcacheStats.setRegionCacheMaxTTLSeconds( region, maxTTLSeconds );
		sendNotification( HibernateStats.CACHE_REGION_CHANGED, getRegionCacheAttributes( region ), region );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setRegionCacheTargetMaxInMemoryCount(String region, int targetMaxInMemoryCount) {
		ehcacheStats.setRegionCacheTargetMaxInMemoryCount( region, targetMaxInMemoryCount );
		sendNotification( HibernateStats.CACHE_REGION_CHANGED, getRegionCacheAttributes( region ), region );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setRegionCacheTargetMaxTotalCount(String region, int targetMaxTotalCount) {
		ehcacheStats.setRegionCacheTargetMaxTotalCount( region, targetMaxTotalCount );
		sendNotification( HibernateStats.CACHE_REGION_CHANGED, getRegionCacheAttributes( region ), region );
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see EhcacheStats#getNumberOfElementsInMemory(java.lang.String)
	 */
	@Override
	public int getNumberOfElementsInMemory(String region) {
		return ehcacheStats.getNumberOfElementsInMemory( region );
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see EhcacheStats#getNumberOfElementsInMemory(java.lang.String)
	 */
	@Override
	public int getNumberOfElementsOffHeap(String region) {
		return ehcacheStats.getNumberOfElementsOffHeap( region );
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see EhcacheStats#getNumberOfElementsOnDisk(java.lang.String)
	 */
	@Override
	public int getNumberOfElementsOnDisk(String region) {
		return ehcacheStats.getNumberOfElementsOnDisk( region );
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see EhcacheStats#getMaxGetTimeMillis()
	 */
	@Override
	public long getMaxGetTimeMillis() {
		return ehcacheStats.getMaxGetTimeMillis();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see EhcacheStats#getMaxGetTimeMillis(java.lang.String)
	 */
	@Override
	public long getMaxGetTimeMillis(String cacheName) {
		return ehcacheStats.getMaxGetTimeMillis( cacheName );
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see EhcacheStats#getMinGetTimeMillis()
	 */
	@Override
	public long getMinGetTimeMillis() {
		return ehcacheStats.getMinGetTimeMillis();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see EhcacheStats#getMinGetTimeMillis(java.lang.String)
	 */
	@Override
	public long getMinGetTimeMillis(String cacheName) {
		return ehcacheStats.getMinGetTimeMillis( cacheName );
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see EhcacheStats#getAverageGetTimeMillis(java.lang.String)
	 */
	@Override
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
	 * {@inheritDoc}
	 *
	 * @see AbstractEmitterBean#getNotificationInfo()
	 */
	@Override
	public MBeanNotificationInfo[] getNotificationInfo() {
		return new MBeanNotificationInfo[] { NOTIFICATION_INFO };
	}
}
