/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.resource.transaction.jta;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;
import org.checkerframework.checker.nullness.qual.Nullable;

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
	public @Nullable TransactionManager retrieveTransactionManager() {
		if ( preferExceptions ) {
			throw new JtaPlatformInaccessibleException();
		}
		return null;
	}

	@Override
	public @Nullable UserTransaction retrieveUserTransaction() {
		if ( preferExceptions ) {
			throw new JtaPlatformInaccessibleException();
		}
		return null;
	}

	@Override
	public @Nullable Object getTransactionIdentifier(Transaction transaction) {
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
