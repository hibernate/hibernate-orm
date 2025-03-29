/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.jta;

import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;

/**
 * @author Steve Ebersole
 */
public class TestingJtaTransactionCoordinatorBuilder implements TransactionCoordinatorBuilder {
	@Override
	public TransactionCoordinator buildTransactionCoordinator(TransactionCoordinatorOwner owner, Options options) {
		return null;
	}

	@Override
	public boolean isJta() {
		return false;
	}

	@Override
	public PhysicalConnectionHandlingMode getDefaultConnectionHandlingMode() {
		return null;
	}
}
