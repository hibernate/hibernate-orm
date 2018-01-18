/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.transaction.spi;

import org.hibernate.ConnectionAcquisitionMode;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.transaction.backend.jdbc.internal.DdlTransactionIsolatorNonJtaImpl;
import org.hibernate.resource.transaction.backend.jta.internal.DdlTransactionIsolatorJtaImpl;
import org.hibernate.service.Service;
import org.hibernate.tool.schema.internal.exec.JdbcContext;

/**
 * Builder for TransactionCoordinator instances
 *
 * @author Steve Ebersole
 */
public interface TransactionCoordinatorBuilder extends Service {
	/**
	 * Access to options to are specific to each TransactionCoordinator instance
	 */
	interface Options {
		/**
		 * Indicates whether an active transaction should be automatically joined.  Only relevant
		 * for JTA-based TransactionCoordinator instances.
		 *
		 * @return {@code true} indicates the active transaction should be auto joined; {@code false}
		 * indicates it should not (until {@link TransactionCoordinator#explicitJoin} is called).
		 */
		boolean shouldAutoJoinTransaction();
	}

	TransactionCoordinator buildTransactionCoordinator(TransactionCoordinatorOwner owner, Options options);

	boolean isJta();

	PhysicalConnectionHandlingMode getDefaultConnectionHandlingMode();

	/**
	 * @deprecated (since 5.2) Use {@link #getDefaultConnectionHandlingMode} instead
	 */
	@Deprecated
	default ConnectionAcquisitionMode getDefaultConnectionAcquisitionMode() {
		return getDefaultConnectionHandlingMode().getAcquisitionMode();
	}

	/**
	 * @deprecated (since 5.2) Use {@link #getDefaultConnectionHandlingMode} instead
	 */
	@Deprecated
	default ConnectionReleaseMode getDefaultConnectionReleaseMode() {
		return getDefaultConnectionHandlingMode().getReleaseMode();
	}

	default DdlTransactionIsolator buildDdlTransactionIsolator(JdbcContext jdbcContext) {
		return isJta() ? new DdlTransactionIsolatorJtaImpl( jdbcContext ) : new DdlTransactionIsolatorNonJtaImpl( jdbcContext );
	}
}
