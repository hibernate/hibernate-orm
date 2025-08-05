/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MariaDBDialect;

public class MariaDBFunctionContributor implements FunctionContributor {
	@Override
	public void contributeFunctions(FunctionContributions functionContributions) {
		final Dialect dialect = functionContributions.getDialect();
		if ( dialect instanceof MariaDBDialect && dialect.getVersion().isSameOrAfter( 11, 7 ) ) {
			final VectorFunctionFactory vectorFunctionFactory = new VectorFunctionFactory( functionContributions );

			vectorFunctionFactory.cosineDistance( "vec_distance_cosine(?1,?2)" );
			vectorFunctionFactory.euclideanDistance( "vec_distance_euclidean(?1,?2)" );
		}
	}

	@Override
	public int ordinal() {
		return 200;
	}
}
