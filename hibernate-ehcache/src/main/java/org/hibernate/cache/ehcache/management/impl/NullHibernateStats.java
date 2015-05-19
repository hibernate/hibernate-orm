/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.ehcache.management.impl;


import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.openmbean.TabularData;

import net.sf.ehcache.hibernate.management.api.HibernateStats;

/**
 * Implementation of {@link HibernateStats} that does nothing
 * <p/>
 * <p/>
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 */
public final class NullHibernateStats implements HibernateStats {

	/**
	 * Singleton instance.
	 */
	public static final HibernateStats INSTANCE = new NullHibernateStats();

	/**
	 * private constructor. No need to create instances of this. Use singleton instance
	 */
	private NullHibernateStats() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#clearStats()
	 */
	@Override
	public void clearStats() {
		// no-op

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#disableStats()
	 */
	@Override
	public void disableStats() {
		// no-op

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#enableStats()
	 */
	@Override
	public void enableStats() {
		// no-op

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getCloseStatementCount()
	 */
	@Override
	public long getCloseStatementCount() {
		// no-op
		return 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getCollectionStats()
	 */
	@Override
	public TabularData getCollectionStats() {
		// no-op
		return null;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getConnectCount()
	 */
	@Override
	public long getConnectCount() {
		// no-op
		return 0;
	}

	/**
	 * Not supported right now
	 * @return 0 always
	 */
	@SuppressWarnings("UnusedDeclaration")
	public long getDBSQLExecutionSample() {
		// no-op
		return 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getEntityStats()
	 */
	@Override
	public TabularData getEntityStats() {
		// no-op
		return null;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getFlushCount()
	 */
	@Override
	public long getFlushCount() {
		// no-op
		return 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getOptimisticFailureCount()
	 */
	@Override
	public long getOptimisticFailureCount() {
		// no-op
		return 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getPrepareStatementCount()
	 */
	@Override
	public long getPrepareStatementCount() {
		// no-op
		return 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getQueryExecutionCount()
	 */
	@Override
	public long getQueryExecutionCount() {
		// no-op
		return 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getQueryExecutionRate()
	 */
	@Override
	public double getQueryExecutionRate() {
		// no-op
		return 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getQueryExecutionSample()
	 */
	@Override
	public long getQueryExecutionSample() {
		// no-op
		return 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getQueryStats()
	 */
	@Override
	public TabularData getQueryStats() {
		// no-op
		return null;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getSessionCloseCount()
	 */
	@Override
	public long getSessionCloseCount() {
		// no-op
		return 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getSessionOpenCount()
	 */
	@Override
	public long getSessionOpenCount() {
		// no-op
		return 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getSuccessfulTransactionCount()
	 */
	@Override
	public long getSuccessfulTransactionCount() {
		// no-op
		return 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getTransactionCount()
	 */
	@Override
	public long getTransactionCount() {
		// no-op
		return 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#isStatisticsEnabled()
	 */
	@Override
	public boolean isStatisticsEnabled() {
		// no-op
		return false;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#setStatisticsEnabled(boolean)
	 */
	@Override
	public void setStatisticsEnabled(boolean flag) {
		// no-op
	}

	/**
	 * {@inheritDoc}
	 * @see HibernateStats#getCacheRegionStats()
	 */
	@Override
	public TabularData getCacheRegionStats() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationEmitter#removeNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	@Override
	public void removeNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback)
			throws ListenerNotFoundException {
		/**/
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcaster#addNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	@Override
	public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback)
			throws IllegalArgumentException {
		/**/
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcaster#getNotificationInfo()
	 */
	@Override
	public MBeanNotificationInfo[] getNotificationInfo() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcaster#removeNotificationListener(javax.management.NotificationListener)
	 */
	@Override
	public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
		/**/
	}
}
