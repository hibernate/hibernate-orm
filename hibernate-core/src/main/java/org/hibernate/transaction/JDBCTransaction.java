/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.transaction;

import java.sql.SQLException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import org.hibernate.HibernateException;
import org.hibernate.Logger;
import org.hibernate.Transaction;
import org.hibernate.TransactionException;
import org.hibernate.engine.jdbc.spi.JDBCContext;
import org.hibernate.engine.transaction.SynchronizationRegistry;

/**
 * {@link Transaction} implementation based on transaction management through a JDBC {@link java.sql.Connection}.
 * <p/>
 * This the Hibernate's default transaction strategy.
 *
 * @author Anton van Straaten
 * @author Gavin King
 */
public class JDBCTransaction implements Transaction {

    private static final Logger LOG = org.jboss.logging.Logger.getMessageLogger(Logger.class,
                                                                                JDBCTransaction.class.getPackage().getName());

	private final SynchronizationRegistry synchronizationRegistry = new SynchronizationRegistry();
	private final JDBCContext jdbcContext;
	private final TransactionFactory.Context transactionContext;

	private boolean toggleAutoCommit;
	private boolean begun;
	private boolean rolledBack;
	private boolean committed;
	private boolean commitFailed;
	private boolean callback;
	private int timeout = -1;

	public JDBCTransaction(JDBCContext jdbcContext, TransactionFactory.Context transactionContext) {
		this.jdbcContext = jdbcContext;
		this.transactionContext = transactionContext;
	}

	/**
	 * {@inheritDoc}
	 */
	public void begin() throws HibernateException {
		if (begun) {
			return;
		}
		if (commitFailed) {
			throw new TransactionException("cannot re-start transaction after failed commit");
		}

        LOG.debug("Begin");

		try {
			toggleAutoCommit = jdbcContext.connection().getAutoCommit();
            LOG.debug("current autocommit status: " + toggleAutoCommit);
			if (toggleAutoCommit) {
                LOG.debug("Disabling autocommit");
				jdbcContext.connection().setAutoCommit(false);
			}
		}
		catch (SQLException e) {
            LOG.error(LOG.jdbcBeginFailed(), e);
            throw new TransactionException(LOG.jdbcBeginFailed(), e);
		}

		callback = jdbcContext.registerCallbackIfNecessary();

		begun = true;
		committed = false;
		rolledBack = false;

		if ( timeout>0 ) {
			jdbcContext.getConnectionManager()
					.setTransactionTimeout(timeout);
		}

		jdbcContext.afterTransactionBegin(this);
	}

	private void closeIfRequired() throws HibernateException {
		if ( callback && transactionContext.shouldAutoClose() && !transactionContext.isClosed() ) {
			try {
				transactionContext.managedClose();
			}
			catch (HibernateException he) {
                LOG.error(LOG.unableToCloseSession(), he);
				//swallow, the transaction was finished
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void commit() throws HibernateException {
		if (!begun) {
			throw new TransactionException("Transaction not successfully started");
		}

        LOG.debug("Commit");

		if ( !transactionContext.isFlushModeNever() && callback ) {
			transactionContext.managedFlush(); //if an exception occurs during flush, user must call rollback()
		}

		notifySynchronizationsBeforeTransactionCompletion();
		if ( callback ) {
			jdbcContext.beforeTransactionCompletion( this );
		}

		try {
			commitAndResetAutoCommit();
            LOG.debug("Committed JDBC Connection");
			committed = true;
			if ( callback ) {
				jdbcContext.afterTransactionCompletion( true, this );
			}
			notifySynchronizationsAfterTransactionCompletion( Status.STATUS_COMMITTED );
		}
		catch (SQLException e) {
            LOG.error(LOG.unableToPerformJdbcCommit(), e);
			commitFailed = true;
			if ( callback ) {
				jdbcContext.afterTransactionCompletion( false, this );
			}
			notifySynchronizationsAfterTransactionCompletion( Status.STATUS_UNKNOWN );
            throw new TransactionException(LOG.unableToPerformJdbcCommit(), e);
		}
		finally {
			closeIfRequired();
		}
	}

	private void commitAndResetAutoCommit() throws SQLException {
		try {
			jdbcContext.connection().commit();
		}
		finally {
			toggleAutoCommit();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void rollback() throws HibernateException {

		if (!begun && !commitFailed) {
			throw new TransactionException("Transaction not successfully started");
		}

        LOG.debug("Rollback");

		if (!commitFailed) {

			/*notifyLocalSynchsBeforeTransactionCompletion();
			if ( callback ) {
				jdbcContext.notifyLocalSynchsBeforeTransactionCompletion( this );
			}*/

			try {
				rollbackAndResetAutoCommit();
                LOG.debug("Rolled back JDBC Connection");
				rolledBack = true;
				notifySynchronizationsAfterTransactionCompletion(Status.STATUS_ROLLEDBACK);
			}
			catch (SQLException e) {
                LOG.error(LOG.jdbcRollbackFailed(), e);
				notifySynchronizationsAfterTransactionCompletion(Status.STATUS_UNKNOWN);
                throw new TransactionException(LOG.jdbcRollbackFailed(), e);
			}
			finally {
				if ( callback ) {
					jdbcContext.afterTransactionCompletion( false, this );
				}
				closeIfRequired();
			}
		}
	}

	private void rollbackAndResetAutoCommit() throws SQLException {
		try {
			jdbcContext.connection().rollback();
		}
		finally {
			toggleAutoCommit();
		}
	}

	private void toggleAutoCommit() {
		try {
			if (toggleAutoCommit) {
                LOG.debug("Re-enabling autocommit");
				jdbcContext.connection().setAutoCommit( true );
			}
		}
		catch (Exception sqle) {
            LOG.error(LOG.unableToToggleAutoCommit(), sqle);
			//swallow it (the transaction _was_ successful or successfully rolled back)
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean wasRolledBack() {
		return rolledBack;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean wasCommitted() {
		return committed;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isActive() {
		return begun && ! ( rolledBack || committed | commitFailed );
	}

	/**
	 * {@inheritDoc}
	 */
	public void registerSynchronization(Synchronization sync) {
		synchronizationRegistry.registerSynchronization( sync );
	}

	private void notifySynchronizationsBeforeTransactionCompletion() {
		synchronizationRegistry.notifySynchronizationsBeforeTransactionCompletion();
	}

	private void notifySynchronizationsAfterTransactionCompletion(int status) {
		begun = false;
		synchronizationRegistry.notifySynchronizationsAfterTransactionCompletion( status );
	}

	/**
	 * {@inheritDoc}
	 */
	public void setTimeout(int seconds) {
		timeout = seconds;
	}
}
