/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.SqmFunctionTemplate;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.internal.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.spi.AbstractSqmFunctionTemplate;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.type.spi.TypeConfiguration;

import static java.util.Arrays.asList;

/**
 * Emulation `coalesce` using `nvl` on versions Oracle not supporting `coalesce`
 *
 * todo (6.0) : what was the first version of Oracle to support `coalesce`?
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public class NvlCoalesceEmulation
		extends AbstractSqmFunctionTemplate {

	public NvlCoalesceEmulation() {
		super(
				StandardArgumentsValidators.min( 2 ),
				StandardFunctionReturnTypeResolvers.useFirstNonNull()
		);
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {

		SqmFunctionTemplate nvl = queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("nvl").setExactArgumentCount(2).template();

		int pos = arguments.size();
		SqmExpression<?> result = (SqmExpression<?>) arguments.get( --pos );
		AllowableFunctionReturnType<?> type = (AllowableFunctionReturnType<?>) result.getExpressableType();

		while (pos>0) {
			SqmExpression<?> next = (SqmExpression<?>) arguments.get( --pos );
			result = nvl.makeSqmFunctionExpression(
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
