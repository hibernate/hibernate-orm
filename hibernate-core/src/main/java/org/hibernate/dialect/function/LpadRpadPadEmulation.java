/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.TrimSpec;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.AbstractSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctions;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmTrimSpecification;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.INTEGER;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.TRIM_SPEC;

/**
 * HQL function inspired by the {@linkplain TrimFunction ANSI SQL trim function},
 * with a funny syntax involving a {@link TrimSpec}. Emulated using {@code rpad()}
 * and {@code lpad()} or by equivalent emulations of those functions.
 * <p>
 * For example, {@code pad(text with 5 leading ' ')}.
 *
 * @author Gavin King
 */
public class LpadRpadPadEmulation
		extends AbstractSqmFunctionDescriptor {

	public LpadRpadPadEmulation(TypeConfiguration typeConfiguration) {
		super(
				StandardFunctions.PAD,
				new ArgumentTypesValidator(
						StandardArgumentsValidators.between( 3, 4 ),
						STRING, INTEGER, TRIM_SPEC, STRING
				),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				),
				StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, STRING, INTEGER, TRIM_SPEC, STRING )
		);
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		SqmTrimSpecification padSpec = (SqmTrimSpecification) arguments.get(2);
		String padName = padSpec.getSpecification() == TrimSpec.LEADING ? StandardFunctions.LPAD : StandardFunctions.RPAD;
		return queryEngine.getSqmFunctionRegistry().findFunctionDescriptor( padName )
				.generateSqmExpression(
						arguments.size() > 3
						? asList(
								arguments.get(0),
								arguments.get(1),
								arguments.get(3)
						)
						: asList(
								arguments.get(0),
								arguments.get(1)
						),
						impliedResultType,
						queryEngine,
						typeConfiguration
				);
	}

	@Override
	public String getArgumentListSignature() {
		return "(STRING string with INTEGER length {leading|trailing}[ STRING character])";
	}
}
