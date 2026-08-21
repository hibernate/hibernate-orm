/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.jdbc.internal;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.TransactionException;
import org.hibernate.resource.jdbc.LogicalConnection;
import org.hibernate.resource.jdbc.ResourceRegistry;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.resource.jdbc.spi.PhysicalJdbcTransaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import static org.hibernate.engine.jdbc.JdbcLogging.JDBC_LOGGER;
import static org.hibernate.resource.jdbc.internal.LogicalConnectionLogging.CONNECTION_LOGGER;

/**
 * Base support for {@link LogicalConnection} implementations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractLogicalConnectionImplementor implements LogicalConnectionImplementor, PhysicalJdbcTransaction {

	private TransactionStatus status = TransactionStatus.NOT_ACTIVE;
	protected ResourceRegistry resourceRegistry;

	@Override
	public PhysicalJdbcTransaction getPhysicalJdbcTransaction() {
		errorIfClosed();
		return this;
	}

	protected void errorIfClosed() {
		if ( !isOpen() ) {
			throw new IllegalStateException( this + " is closed" );
		}
	}

	@Override
	public ResourceRegistry getResourceRegistry() {
		return resourceRegistry;
	}

	@Override
	public void afterStatement() {
	}

	@Override
	public void beforeTransactionCompletion() {
	}

	@Override
	public void afterTransaction() {
		resourceRegistry.releaseResources();
	}

	// PhysicalJdbcTransaction impl ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	protected abstract Connection getConnectionForTransactionManagement();

	@Override
	public void begin() {
		try {
			if ( !doConnectionsFromProviderHaveAutoCommitDisabled() ) {
				CONNECTION_LOGGER.preparingToBeginViaSetAutoCommitFalse();
				getConnectionForTransactionManagement().setAutoCommit( false );
				CONNECTION_LOGGER.transactionBegunViaSetAutoCommitFalse();
			}
			status = TransactionStatus.ACTIVE;
		}
		catch( SQLException e ) {
			throw new TransactionException( "JDBC begin transaction failed: ", e );
		}
	}

	@Override
	public void commit() {
		if ( isPhysicallyConnected() ) {
			commitConnection();
		}
		else {
			errorIfClosed();
			status = TransactionStatus.COMMITTED;
		}
		afterCompletion();
	}

	private void commitConnection() {
		try {
			CONNECTION_LOGGER.preparingToCommitViaConnectionCommit();
			status = TransactionStatus.COMMITTING;
			getConnectionForTransactionManagement().commit();
			status = TransactionStatus.COMMITTED;
			CONNECTION_LOGGER.transactionCommittedViaConnectionCommit();
		}
		catch (SQLException e) {
			// commit failed, the current status of the
			// transaction is ambiguous
			status = TransactionStatus.FAILED_COMMIT;
			// make a last ditch attempt to roll it back
			try {
				getConnectionForTransactionManagement().rollback();
				status = TransactionStatus.ROLLED_BACK;
			}
			catch (SQLException e2) {
				e.addSuppressed( e2 );
				JDBC_LOGGER.encounteredFailureRollingBackFailedCommit( e2 );
				// at this point we can't really know for
				// sure what happened to the transaction
			}
			throw new TransactionException( "Unable to commit against JDBC Connection", e );
		}
	}

	protected void afterCompletion() {
		// by default, nothing to do
	}

	protected void resetConnection(boolean initiallyAutoCommit) {
		try {
			if ( initiallyAutoCommit ) {
				CONNECTION_LOGGER.reenablingAutoCommitAfterJdbcTransaction();
				getConnectionForTransactionManagement().setAutoCommit( true );
				status = TransactionStatus.NOT_ACTIVE;
			}
		}
		catch ( Exception e ) {
			CONNECTION_LOGGER.couldNotReEnableAutoCommit( e );
		}
	}

	@Override
	public void rollback() {
		try {
			CONNECTION_LOGGER.preparingToRollbackViaConnectionRollback();
			status = TransactionStatus.ROLLING_BACK;
			if ( isPhysicallyConnected() ) {
				getConnectionForTransactionManagement().rollback();
			}
			else {
				errorIfClosed();
			}
			status = TransactionStatus.ROLLED_BACK;
			CONNECTION_LOGGER.transactionRolledBackViaConnectionRollback();
		}
		catch ( SQLException e ) {
			status = TransactionStatus.FAILED_ROLLBACK;
			throw new TransactionException( "Unable to rollback against JDBC Connection", e );
		}

		afterCompletion();
	}

	protected static boolean determineInitialAutoCommitMode(Connection providedConnection) {
		try {
			return providedConnection.getAutoCommit();
		}
		catch (SQLException e) {
			CONNECTION_LOGGER.unableToAscertainInitialAutoCommit();
			return true;
		}
	}

	@Override
	public TransactionStatus getStatus() {
		return status;
	}

	protected boolean doConnectionsFromProviderHaveAutoCommitDisabled() {
		return false;
	}

	@Override
	public void markRollbackOnly() {
		if ( status == TransactionStatus.ACTIVE ) {
			status = TransactionStatus.MARKED_ROLLBACK;
		}
	}
}
