/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.INTEGER;

public class PGVectorFunctionContributor implements FunctionContributor {

	@Override
	public void contributeFunctions(FunctionContributions functionContributions) {
		final Dialect dialect = functionContributions.getDialect();
		if (dialect instanceof PostgreSQLDialect || dialect instanceof CockroachDialect) {
			final VectorFunctionFactory vectorFunctionFactory = new VectorFunctionFactory( functionContributions );

			vectorFunctionFactory.cosineDistance( "?1<=>?2" );
			vectorFunctionFactory.euclideanDistance( "?1<->?2" );
			vectorFunctionFactory.l1Distance( "l1_distance(?1,?2)" );
			vectorFunctionFactory.hammingDistance( "?1<~>?2" );
			vectorFunctionFactory.jaccardDistance( "?1<%>?2" );

			vectorFunctionFactory.innerProduct( "(?1<#>?2)*-1" );
			vectorFunctionFactory.negativeInnerProduct( "?1<#>?2" );

			final TypeConfiguration typeConfiguration = functionContributions.getTypeConfiguration();
			functionContributions.getFunctionRegistry()
					.register( "vector_dims", new PGVectorDimsFunction( typeConfiguration ) );
			functionContributions.getFunctionRegistry()
					.register( "vector_norm", new PGVectorNormFunction( typeConfiguration ) );

			functionContributions.getFunctionRegistry().namedDescriptorBuilder( "binary_quantize" )
					.setArgumentsValidator( StandardArgumentsValidators.composite(
							StandardArgumentsValidators.exactly( 1 ),
							VectorArgumentValidator.INSTANCE
					) )
					.setArgumentTypeResolver( VectorArgumentTypeResolver.INSTANCE )
					.setReturnTypeResolver( StandardFunctionReturnTypeResolvers.invariant(
							typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.VECTOR_BINARY )
					) )
					.register();
			functionContributions.getFunctionRegistry().namedDescriptorBuilder( "subvector" )
					.setArgumentsValidator( StandardArgumentsValidators.composite(
							StandardArgumentsValidators.exactly( 3 ),
							VectorArgumentValidator.INSTANCE
					) )
					.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.byArgument(
							VectorArgumentTypeResolver.INSTANCE,
							StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, INTEGER ),
							StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, INTEGER )
					) )
					.setReturnTypeResolver( StandardFunctionReturnTypeResolvers.useArgType( 1 ) )
					.register();
			functionContributions.getFunctionRegistry().registerAlternateKey( "l2_norm", "vector_norm" );
			functionContributions.getFunctionRegistry().namedDescriptorBuilder( "l2_normalize" )
					.setArgumentsValidator( VectorArgumentValidator.INSTANCE )
					.setArgumentTypeResolver( VectorArgumentTypeResolver.INSTANCE )
					.setReturnTypeResolver( StandardFunctionReturnTypeResolvers.useArgType( 1 ) )
					.register();
		}
	}

	@Override
	public int ordinal() {
		return 200;
	}
}
