/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.transaction.backend.jdbc.internal;

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
	public static final JdbcResourceLocalTransactionCoordinatorBuilderImpl INSTANCE = new JdbcResourceLocalTransactionCoordinatorBuilderImpl();

	@Override
	public TransactionCoordinator buildTransactionCoordinator(TransactionCoordinatorOwner owner, Options options) {
		if ( owner instanceof JdbcResourceTransactionAccess ) {
			return new JdbcResourceLocalTransactionCoordinatorImpl(
					this,
					owner,
					(JdbcResourceTransactionAccess) owner
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
	public PhysicalConnectionHandlingMode getDefaultConnectionHandlingMode() {
		return DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION;
	}

	@Override
	public DdlTransactionIsolator buildDdlTransactionIsolator(JdbcContext jdbcContext) {
		return new DdlTransactionIsolatorNonJtaImpl( jdbcContext );
	}
}
