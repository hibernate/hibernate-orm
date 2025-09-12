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

import static org.hibernate.resource.jdbc.internal.LogicalConnectionLogging.LOGGER;

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
//		LOG.trace( "LogicalConnection#afterStatement" );
	}

	@Override
	public void beforeTransactionCompletion() {
//		LOG.trace( "LogicalConnection#beforeTransactionCompletion" );
	}

	@Override
	public void afterTransaction() {
//		LOG.trace( "LogicalConnection#afterTransaction" );
		resourceRegistry.releaseResources();
	}

	// PhysicalJdbcTransaction impl ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	protected abstract Connection getConnectionForTransactionManagement();

	@Override
	public void begin() {
		try {
			if ( !doConnectionsFromProviderHaveAutoCommitDisabled() ) {
				LOGGER.preparingToBeginViaSetAutoCommitFalse();
				getConnectionForTransactionManagement().setAutoCommit( false );
				LOGGER.transactionBegunViaSetAutoCommitFalse();
			}
			status = TransactionStatus.ACTIVE;
		}
		catch( SQLException e ) {
			throw new TransactionException( "JDBC begin transaction failed: ", e );
		}
	}

	@Override
	public void commit() {
		try {
			LOGGER.preparingToCommitViaConnectionCommit();
			status = TransactionStatus.COMMITTING;
			if ( isPhysicallyConnected() ) {
				getConnectionForTransactionManagement().commit();
			}
			else {
				errorIfClosed();
			}
			status = TransactionStatus.COMMITTED;
			LOGGER.transactionCommittedViaConnectionCommit();
		}
		catch( SQLException e ) {
			status = TransactionStatus.FAILED_COMMIT;
			throw new TransactionException( "Unable to commit against JDBC Connection", e );
		}

		afterCompletion();
	}

	protected void afterCompletion() {
		// by default, nothing to do
	}

	protected void resetConnection(boolean initiallyAutoCommit) {
		try {
			if ( initiallyAutoCommit ) {
				LOGGER.reenablingAutoCommitAfterJdbcTransaction();
				getConnectionForTransactionManagement().setAutoCommit( true );
				status = TransactionStatus.NOT_ACTIVE;
			}
		}
		catch ( Exception e ) {
			LOGGER.couldNotReEnableAutoCommit( e );
		}
	}

	@Override
	public void rollback() {
		try {
			LOGGER.preparingToRollbackViaConnectionRollback();
			status = TransactionStatus.ROLLING_BACK;
			if ( isPhysicallyConnected() ) {
				getConnectionForTransactionManagement().rollback();
			}
			else {
				errorIfClosed();
			}
			status = TransactionStatus.ROLLED_BACK;
			LOGGER.transactionRolledBackViaConnectionRollback();
		}
		catch( SQLException e ) {
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
			LOGGER.unableToAscertainInitialAutoCommit();
			return true;
		}
	}

	@Override
	public TransactionStatus getStatus(){
		return status;
	}

	protected boolean doConnectionsFromProviderHaveAutoCommitDisabled() {
		return false;
	}
}
