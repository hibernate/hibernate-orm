/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * Defines certain very important flavors of {@link org.hibernate.JDBCException},
 * along with an SPI for interpreting product-specific {@link java.sql.SQLException}s
 * arising from a JDBC driver into something more uniform and meaningful.
 */
package org.hibernate.exception;
