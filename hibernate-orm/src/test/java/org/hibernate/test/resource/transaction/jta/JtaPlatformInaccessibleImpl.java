/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.resource.transaction.jta;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;

/**
 * JtaPlatform used to test cases when JTA in not available
 *
 * @author Steve Ebersole
 */
public class JtaPlatformInaccessibleImpl implements JtaPlatform {
	private final boolean preferExceptions;

	public JtaPlatformInaccessibleImpl(boolean preferExceptions) {
		this.preferExceptions = preferExceptions;
	}

	@Override
	public TransactionManager retrieveTransactionManager() {
		if ( preferExceptions ) {
			throw new JtaPlatformInaccessibleException();
		}
		return null;
	}

	@Override
	public UserTransaction retrieveUserTransaction() {
		if ( preferExceptions ) {
			throw new JtaPlatformInaccessibleException();
		}
		return null;
	}

	@Override
	public Object getTransactionIdentifier(Transaction transaction) {
		if ( preferExceptions ) {
			throw new JtaPlatformInaccessibleException();
		}
		return null;
	}

	@Override
	public boolean canRegisterSynchronization() {
		return false;
	}

	@Override
	public void registerSynchronization(Synchronization synchronization) {
		if ( preferExceptions ) {
			throw new JtaPlatformInaccessibleException();
		}
	}

	@Override
	public int getCurrentStatus() throws SystemException {
		return Status.STATUS_NO_TRANSACTION;
	}

	public static class JtaPlatformInaccessibleException extends RuntimeException {
	}
}
