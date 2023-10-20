/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionArgumentException;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * A {@link ArgumentsValidator} that validates all arguments are of the same array type.
 */
public class ArrayConcatArgumentValidator implements ArgumentsValidator {

	public static final ArgumentsValidator INSTANCE = new ArrayConcatArgumentValidator();

	@Override
	public void validate(
			List<? extends SqmTypedNode<?>> arguments,
			String functionName,
			TypeConfiguration typeConfiguration) {
		BasicPluralType<?, ?> arrayType = null;
		for ( int i = 0; i < arguments.size(); i++ ) {
			final DomainType<?> sqmType = arguments.get( i ).getExpressible().getSqmType();
			if ( sqmType != null ) {
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
				else if ( !arrayType.equals( sqmType ) ) {
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

	@Override
	public String getSignature() {
		return "(ARRAY array0, ARRAY array1[, ARRAY array2, ...])";
	}
}
