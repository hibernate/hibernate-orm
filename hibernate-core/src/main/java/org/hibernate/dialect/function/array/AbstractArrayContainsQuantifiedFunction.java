/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.array;

import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Encapsulates the validator, return type and argument type resolvers for the quantified array_contains functions.
 * Subclasses only have to implement the rendering.
 */
public abstract class AbstractArrayContainsQuantifiedFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public AbstractArrayContainsQuantifiedFunction(String functionName, TypeConfiguration typeConfiguration) {
		super(
				functionName,
				StandardArgumentsValidators.composite(
						StandardArgumentsValidators.exactly( 2 ),
						ArraysOfSameTypeArgumentValidator.INSTANCE
				),
				StandardFunctionReturnTypeResolvers.invariant( typeConfiguration.standardBasicTypeForJavaType( Boolean.class ) ),
				StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE
		);
	}

	@Override
	public String getArgumentListSignature() {
		return "(ARRAY haystackArray, ARRAY needleArray)";
	}
}
