/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Contributes Cloud Spanner PostgreSQL vector distance and utility functions to Hibernate ORM.
 */
public class SpannerPostgreSQLVectorFunctionContributor implements FunctionContributor {

	@Override
	public void contributeFunctions(FunctionContributions functionContributions) {
		final Dialect dialect = functionContributions.getDialect();

		if ( dialect instanceof SpannerPostgreSQLDialect ) {
			final VectorFunctionFactory factory = new VectorFunctionFactory( functionContributions );

			// Exact distance functions under spanner.* namespace
			factory.cosineDistance( "spanner.cosine_distance(?1, ?2)" );
			factory.euclideanDistance( "spanner.euclidean_distance(?1, ?2)" );
			factory.euclideanSquaredDistance( "power(spanner.euclidean_distance(?1, ?2), 2)" );
			factory.innerProduct( "spanner.dot_product(?1, ?2)" );
			factory.negativeInnerProduct( "(spanner.dot_product(?1, ?2) * -1)" );

			// Approximate nearest neighbor (ANN) distance functions
			final TypeConfiguration typeConfiguration = functionContributions.getTypeConfiguration();
			functionContributions.getFunctionRegistry().register(
					"approx_cosine_distance",
					new SpannerPostgreSqlApproxDistanceFunction( "approx_cosine_distance", "spanner.approx_cosine_distance", typeConfiguration )
			);
			functionContributions.getFunctionRegistry().register(
					"approx_euclidean_distance",
					new SpannerPostgreSqlApproxDistanceFunction( "approx_euclidean_distance", "spanner.approx_euclidean_distance", typeConfiguration )
			);
			functionContributions.getFunctionRegistry().register(
					"approx_dot_product",
					new SpannerPostgreSqlApproxDistanceFunction( "approx_dot_product", "spanner.approx_dot_product", typeConfiguration )
			);

			// Dimension and Norm functions
			final BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
			final BasicType<Integer> integerType = basicTypeRegistry.resolve( StandardBasicTypes.INTEGER );
			final BasicType<Double> doubleType = basicTypeRegistry.resolve( StandardBasicTypes.DOUBLE );

			factory.registerPatternVectorFunction( "vector_dims", "case when ?1 is null then null else coalesce(array_length(?1, 1), 0) end", integerType, 1 );
			factory.registerPatternVectorFunction( "vector_norm", "sqrt(spanner.dot_product(?1, ?1))", doubleType, 1 );
			functionContributions.getFunctionRegistry().registerAlternateKey( "l2_norm", "vector_norm" );
		}
	}

	@Override
	public int ordinal() {
		return 200;
	}
}
