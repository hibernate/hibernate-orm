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
 * Encapsulates the validator, return type and argument type resolvers for the array_reverse functions.
 * Subclasses only have to implement the rendering.
 */
public abstract class AbstractArrayReverseFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public AbstractArrayReverseFunction() {
		super(
				"array_reverse",
				StandardArgumentsValidators.composite(
						StandardArgumentsValidators.exactly( 1 ),
						ArrayArgumentValidator.DEFAULT_INSTANCE
				),
				ArrayViaArgumentReturnTypeResolver.DEFAULT_INSTANCE,
				StandardFunctionArgumentTypeResolvers.composite(
						StandardFunctionArgumentTypeResolvers.invariant( ANY )
				)
		);
	}
}
