/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * A small API allowing the client of a Hibernate session to interact directly
 * with JDBC, using the same connection and transaction obtained by the session.
 * <p>
 * {@link org.hibernate.jdbc.Work} and {@link org.hibernate.jdbc.ReturningWork}
 * define the notion of a unit of JDBC work that may be executed by the session
 * at the request of the client. Execution of a unit of work may be requested by
 * calling:
 * <ul>
 * <li>
 * {@link org.hibernate.SharedSessionContract#doWork(org.hibernate.jdbc.Work)} or
 * <li>
 * {@link org.hibernate.SharedSessionContract#doReturningWork(org.hibernate.jdbc.ReturningWork)}.
 * </ul>
 * <p>
 * For example:
 * <pre>
 * session.doWork(connection -> {
 *     try ( PreparedStatement ps = connection.prepareStatement( " ... " ) ) {
 *         ps.execute();
 *     }
 * });
 * </pre>
 * <p>
 * The interface {@link org.hibernate.jdbc.Expectation} defines a contract for
 * checking the results of a JDBC operation which executes user-written SQL:
 * <ul>
 * <li>{@link org.hibernate.jdbc.Expectation.RowCount} is used to check returned
 *     row counts,
 * <li>{@link org.hibernate.jdbc.Expectation.OutParameter} is used to check out
 *     parameters of stored procedures, and
 * <li>user-written implementations of {@code Expectation} are also supported.
 * </ul>
 * An {@code Expectation} class may be specified along with the user-written SQL
 * using {@link org.hibernate.annotations.SQLInsert#verify},
 * {@link org.hibernate.annotations.SQLUpdate#verify}, or
 * {@link org.hibernate.annotations.SQLDelete#verify}.
 *
 *
 * @see org.hibernate.jdbc.Work
 * @see org.hibernate.jdbc.ReturningWork
 * @see org.hibernate.jdbc.Expectation
 */
package org.hibernate.jdbc;
