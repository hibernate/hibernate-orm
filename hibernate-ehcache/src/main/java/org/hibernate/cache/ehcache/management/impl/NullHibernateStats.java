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
	public void clearStats() {
		// no-op

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#disableStats()
	 */
	public void disableStats() {
		// no-op

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#enableStats()
	 */
	public void enableStats() {
		// no-op

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getCloseStatementCount()
	 */
	public long getCloseStatementCount() {
		// no-op
		return 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getCollectionStats()
	 */
	public TabularData getCollectionStats() {
		// no-op
		return null;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getConnectCount()
	 */
	public long getConnectCount() {
		// no-op
		return 0;
	}

	/**
	 * Not supported right now
	 */
	public long getDBSQLExecutionSample() {
		// no-op
		return 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getEntityStats()
	 */
	public TabularData getEntityStats() {
		// no-op
		return null;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getFlushCount()
	 */
	public long getFlushCount() {
		// no-op
		return 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getOptimisticFailureCount()
	 */
	public long getOptimisticFailureCount() {
		// no-op
		return 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getPrepareStatementCount()
	 */
	public long getPrepareStatementCount() {
		// no-op
		return 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getQueryExecutionCount()
	 */
	public long getQueryExecutionCount() {
		// no-op
		return 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getQueryExecutionRate()
	 */
	public double getQueryExecutionRate() {
		// no-op
		return 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getQueryExecutionSample()
	 */
	public long getQueryExecutionSample() {
		// no-op
		return 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getQueryStats()
	 */
	public TabularData getQueryStats() {
		// no-op
		return null;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getSessionCloseCount()
	 */
	public long getSessionCloseCount() {
		// no-op
		return 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getSessionOpenCount()
	 */
	public long getSessionOpenCount() {
		// no-op
		return 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getSuccessfulTransactionCount()
	 */
	public long getSuccessfulTransactionCount() {
		// no-op
		return 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#getTransactionCount()
	 */
	public long getTransactionCount() {
		// no-op
		return 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#isStatisticsEnabled()
	 */
	public boolean isStatisticsEnabled() {
		// no-op
		return false;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see HibernateStats#setStatisticsEnabled(boolean)
	 */
	public void setStatisticsEnabled(boolean flag) {
		// no-op
	}

	/**
	 * @see HibernateStats#getCacheRegionStats()
	 */
	public TabularData getCacheRegionStats() {
		return null;
	}

	/**
	 * @see javax.management.NotificationEmitter#removeNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	public void removeNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback)
			throws ListenerNotFoundException {
		/**/
	}

	/**
	 * @see javax.management.NotificationBroadcaster#addNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback)
			throws IllegalArgumentException {
		/**/
	}

	/**
	 * @see javax.management.NotificationBroadcaster#getNotificationInfo()
	 */
	public MBeanNotificationInfo[] getNotificationInfo() {
		return null;
	}

	/**
	 * @see javax.management.NotificationBroadcaster#removeNotificationListener(javax.management.NotificationListener)
	 */
	public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
		/**/
	}
}
