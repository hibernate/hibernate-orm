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
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

public class PGVectorFunctionContributor implements FunctionContributor {

	@Override
	public void contributeFunctions(FunctionContributions functionContributions) {
		final Dialect dialect = functionContributions.getDialect();
		if (dialect instanceof PostgreSQLDialect || dialect instanceof CockroachDialect) {
			final SqmFunctionRegistry functionRegistry = functionContributions.getFunctionRegistry();
			final TypeConfiguration typeConfiguration = functionContributions.getTypeConfiguration();
			final BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
			final BasicType<Double> doubleType = basicTypeRegistry.resolve(StandardBasicTypes.DOUBLE);
			final BasicType<Integer> integerType = basicTypeRegistry.resolve(StandardBasicTypes.INTEGER);

			registerVectorDistanceFunction(functionRegistry, "cosine_distance", "?1<=>?2", doubleType);
			registerVectorDistanceFunction(functionRegistry, "euclidean_distance", "?1<->?2", doubleType);
			functionRegistry.registerAlternateKey("l2_distance", "euclidean_distance");

			registerNamedVectorFunction(functionRegistry, "l1_distance", doubleType, 2);
			functionRegistry.registerAlternateKey("taxicab_distance", "l1_distance");

			registerVectorDistanceFunction(functionRegistry, "negative_inner_product", "?1<#>?2", doubleType);
			registerVectorDistanceFunction(functionRegistry, "inner_product", "(?1<#>?2)*-1", doubleType);

			registerNamedVectorFunction(functionRegistry, "vector_dims", integerType, 1);
			registerNamedVectorFunction(functionRegistry, "vector_norm", doubleType, 1);
		}
	}

	private void registerVectorDistanceFunction(
			SqmFunctionRegistry functionRegistry,
			String functionName,
			String pattern,
			BasicType<?> returnType) {

		functionRegistry.patternDescriptorBuilder(functionName, pattern)
				.setArgumentsValidator(StandardArgumentsValidators.composite(
						StandardArgumentsValidators.exactly(2),
						VectorArgumentValidator.INSTANCE
				))
				.setArgumentTypeResolver(VectorArgumentTypeResolver.INSTANCE)
				.setReturnTypeResolver(StandardFunctionReturnTypeResolvers.invariant(returnType))
				.register();
	}

	private void registerNamedVectorFunction(
			SqmFunctionRegistry functionRegistry,
			String functionName,
			BasicType<?> returnType,
			int argumentCount) {

		functionRegistry.namedDescriptorBuilder(functionName)
				.setArgumentsValidator(StandardArgumentsValidators.composite(
						StandardArgumentsValidators.exactly(argumentCount),
						VectorArgumentValidator.INSTANCE
				))
				.setArgumentTypeResolver(VectorArgumentTypeResolver.INSTANCE)
				.setReturnTypeResolver(StandardFunctionReturnTypeResolvers.invariant(returnType))
				.register();
	}

	@Override
	public int ordinal() {
		return 200;
	}
}
