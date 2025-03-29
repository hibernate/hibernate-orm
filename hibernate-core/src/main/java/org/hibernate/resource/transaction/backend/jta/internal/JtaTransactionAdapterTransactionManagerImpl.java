/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.transaction.backend.jta.internal;

import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;

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
			final TransactionStatus status = StatusTranslator.translate( transactionManager.getStatus() );
			if ( status == null ) {
				throw new TransactionException( "TransactionManager reported transaction status as unknown" );
			}
			return status;
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
		if ( seconds > 0 ) {
			try {
				transactionManager.setTransactionTimeout( seconds );
			}
			catch (SystemException e) {
				throw new TransactionException( "Unable to apply requested transaction timeout", e );
			}
		}
	}
}
