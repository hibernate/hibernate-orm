/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

/**
 * An SQL dialect for Postgres 9.5 and later.
 * Adds support for SKIP LOCKED.
 *
 * @deprecated use {@code PostgreSQLDialect(950)}
 */
@Deprecated
public class PostgreSQL95Dialect extends PostgreSQLDialect {

	public PostgreSQL95Dialect() {
		super(950);
	}

}
