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
import org.hibernate.type.spi.TypeConfiguration;

/**
 * A {@link ArgumentsValidator} that validates the arguments for the {@code array_contains} function.
 */
public class ArrayContainsArgumentValidator extends ArrayArgumentValidator {

	public static final ArgumentsValidator INSTANCE = new ArrayContainsArgumentValidator();

	protected ArrayContainsArgumentValidator() {
		super( 0 );
	}

	@Override
	public void validate(
			List<? extends SqmTypedNode<?>> arguments,
			String functionName,
			TypeConfiguration typeConfiguration) {
		final BasicPluralType<?, ?> haystackType =
				getPluralType( 0, arguments, functionName, typeConfiguration );
		final SqmExpressible<?> expressible = arguments.get( 1 ).getExpressible();
		final SqmExpressible<?> needleType = expressible == null ? null : expressible.getSqmType();
		if ( haystackType!= null && needleType != null
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
