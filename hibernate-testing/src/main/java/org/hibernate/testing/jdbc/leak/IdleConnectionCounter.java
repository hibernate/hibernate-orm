/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.jdbc.leak;

import java.sql.Connection;

import org.hibernate.dialect.Dialect;

/**
 * @author Vlad Mihalcea
 */
public interface IdleConnectionCounter {

	/**
	 * Specifies which Dialect the counter applies to.
	 *
	 * @param dialect dialect
	 *
	 * @return applicability.
	 */
	boolean appliesTo(Class<? extends Dialect> dialect);

	/**
	 * Count the number of idle connections.
	 *
	 * @param connection current JDBC connection to be used for querying the number of idle connections.
	 *
	 * @return idle connection count.
	 */
	int count(Connection connection);
}
