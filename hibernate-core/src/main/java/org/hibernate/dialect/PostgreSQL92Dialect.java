/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

/**
 * An SQL dialect for Postgres 9.2 and later,
 * adds support for JSON data type, and IF EXISTS
 * after ALTER TABLE.
 * 
 * @author Mark Robinson
 *
 * @deprecated use {@code PostgreSQLDialect(920)}
 */
@Deprecated
public class PostgreSQL92Dialect extends PostgreSQLDialect {

	public PostgreSQL92Dialect() {
		super(920);
	}

}
