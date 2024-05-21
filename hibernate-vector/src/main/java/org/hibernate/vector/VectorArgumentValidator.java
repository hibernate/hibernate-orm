/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.vector;

import java.util.List;

import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionArgumentException;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * A {@link ArgumentsValidator} that validates the arguments are all vector types i.e. {@link org.hibernate.type.SqlTypes#VECTOR}.
 */
public class VectorArgumentValidator implements ArgumentsValidator {

	public static final ArgumentsValidator INSTANCE = new VectorArgumentValidator();

	@Override
	public void validate(
			List<? extends SqmTypedNode<?>> arguments,
			String functionName,
			TypeConfiguration typeConfiguration) {
		for ( int i = 0; i < arguments.size(); i++ ) {
			final SqmExpressible<?> expressible = arguments.get( i ).getExpressible();
			final DomainType<?> type;
			if ( expressible != null && ( type = expressible.getSqmType() ) != null && !isVectorType( type ) ) {
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

	private static boolean isVectorType(SqmExpressible<?> vectorType) {
		if ( !( vectorType instanceof BasicPluralType<?, ?> ) ) {
			return false;
		}

		switch ( ( (BasicType<?>) vectorType ).getJdbcType().getDefaultSqlTypeCode() ) {
			case SqlTypes.VECTOR:
			case SqlTypes.VECTOR_INT8:
			case SqlTypes.VECTOR_FLOAT32:
			case SqlTypes.VECTOR_FLOAT64:
				return true;
			default:
				return false;
		}
	}
}
