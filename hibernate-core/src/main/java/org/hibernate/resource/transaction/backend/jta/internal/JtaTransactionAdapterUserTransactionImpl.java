/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.resource.transaction.backend.jta.internal;

import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.hibernate.TransactionException;
import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import org.jboss.logging.Logger;

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
				log.trace( "Calling UserTransaction#commit" );
				userTransaction.commit();
				log.trace( "Called UserTransaction#commit" );
			}
			else {
				log.trace( "Skipping TransactionManager#commit due to not being initiator" );
			}
			initiator = false;
		}
		catch (Exception e) {
			throw new TransactionException( "JTA UserTransaction#commit failed", e );
		}
	}

	@Override
	public void rollback() {
		try {
			if ( initiator ) {
				log.trace( "Calling UserTransaction#rollback" );
				userTransaction.rollback();
				log.trace( "Called UserTransaction#rollback" );
			}
			else {
				log.trace( "Skipping TransactionManager#commit due to not being initiator" );
			}
			initiator = false;
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
