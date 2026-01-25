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
import org.hibernate.type.BasicPluralType;

import java.util.List;

/**
 * A {@link FunctionArgumentTypeResolver} that resolves the argument types for the {@code array_intersects} function.
 */
public class ArrayIntersectsArgumentTypeResolver implements AbstractFunctionArgumentTypeResolver {

	public static final FunctionArgumentTypeResolver INSTANCE = new ArrayIntersectsArgumentTypeResolver();

	@Override
	public @Nullable MappingModelExpressible<?> resolveFunctionArgumentType(List<? extends SqmTypedNode<?>> arguments, int argumentIndex, SqmToSqlAstConverter converter) {
		if ( argumentIndex == 0 ) {
			final SqmTypedNode<?> node = arguments.get( 1 );
			if ( node instanceof SqmExpression<?> sqmExpression ) {
				final MappingModelExpressible<?> expressible = converter.determineValueMapping( sqmExpression );
				if ( expressible instanceof BasicPluralType<?, ?> ) {
					return expressible;
				}
			}
		}
		else if ( argumentIndex == 1 ) {
			final SqmTypedNode<?> node = arguments.get( 0 );
			if ( node instanceof SqmExpression<?> sqmExpression ) {
				final MappingModelExpressible<?> expressible = converter.determineValueMapping( sqmExpression );
				if ( expressible instanceof BasicPluralType<?, ?> ) {
					return expressible;
				}
			}
		}
		return null;
	}
}
