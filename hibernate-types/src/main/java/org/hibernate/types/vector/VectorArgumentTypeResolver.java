/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.types.vector;

import java.util.List;

import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.sqm.produce.function.FunctionArgumentTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmFunction;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;

/**
 * A {@link FunctionArgumentTypeResolver} for {@link SqlTypes#VECTOR} functions.
 */
public class VectorArgumentTypeResolver implements FunctionArgumentTypeResolver {

	public static final FunctionArgumentTypeResolver INSTANCE = new VectorArgumentTypeResolver();

	@Override
	public MappingModelExpressible<?> resolveFunctionArgumentType(
			SqmFunction<?> function,
			int argumentIndex,
			SqmToSqlAstConverter converter) {
		final List<? extends SqmTypedNode<?>> arguments = function.getArguments();
		for ( int i = 0; i < arguments.size(); i++ ) {
			if ( i != argumentIndex ) {
				final SqmTypedNode<?> node = arguments.get( i );
				if ( node instanceof SqmExpression<?> ) {
					final MappingModelExpressible<?> expressible = converter.determineValueMapping( (SqmExpression<?>) node );
					if ( expressible != null ) {
						return expressible;
					}
				}
			}
		}

		return converter.getCreationContext()
				.getSessionFactory()
				.getTypeConfiguration()
				.getBasicTypeRegistry()
				.resolve( StandardBasicTypes.VECTOR );
	}
}
