/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
