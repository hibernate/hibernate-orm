/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

/**
 * An SQL dialect for Postgres 9.1 and later, adds support for PARTITION BY as a keyword.
 * 
 * @author Mark Robinson
 */
public class PostgreSQL91Dialect extends PostgreSQL9Dialect {

	@Override
	public boolean supportsPartitionBy() {
		return true;
	}
}
