/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.query.sqm.produce.function.spi.NamedSqmFunctionTemplate;
import org.hibernate.type.StandardBasicTypes;

/**
 * An SQL dialect for Postgres 9.4 and later. Adds support for various date and time functions
 */
public class PostgreSQL94Dialect extends PostgreSQL93Dialect {

	/**
	 * Constructs a PostgreSQL94Dialect
	 */
	public PostgreSQL94Dialect() {
		super();
		registerFunction( "make_interval", new NamedSqmFunctionTemplate( "make_interval", StandardBasicTypes.TIMESTAMP) );
		registerFunction( "make_timestamp", new NamedSqmFunctionTemplate( "make_timestamp", StandardBasicTypes.TIMESTAMP) );
		registerFunction( "make_timestamptz", new NamedSqmFunctionTemplate( "make_timestamptz", StandardBasicTypes.TIMESTAMP) );
		registerFunction( "make_date", new NamedSqmFunctionTemplate( "make_date", StandardBasicTypes.DATE) );
		registerFunction( "make_time", new NamedSqmFunctionTemplate( "make_time", StandardBasicTypes.TIME) );
	}
}
