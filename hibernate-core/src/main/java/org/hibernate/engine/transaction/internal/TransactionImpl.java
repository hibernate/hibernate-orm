/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.transaction.internal;

import javax.transaction.Synchronization;

import org.hibernate.HibernateException;
import org.hibernate.TransactionException;
import org.hibernate.engine.spi.ExceptionConverter;
import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.jpa.JpaCompliance;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import org.jboss.logging.Logger;

import static org.hibernate.resource.transaction.spi.TransactionCoordinator.TransactionDriver;

/**
 * @author Andrea Boriero
 * @author Steve Ebersole
 */
public class TransactionImpl implements TransactionImplementor {
	private static final Logger LOG = CoreLogging.logger( TransactionImpl.class );

	private final TransactionCoordinator transactionCoordinator;
	private final ExceptionConverter exceptionConverter;
	private final JpaCompliance jpaCompliance;

	private TransactionDriver transactionDriverControl;

	public TransactionImpl(
			TransactionCoordinator transactionCoordinator,
			ExceptionConverter exceptionConverter,
			JpaCompliance jpaCompliance) {
		this.transactionCoordinator = transactionCoordinator;
		this.exceptionConverter = exceptionConverter;
		this.jpaCompliance = jpaCompliance;

		this.transactionDriverControl = transactionCoordinator.getTransactionDriverControl();

		LOG.debugf(
				"On TransactionImpl creation, JpaCompliance#isJpaTransactionComplianceEnabled == %s",
				jpaCompliance.isJpaTransactionComplianceEnabled()
		);
	}

	@Override
	public void begin() {
		if ( !transactionCoordinator.isActive() ) {
			throw new TransactionException( "Cannot begin Transaction on closed Session/EntityManager" );
		}

		if ( transactionDriverControl == null ) {
			transactionDriverControl = transactionCoordinator.getTransactionDriverControl();
		}

		// per-JPA
		if ( isActive() ) {
			throw new IllegalStateException( "Transaction already active" );
		}

		LOG.debug( "begin" );
		this.transactionDriverControl.begin();
	}

	@Override
	public void commit() {
		if ( !isActive( true ) ) {
			// allow MARKED_ROLLBACK to propagate through to transactionDriverControl
			// the boolean passed to isActive indicates whether MARKED_ROLLBACK should be
			// considered active
			//
			// essentially here we have a transaction that is not active and
			// has not been marked for rollback only
			throw new IllegalStateException( "Transaction not successfully started" );
		}

		LOG.debug( "committing" );
		try {
			internalGetTransactionDriverControl().commit();
		}
		catch (RuntimeException e) {
			throw exceptionConverter.convertCommitException( e );
		}
	}

	public TransactionDriver internalGetTransactionDriverControl() {
		// NOTE here to help be a more descriptive NullPointerException
		if ( this.transactionDriverControl == null ) {
			throw new IllegalStateException( "Transaction was not properly begun/started" );
		}
		return this.transactionDriverControl;
	}

	@Override
	public void rollback() {
		if ( jpaCompliance.isJpaTransactionComplianceEnabled() ) {
			if ( !isActive() ) {
				throw new IllegalStateException(
						"JPA compliance dictates throwing IllegalStateException when #rollback " +
								"is called on non-active transaction"
				);
			}
		}

		TransactionStatus status = getStatus();
		if ( status == TransactionStatus.ROLLED_BACK || status == TransactionStatus.NOT_ACTIVE ) {
			// Allow rollback() calls on completed transactions, just no-op.
			LOG.debug( "rollback() called on an inactive transaction" );
			return;
		}

		if ( !status.canRollback() ) {
			throw new TransactionException( "Cannot rollback transaction in current status [" + status.name() + "]" );
		}

		LOG.debug( "rolling back" );
		if ( status != TransactionStatus.FAILED_COMMIT || allowFailedCommitToPhysicallyRollback() ) {
			internalGetTransactionDriverControl().rollback();
		}
	}

	@Override
	public boolean isActive() {
		// old behavior considered TransactionStatus#MARKED_ROLLBACK as active
//		return isActive( jpaCompliance.isJpaTransactionComplianceEnabled() ? false : true );
		return isActive( true );
	}

	@Override
	public boolean isActive(boolean isMarkedForRollbackConsideredActive) {
		if ( transactionDriverControl == null ) {
			transactionDriverControl = transactionCoordinator.getTransactionDriverControl();
		}
		return transactionDriverControl.isActive( isMarkedForRollbackConsideredActive );
	}

	@Override
	public TransactionStatus getStatus() {
		if ( transactionDriverControl == null ) {
			transactionDriverControl = transactionCoordinator.getTransactionDriverControl();
		}
		return transactionDriverControl.getStatus();
	}

	@Override
	public void registerSynchronization(Synchronization synchronization) throws HibernateException {
		this.transactionCoordinator.getLocalSynchronizations().registerSynchronization( synchronization );
	}

	@Override
	public void setTimeout(int seconds) {
		this.transactionCoordinator.setTimeOut( seconds );
	}

	@Override
	public int getTimeout() {
		return this.transactionCoordinator.getTimeOut();
	}

	@Override
	public void markRollbackOnly() {
		// this is the Hibernate-specific API, whereas #setRollbackOnly is the
		// JPA-defined API.  In our opinion it is much more user-friendly to
		// always allow user/integration to indicate that the transaction
		// should not be allowed to commit.
		internalGetTransactionDriverControl().markRollbackOnly();
	}

	@Override
	public void setRollbackOnly() {
		// Since this is the JPA-defined one, we make sure the txn is active first
		// so long as compliance (JpaCompliance) has not been defined to disable
		// that check - making this active more like Hibernate's #markRollbackOnly
		if ( jpaCompliance.isJpaTransactionComplianceEnabled() ) {
			if ( !isActive() ) {
				throw new IllegalStateException(
						"JPA compliance dictates throwing IllegalStateException when #setRollbackOnly " +
								"is called on non-active transaction"
				);
			}
		}

		markRollbackOnly();
	}

	@Override
	public boolean getRollbackOnly() {
		if ( jpaCompliance.isJpaTransactionComplianceEnabled() ) {
			if ( !isActive() ) {
				throw new IllegalStateException(
						"JPA compliance dictates throwing IllegalStateException when #getRollbackOnly " +
								"is called on non-active transaction"
				);
			}
		}

		return getStatus() == TransactionStatus.MARKED_ROLLBACK;
	}

	protected boolean allowFailedCommitToPhysicallyRollback() {
		return false;
	}
}
