/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.OracleDialect;

public class OracleVectorFunctionContributor implements FunctionContributor {

	@Override
	public void contributeFunctions(FunctionContributions functionContributions) {
		final Dialect dialect = functionContributions.getDialect();
		if ( dialect instanceof OracleDialect && dialect.getVersion().isSameOrAfter( 23, 4 ) ) {
			final VectorFunctionFactory vectorFunctionFactory = new VectorFunctionFactory( functionContributions );

			vectorFunctionFactory.cosineDistance( "vector_distance(?1,?2,COSINE)" );
			vectorFunctionFactory.euclideanDistance( "vector_distance(?1,?2,EUCLIDEAN)" );
			vectorFunctionFactory.l1Distance( "vector_distance(?1,?2,MANHATTAN)" );
			vectorFunctionFactory.hammingDistance( "vector_distance(?1,?2,HAMMING)" );

			vectorFunctionFactory.innerProduct( "vector_distance(?1,?2,DOT)*-1" );
			vectorFunctionFactory.negativeInnerProduct( "vector_distance(?1,?2,DOT)" );

			vectorFunctionFactory.vectorDimensions();
			vectorFunctionFactory.vectorNorm();
		}
	}

	@Override
	public int ordinal() {
		return 200;
	}
}
