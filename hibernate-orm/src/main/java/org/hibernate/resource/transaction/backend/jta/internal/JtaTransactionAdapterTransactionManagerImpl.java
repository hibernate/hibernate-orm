/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.transaction.backend.jta.internal;

import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.jboss.logging.Logger;

import org.hibernate.TransactionException;
import org.hibernate.resource.transaction.spi.TransactionStatus;

/**
 * JtaTransactionAdapter for coordinating with the JTA TransactionManager
 *
 * @author Steve Ebersole
 */
public class JtaTransactionAdapterTransactionManagerImpl implements JtaTransactionAdapter {
	private static final Logger log = Logger.getLogger( JtaTransactionAdapterTransactionManagerImpl.class );

	private final TransactionManager transactionManager;

	private boolean initiator;

	public JtaTransactionAdapterTransactionManagerImpl(TransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Override
	public void begin() {
		try {
			if ( getStatus() == TransactionStatus.NOT_ACTIVE ) {
				log.trace( "Calling TransactionManager#begin" );
				transactionManager.begin();
				initiator = true;
				log.trace( "Called TransactionManager#begin" );
			}
			else {
				log.trace( "Skipping TransactionManager#begin due to already active transaction" );
			}
		}
		catch (Exception e) {
			throw new TransactionException( "JTA TransactionManager#begin failed", e );
		}
	}

	@Override
	public void commit() {
		try {
			if ( initiator ) {
				initiator = false;
				log.trace( "Calling TransactionManager#commit" );
				transactionManager.commit();
				log.trace( "Called TransactionManager#commit" );
			}
			else {
				log.trace( "Skipping TransactionManager#commit due to not being initiator" );
			}
		}
		catch (Exception e) {
			throw new TransactionException( "JTA TransactionManager#commit failed", e );
		}
	}

	@Override
	public void rollback() {
		try {
			if ( initiator ) {
				initiator = false;
				log.trace( "Calling TransactionManager#rollback" );
				transactionManager.rollback();
				log.trace( "Called TransactionManager#rollback" );
			}
			else {
				markRollbackOnly();
			}
		}
		catch (Exception e) {
			throw new TransactionException( "JTA TransactionManager#rollback failed", e );
		}
	}

	@Override
	public TransactionStatus getStatus() {
		try {
			return StatusTranslator.translate( transactionManager.getStatus() );
		}
		catch (SystemException e) {
			throw new TransactionException( "JTA TransactionManager#getStatus failed", e );
		}
	}

	@Override
	public void markRollbackOnly() {
		try {
			transactionManager.setRollbackOnly();
		}
		catch (SystemException e) {
			throw new TransactionException( "Could not set transaction to rollback only", e );
		}
	}

	@Override
	public void setTimeOut(int seconds) {

	}
}
