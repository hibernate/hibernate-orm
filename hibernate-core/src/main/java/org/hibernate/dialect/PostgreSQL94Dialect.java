/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.query.spi.QueryEngine;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * An SQL dialect for Postgres 9.4 and later. Adds support for various date and time functions
 */
public class PostgreSQL94Dialect extends PostgreSQL93Dialect {

	/**
	 * Constructs a PostgreSQL94Dialect
	 */
	public PostgreSQL94Dialect() {
		super();
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "make_interval" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setArgumentCountBetween( 1, 7 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "make_timestamp" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setExactArgumentCount( 6 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "make_timestamptz" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setArgumentCountBetween( 6, 7 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "make_date" )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.setExactArgumentCount( 3 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "make_time" )
				.setInvariantType( StandardSpiBasicTypes.TIME )
				.setExactArgumentCount( 3 )
				.register();
	}
}
