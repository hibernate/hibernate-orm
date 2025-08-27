/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.transaction.backend.jta.internal;

import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;

import org.hibernate.TransactionException;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import static org.hibernate.resource.transaction.backend.jta.internal.JtaLogging.JTA_LOGGER;

/**
 * JtaTransactionAdapter for coordinating with the JTA TransactionManager
 *
 * @author Steve Ebersole
 */
public class JtaTransactionAdapterTransactionManagerImpl implements JtaTransactionAdapter {

	private final TransactionManager transactionManager;

	private boolean initiator;

	public JtaTransactionAdapterTransactionManagerImpl(TransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Override
	public void begin() {
		try {
			if ( getStatus() == TransactionStatus.NOT_ACTIVE ) {
				JTA_LOGGER.callingTransactionManagerBegin();
				transactionManager.begin();
				initiator = true;
				JTA_LOGGER.calledTransactionManagerBegin();
			}
			else {
				JTA_LOGGER.skippingTransactionManagerBegin();
			}
		}
		catch (Exception e) {
			throw new TransactionException( "JTA TransactionManager.begin() failed", e );
		}
	}

	@Override
	public void commit() {
		try {
			if ( initiator ) {
				initiator = false;
				JTA_LOGGER.callingTransactionManagerCommit();
				transactionManager.commit();
				JTA_LOGGER.calledTransactionManagerCommit();
			}
			else {
				JTA_LOGGER.skippingTransactionManagerCommit();
			}
		}
		catch (Exception e) {
			throw new TransactionException( "JTA TransactionManager.commit() failed", e );
		}
	}

	@Override
	public void rollback() {
		try {
			if ( initiator ) {
				initiator = false;
				JTA_LOGGER.callingTransactionManagerRollback();
				transactionManager.rollback();
				JTA_LOGGER.calledTransactionManagerRollback();
			}
			else {
				markRollbackOnly();
			}
		}
		catch (Exception e) {
			throw new TransactionException( "JTA TransactionManager.rollback() failed", e );
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
