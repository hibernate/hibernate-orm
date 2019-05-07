/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.query.spi.QueryEngine;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * A group common function template definitions.  Centralized for easier use from
 * Dialects
 *
 * @author Steve Ebersole
 */
public class CommonFunctionFactory {
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// trigonometric/geometric functions

	public static void cosh(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "cosh" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void cot(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "cot" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();
	}

	public static void degrees(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "degrees" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();
	}

	public static void log(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "log" )
				.setArgumentCountBetween( 1, 2 )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();
	}

	public static void log10(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "log10" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();
	}

	public static void radians(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "radians" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();
	}

	public static void sinh(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "sinh" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();
	}

	public static void tanh(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "tanh" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// basic math functions

	public static void trunc(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "trunc" )
				.setArgumentCountBetween( 1, 2 )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();
	}

	public static void truncate(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "truncate" )
				.setArgumentCountBetween( 1, 2 )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();
	}

	public static void rand(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "rand" )
				.setArgumentCountBetween( 0, 1 )
				.setUseParenthesesWhenNoArgs( true )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();
	}

	public static void stddev(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "stddev" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void variance(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "variance" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void pi(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "pi", StandardSpiBasicTypes.DOUBLE );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// character functions

	public static void soundex(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "soundex" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.register();
	}

}
