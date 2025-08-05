/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Enumerates common vector function template definitions.
 * Centralized for easier use from dialects.
 */
public class VectorFunctionFactory {

	private final SqmFunctionRegistry functionRegistry;
	private final TypeConfiguration typeConfiguration;
	private final BasicType<Double> doubleType;
	private final BasicType<Integer> integerType;

	public VectorFunctionFactory(FunctionContributions functionContributions) {
		this.functionRegistry = functionContributions.getFunctionRegistry();
		this.typeConfiguration = functionContributions.getTypeConfiguration();
		final BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
		this.doubleType = basicTypeRegistry.resolve( StandardBasicTypes.DOUBLE );
		this.integerType = basicTypeRegistry.resolve( StandardBasicTypes.INTEGER );
	}

	public void cosineDistance(String pattern) {
		registerVectorDistanceFunction( "cosine_distance", pattern );
	}

	public void euclideanDistance(String pattern) {
		registerVectorDistanceFunction( "euclidean_distance", pattern );
		functionRegistry.registerAlternateKey( "l2_distance", "euclidean_distance" );
	}

	public void l1Distance(String pattern) {
		registerVectorDistanceFunction( "l1_distance", pattern );
		functionRegistry.registerAlternateKey( "taxicab_distance", "l1_distance" );
	}

	public void innerProduct(String pattern) {
		registerVectorDistanceFunction( "inner_product", pattern );
	}

	public void negativeInnerProduct(String pattern) {
		registerVectorDistanceFunction( "negative_inner_product", pattern );
	}

	public void hammingDistance(String pattern) {
		registerVectorDistanceFunction( "hamming_distance", pattern );
	}

	public void jaccardDistance(String pattern) {
		registerVectorDistanceFunction( "jaccard_distance", pattern );
	}

	public void vectorDimensions() {
		registerNamedVectorFunction( "vector_dims", integerType, 1 );
	}

	public void vectorNorm() {
		registerNamedVectorFunction( "vector_norm", doubleType, 1 );
	}

	public void registerVectorDistanceFunction(String functionName, String pattern) {
		functionRegistry.patternDescriptorBuilder( functionName, pattern )
				.setArgumentsValidator( StandardArgumentsValidators.composite(
						StandardArgumentsValidators.exactly( 2 ),
						VectorArgumentValidator.DISTANCE_INSTANCE
				) )
				.setArgumentTypeResolver( VectorArgumentTypeResolver.DISTANCE_INSTANCE )
				.setReturnTypeResolver( StandardFunctionReturnTypeResolvers.invariant( doubleType ) )
				.register();
	}

	public void registerNamedVectorFunction(String functionName, BasicType<?> returnType, int argumentCount) {
		functionRegistry.namedDescriptorBuilder( functionName )
				.setArgumentsValidator( StandardArgumentsValidators.composite(
						StandardArgumentsValidators.exactly( argumentCount ),
						VectorArgumentValidator.INSTANCE
				) )
				.setArgumentTypeResolver( VectorArgumentTypeResolver.INSTANCE )
				.setReturnTypeResolver( StandardFunctionReturnTypeResolvers.invariant( returnType ) )
				.register();
	}

}
