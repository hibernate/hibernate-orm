/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * Defines the notion of a unit of JDBC work that may be executed by the session
 * at the request of the client.
 * <p>
 * Execution of a unit of work may be requested by calling
 * {@link org.hibernate.SharedSessionContract#doWork(org.hibernate.jdbc.Work)} or
 * {@link org.hibernate.SharedSessionContract#doReturningWork(org.hibernate.jdbc.ReturningWork)}.
 * <p>
 * For example:
 * <pre>
 * session.doWork(connection -> {
 *     try ( PreparedStatement ps = connection.prepareStatement( " ... " ) ) {
 *         ps.execute();
 *     }
 * });
 * </pre>
 *
 *
 * @see org.hibernate.jdbc.Work
 * @see org.hibernate.jdbc.ReturningWork
 * @see org.hibernate.jdbc.Expectation
 */
package org.hibernate.jdbc;
