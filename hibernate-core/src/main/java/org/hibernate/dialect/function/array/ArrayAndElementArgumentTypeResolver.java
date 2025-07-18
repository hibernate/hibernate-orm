/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.internal.util.collections.ArrayHelper;
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
 * A {@link FunctionArgumentTypeResolver} that resolves the array argument type based on the element argument type
 * or the element argument type based on the array argument type.
 */
public class ArrayAndElementArgumentTypeResolver extends AbstractFunctionArgumentTypeResolver {

	public static final FunctionArgumentTypeResolver DEFAULT_INSTANCE = new ArrayAndElementArgumentTypeResolver( 0, 1 );

	private final int arrayIndex;
	private final int[] elementIndexes;

	public ArrayAndElementArgumentTypeResolver(int arrayIndex, int... elementIndexes) {
		this.arrayIndex = arrayIndex;
		this.elementIndexes = elementIndexes;
	}

	@Override
	public @Nullable MappingModelExpressible<?> resolveFunctionArgumentType(List<? extends SqmTypedNode<?>> arguments, int argumentIndex, SqmToSqlAstConverter converter) {
		if ( argumentIndex == arrayIndex ) {
			for ( int elementIndex : elementIndexes ) {
				if ( elementIndex >= arguments.size() ) {
					continue;
				}
				final SqmTypedNode<?> node = arguments.get( elementIndex );
				if ( node instanceof SqmExpression<?> sqmExpression ) {
					final MappingModelExpressible<?> expressible = converter.determineValueMapping( sqmExpression );
					if ( expressible != null ) {
						return DdlTypeHelper.resolveArrayType(
								(DomainType<?>) expressible.getSingleJdbcMapping(),
								converter.getCreationContext().getTypeConfiguration()
						);
					}
				}
			}
		}
		else if ( ArrayHelper.contains( elementIndexes, argumentIndex ) ) {
			final SqmTypedNode<?> node = arguments.get( arrayIndex );
			if ( node instanceof SqmExpression<?> sqmExpression ) {
				final MappingModelExpressible<?> expressible = converter.determineValueMapping( sqmExpression );
				if ( expressible != null ) {
					if ( expressible.getSingleJdbcMapping() instanceof BasicPluralType<?, ?> basicPluralType ) {
						return basicPluralType.getElementType();
					}
				}
			}
		}
		return null;
	}
}
