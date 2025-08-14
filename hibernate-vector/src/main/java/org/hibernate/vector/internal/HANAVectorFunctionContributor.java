/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.INTEGER;

public class HANAVectorFunctionContributor implements FunctionContributor {

	@Override
	public void contributeFunctions(FunctionContributions functionContributions) {
		final Dialect dialect = functionContributions.getDialect();
		if ( dialect instanceof HANADialect hanaDialect && hanaDialect.isCloud() ) {
			final VectorFunctionFactory vectorFunctionFactory = new VectorFunctionFactory( functionContributions );

			vectorFunctionFactory.registerVectorDistanceFunction( "cosine_similarity", "cosine_similarity(?1,?2)" );
			vectorFunctionFactory.cosineDistance( "(1-cosine_similarity(?1,?2))" );
			vectorFunctionFactory.euclideanDistance( "l2distance(?1,?2)" );
			vectorFunctionFactory.euclideanSquaredDistance( "power(l2distance(?1,?2),2)" );

			final TypeConfiguration typeConfiguration = functionContributions.getTypeConfiguration();
			vectorFunctionFactory.registerPatternVectorFunction(
					"vector_dims",
					"cardinality(?1)",
					typeConfiguration.getBasicTypeForJavaType( Integer.class ),
					1
			);

			if ( hanaDialect.getVersion().isSameOrAfter( 4, 2025_2 ) ) {
				vectorFunctionFactory.registerNamedVectorFunction(
						"l2norm",
						typeConfiguration.getBasicTypeForJavaType( Double.class ),
						1
				);
				functionContributions.getFunctionRegistry().registerAlternateKey( "vector_norm", "l2norm" );
				functionContributions.getFunctionRegistry().registerAlternateKey( "l2_norm", "l2norm" );

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
				functionContributions.getFunctionRegistry().namedDescriptorBuilder( "l2normalize" )
						.setArgumentsValidator( VectorArgumentValidator.INSTANCE )
						.setArgumentTypeResolver( VectorArgumentTypeResolver.INSTANCE )
						.setReturnTypeResolver( StandardFunctionReturnTypeResolvers.useArgType( 1 ) )
						.register();
				functionContributions.getFunctionRegistry().registerAlternateKey( "l2_normalize", "l2normalize" );
			}
		}
	}

	@Override
	public int ordinal() {
		return 200;
	}
}
