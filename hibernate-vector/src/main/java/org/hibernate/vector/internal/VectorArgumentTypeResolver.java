/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.sqm.produce.function.FunctionArgumentTypeResolver;
import org.hibernate.query.sqm.produce.function.internal.AbstractFunctionArgumentTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;

/**
 * A {@link FunctionArgumentTypeResolver} for {@link SqlTypes#VECTOR} functions.
 */
public class VectorArgumentTypeResolver implements AbstractFunctionArgumentTypeResolver {

	public static final FunctionArgumentTypeResolver INSTANCE = new VectorArgumentTypeResolver( 0 );
	public static final FunctionArgumentTypeResolver DISTANCE_INSTANCE = new VectorArgumentTypeResolver( 0, 1 );

	private final int[] vectorIndices;

	public VectorArgumentTypeResolver(int... vectorIndices) {
		this.vectorIndices = vectorIndices;
	}

	@Override
	public @Nullable MappingModelExpressible<?> resolveFunctionArgumentType(List<? extends SqmTypedNode<?>> arguments, int argumentIndex, SqmToSqlAstConverter converter) {
		for ( int i : vectorIndices ) {
			if ( i != argumentIndex ) {
				final SqmTypedNode<?> node = arguments.get( i );
				if ( node instanceof SqmExpression<?> ) {
					final MappingModelExpressible<?> expressible = converter.determineValueMapping( (SqmExpression<?>) node );
					if ( expressible != null ) {
						return expressible;
					}
				}
			}
		}

		return converter.getCreationContext().getTypeConfiguration().getBasicTypeRegistry()
				.resolve( StandardBasicTypes.VECTOR );
	}
}
