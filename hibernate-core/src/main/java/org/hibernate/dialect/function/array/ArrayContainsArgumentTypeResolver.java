/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.query.sqm.produce.function.FunctionArgumentTypeResolver;
import org.hibernate.query.sqm.produce.function.internal.AbstractFunctionArgumentTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.type.BasicPluralType;

import java.util.List;

/**
 * A {@link FunctionArgumentTypeResolver} that resolves the argument types for the {@code array_contains} function.
 */
public class ArrayContainsArgumentTypeResolver extends AbstractFunctionArgumentTypeResolver {

	public static final FunctionArgumentTypeResolver INSTANCE = new ArrayContainsArgumentTypeResolver();

	@Override
	public @Nullable MappingModelExpressible<?> resolveFunctionArgumentType(List<? extends SqmTypedNode<?>> arguments, int argumentIndex, SqmToSqlAstConverter converter) {
		if ( argumentIndex == 0 ) {
			final SqmTypedNode<?> node = arguments.get( 1 );
			if ( node instanceof SqmExpression<?> sqmExpression ) {
				final MappingModelExpressible<?> expressible = converter.determineValueMapping( sqmExpression );
				if ( expressible != null ) {
					if ( expressible.getSingleJdbcMapping() instanceof BasicPluralType<?, ?> ) {
						return expressible;
					}
					else {
						return DdlTypeHelper.resolveArrayType(
								(DomainType<?>) expressible.getSingleJdbcMapping(),
								converter.getCreationContext().getTypeConfiguration()
						);
					}
				}
			}
		}
		else if ( argumentIndex == 1 ) {
			final SqmTypedNode<?> nodeToResolve = arguments.get( 1 );
			if ( nodeToResolve.getExpressible() instanceof MappingModelExpressible<?> ) {
				// If the node already has suitable type, don't infer it to be treated as an array
				return null;
			}
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
