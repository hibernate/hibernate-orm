/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.type.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

public class SQLServerVectorFunctionContributor implements FunctionContributor {

	@Override
	public void contributeFunctions(FunctionContributions functionContributions) {
		final Dialect dialect = functionContributions.getDialect();
		if ( dialect instanceof SQLServerDialect && dialect.getVersion().isSameOrAfter( 17 ) ) {
			final VectorFunctionFactory vectorFunctionFactory = new VectorFunctionFactory( functionContributions );

			vectorFunctionFactory.cosineDistance( "vector_distance('cosine',?1,?2)" );
			vectorFunctionFactory.euclideanDistance( "vector_distance('euclidean',?1,?2)" );
			vectorFunctionFactory.euclideanSquaredDistance( "square(vector_distance('euclidean',?1,?2))" );

			vectorFunctionFactory.innerProduct( "vector_distance('dot',?1,?2)*-1" );
			vectorFunctionFactory.negativeInnerProduct( "vector_distance('dot',?1,?2)" );

			final TypeConfiguration typeConfiguration = functionContributions.getTypeConfiguration();
			final BasicType<Integer> integerType = typeConfiguration.getBasicTypeForJavaType( Integer.class );
			final BasicType<Double> doubleType = typeConfiguration.getBasicTypeForJavaType( Double.class );

			vectorFunctionFactory.registerPatternVectorFunction( "vector_dims", "vectorproperty(?1,'Dimensions')", integerType, 1 );
			vectorFunctionFactory.registerPatternVectorFunction( "vector_norm", "vector_norm(?1,'norm2')", doubleType, 1 );
			functionContributions.getFunctionRegistry().registerAlternateKey( "l2_norm", "vector_norm" );
			functionContributions.getFunctionRegistry().patternDescriptorBuilder( "l2_normalize", "vector_normalize(?1,'norm2')" )
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
