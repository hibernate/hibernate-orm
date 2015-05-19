/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.transaction;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;
import org.hibernate.service.Service;

import org.hibernate.ConnectionAcquisitionMode;

/**
 * Builder for TransactionCoordinator instances
 *
 * @author Steve Ebersole
 */
public interface TransactionCoordinatorBuilder extends Service {
	/**
	 * Access to options to are specific to each TransactionCoordinator instance
	 */
	public static interface TransactionCoordinatorOptions {
		/**
		 * Indicates whether an active transaction should be automatically joined.  Only relevant
		 * for JTA-based TransactionCoordinator instances.
		 *
		 * @return {@code true} indicates the active transaction should be auto joined; {@code false}
		 * indicates it should not (until {@link TransactionCoordinator#explicitJoin} is called).
		 */
		public boolean shouldAutoJoinTransaction();
	}

	public TransactionCoordinator buildTransactionCoordinator(TransactionCoordinatorOwner owner, TransactionCoordinatorOptions options);

	public boolean isJta();

	public ConnectionReleaseMode getDefaultConnectionReleaseMode();

	public ConnectionAcquisitionMode getDefaultConnectionAcquisitionMode();
}
