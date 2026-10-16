/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.transaction.jta.platform.internal;

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.UserTransaction;
import org.hibernate.Incubating;
import org.jboss.logging.Logger;

import javax.transaction.xa.XAResource;

/**
 * A {@link org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform}
 * that in principle is supposed to work in any Jakarta EE container. This
 * implementation is crippled by the fact that it is unable to suspend and
 * resume transactions.
 * @since 4.0
 * @author Gavin King
 */
@Incubating
public class JakartaStandardJtaPlatform extends AbstractJtaPlatform
		implements TransactionManager, Transaction {

	public static final JakartaStandardJtaPlatform INSTANCE = new JakartaStandardJtaPlatform();
	public static final String USER_TRANSACTION = "java:comp/UserTransaction";
	public static final String TRANSACTION_SYNCHRONIZATION_REGISTRY =
			"java:comp/TransactionSynchronizationRegistry";

	private static final Logger LOG = Logger.getLogger( JakartaStandardJtaPlatform.class );

	@Override
	protected TransactionManager locateTransactionManager() {
		return this;
	}

	@Override
	protected UserTransaction locateUserTransaction() {
		return (UserTransaction) jndiService().locate( USER_TRANSACTION );
	}

	@Override
	protected JtaSynchronizationStrategy getSynchronizationStrategy() {
		return new SynchronizationRegistryBasedSynchronizationStrategy(
				this::getTransactionSynchronizationRegistry );
	}

	private TransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
		return (TransactionSynchronizationRegistry)
				jndiService().locate( TRANSACTION_SYNCHRONIZATION_REGISTRY );
	}

	@Override
	public void begin() throws NotSupportedException, SystemException {
		locateUserTransaction().begin();
	}

	@Override
	public void commit()
			throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
			SecurityException, IllegalStateException, SystemException {
		locateUserTransaction().commit();
	}

	@Override
	public void rollback() throws IllegalStateException, SecurityException, SystemException {
		locateUserTransaction().rollback();
	}

	@Override
	public void setRollbackOnly() throws IllegalStateException, SystemException {
		locateUserTransaction().setRollbackOnly();
	}

	@Override
	public int getStatus() throws SystemException {
		return locateUserTransaction().getStatus();
	}

	@Override
	public Transaction getTransaction() throws SystemException {
		return this;
	}

	@Override
	public void setTransactionTimeout(int seconds) throws SystemException {
		locateUserTransaction().setTransactionTimeout( seconds );
	}

	@Override
	public boolean delistResource(XAResource xaRes, int flag) {
		throw new UnsupportedOperationException( "JakartaStandardJtaPlatform does not have access to the TransactionManager" );
	}

	@Override
	public boolean enlistResource(XAResource xaRes) {
		throw new UnsupportedOperationException( "JakartaStandardJtaPlatform does not have access to the TransactionManager" );
	}

	@Override
	public Transaction suspend() throws SystemException {
		LOG.debug( "Cannot really suspend (JakartaStandardJtaPlatform does not have access to the TransactionManager)" );
		return this;
//		throw new UnsupportedOperationException( "JakartaStandardJtaPlatform does not have access to the TransactionManager" );
	}

	@Override
	public void resume(Transaction tobj) {
		LOG.debug( "Cannot really resume (JakartaStandardJtaPlatform does not have access to the TransactionManager)" );
//		throw new UnsupportedOperationException( "JakartaStandardJtaPlatform does not have access to the TransactionManager" );
	}

}
