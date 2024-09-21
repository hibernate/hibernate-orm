/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import org.hibernate.query.ReturnableType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.AbstractSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.tree.SqmTypedNode;

import java.util.List;

/**
 * @author Gavin King
 */
public class CoalesceIfnullEmulation
		extends AbstractSqmFunctionDescriptor {

	public CoalesceIfnullEmulation() {
		super(
				"ifnull",
				StandardArgumentsValidators.exactly( 2 ),
				StandardFunctionArgumentTypeResolvers.IMPLIED_RESULT_TYPE
		);
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		return queryEngine.getSqmFunctionRegistry().findFunctionDescriptor( "coalesce" )
				.generateSqmExpression(
						arguments,
						impliedResultType,
						queryEngine
				);
	}
}
