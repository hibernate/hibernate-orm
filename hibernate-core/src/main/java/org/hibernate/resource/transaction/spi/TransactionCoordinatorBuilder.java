/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.transaction.spi;

import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.transaction.backend.jdbc.internal.DdlTransactionIsolatorNonJtaImpl;
import org.hibernate.resource.transaction.backend.jta.internal.DdlTransactionIsolatorJtaImpl;
import org.hibernate.service.Service;
import org.hibernate.tool.schema.internal.exec.JdbcContext;

/**
 * Builder for {@link TransactionCoordinator} instances.
 * <p>
 * A {@code TransactionCoordinator} may be selected using the configuration property
 * {@value org.hibernate.cfg.AvailableSettings#TRANSACTION_COORDINATOR_STRATEGY}.
 *
 * @author Steve Ebersole
 */
public interface TransactionCoordinatorBuilder extends Service {
	/**
	 * Access to options that are specific to each {@link TransactionCoordinator} instance.
	 */
	interface Options {
		/**
		 * Indicates whether an active transaction should be automatically joined.  Only relevant
		 * for JTA-based {@link TransactionCoordinator} instances.
		 *
		 * @return {@code true} indicates the active transaction should be auto joined; {@code false}
		 * indicates it should not (until {@link TransactionCoordinator#explicitJoin} is called).
		 */
		boolean shouldAutoJoinTransaction();
	}

	TransactionCoordinator buildTransactionCoordinator(TransactionCoordinatorOwner owner, Options options);

	boolean isJta();

	PhysicalConnectionHandlingMode getDefaultConnectionHandlingMode();

	default DdlTransactionIsolator buildDdlTransactionIsolator(JdbcContext jdbcContext) {
		return isJta()
				? new DdlTransactionIsolatorJtaImpl( jdbcContext )
				: new DdlTransactionIsolatorNonJtaImpl( jdbcContext );
	}
}
