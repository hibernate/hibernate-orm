/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.function;

import java.util.List;

import org.hibernate.Incubating;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionArgumentTypeResolver;
import org.hibernate.query.sqm.produce.function.SetReturningFunctionTypeResolver;
import org.hibernate.query.sqm.tree.SqmTypedNode;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @since 7.0
 */
@Incubating
public abstract class AbstractSqmSelfRenderingSetReturningFunctionDescriptor
		extends AbstractSqmSetReturningFunctionDescriptor implements SetReturningFunctionRenderer {

	public AbstractSqmSelfRenderingSetReturningFunctionDescriptor(
			String name,
			@Nullable ArgumentsValidator argumentsValidator,
			SetReturningFunctionTypeResolver typeResolver,
			@Nullable FunctionArgumentTypeResolver argumentTypeResolver) {
		super( name, argumentsValidator, typeResolver, argumentTypeResolver );
	}

	@Override
	protected <T> SelfRenderingSqmSetReturningFunction<T> generateSqmSetReturningFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			QueryEngine queryEngine) {
		return new SelfRenderingSqmSetReturningFunction<>(
				this,
				this,
				arguments,
				getArgumentsValidator(),
				getSetReturningTypeResolver(),
				queryEngine.getCriteriaBuilder(),
				getName()
		);
	}
}
