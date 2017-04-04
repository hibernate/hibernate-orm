/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.ehcache.management.impl;

import java.util.ArrayList;
import java.util.List;
import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import net.sf.ehcache.hibernate.management.api.HibernateStats;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

/**
 * Implementation of {@link HibernateStats}
 * <p/>
 * <p/>
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 */
public class HibernateStatsImpl extends AbstractEmitterBean implements HibernateStats {
	private static final double MILLIS_PER_SECOND = 1000;
	private static final MBeanNotificationInfo NOTIFICATION_INFO;

	private final SessionFactory sessionFactory;

	static {
		final String[] notifTypes = new String[] {};
		final String name = Notification.class.getName();
		final String description = "Hibernate Statistics Event";
		NOTIFICATION_INFO = new MBeanNotificationInfo( notifTypes, name, description );
	}

	/**
	 * Constructor accepting the backing {@link SessionFactory}
	 *
	 * @param sessionFactory the {@link SessionFactory} to source stats from
	 *
	 * @throws javax.management.NotCompliantMBeanException thrown from JMX super ctor
	 */
	public HibernateStatsImpl(SessionFactory sessionFactory) throws NotCompliantMBeanException {
		super( HibernateStats.class );
		this.sessionFactory = sessionFactory;
	}

	/**
	 * @return statistics
	 */
	private Statistics getStatistics() {
		return sessionFactory.getStatistics();
	}

	@Override
	public void clearStats() {
		getStatistics().clear();
		sendNotification( CACHE_STATISTICS_RESET );
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
	public long getCloseStatementCount() {
		return getStatistics().getCloseStatementCount();
	}

	@Override
	public long getConnectCount() {
		return getStatistics().getConnectCount();
	}

	/**
	 * Unsupported operation
	 * @return nothing ever, this only throws!
	 * @throws UnsupportedOperationException
	 * @deprecated DO NOT USE, WILL ONLY THROW AT YOU!
	 */
	@Deprecated
	@SuppressWarnings("UnusedDeclaration")
	public long getDBSQLExecutionSample() {
		throw new UnsupportedOperationException( "Use getQueryExecutionCount() instead" );
	}

	@Override
	public long getFlushCount() {
		return getStatistics().getFlushCount();
	}

	@Override
	public long getOptimisticFailureCount() {
		return getStatistics().getOptimisticFailureCount();
	}

	@Override
	public long getPrepareStatementCount() {
		return getStatistics().getPrepareStatementCount();
	}

	@Override
	public long getQueryExecutionCount() {
		return getStatistics().getQueryExecutionCount();
	}

	@Override
	public double getQueryExecutionRate() {
		final long startTime = getStatistics().getStartTime();
		final long now = System.currentTimeMillis();
		final double deltaSecs = (now - startTime) / MILLIS_PER_SECOND;
		return getQueryExecutionCount() / deltaSecs;
	}

	@Override
	public long getQueryExecutionSample() {
		throw new UnsupportedOperationException( "TODO: need to impl. rates for query execution" );
	}

	@Override
	public long getSessionCloseCount() {
		return getStatistics().getSessionCloseCount();
	}

	@Override
	public long getSessionOpenCount() {
		return getStatistics().getSessionOpenCount();
	}

	@Override
	public long getSuccessfulTransactionCount() {
		return getStatistics().getSuccessfulTransactionCount();
	}

	@Override
	public long getTransactionCount() {
		return getStatistics().getTransactionCount();
	}

	@Override
	public boolean isStatisticsEnabled() {
		return getStatistics().isStatisticsEnabled();
	}

	@Override
	public void setStatisticsEnabled(boolean flag) {
		getStatistics().setStatisticsEnabled( flag );
		sendNotification( CACHE_STATISTICS_ENABLED, flag );
	}

	@Override
	public TabularData getEntityStats() {
		final List<CompositeData> result = new ArrayList<CompositeData>();
		final Statistics statistics = getStatistics();
		for ( String entity : statistics.getEntityNames() ) {
			final EntityStats entityStats = new EntityStats( entity, statistics.getEntityStatistics( entity ) );
			result.add( entityStats.toCompositeData() );
		}
		final TabularData td = EntityStats.newTabularDataInstance();
		td.putAll( result.toArray( new CompositeData[result.size()] ) );
		return td;
	}

	@Override
	public TabularData getCollectionStats() {
		final List<CompositeData> result = new ArrayList<CompositeData>();
		final Statistics statistics = getStatistics();
		for ( String roleName : statistics.getCollectionRoleNames() ) {
			final CollectionStats collectionStats = new CollectionStats(
					roleName,
					statistics.getCollectionStatistics( roleName )
			);
			result.add( collectionStats.toCompositeData() );
		}
		final TabularData td = CollectionStats.newTabularDataInstance();
		td.putAll( result.toArray( new CompositeData[result.size()] ) );
		return td;
	}

	@Override
	public TabularData getQueryStats() {
		final List<CompositeData> result = new ArrayList<CompositeData>();
		final Statistics statistics = getStatistics();
		for ( String query : statistics.getQueries() ) {
			final QueryStats queryStats = new QueryStats( query, statistics.getQueryStatistics( query ) );
			result.add( queryStats.toCompositeData() );
		}
		final TabularData td = QueryStats.newTabularDataInstance();
		td.putAll( result.toArray( new CompositeData[result.size()] ) );
		return td;
	}

	@Override
	public TabularData getCacheRegionStats() {
		final List<CompositeData> list = new ArrayList<CompositeData>();
		final Statistics statistics = getStatistics();
		for ( String region : statistics.getSecondLevelCacheRegionNames() ) {
			final CacheRegionStats l2CacheStats = new CacheRegionStats(
					region,
					statistics.getSecondLevelCacheStatistics( region )
			);
			list.add( l2CacheStats.toCompositeData() );
		}
		final TabularData td = CacheRegionStats.newTabularDataInstance();
		td.putAll( list.toArray( new CompositeData[list.size()] ) );
		return td;
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
