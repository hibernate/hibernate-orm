/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.query.sqm.function.NamedSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.type.BasicTypeRegistry;

public class BaseSqmFunctionDescriptors implements KeyedSqmFunctionDescriptors {
	protected final Map<FunctionKey, SqmFunctionDescriptor> map = new HashMap<>();

	//TODO -- refactor so that the function registration is done a separate function so that it can be
	//overwritten by subclasses
	public  BaseSqmFunctionDescriptors(FunctionContributions functionContributions) {
		final BasicTypeRegistry basicTypeRegistry = functionContributions.getTypeConfiguration().getBasicTypeRegistry();
		for ( CommonSpatialFunction func : filter( CommonSpatialFunction.values() ) ) {
			final FunctionReturnTypeResolver returnTypeResolver;
			if ( func.getReturnType() == null ) {
				returnTypeResolver = StandardFunctionReturnTypeResolvers.useFirstNonNull();
			}
			else {
				returnTypeResolver = StandardFunctionReturnTypeResolvers.invariant(
						basicTypeRegistry.resolve( func.getReturnType() )
				);
			}
			map.put(
					func.getKey(),
					new NamedSqmFunctionDescriptor(
							func.getKey().getName(),
							true,
							StandardArgumentsValidators.exactly( func.getNumArgs() ),
							returnTypeResolver
					)
			);
		}
	}

	public CommonSpatialFunction[] filter(CommonSpatialFunction[] functions) {
		return functions;
	}

	public Map<FunctionKey, SqmFunctionDescriptor> asMap() {
		return Collections.unmodifiableMap( map );
	}
}
