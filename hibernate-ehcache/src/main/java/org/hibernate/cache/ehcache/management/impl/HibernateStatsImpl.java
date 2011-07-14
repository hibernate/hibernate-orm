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
public class HibernateStatsImpl extends BaseEmitterBean implements HibernateStats {
	private static final double MILLIS_PER_SECOND = 1000;
	private static final MBeanNotificationInfo NOTIFICATION_INFO;

	private final SessionFactory sessionFactory;

	static {
		final String[] notifTypes = new String[] { };
		final String name = Notification.class.getName();
		final String description = "Hibernate Statistics Event";
		NOTIFICATION_INFO = new MBeanNotificationInfo( notifTypes, name, description );
	}

	/**
	 * Constructor accepting the backing {@link SessionFactory}
	 *
	 * @param sessionFactory
	 *
	 * @throws javax.management.NotCompliantMBeanException
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

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#clearStats()
	 */
	public void clearStats() {
		getStatistics().clear();
		sendNotification( CACHE_STATISTICS_RESET );
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#disableStats()
	 */
	public void disableStats() {
		setStatisticsEnabled( false );
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#enableStats()
	 */
	public void enableStats() {
		setStatisticsEnabled( true );
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getCloseStatementCount()
	 */
	public long getCloseStatementCount() {
		return getStatistics().getCloseStatementCount();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getConnectCount()
	 */
	public long getConnectCount() {
		return getStatistics().getConnectCount();
	}

	/**
	 * Not supported right now
	 */
	public long getDBSQLExecutionSample() {
		throw new UnsupportedOperationException( "Use getQueryExecutionCount() instead" );
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getFlushCount()
	 */
	public long getFlushCount() {
		return getStatistics().getFlushCount();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getOptimisticFailureCount()
	 */
	public long getOptimisticFailureCount() {
		return getStatistics().getOptimisticFailureCount();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getPrepareStatementCount()
	 */
	public long getPrepareStatementCount() {
		return getStatistics().getPrepareStatementCount();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getQueryExecutionCount()
	 */
	public long getQueryExecutionCount() {
		return getStatistics().getQueryExecutionCount();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getQueryExecutionRate()
	 */
	public double getQueryExecutionRate() {
		long startTime = getStatistics().getStartTime();
		long now = System.currentTimeMillis();
		double deltaSecs = ( now - startTime ) / MILLIS_PER_SECOND;
		return getQueryExecutionCount() / deltaSecs;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getQueryExecutionSample()
	 */
	public long getQueryExecutionSample() {
		throw new UnsupportedOperationException( "TODO: need to impl. rates for query execution" );
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getSessionCloseCount()
	 */
	public long getSessionCloseCount() {
		return getStatistics().getSessionCloseCount();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getSessionOpenCount()
	 */
	public long getSessionOpenCount() {
		return getStatistics().getSessionOpenCount();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getSuccessfulTransactionCount()
	 */
	public long getSuccessfulTransactionCount() {
		return getStatistics().getSuccessfulTransactionCount();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getTransactionCount()
	 */
	public long getTransactionCount() {
		return getStatistics().getTransactionCount();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#isStatisticsEnabled()
	 */
	public boolean isStatisticsEnabled() {
		return getStatistics().isStatisticsEnabled();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#setStatisticsEnabled(boolean)
	 */
	public void setStatisticsEnabled(boolean flag) {
		getStatistics().setStatisticsEnabled( flag );
		sendNotification( CACHE_STATISTICS_ENABLED, flag );
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getEntityStats()
	 */
	public TabularData getEntityStats() {
		List<CompositeData> result = new ArrayList<CompositeData>();
		Statistics statistics = getStatistics();
		for ( String entity : statistics.getEntityNames() ) {
			EntityStats entityStats = new EntityStats( entity, statistics.getEntityStatistics( entity ) );
			result.add( entityStats.toCompositeData() );
		}
		TabularData td = EntityStats.newTabularDataInstance();
		td.putAll( result.toArray( new CompositeData[result.size()] ) );
		return td;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getCollectionStats()
	 */
	public TabularData getCollectionStats() {
		List<CompositeData> result = new ArrayList<CompositeData>();
		Statistics statistics = getStatistics();
		for ( String roleName : statistics.getCollectionRoleNames() ) {
			CollectionStats collectionStats = new CollectionStats(
					roleName,
					statistics.getCollectionStatistics( roleName )
			);
			result.add( collectionStats.toCompositeData() );
		}
		TabularData td = CollectionStats.newTabularDataInstance();
		td.putAll( result.toArray( new CompositeData[result.size()] ) );
		return td;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getQueryStats()
	 */
	public TabularData getQueryStats() {
		List<CompositeData> result = new ArrayList<CompositeData>();
		Statistics statistics = getStatistics();
		for ( String query : statistics.getQueries() ) {
			QueryStats queryStats = new QueryStats( query, statistics.getQueryStatistics( query ) );
			result.add( queryStats.toCompositeData() );
		}
		TabularData td = QueryStats.newTabularDataInstance();
		td.putAll( result.toArray( new CompositeData[result.size()] ) );
		return td;
	}

	/**
	 * {@inheritDoc}
	 */
	public TabularData getCacheRegionStats() {
		List<CompositeData> list = new ArrayList<CompositeData>();
		Statistics statistics = getStatistics();
		for ( String region : statistics.getSecondLevelCacheRegionNames() ) {
			CacheRegionStats l2CacheStats = new CacheRegionStats(
					region,
					statistics.getSecondLevelCacheStatistics( region )
			);
			list.add( l2CacheStats.toCompositeData() );
		}
		TabularData td = CacheRegionStats.newTabularDataInstance();
		td.putAll( list.toArray( new CompositeData[list.size()] ) );
		return td;
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
