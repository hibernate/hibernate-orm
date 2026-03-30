/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.ANY;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.INTEGER;

/**
 * Basic array get function configuration.
 */
public abstract class AbstractArrayGetFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public AbstractArrayGetFunction() {
		super(
				"array_get",
				StandardArgumentsValidators.composite(
						ArrayArgumentValidator.DEFAULT_INSTANCE,
						new ArgumentTypesValidator( null, ANY, INTEGER )
				),
				ElementViaArrayArgumentReturnTypeResolver.DEFAULT_INSTANCE,
				StandardFunctionArgumentTypeResolvers.invariant( ANY, INTEGER )
		);
	}
}
