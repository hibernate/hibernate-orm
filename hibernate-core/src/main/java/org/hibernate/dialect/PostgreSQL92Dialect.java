/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

/**
 * An SQL dialect for Postgres 9.2 and later, adds support for JSON data type
 * 
 * @author Mark Robinson
 */
public class PostgreSQL92Dialect extends PostgreSQL9Dialect {

	/**
	 * Constructs a PostgreSQL92Dialect
	 */
	public PostgreSQL92Dialect() {
		super();
		this.registerColumnType( Types.JAVA_OBJECT, "json" );
	}
}
