/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.util;

import java.sql.Connection;
import java.sql.SQLException;
import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.hibernate.HibernateException;
import org.hibernate.jdbc.WorkExecutor;
import org.hibernate.jdbc.WorkExecutorVisitable;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;

/**
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public abstract class InvocationAfterCompletion implements Synchronization {
	protected static final InfinispanMessageLogger log = InfinispanMessageLogger.Provider.getLog( InvocationAfterCompletion.class );

	protected final TransactionCoordinator tc;
	protected final boolean requiresTransaction;

	public InvocationAfterCompletion(TransactionCoordinator tc, boolean requiresTransaction) {
		this.tc = tc;
		this.requiresTransaction = requiresTransaction;
	}

	@Override
	public void beforeCompletion() {
	}

	@Override
	public void afterCompletion(int status) {
		switch (status) {
			case Status.STATUS_COMMITTING:
			case Status.STATUS_COMMITTED:
				invokeIsolated(true);
				break;
			default:
				// it would be nicer to react only on ROLLING_BACK and ROLLED_BACK statuses
				// but TransactionCoordinator gives us UNKNOWN on rollback
				invokeIsolated(false);
				break;
		}
	}

	protected void invokeIsolated(final boolean success) {
		try {
			// TODO: isolation without obtaining Connection -> needs HHH-9993
			tc.createIsolationDelegate().delegateWork(new WorkExecutorVisitable<Void>() {
				@Override
				public Void accept(WorkExecutor<Void> executor, Connection connection) throws SQLException {
					invoke(success);
					return null;
				}
			}, requiresTransaction);
		}
		catch (HibernateException e) {
			// silently fail any exceptions
			if (log.isTraceEnabled()) {
				log.trace("Exception during query cache update", e);
			}
		}
	}

	protected abstract void invoke(boolean success);
}
