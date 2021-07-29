/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.metamodel.model.domain.AllowableFunctionReturnType;
import org.hibernate.query.TrimSpec;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.AbstractSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmTrimSpecification;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * @author Gavin King
 */
public class LpadRpadPadEmulation
		extends AbstractSqmFunctionDescriptor {

	public LpadRpadPadEmulation() {
		super(
				"pad",
				StandardArgumentsValidators.between( 3, 4 ),
				StandardFunctionReturnTypeResolvers.invariant( StandardBasicTypes.STRING )
		);
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		SqmTrimSpecification padSpec = (SqmTrimSpecification) arguments.get(2);
		String padName = padSpec.getSpecification() == TrimSpec.LEADING ? "lpad" : "rpad";
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
		return "(string with length {leading|trailing}[ character])";
	}
}
