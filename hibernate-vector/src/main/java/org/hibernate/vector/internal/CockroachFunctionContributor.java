/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.Dialect;

public class CockroachFunctionContributor implements FunctionContributor {

	@Override
	public void contributeFunctions(FunctionContributions functionContributions) {
		final Dialect dialect = functionContributions.getDialect();
		if ( dialect instanceof CockroachDialect && dialect.getVersion().isSameOrAfter( 24, 2 ) ) {
			final VectorFunctionFactory vectorFunctionFactory = new VectorFunctionFactory( functionContributions );

			vectorFunctionFactory.cosineDistance( "?1<=>?2" );
			vectorFunctionFactory.euclideanDistance( "?1<->?2" );
			vectorFunctionFactory.euclideanSquaredDistance( "(?1<->?2)^2" );
			vectorFunctionFactory.l1Distance( "l1_distance(?1,?2)" );

			vectorFunctionFactory.innerProduct( "(?1<#>?2)*-1" );
			vectorFunctionFactory.negativeInnerProduct( "?1<#>?2" );

			vectorFunctionFactory.vectorDimensions();
			vectorFunctionFactory.vectorNorm();

			functionContributions.getFunctionRegistry().registerAlternateKey( "l2_norm", "vector_norm" );
		}
	}

	@Override
	public int ordinal() {
		return 200;
	}
}
