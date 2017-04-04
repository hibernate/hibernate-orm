/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.transaction.backend.jta.internal;

import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;
import org.hibernate.tool.schema.internal.exec.JdbcContext;

/**
 * Concrete builder for JTA-based TransactionCoordinator instances.
 *
 * @author Steve Ebersole
 */
public class JtaTransactionCoordinatorBuilderImpl implements TransactionCoordinatorBuilder {
	public static final String SHORT_NAME = "jta";

	@Override
	public TransactionCoordinator buildTransactionCoordinator(TransactionCoordinatorOwner owner, Options options) {
		return new JtaTransactionCoordinatorImpl(
				this,
				owner,
				options.shouldAutoJoinTransaction()
		);
	}

	@Override
	public boolean isJta() {
		return true;
	}

	@Override
	public PhysicalConnectionHandlingMode getDefaultConnectionHandlingMode() {
		// todo : I want to change this to PhysicalConnectionHandlingMode#IMMEDIATE_ACQUISITION_AND_HOLD
		return PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT;
	}

	@Override
	public DdlTransactionIsolator buildDdlTransactionIsolator(JdbcContext jdbcContext) {
		return new DdlTransactionIsolatorJtaImpl( jdbcContext );
	}
}
