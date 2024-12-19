/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

public class MariaDBFunctionContributor implements FunctionContributor {

	@Override
	public void contributeFunctions(FunctionContributions functionContributions) {
		final SqmFunctionRegistry functionRegistry = functionContributions.getFunctionRegistry();
		final TypeConfiguration typeConfiguration = functionContributions.getTypeConfiguration();
		final BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
		final Dialect dialect = functionContributions.getDialect();
		if ( dialect instanceof MariaDBDialect ) {
			final BasicType<Double> doubleType = basicTypeRegistry.resolve( StandardBasicTypes.DOUBLE );

			functionRegistry.patternDescriptorBuilder( "cosine_distance", "vec_distance_cosine(?1,?2)" )
					.setArgumentsValidator( StandardArgumentsValidators.composite(
							StandardArgumentsValidators.exactly( 2 ),
							VectorArgumentValidator.INSTANCE
					) )
					.setArgumentTypeResolver( VectorArgumentTypeResolver.INSTANCE )
					.setReturnTypeResolver( StandardFunctionReturnTypeResolvers.invariant( doubleType ) )
					.register();
			functionRegistry.patternDescriptorBuilder( "euclidean_distance", "vec_distance_euclidean(?1,?2)" )
					.setArgumentsValidator( StandardArgumentsValidators.composite(
							StandardArgumentsValidators.exactly( 2 ),
							VectorArgumentValidator.INSTANCE
					) )
					.setArgumentTypeResolver( VectorArgumentTypeResolver.INSTANCE )
					.setReturnTypeResolver( StandardFunctionReturnTypeResolvers.invariant( doubleType ) )
					.register();
			functionRegistry.registerAlternateKey( "l2_distance", "euclidean_distance" );

		}
	}

	@Override
	public int ordinal() {
		return 200;
	}
}
