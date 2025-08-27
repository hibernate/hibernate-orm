/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.sqm.produce.function.FunctionArgumentTypeResolver;
import org.hibernate.query.sqm.produce.function.internal.AbstractFunctionArgumentTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

import java.util.List;

/**
 * A {@link FunctionArgumentTypeResolver} that resolves the argument types for the {@code array_includes} function.
 */
public class ArrayIncludesArgumentTypeResolver implements AbstractFunctionArgumentTypeResolver {

	public static final FunctionArgumentTypeResolver INSTANCE = new ArrayIncludesArgumentTypeResolver();

	@Override
	public @Nullable MappingModelExpressible<?> resolveFunctionArgumentType(List<? extends SqmTypedNode<?>> arguments, int argumentIndex, SqmToSqlAstConverter converter) {
		if ( argumentIndex == 0 ) {
			final SqmTypedNode<?> node = arguments.get( 1 );
			if ( node instanceof SqmExpression<?> sqmExpression ) {
				return converter.determineValueMapping( sqmExpression );
			}
		}
		else if ( argumentIndex == 1 ) {
			final SqmTypedNode<?> node = arguments.get( 0 );
			if ( node instanceof SqmExpression<?> sqmExpression ) {
				return converter.determineValueMapping( sqmExpression );
			}
		}
		return null;
	}
}
