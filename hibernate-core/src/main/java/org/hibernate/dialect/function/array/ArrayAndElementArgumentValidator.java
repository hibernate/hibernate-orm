/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.type.BindingContext;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionArgumentException;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.type.BasicType;

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
		final BasicType<?> expectedElementType = getElementType( arguments, functionName, bindingContext );
		for ( int elementIndex : elementIndexes ) {
			if ( elementIndex < arguments.size() ) {
				final SqmTypedNode<?> elementArgument = arguments.get( elementIndex );
				final SqmBindableType<?> expressible = elementArgument.getExpressible();
				final SqmExpressible<?> elementType = expressible != null ? expressible.getSqmType() : null;
				if ( expectedElementType != null && elementType != null
						&& !expectedElementType.getJavaType()
							.isAssignableFrom( elementType.getExpressibleJavaType().getJavaTypeClass() ) ) {
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
