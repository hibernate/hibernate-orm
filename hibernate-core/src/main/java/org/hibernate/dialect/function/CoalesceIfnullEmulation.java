/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.query.AllowableFunctionReturnType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.AbstractSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

/**
 * @author Gavin King
 */
public class CoalesceIfnullEmulation
		extends AbstractSqmFunctionDescriptor {

	public CoalesceIfnullEmulation() {
		super(
				"ifnull",
				StandardArgumentsValidators.exactly( 2 )
		);
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		return queryEngine.getSqmFunctionRegistry().findFunctionDescriptor( "coalesce" )
				.generateSqmExpression(
						arguments,
						impliedResultType,
						queryEngine,
						typeConfiguration
				);
	}
}
