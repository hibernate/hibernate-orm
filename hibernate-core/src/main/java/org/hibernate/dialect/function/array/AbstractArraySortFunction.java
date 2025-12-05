/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.ANY;

/**
 * Encapsulates the validator, return type and argument type resolvers for the array_sort functions.
 * Subclasses only have to implement the rendering.
 */
public abstract class AbstractArraySortFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public AbstractArraySortFunction() {
		super(
				"array_sort",
				StandardArgumentsValidators.composite(
						StandardArgumentsValidators.between( 1, 3 ),
						ArrayArgumentValidator.DEFAULT_INSTANCE
				),
				ArrayViaArgumentReturnTypeResolver.DEFAULT_INSTANCE,
				StandardFunctionArgumentTypeResolvers.composite(
						StandardFunctionArgumentTypeResolvers.invariant( ANY ),
						StandardFunctionArgumentTypeResolvers.IMPLIED_RESULT_TYPE,
						StandardFunctionArgumentTypeResolvers.IMPLIED_RESULT_TYPE
				)
		);
	}

}
