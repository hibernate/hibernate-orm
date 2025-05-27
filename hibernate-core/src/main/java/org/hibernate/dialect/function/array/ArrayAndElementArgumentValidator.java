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
import org.hibernate.type.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

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
			TypeConfiguration typeConfiguration) {
		final BasicType<?> expectedElementType = getElementType( arguments, functionName, typeConfiguration );
		for ( int elementIndex : elementIndexes ) {
			if ( elementIndex < arguments.size() ) {
				final SqmTypedNode<?> elementArgument = arguments.get( elementIndex );
				final SqmExpressible<?> expressible = elementArgument.getExpressible();
				final SqmExpressible<?> elementType = expressible != null ? expressible.getSqmType() : null;
				if ( expectedElementType != null && elementType != null && expectedElementType != elementType ) {
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
