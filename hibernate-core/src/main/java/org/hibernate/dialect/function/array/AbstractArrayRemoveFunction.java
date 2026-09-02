/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import org.hibernate.SPI;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// Base descriptor for `array_remove`, providing validation and type resolution
/// while subclasses implement database-specific rendering.
@SPI({ USE, IMPLEMENT })
public abstract class AbstractArrayRemoveFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	@SPI(IMPLEMENT)
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
