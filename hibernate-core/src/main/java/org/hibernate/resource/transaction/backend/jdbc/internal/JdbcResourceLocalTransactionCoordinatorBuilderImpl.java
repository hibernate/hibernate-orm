/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

/**
 * Concrete builder for resource-local TransactionCoordinator instances.
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
			return new JdbcResourceLocalTransactionCoordinatorImpl( this, owner, (JdbcResourceTransactionAccess) owner );
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
		return PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION;
	}

	@Override
	public DdlTransactionIsolator buildDdlTransactionIsolator(JdbcContext jdbcContext) {
		return new DdlTransactionIsolatorNonJtaImpl( jdbcContext );
	}
}
