/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Encapsulates the validator, return type and argument type resolvers for the array_includes function.
 * Subclasses only have to implement the rendering.
 */
public abstract class AbstractArrayIncludesFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	protected final boolean nullable;

	public AbstractArrayIncludesFunction(boolean nullable, TypeConfiguration typeConfiguration) {
		super(
				"array_includes" + ( nullable ? "_nullable" : "" ),
				StandardArgumentsValidators.composite(
						StandardArgumentsValidators.exactly( 2 ),
						ArrayIncludesArgumentValidator.INSTANCE
				),
				StandardFunctionReturnTypeResolvers.invariant( typeConfiguration.standardBasicTypeForJavaType( Boolean.class ) ),
				ArrayIncludesArgumentTypeResolver.INSTANCE
		);
		this.nullable = nullable;
	}

	@Override
	public String getArgumentListSignature() {
		return "(ARRAY haystackArray, OBJECT needleArray)";
	}
}
