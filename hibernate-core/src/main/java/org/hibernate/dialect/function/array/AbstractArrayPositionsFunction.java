/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import java.lang.reflect.Type;
import java.util.List;

import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.type.internal.ParameterizedTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Encapsulates the validator, return type and argument type resolvers for the array_positions functions.
 * Subclasses only have to implement the rendering.
 */
public abstract class AbstractArrayPositionsFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public AbstractArrayPositionsFunction(boolean list, TypeConfiguration typeConfiguration) {
		super(
				"array_positions" + ( list ? "_list" : "" ),
				new ArgumentTypesValidator(
						StandardArgumentsValidators.composite(
								StandardArgumentsValidators.exactly( 2 ),
								ArrayAndElementArgumentValidator.DEFAULT_INSTANCE
						),
						FunctionParameterType.ANY,
						FunctionParameterType.ANY
				),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.standardBasicTypeForJavaType(
								list
										? new ParameterizedTypeImpl( List.class, new Type[]{ Integer.class }, null )
										: int[].class
						)
				),
				ArrayAndElementArgumentTypeResolver.DEFAULT_INSTANCE
		);
	}

	@Override
	public String getArgumentListSignature() {
		return "(ARRAY array, OBJECT element)";
	}
}
