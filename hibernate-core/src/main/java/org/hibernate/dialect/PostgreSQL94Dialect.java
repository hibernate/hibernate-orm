/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.query.spi.QueryEngine;
import org.hibernate.type.StandardBasicTypes;

/**
 * An SQL dialect for Postgres 9.4 and later. Adds support for various date and time functions
 */
public class PostgreSQL94Dialect extends PostgreSQL93Dialect {

	/**
	 * Constructs a PostgreSQL94Dialect
	 */
	@SuppressWarnings("WeakerAccess")
	public PostgreSQL94Dialect() {
		super();
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "make_interval" )
				.setInvariantType( StandardBasicTypes.TIMESTAMP )
				.setArgumentCountBetween( 1, 7 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "make_timestamp" )
				.setInvariantType( StandardBasicTypes.TIMESTAMP )
				.setExactArgumentCount( 6 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "make_timestamptz" )
				.setInvariantType( StandardBasicTypes.TIMESTAMP )
				.setArgumentCountBetween( 6, 7 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "make_date" )
				.setInvariantType( StandardBasicTypes.DATE )
				.setExactArgumentCount( 3 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "make_time" )
				.setInvariantType( StandardBasicTypes.TIME )
				.setExactArgumentCount( 3 )
				.register();
	}
}
