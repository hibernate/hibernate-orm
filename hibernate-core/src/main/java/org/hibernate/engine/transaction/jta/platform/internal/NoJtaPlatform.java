/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.transaction.jta.platform.internal;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;

/**
 * The non-configured form of JTA platform.  This is what is used if none was set up.
 *
 * @author Steve Ebersole
 */
public class NoJtaPlatform implements JtaPlatform {
	public static final NoJtaPlatform INSTANCE = new NoJtaPlatform();

	@Override
	public @Nullable TransactionManager retrieveTransactionManager() {
		return null;
	}

	@Override
	public @Nullable UserTransaction retrieveUserTransaction() {
		return null;
	}

	@Override
	public @Nullable Object getTransactionIdentifier(Transaction transaction) {
		return null;
	}

	@Override
	public void registerSynchronization(Synchronization synchronization) {
	}

	@Override
	public boolean canRegisterSynchronization() {
		return false;
	}

	@Override
	public int getCurrentStatus() throws SystemException {
		return Status.STATUS_UNKNOWN;
	}
}
