/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.transaction.backend.jdbc.spi;

/**
 * Provides access to DataStoreTransaction (JDBC transaction stand-in) for use in building resource-local
 * TransactionCoordinator instances.
 *
 * @author Steve Ebersole
 */
public interface JdbcResourceTransactionAccess {
	/**
	 * Provides access to the resource local transaction of this data store, which is used by the TransactionCoordinator
	 * to manage transactions against the data store when not using JTA.
	 *
	 * @return The resource-local transaction
	 */
	JdbcResourceTransaction getResourceLocalTransaction();
}
