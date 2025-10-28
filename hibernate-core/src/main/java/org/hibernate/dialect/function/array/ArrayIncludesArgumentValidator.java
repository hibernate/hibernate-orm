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

/**
 * A {@link ArgumentsValidator} that validates the arguments for the {@code array_includes} function.
 */
public class ArrayIncludesArgumentValidator extends ArrayArgumentValidator {

	public static final ArgumentsValidator INSTANCE = new ArrayIncludesArgumentValidator();

	protected ArrayIncludesArgumentValidator() {
		super( 0 );
	}

	@Override
	public void validate(
			List<? extends SqmTypedNode<?>> arguments,
			String functionName,
			BindingContext bindingContext) {
		final var haystackType = getPluralType( 0, arguments, functionName );
		final var needleType = getPluralType( 1, arguments, functionName );
		if ( haystackType != null && needleType != null
				&& !haystackType.equals( needleType )
				&& !haystackType.getElementType().equals( needleType ) ) {
			throw new FunctionArgumentException(
					String.format(
							"Parameter 1 of function '%s()' has type %s, but argument is of type '%s'",
							functionName,
							haystackType.getJavaTypeDescriptor().getTypeName(),
							needleType.getTypeName()
					)
			);
		}
	}
}
