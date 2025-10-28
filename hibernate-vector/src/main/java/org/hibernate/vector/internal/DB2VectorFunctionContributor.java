/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.type.BasicType;
import org.hibernate.type.spi.TypeConfiguration;


public class DB2VectorFunctionContributor implements FunctionContributor {

	@Override
	public void contributeFunctions(FunctionContributions functionContributions) {
		final Dialect dialect = functionContributions.getDialect();
		if ( dialect instanceof DB2Dialect db2Dialect && db2Dialect.getDB2Version().isSameOrAfter( 12, 1, 2 ) ) {
			final VectorFunctionFactory vectorFunctionFactory = new VectorFunctionFactory( functionContributions );

			vectorFunctionFactory.cosineDistance( "vector_distance(?1,?2,COSINE)" );
			vectorFunctionFactory.euclideanDistance( "vector_distance(?1,?2,EUCLIDEAN)" );
			vectorFunctionFactory.euclideanSquaredDistance( "vector_distance(?1,?2,EUCLIDEAN_SQUARED)" );
			vectorFunctionFactory.l1Distance( "vector_distance(?1,?2,MANHATTAN)" );
			vectorFunctionFactory.hammingDistance( "vector_distance(?1,?2,HAMMING)" );

			vectorFunctionFactory.innerProduct( "vector_distance(?1,?2,DOT)*-1" );
			vectorFunctionFactory.negativeInnerProduct( "vector_distance(?1,?2,DOT)" );

			final TypeConfiguration typeConfiguration = functionContributions.getTypeConfiguration();
			final BasicType<Integer> integerType = typeConfiguration.getBasicTypeForJavaType( Integer.class );
			final BasicType<Double> doubleType = typeConfiguration.getBasicTypeForJavaType( Double.class );
			vectorFunctionFactory.registerNamedVectorFunction("vector_dimension_count", integerType, 1 );
			functionContributions.getFunctionRegistry().registerAlternateKey( "vector_dims", "vector_dimension_count" );
			vectorFunctionFactory.registerPatternVectorFunction( "vector_norm", "vector_norm(?1,EUCLIDEAN)", doubleType, 1 );
		}
	}

	@Override
	public int ordinal() {
		return 200;
	}
}
