/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.StandardBasicTypes;

/**
 * An SQL dialect for Postgres 9.4 and later. Adds support for various date and time functions
 */
public class PostgreSQL94Dialect extends PostgreSQL92Dialect {

	/**
	 * Constructs a PostgreSQL94Dialect
	 */
	public PostgreSQL94Dialect() {
		super();
		registerFunction( "make_interval", new StandardSQLFunction("make_interval", StandardBasicTypes.TIMESTAMP) );
		registerFunction( "make_timestamp", new StandardSQLFunction("make_timestamp", StandardBasicTypes.TIMESTAMP) );
		registerFunction( "make_timestamptz", new StandardSQLFunction("make_timestamptz", StandardBasicTypes.TIMESTAMP) );
		registerFunction( "make_date", new StandardSQLFunction("make_date", StandardBasicTypes.DATE) );
		registerFunction( "make_time", new StandardSQLFunction("make_time", StandardBasicTypes.TIME) );
	}
}
