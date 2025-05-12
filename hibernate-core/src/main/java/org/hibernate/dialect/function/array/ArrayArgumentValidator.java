/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.type.BindingContext;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionArgumentException;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;

/**
 * A {@link ArgumentsValidator} that validates the array type is compatible with the element type.
 */
public class ArrayArgumentValidator implements ArgumentsValidator {

	public static final ArgumentsValidator DEFAULT_INSTANCE = new ArrayArgumentValidator( 0 );

	private final int arrayIndex;

	public ArrayArgumentValidator(int arrayIndex) {
		this.arrayIndex = arrayIndex;
	}

	@Override
	public void validate(
			List<? extends SqmTypedNode<?>> arguments,
			String functionName,
			BindingContext bindingContext) {
		getElementType( arguments, functionName, bindingContext );
	}

	protected BasicType<?> getElementType(
			List<? extends SqmTypedNode<?>> arguments,
			String functionName,
			BindingContext bindingContext) {
		return getElementType( arrayIndex, arguments, functionName, bindingContext );
	}

	protected BasicPluralType<?, ?> getPluralType(
			int arrayIndex,
			List<? extends SqmTypedNode<?>> arguments,
			String functionName,
			BindingContext bindingContext) {
		final SqmTypedNode<?> arrayArgument = arguments.get( arrayIndex );
		final SqmExpressible<?> expressible = arrayArgument.getExpressible();
		if ( expressible == null ) {
			return null;
		}
		else {
			final SqmExpressible<?> arrayType = expressible.getSqmType();
			if ( arrayType == null ) {
				return null;
			}
			else if ( arrayType instanceof BasicPluralType<?, ?> basicPluralType ) {
				return basicPluralType;
			}
			else {
				throw new FunctionArgumentException(
						String.format(
								"Parameter %d of function '%s()' requires an array type, but argument is of type '%s'",
								arrayIndex,
								functionName,
								arrayType.getTypeName()
						)
				);
			}
		}
	}

	protected BasicType<?> getElementType(
			int arrayIndex,
			List<? extends SqmTypedNode<?>> arguments,
			String functionName,
			BindingContext bindingContext) {
		return getPluralType( arrayIndex, arguments, functionName, bindingContext ).getElementType();
	}
}
