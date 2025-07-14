/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.transaction.backend.jta.internal;

import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;

import org.hibernate.TransactionException;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import static org.hibernate.resource.transaction.backend.jta.internal.JtaLogging.JTA_LOGGER;

/**
 * JtaTransactionAdapter for coordinating with the JTA UserTransaction
 *
 * @author Steve Ebersole
 */
public class JtaTransactionAdapterUserTransactionImpl implements JtaTransactionAdapter {

	private final UserTransaction userTransaction;


	private boolean initiator;

	public JtaTransactionAdapterUserTransactionImpl(UserTransaction userTransaction) {
		this.userTransaction = userTransaction;
	}

	@Override
	public void begin() {
		try {
			if ( getStatus() == TransactionStatus.NOT_ACTIVE ) {
				JTA_LOGGER.callingUserTransactionBegin();
				userTransaction.begin();
				initiator = true;
				JTA_LOGGER.calledUserTransactionBegin();
			}
			else {
				JTA_LOGGER.skippingTransactionManagerBegin();
			}
		}
		catch (Exception e) {
			throw new TransactionException( "JTA UserTransaction.begin() failed", e );
		}
	}

	@Override
	public void commit() {
		try {
			if ( initiator ) {
				initiator = false;
				JTA_LOGGER.callingUserTransactionCommit();
				userTransaction.commit();
				JTA_LOGGER.calledUserTransactionCommit();
			}
			else {
				JTA_LOGGER.skippingTransactionManagerCommit();
			}
		}
		catch (Exception e) {
			throw new TransactionException( "JTA UserTransaction.commit() failed", e );
		}
	}

	@Override
	public void rollback() {
		try {
			if ( initiator ) {
				initiator = false;
				JTA_LOGGER.callingUserTransactionRollback();
				userTransaction.rollback();
				JTA_LOGGER.calledUserTransactionRollback();
			}
			else {
				markRollbackOnly();
			}
		}
		catch (Exception e) {
			throw new TransactionException( "JTA UserTransaction.rollback() failed", e );
		}
	}

	@Override
	public TransactionStatus getStatus() {
		try {
			final TransactionStatus status = StatusTranslator.translate( userTransaction.getStatus() );
			if ( status == null ) {
				throw new TransactionException( "UserTransaction reported transaction status as unknown" );
			}
			return status;
		}
		catch (SystemException e) {
			throw new TransactionException( "JTA UserTransaction.getStatus() failed", e );
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
