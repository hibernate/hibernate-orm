/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.query.ReturnableType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.AbstractSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctions;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Oracle 8i had no {@code coalesce()} function,
 * so we emulate it using chained {@code nvl()}s.
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public class NvlCoalesceEmulation
		extends AbstractSqmFunctionDescriptor {

	public NvlCoalesceEmulation() {
		super(
				StandardFunctions.COALESCE,
				StandardArgumentsValidators.min( 2 ),
				StandardFunctionReturnTypeResolvers.useFirstNonNull(),
				StandardFunctionArgumentTypeResolvers.IMPLIED_RESULT_TYPE
		);
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {

		SqmFunctionDescriptor nvl =
				queryEngine.getSqmFunctionRegistry()
						.namedDescriptorBuilder("nvl")
						.setExactArgumentCount(2)
						.descriptor();

		int pos = arguments.size();
		SqmExpression<?> result = (SqmExpression<?>) arguments.get( --pos );
		ReturnableType<?> type =
				(ReturnableType<?>) result.getNodeType();

		while (pos>0) {
			SqmExpression<?> next = (SqmExpression<?>) arguments.get( --pos );
			result = nvl.generateSqmExpression(
					asList( next, result ),
					type,
					queryEngine,
					typeConfiguration
			);
		}

		//noinspection unchecked
		return (SelfRenderingSqmFunction<T>) result;
	}

}
