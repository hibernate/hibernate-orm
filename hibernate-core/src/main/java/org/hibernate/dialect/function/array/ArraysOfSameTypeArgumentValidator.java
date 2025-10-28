/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import java.util.List;
import java.util.Objects;

import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.type.BindingContext;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionArgumentException;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.type.BasicPluralType;

/**
 * A {@link ArgumentsValidator} that validates all arguments are of the same array type.
 */
public class ArraysOfSameTypeArgumentValidator implements ArgumentsValidator {

	public static final ArgumentsValidator INSTANCE = new ArraysOfSameTypeArgumentValidator();

	@Override
	public void validate(
			List<? extends SqmTypedNode<?>> arguments,
			String functionName,
			BindingContext bindingContext) {
		BasicPluralType<?, ?> arrayType = null;
		for ( int i = 0; i < arguments.size(); i++ ) {
			final var expressible = arguments.get( i ).getExpressible();
			final DomainType<?> sqmType;
			if ( expressible != null && ( sqmType = expressible.getSqmType() ) != null ) {
				if ( arrayType == null ) {
					if ( !( sqmType instanceof BasicPluralType<?, ?> ) ) {
						throw new FunctionArgumentException(
								String.format(
										"Parameter %d of function '%s()' requires an array type, but argument is of type '%s'",
										i,
										functionName,
										sqmType.getTypeName()
								)
						);
					}
					arrayType = (BasicPluralType<?, ?>) sqmType;
				}
				else if ( !isCompatible( arrayType, sqmType ) ) {
					throw new FunctionArgumentException(
							String.format(
									"Parameter %d of function '%s()' requires an array type %s, but argument is of type '%s'",
									i,
									functionName,
									arrayType.getTypeName(),
									sqmType.getTypeName()
							)
					);
				}
			}
		}
	}

	private static boolean isCompatible(BasicPluralType<?,?> arrayType, DomainType<?> sqmType) {
		return arrayType == sqmType
			|| sqmType instanceof BasicPluralType<?, ?> basicPluralType
				&& Objects.equals( arrayType.getElementType(), basicPluralType.getElementType() );
	}

	@Override
	public String getSignature() {
		return "(ARRAY array0, ARRAY array1[, ARRAY array2, ...])";
	}
}
