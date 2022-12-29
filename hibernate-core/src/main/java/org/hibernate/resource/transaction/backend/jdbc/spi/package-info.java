/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * An SPI which models the concept of a JDBC resource-level transaction.
 * <p>
 * JDBC itself provides no object which represents a transaction.
 *
 * @see org.hibernate.resource.transaction.backend.jdbc.spi.JdbcResourceTransaction
 */
package org.hibernate.resource.transaction.backend.jdbc.spi;
