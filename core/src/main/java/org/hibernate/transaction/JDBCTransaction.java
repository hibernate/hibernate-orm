/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.transaction;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.hibernate.TransactionException;
import org.hibernate.jdbc.JDBCContext;

/**
 * {@link Transaction} implementation based on transaction management through
 * a JDBC {@link java.sql.Connection}.
 * <p/>
 * This the Hibernate's default transaction strategy.
 *
 * @author Anton van Straaten
 * @author Gavin King
 */
public class JDBCTransaction implements Transaction {

	private static final Logger log = LoggerFactory.getLogger(JDBCTransaction.class);

	private final JDBCContext jdbcContext;
	private final TransactionFactory.Context transactionContext;

	private boolean toggleAutoCommit;
	private boolean begun;
	private boolean rolledBack;
	private boolean committed;
	private boolean commitFailed;
	private List synchronizations;
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

		log.debug("begin");

		try {
			toggleAutoCommit = jdbcContext.connection().getAutoCommit();
			if ( log.isDebugEnabled() ) {
				log.debug("current autocommit status: " + toggleAutoCommit);
			}
			if (toggleAutoCommit) {
				log.debug("disabling autocommit");
				jdbcContext.connection().setAutoCommit(false);
			}
		}
		catch (SQLException e) {
			log.error("JDBC begin failed", e);
			throw new TransactionException("JDBC begin failed: ", e);
		}

		callback = jdbcContext.registerCallbackIfNecessary();

		begun = true;
		committed = false;
		rolledBack = false;

		if ( timeout>0 ) {
			jdbcContext.getConnectionManager()
					.getBatcher()
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
				log.error("Could not close session", he);
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

		log.debug("commit");

		if ( !transactionContext.isFlushModeNever() && callback ) {
			transactionContext.managedFlush(); //if an exception occurs during flush, user must call rollback()
		}

		notifyLocalSynchsBeforeTransactionCompletion();
		if ( callback ) {
			jdbcContext.beforeTransactionCompletion( this );
		}

		try {
			commitAndResetAutoCommit();
			log.debug("committed JDBC Connection");
			committed = true;
			if ( callback ) {
				jdbcContext.afterTransactionCompletion( true, this );
			}
			notifyLocalSynchsAfterTransactionCompletion( Status.STATUS_COMMITTED );
		}
		catch (SQLException e) {
			log.error("JDBC commit failed", e);
			commitFailed = true;
			if ( callback ) {
				jdbcContext.afterTransactionCompletion( false, this );
			}
			notifyLocalSynchsAfterTransactionCompletion( Status.STATUS_UNKNOWN );
			throw new TransactionException("JDBC commit failed", e);
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

		log.debug("rollback");

		if (!commitFailed) {

			/*notifyLocalSynchsBeforeTransactionCompletion();
			if ( callback ) {
				jdbcContext.notifyLocalSynchsBeforeTransactionCompletion( this );
			}*/

			try {
				rollbackAndResetAutoCommit();
				log.debug("rolled back JDBC Connection");
				rolledBack = true;
				notifyLocalSynchsAfterTransactionCompletion(Status.STATUS_ROLLEDBACK);
			}
			catch (SQLException e) {
				log.error("JDBC rollback failed", e);
				notifyLocalSynchsAfterTransactionCompletion(Status.STATUS_UNKNOWN);
				throw new TransactionException("JDBC rollback failed", e);
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
				log.debug("re-enabling autocommit");
				jdbcContext.connection().setAutoCommit( true );
			}
		}
		catch (Exception sqle) {
			log.error("Could not toggle autocommit", sqle);
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
	public void registerSynchronization(Synchronization sync) throws HibernateException {
		if (sync==null) throw new NullPointerException("null Synchronization");
		if (synchronizations==null) {
			synchronizations = new ArrayList();
		}
		synchronizations.add(sync);
	}

	private void notifyLocalSynchsBeforeTransactionCompletion() {
		if (synchronizations!=null) {
			for ( int i=0; i<synchronizations.size(); i++ ) {
				Synchronization sync = (Synchronization) synchronizations.get(i);
				try {
					sync.beforeCompletion();
				}
				catch (Throwable t) {
					log.error("exception calling user Synchronization", t);
				}
			}
		}
	}

	private void notifyLocalSynchsAfterTransactionCompletion(int status) {
		begun = false;
		if (synchronizations!=null) {
			for ( int i=0; i<synchronizations.size(); i++ ) {
				Synchronization sync = (Synchronization) synchronizations.get(i);
				try {
					sync.afterCompletion(status);
				}
				catch (Throwable t) {
					log.error("exception calling user Synchronization", t);
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setTimeout(int seconds) {
		timeout = seconds;
	}
}
