/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.vector;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

public class PGVectorFunctionContributor implements FunctionContributor {

	@Override
	public void contributeFunctions(FunctionContributions functionContributions) {
		final SqmFunctionRegistry functionRegistry = functionContributions.getFunctionRegistry();
		final TypeConfiguration typeConfiguration = functionContributions.getTypeConfiguration();
		final BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
		final Dialect dialect = functionContributions.getDialect();
		if ( dialect instanceof PostgreSQLDialect ) {
			final BasicType<Double> doubleType = basicTypeRegistry.resolve( StandardBasicTypes.DOUBLE );
			final BasicType<Integer> integerType = basicTypeRegistry.resolve( StandardBasicTypes.INTEGER );
			functionRegistry.patternDescriptorBuilder( "cosine_distance", "?1<=>?2" )
					.setArgumentsValidator( StandardArgumentsValidators.composite(
							StandardArgumentsValidators.exactly( 2 ),
							VectorArgumentValidator.INSTANCE
					) )
					.setArgumentTypeResolver( VectorArgumentTypeResolver.INSTANCE )
					.setReturnTypeResolver( StandardFunctionReturnTypeResolvers.invariant( doubleType ) )
					.register();
			functionRegistry.patternDescriptorBuilder( "euclidean_distance", "?1<->?2" )
					.setArgumentsValidator( StandardArgumentsValidators.composite(
							StandardArgumentsValidators.exactly( 2 ),
							VectorArgumentValidator.INSTANCE
					) )
					.setArgumentTypeResolver( VectorArgumentTypeResolver.INSTANCE )
					.setReturnTypeResolver( StandardFunctionReturnTypeResolvers.invariant( doubleType ) )
					.register();
			functionRegistry.registerAlternateKey( "l2_distance", "euclidean_distance" );
			functionRegistry.namedDescriptorBuilder( "l1_distance" )
					.setArgumentsValidator( StandardArgumentsValidators.composite(
							StandardArgumentsValidators.exactly( 2 ),
							VectorArgumentValidator.INSTANCE
					) )
					.setArgumentTypeResolver( VectorArgumentTypeResolver.INSTANCE )
					.setReturnTypeResolver( StandardFunctionReturnTypeResolvers.invariant( doubleType ) )
					.register();
			functionRegistry.registerAlternateKey( "taxicab_distance",  "l1_distance" );

			functionRegistry.patternDescriptorBuilder( "negative_inner_product", "?1<#>?2" )
					.setArgumentsValidator( StandardArgumentsValidators.composite(
							StandardArgumentsValidators.exactly( 2 ),
							VectorArgumentValidator.INSTANCE
					) )
					.setArgumentTypeResolver( VectorArgumentTypeResolver.INSTANCE )
					.setReturnTypeResolver( StandardFunctionReturnTypeResolvers.invariant( doubleType ) )
					.register();
			functionRegistry.patternDescriptorBuilder( "inner_product", "(?1<#>?2)*-1" )
					.setArgumentsValidator( StandardArgumentsValidators.composite(
							StandardArgumentsValidators.exactly( 2 ),
							VectorArgumentValidator.INSTANCE
					) )
					.setArgumentTypeResolver( VectorArgumentTypeResolver.INSTANCE )
					.setReturnTypeResolver( StandardFunctionReturnTypeResolvers.invariant( doubleType ) )
					.register();
			functionRegistry.registerAlternateKey("dot", "inner_product");
			functionRegistry.namedDescriptorBuilder( "vector_dims" )
					.setArgumentsValidator( StandardArgumentsValidators.composite(
							StandardArgumentsValidators.exactly( 1 ),
							VectorArgumentValidator.INSTANCE
					) )
					.setArgumentTypeResolver( VectorArgumentTypeResolver.INSTANCE )
					.setReturnTypeResolver( StandardFunctionReturnTypeResolvers.invariant( integerType ) )
					.register();
			functionRegistry.registerAlternateKey("dim", "vector_dims");
			functionRegistry.namedDescriptorBuilder( "vector_norm" )
					.setArgumentsValidator( StandardArgumentsValidators.composite(
							StandardArgumentsValidators.exactly( 1 ),
							VectorArgumentValidator.INSTANCE
					) )
					.setArgumentTypeResolver( VectorArgumentTypeResolver.INSTANCE )
					.setReturnTypeResolver( StandardFunctionReturnTypeResolvers.invariant( doubleType ) )
					.register();
			functionRegistry.registerAlternateKey("norm", "vector_norm");
		}
	}

	@Override
	public int ordinal() {
		return 200;
	}
}
