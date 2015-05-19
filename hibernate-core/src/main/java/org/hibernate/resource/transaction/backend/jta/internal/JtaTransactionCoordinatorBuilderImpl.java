/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.transaction.backend.jta.internal;

import org.hibernate.ConnectionAcquisitionMode;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.resource.transaction.TransactionCoordinator;
import org.hibernate.resource.transaction.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;

/**
 * Concrete builder for JTA-based TransactionCoordinator instances.
 *
 * @author Steve Ebersole
 */
public class JtaTransactionCoordinatorBuilderImpl implements TransactionCoordinatorBuilder {
	public static final String SHORT_NAME = "jta";

	@Override
	public TransactionCoordinator buildTransactionCoordinator(TransactionCoordinatorOwner owner, TransactionCoordinatorOptions options) {
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
	public ConnectionReleaseMode getDefaultConnectionReleaseMode() {
		return ConnectionReleaseMode.AFTER_STATEMENT;
	}

	@Override
	public ConnectionAcquisitionMode getDefaultConnectionAcquisitionMode() {
		return ConnectionAcquisitionMode.DEFAULT;
	}
}
