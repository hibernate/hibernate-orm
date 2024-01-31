/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.array;

import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.ANY;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.INTEGER;

/**
 * Encapsulates the validator, return type and argument type resolvers for the array_remove functions.
 * Subclasses only have to implement the rendering.
 */
public abstract class AbstractArrayTrimFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public AbstractArrayTrimFunction() {
		super(
				"array_trim",
				StandardArgumentsValidators.composite(
						new ArgumentTypesValidator( null, ANY, INTEGER ),
						ArrayArgumentValidator.DEFAULT_INSTANCE
				),
				ArrayViaArgumentReturnTypeResolver.DEFAULT_INSTANCE,
				StandardFunctionArgumentTypeResolvers.composite(
						StandardFunctionArgumentTypeResolvers.invariant( ANY, INTEGER ),
						StandardFunctionArgumentTypeResolvers.IMPLIED_RESULT_TYPE
				)
		);
	}
}
