/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * Package defining support for execution of SQL statements through JDBC.  The statement
 * to execute is modelled by {@link org.hibernate.sql.exec.spi.JdbcOperation} and
 * are executed via the corresponding executor.
 *
 * For operations that return ResultSets, be sure to see {@link org.hibernate.sql.results}
 * which provides support for processing results starting with
 * {@link org.hibernate.sql.results.spi.ResultSetMapping}
 */
package org.hibernate.sql.exec;
