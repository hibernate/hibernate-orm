/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.type.BindingContext;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionArgumentException;
import org.hibernate.query.sqm.tree.SqmTypedNode;

import static org.hibernate.query.sqm.internal.TypecheckUtil.isTypeAssignable;

/**
 * A {@link ArgumentsValidator} that validates the array type is compatible with the element type.
 */
public class ArrayAndElementArgumentValidator extends ArrayArgumentValidator {

	public static final ArgumentsValidator DEFAULT_INSTANCE = new ArrayAndElementArgumentValidator( 0, 1 );

	private final int[] elementIndexes;

	public ArrayAndElementArgumentValidator(int arrayIndex, int... elementIndexes) {
		super( arrayIndex );
		this.elementIndexes = elementIndexes;
	}

	@Override
	public void validate(
			List<? extends SqmTypedNode<?>> arguments,
			String functionName,
			BindingContext bindingContext) {
		final var expectedElementType = getElementType( arguments, functionName );
		for ( int elementIndex : elementIndexes ) {
			if ( elementIndex < arguments.size() ) {
				final var elementArgument = arguments.get( elementIndex );
				final var expressible = elementArgument.getExpressible();
				final var elementType = expressible != null ? expressible.getSqmType() : null;
				if ( expectedElementType != null && elementType != null
						&& !isTypeAssignable( expectedElementType, elementType.getSqmType(), bindingContext ) ) {
//						&& !expectedElementType.getRelationalJavaType().getJavaTypeClass()
//							.isAssignableFrom( elementType.getRelationalJavaType().getJavaTypeClass() ) ) {
					throw new FunctionArgumentException(
							String.format(
									"Parameter %d of function '%s()' has type %s, but argument is of type '%s'",
									elementIndex,
									functionName,
									expectedElementType.getJavaTypeDescriptor().getTypeName(),
									elementType.getTypeName()
							)
					);
				}
			}
		}
	}
}
