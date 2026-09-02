/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import org.hibernate.SPI;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;
import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// Subclassable descriptor for the `regexp_like` predicate.
@SPI({ USE, IMPLEMENT })
public abstract class AbstractRegexpLikeFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	@SPI(IMPLEMENT)
	public AbstractRegexpLikeFunction(TypeConfiguration typeConfiguration) {
		super(
				"regexp_like",
				new ArgumentTypesValidator( StandardArgumentsValidators.between( 2, 3 ), STRING, STRING, STRING ),
				StandardFunctionReturnTypeResolvers.invariant( typeConfiguration.getBasicTypeRegistry().resolve(
						StandardBasicTypes.BOOLEAN ) ),
				StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, STRING, STRING, STRING )
		);
	}

	@Override
	public String getSignature(String name) {
		return "(STRING string, STRING pattern[, STRING flags])";
	}

	@Override
	public boolean isPredicate() {
		return true;
	}
}
