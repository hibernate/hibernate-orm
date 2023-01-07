/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

/**
 * Enumerates strategies for checking JDBC return codes for custom SQL DML queries.
 * <p>
 * Return code checking is used to verify that a SQL statement actually had the
 * intended effect, for example, that an {@code UPDATE} statement actually changed
 * the expected number of rows.
 *
 * @author László Benke
 *
 * @see SQLInsert#check()
 * @see SQLUpdate#check()
 * @see SQLDelete#check()
 * @see SQLDeleteAll#check()
 */
public enum ResultCheckStyle {
	/**
	 * No return code checking. Might mean that no checks are required, or that
	 * failure is indicated by a {@link java.sql.SQLException} being thrown, for
	 * example, by a {@link java.sql.CallableStatement stored procedure} which
	 * performs explicit checks.
	 */
	NONE,
	/**
	 * Row count checking. A row count is an integer value returned by
	 * {@link java.sql.PreparedStatement#executeUpdate()} or
	 * {@link java.sql.Statement#executeBatch()}. The row count is checked
	 * against an expected value. For example, the expected row count for
	 * an {@code INSERT} statement is always 1.
	 */
	COUNT,
	/**
	 * Essentially identical to {@link #COUNT} except that the row count is
	 * obtained via an output parameter of a {@link java.sql.CallableStatement
	 * stored procedure}.
	 * <p>
	 * Statement batching is disabled when {@code PARAM} is selected.
	 */
	PARAM
}
