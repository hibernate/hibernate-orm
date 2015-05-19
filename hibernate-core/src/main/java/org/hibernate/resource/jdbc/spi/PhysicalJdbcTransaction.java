/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.jdbc.spi;

import org.hibernate.resource.transaction.backend.jdbc.spi.JdbcResourceTransaction;

/**
 * Provides access to manage "transactionality" via the JDBC Connection
 *
 * @author Steve Ebersole
 */
public interface PhysicalJdbcTransaction extends JdbcResourceTransaction {
}
