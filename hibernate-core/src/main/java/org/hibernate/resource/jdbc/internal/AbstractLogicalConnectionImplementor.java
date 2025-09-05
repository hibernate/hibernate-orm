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

import org.jboss.logging.Logger;

/**
 * Base support for {@link LogicalConnection} implementations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractLogicalConnectionImplementor implements LogicalConnectionImplementor, PhysicalJdbcTransaction {
	private static final Logger LOG = Logger.getLogger( AbstractLogicalConnectionImplementor.class );

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
				LOG.trace( "Preparing to begin transaction via JDBC Connection.setAutoCommit(false)" );
				getConnectionForTransactionManagement().setAutoCommit( false );
				LOG.trace( "Transaction begun via JDBC Connection.setAutoCommit(false)" );
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
			LOG.trace( "Preparing to commit transaction via JDBC Connection.commit()" );
			status = TransactionStatus.COMMITTING;
			if ( isPhysicallyConnected() ) {
				getConnectionForTransactionManagement().commit();
			}
			else {
				errorIfClosed();
			}
			status = TransactionStatus.COMMITTED;
			LOG.trace( "Transaction committed via JDBC Connection.commit()" );
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
				LOG.trace( "Re-enabling auto-commit on JDBC Connection after completion of JDBC-based transaction" );
				getConnectionForTransactionManagement().setAutoCommit( true );
				status = TransactionStatus.NOT_ACTIVE;
			}
		}
		catch ( Exception e ) {
			LOG.debug( "Could not re-enable auto-commit on JDBC Connection after completion of JDBC-based transaction", e );
		}
	}

	@Override
	public void rollback() {
		try {
			LOG.trace( "Preparing to roll back transaction via JDBC Connection.rollback()" );
			status = TransactionStatus.ROLLING_BACK;
			if ( isPhysicallyConnected() ) {
				getConnectionForTransactionManagement().rollback();
			}
			else {
				errorIfClosed();
			}
			status = TransactionStatus.ROLLED_BACK;
			LOG.trace( "Transaction rolled back via JDBC Connection.rollback()" );
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
			LOG.debug( "Unable to ascertain initial auto-commit state of provided connection; assuming auto-commit" );
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
