/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;

/**
 * Encapsulates the validator, return type and argument type resolvers for the array_remove functions.
 * Subclasses only have to implement the rendering.
 */
public abstract class AbstractArrayRemoveFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public AbstractArrayRemoveFunction() {
		super(
				"array_remove",
				StandardArgumentsValidators.composite(
						StandardArgumentsValidators.exactly( 2 ),
						ArrayAndElementArgumentValidator.DEFAULT_INSTANCE
				),
				ArrayViaArgumentReturnTypeResolver.DEFAULT_INSTANCE,
				ArrayAndElementArgumentTypeResolver.DEFAULT_INSTANCE
		);
	}
}
