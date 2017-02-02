/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.transaction.backend.jta.internal;

import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.jboss.logging.Logger;

import org.hibernate.TransactionException;
import org.hibernate.resource.transaction.spi.TransactionStatus;

/**
 * JtaTransactionAdapter for coordinating with the JTA UserTransaction
 *
 * @author Steve Ebersole
 */
public class JtaTransactionAdapterUserTransactionImpl implements JtaTransactionAdapter {
	private static final Logger log = Logger.getLogger( JtaTransactionAdapterUserTransactionImpl.class );

	private final UserTransaction userTransaction;


	private boolean initiator;

	public JtaTransactionAdapterUserTransactionImpl(UserTransaction userTransaction) {
		this.userTransaction = userTransaction;
	}

	@Override
	public void begin() {
		try {
			if ( getStatus() == TransactionStatus.NOT_ACTIVE ) {
				log.trace( "Calling UserTransaction#begin" );
				userTransaction.begin();
				initiator = true;
				log.trace( "Called UserTransaction#begin" );
			}
			else {
				log.trace( "Skipping TransactionManager#begin due to already active transaction" );
			}
		}
		catch (Exception e) {
			throw new TransactionException( "JTA UserTransaction#begin failed", e );
		}
	}

	@Override
	public void commit() {
		try {
			if ( initiator ) {
				initiator = false;
				log.trace( "Calling UserTransaction#commit" );
				userTransaction.commit();
				log.trace( "Called UserTransaction#commit" );
			}
			else {
				log.trace( "Skipping TransactionManager#commit due to not being initiator" );
			}
		}
		catch (Exception e) {
			throw new TransactionException( "JTA UserTransaction#commit failed", e );
		}
	}

	@Override
	public void rollback() {
		try {
			if ( initiator ) {
				initiator = false;
				log.trace( "Calling UserTransaction#rollback" );
				userTransaction.rollback();
				log.trace( "Called UserTransaction#rollback" );
			}
			else {
				markRollbackOnly();
			}
		}
		catch (Exception e) {
			throw new TransactionException( "JTA UserTransaction#rollback failed", e );
		}
	}

	@Override
	public TransactionStatus getStatus() {
		try {
			return StatusTranslator.translate( userTransaction.getStatus() );
		}
		catch (SystemException e) {
			throw new TransactionException( "JTA TransactionManager#getStatus failed", e );
		}
	}

	@Override
	public void markRollbackOnly(){
		try {
			userTransaction.setRollbackOnly();
		}
		catch (SystemException e) {
			throw new TransactionException( "Unable to mark transaction for rollback only", e );
		}
	}

	@Override
	public void setTimeOut(int seconds) {
		if ( seconds > 0 ) {
			try {
				userTransaction.setTransactionTimeout( seconds );
			}
			catch (SystemException e) {
				throw new TransactionException( "Unable to apply requested transaction timeout", e );
			}
		}
	}
}
