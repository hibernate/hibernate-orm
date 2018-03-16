/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.transaction.backend.jdbc.spi;

import org.hibernate.resource.transaction.spi.TransactionStatus;

/**
 * Models access to the resource transaction of the underlying JDBC resource.
 *
 * @author Steve Ebersole
 */
public interface JdbcResourceTransaction {
	/**
	 * Begin the resource transaction
	 */
	void begin();

	/**
	 * Commit the resource transaction
	 */
	void commit();

	/**
	 * Rollback the resource transaction
	 */
	void rollback();

	TransactionStatus getStatus();
}
