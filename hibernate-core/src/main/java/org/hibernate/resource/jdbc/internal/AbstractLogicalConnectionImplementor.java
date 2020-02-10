/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.jdbc.internal;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.TransactionException;
import org.hibernate.resource.jdbc.ResourceRegistry;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.resource.jdbc.spi.PhysicalJdbcTransaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractLogicalConnectionImplementor implements LogicalConnectionImplementor, PhysicalJdbcTransaction {
	private static final Logger log = Logger.getLogger( AbstractLogicalConnectionImplementor.class );

	private TransactionStatus status = TransactionStatus.NOT_ACTIVE;
	protected ResourceRegistry resourceRegistry;

	@Override
	public PhysicalJdbcTransaction getPhysicalJdbcTransaction() {
		errorIfClosed();
		return this;
	}

	protected void errorIfClosed() {
		if ( !isOpen() ) {
			throw new IllegalStateException( this.toString() + " is closed" );
		}
	}

	@Override
	public ResourceRegistry getResourceRegistry() {
		return resourceRegistry;
	}

	@Override
	public void afterStatement() {
		log.trace( "LogicalConnection#afterStatement" );
	}

	@Override
	public void beforeTransactionCompletion() {
		log.trace( "LogicalConnection#beforeTransactionCompletion" );
	}

	@Override
	public void afterTransaction() {
		log.trace( "LogicalConnection#afterTransaction" );

		resourceRegistry.releaseResources();
	}

	// PhysicalJdbcTransaction impl ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	protected abstract Connection getConnectionForTransactionManagement();

	@Override
	public void begin() {
		try {
			if ( !doConnectionsFromProviderHaveAutoCommitDisabled() ) {
				log.trace( "Preparing to begin transaction via JDBC Connection.setAutoCommit(false)" );
				getConnectionForTransactionManagement().setAutoCommit( false );
				log.trace( "Transaction begun via JDBC Connection.setAutoCommit(false)" );
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
			log.trace( "Preparing to commit transaction via JDBC Connection.commit()" );
			getConnectionForTransactionManagement().commit();
			status = TransactionStatus.COMMITTED;
			log.trace( "Transaction committed via JDBC Connection.commit()" );
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
				log.trace( "re-enabling auto-commit on JDBC Connection after completion of JDBC-based transaction" );
				getConnectionForTransactionManagement().setAutoCommit( true );
				status = TransactionStatus.NOT_ACTIVE;
			}
		}
		catch ( Exception e ) {
			log.debug(
					"Could not re-enable auto-commit on JDBC Connection after completion of JDBC-based transaction : " + e
			);
		}
	}

	@Override
	public void rollback() {
		try {
			log.trace( "Preparing to rollback transaction via JDBC Connection.rollback()" );
			getConnectionForTransactionManagement().rollback();
			status = TransactionStatus.ROLLED_BACK;
			log.trace( "Transaction rolled-back via JDBC Connection.rollback()" );
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
			log.debug( "Unable to ascertain initial auto-commit state of provided connection; assuming auto-commit" );
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
