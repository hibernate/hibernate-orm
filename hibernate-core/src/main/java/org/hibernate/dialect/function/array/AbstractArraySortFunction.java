/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.ANY;

/**
 * Encapsulates the validator, return type and argument type resolvers for the array_sort functions.
 * Subclasses only have to implement the rendering.
 */
public abstract class AbstractArraySortFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public AbstractArraySortFunction(TypeConfiguration typeConfiguration) {
		super(
				"array_sort",
				new ArgumentTypesValidator(
						StandardArgumentsValidators.composite(
								StandardArgumentsValidators.between( 1, 3 ),
								ArrayArgumentValidator.DEFAULT_INSTANCE
						),
						FunctionParameterType.ANY,
						FunctionParameterType.BOOLEAN,
						FunctionParameterType.BOOLEAN
				),
				ArrayViaArgumentReturnTypeResolver.DEFAULT_INSTANCE,
				StandardFunctionArgumentTypeResolvers.composite(
						StandardFunctionArgumentTypeResolvers.invariant( ANY ),
						StandardFunctionArgumentTypeResolvers.invariant(
								typeConfiguration,
								FunctionParameterType.BOOLEAN
						),
						StandardFunctionArgumentTypeResolvers.invariant(
								typeConfiguration,
								FunctionParameterType.BOOLEAN
						)
				)
		);
	}

}
