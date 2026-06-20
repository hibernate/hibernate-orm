/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.transaction.backend.jdbc.internal;

import jakarta.annotation.Nonnull;
import org.hibernate.HibernateException;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.transaction.backend.jdbc.spi.JdbcResourceTransactionAccess;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;
import org.hibernate.tool.schema.internal.exec.JdbcContext;

import static org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION;

/**
 * Concrete builder for resource-local {@link TransactionCoordinator} instances.
 *
 * @author Steve Ebersole
 */
public class JdbcResourceLocalTransactionCoordinatorBuilderImpl implements TransactionCoordinatorBuilder {
	public static final String SHORT_NAME = "jdbc";

	/**
	 * Singleton access
	 */
	public static final TransactionCoordinatorBuilder INSTANCE = new JdbcResourceLocalTransactionCoordinatorBuilderImpl();

	@Override
	@Nonnull
	public TransactionCoordinator buildTransactionCoordinator(TransactionCoordinatorOwner owner, Options options) {
		if ( owner instanceof JdbcResourceTransactionAccess transactionAccess ) {
			return new JdbcResourceLocalTransactionCoordinatorImpl(
					this,
					owner,
					transactionAccess
			);
		}

		throw new HibernateException(
				"Could not determine ResourceLocalTransactionAccess to use in building TransactionCoordinator"
		);
	}

	@Override
	public boolean isJta() {
		return false;
	}

	@Override
	@Nonnull
	public PhysicalConnectionHandlingMode getDefaultConnectionHandlingMode() {
		return DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION;
	}

	@Override
	@Nonnull
	public DdlTransactionIsolator buildDdlTransactionIsolator(JdbcContext jdbcContext) {
		return new DdlTransactionIsolatorNonJtaImpl( jdbcContext );
	}
}
