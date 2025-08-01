/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQLDialect;

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
			vectorFunctionFactory.registerVectorDistanceFunction( "jaccard_distance", "?1<%>?2" );

			vectorFunctionFactory.innerProduct( "(?1<#>?2)*-1" );
			vectorFunctionFactory.negativeInnerProduct( "?1<#>?2" );

			vectorFunctionFactory.vectorDimensions();
			vectorFunctionFactory.vectorNorm();
		}
	}

	@Override
	public int ordinal() {
		return 200;
	}
}
