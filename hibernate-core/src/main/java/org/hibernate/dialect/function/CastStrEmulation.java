/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.internal.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.spi.AbstractSqmFunctionTemplate;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.function.SqmCastTarget;
import org.hibernate.type.spi.StandardSpiBasicTypes;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * @author Gavin King
 */
public class CastStrEmulation
		extends AbstractSqmFunctionTemplate {

	public CastStrEmulation() {
		super(
				StandardArgumentsValidators.exactly( 1 ),
				StandardFunctionReturnTypeResolvers.invariant( StandardSpiBasicTypes.STRING )
		);
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine) {
		SqmTypedNode<?> argument = arguments.get(0);
		return queryEngine.getSqmFunctionRegistry().findFunctionTemplate( "cast" )
				.makeSqmFunctionExpression(
						asList(
								argument,
								new SqmCastTarget<>(
										impliedResultType,
										argument.nodeBuilder()
								)
						),
						impliedResultType,
						queryEngine
				);
	}
}
