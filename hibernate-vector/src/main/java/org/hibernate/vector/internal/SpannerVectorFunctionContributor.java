/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

public class SpannerVectorFunctionContributor implements FunctionContributor {

	@Override
	public void contributeFunctions(FunctionContributions functionContributions) {
		final Dialect dialect = functionContributions.getDialect();

		if ( dialect instanceof SpannerDialect && !( dialect instanceof SpannerPostgreSQLDialect ) ) {
			VectorFunctionFactory factory = new VectorFunctionFactory(functionContributions);

			factory.cosineDistance("COSINE_DISTANCE(?1, ?2)");
			factory.euclideanDistance("EUCLIDEAN_DISTANCE(?1, ?2)");
			factory.euclideanSquaredDistance("POW(EUCLIDEAN_DISTANCE(?1, ?2), 2)");
			factory.innerProduct("DOT_PRODUCT(?1, ?2)");
			factory.negativeInnerProduct("(DOT_PRODUCT(?1, ?2) * -1)");

			final TypeConfiguration typeConfiguration = functionContributions.getTypeConfiguration();
			functionContributions.getFunctionRegistry().register(
					"approx_cosine_distance",
					new SpannerGoogleSqlApproxDistanceFunction( "approx_cosine_distance", "APPROX_COSINE_DISTANCE", typeConfiguration )
			);
			functionContributions.getFunctionRegistry().register(
					"approx_euclidean_distance",
					new SpannerGoogleSqlApproxDistanceFunction( "approx_euclidean_distance", "APPROX_EUCLIDEAN_DISTANCE", typeConfiguration )
			);
			functionContributions.getFunctionRegistry().register(
					"approx_dot_product",
					new SpannerGoogleSqlApproxDistanceFunction( "approx_dot_product", "APPROX_DOT_PRODUCT", typeConfiguration )
			);

			BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
			BasicType<Integer> integerType = basicTypeRegistry.resolve(StandardBasicTypes.INTEGER);
			BasicType<Double> doubleType = basicTypeRegistry.resolve(StandardBasicTypes.DOUBLE);

			factory.registerPatternVectorFunction("vector_dims", "ARRAY_LENGTH(?1)", integerType, 1);
			factory.registerPatternVectorFunction("vector_norm", "SQRT(DOT_PRODUCT(?1, ?1))", doubleType, 1);
			functionContributions.getFunctionRegistry().registerAlternateKey( "l2_norm", "vector_norm" );
		}
	}

	@Override
	public int ordinal() {
		return 200;
	}
}
