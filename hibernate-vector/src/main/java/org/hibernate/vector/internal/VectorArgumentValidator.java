/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import java.util.List;

import org.hibernate.type.BasicType;
import org.hibernate.type.BindingContext;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionArgumentException;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.domain.SqmDomainType;
import org.hibernate.type.SqlTypes;

/**
 * A {@link ArgumentsValidator} that validates the arguments are all vector types i.e. {@link org.hibernate.type.SqlTypes#VECTOR}.
 */
public class VectorArgumentValidator implements ArgumentsValidator {

	public static final ArgumentsValidator INSTANCE = new VectorArgumentValidator( 0 );
	public static final ArgumentsValidator DISTANCE_INSTANCE = new VectorArgumentValidator( 0, 1 );

	private final int[] vectorIndices;

	public VectorArgumentValidator(int... vectorIndices) {
		this.vectorIndices = vectorIndices;
	}

	@Override
	public void validate(
			List<? extends SqmTypedNode<?>> arguments,
			String functionName,
			BindingContext bindingContext) {
		for ( int i : vectorIndices ) {
			final SqmExpressible<?> expressible = arguments.get( i ).getExpressible();
			if ( expressible != null ) {
				final SqmDomainType<?> type = expressible.getSqmType();
				if ( type != null && !isVectorType( type ) ) {
					throw new FunctionArgumentException(
							String.format(
									"Parameter %d of function '%s()' requires a vector type, but argument is of type '%s'",
									i,
									functionName,
									type.getTypeName()
							)
					);
				}
			}
		}
	}

	private static boolean isVectorType(SqmExpressible<?> vectorType) {
		return vectorType instanceof BasicType<?> basicType
			&& switch ( basicType.getJdbcType().getDefaultSqlTypeCode() ) {
			case SqlTypes.VECTOR, SqlTypes.VECTOR_INT8, SqlTypes.VECTOR_FLOAT16, SqlTypes.VECTOR_FLOAT32, SqlTypes.VECTOR_FLOAT64,
				SqlTypes.VECTOR_BINARY, SqlTypes.SPARSE_VECTOR_INT8, SqlTypes.SPARSE_VECTOR_FLOAT32, SqlTypes.SPARSE_VECTOR_FLOAT64-> true;
			default -> false;
		};
	}
}
