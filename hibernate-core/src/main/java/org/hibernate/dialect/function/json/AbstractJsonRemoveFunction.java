/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.hibernate.SPI;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// Subclassable descriptor for the standard `json_remove` function.
@SPI({ USE, IMPLEMENT })
public abstract class AbstractJsonRemoveFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	@SPI(IMPLEMENT)
	public AbstractJsonRemoveFunction(TypeConfiguration typeConfiguration) {
		super(
				"json_remove",
				FunctionKind.NORMAL,
				new ArgumentTypesValidator(
						StandardArgumentsValidators.exactly( 2 ),
						FunctionParameterType.IMPLICIT_JSON,
						FunctionParameterType.STRING
				),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( String.class, SqlTypes.JSON )
				),
				null
		);
	}

}
