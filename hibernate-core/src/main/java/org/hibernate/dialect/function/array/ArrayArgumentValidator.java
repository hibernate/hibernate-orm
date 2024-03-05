/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionArgumentException;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

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
			TypeConfiguration typeConfiguration) {
		getElementType( arguments, functionName, typeConfiguration );
	}

	protected BasicType<?> getElementType(
			List<? extends SqmTypedNode<?>> arguments,
			String functionName,
			TypeConfiguration typeConfiguration) {
		return getElementType( arrayIndex, arguments, functionName, typeConfiguration );
	}

	protected BasicPluralType<?, ?> getPluralType(
			int arrayIndex,
			List<? extends SqmTypedNode<?>> arguments,
			String functionName,
			TypeConfiguration typeConfiguration) {
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
			else if ( !( arrayType instanceof BasicPluralType<?, ?> ) ) {
				throw new FunctionArgumentException(
						String.format(
								"Parameter %d of function '%s()' requires an array type, but argument is of type '%s'",
								arrayIndex,
								functionName,
								arrayType.getTypeName()
						)
				);
			}
			return (BasicPluralType<?, ?>) arrayType;
		}
	}

	protected BasicType<?> getElementType(
			int arrayIndex,
			List<? extends SqmTypedNode<?>> arguments,
			String functionName,
			TypeConfiguration typeConfiguration) {
		return getPluralType( arrayIndex, arguments, functionName, typeConfiguration ).getElementType();
	}
}
