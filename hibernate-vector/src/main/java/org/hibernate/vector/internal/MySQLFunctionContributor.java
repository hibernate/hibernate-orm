/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;

public class MySQLFunctionContributor implements FunctionContributor {
	@Override
	public void contributeFunctions(FunctionContributions functionContributions) {
		final Dialect dialect = functionContributions.getDialect();
		if ( dialect instanceof MySQLDialect mySQLDialect && mySQLDialect.getMySQLVersion().isSameOrAfter( 9, 0 ) ) {
			final VectorFunctionFactory vectorFunctionFactory = new VectorFunctionFactory( functionContributions );

			vectorFunctionFactory.cosineDistance( "distance(?1,?2,'cosine')" );
			vectorFunctionFactory.euclideanDistance( "distance(?1,?2,'euclidean')" );
			vectorFunctionFactory.innerProduct( "distance(?1,?2,'dot')*-1" );
			vectorFunctionFactory.negativeInnerProduct( "distance(?1,?2,'dot')" );

			vectorFunctionFactory.registerNamedVectorFunction(
					"vector_dim",
					functionContributions.getTypeConfiguration().getBasicTypeForJavaType( Integer.class ),
					1
			);
			functionContributions.getFunctionRegistry().registerAlternateKey( "vector_dims", "vector_dim" );
		}
	}

	@Override
	public int ordinal() {
		return 200;
	}
}
