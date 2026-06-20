/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.jta;

import jakarta.annotation.Nonnull;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;

/**
 * @author Steve Ebersole
 */
public class TestingJtaTransactionCoordinatorBuilder implements TransactionCoordinatorBuilder {
	@Override
	@Nonnull
	public TransactionCoordinator buildTransactionCoordinator(TransactionCoordinatorOwner owner, Options options) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isJta() {
		return false;
	}

	@Override
	@Nonnull
	public PhysicalConnectionHandlingMode getDefaultConnectionHandlingMode() {
		throw new UnsupportedOperationException();
	}
}
