/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

/**
 * An SQL dialect for Postgres 9 and later.  Adds support for "if exists" when dropping constraints
 * 
 * @author edalquist
 */
public class PostgreSQL9Dialect extends PostgreSQL82Dialect {
	@Override
	public boolean supportsIfExistsBeforeConstraintName() {
		return true;
	}
}
