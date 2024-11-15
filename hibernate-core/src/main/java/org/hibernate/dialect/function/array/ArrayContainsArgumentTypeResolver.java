/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.function.array;

import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.produce.function.FunctionArgumentTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmFunction;
import org.hibernate.type.BasicPluralType;

/**
 * A {@link FunctionArgumentTypeResolver} that resolves the argument types for the {@code array_contains} function.
 */
public class ArrayContainsArgumentTypeResolver implements FunctionArgumentTypeResolver {

	public static final FunctionArgumentTypeResolver INSTANCE = new ArrayContainsArgumentTypeResolver();

	@Override
	public MappingModelExpressible<?> resolveFunctionArgumentType(
			SqmFunction<?> function,
			int argumentIndex,
			SqmToSqlAstConverter converter) {
		if ( argumentIndex == 0 ) {
			final SqmTypedNode<?> node = function.getArguments().get( 1 );
			if ( node instanceof SqmExpression<?> ) {
				final MappingModelExpressible<?> expressible = converter.determineValueMapping( (SqmExpression<?>) node );
				if ( expressible != null ) {
					if ( expressible.getSingleJdbcMapping() instanceof BasicPluralType<?, ?> ) {
						return expressible;
					}
					else {
						return DdlTypeHelper.resolveArrayType(
								(DomainType<?>) expressible.getSingleJdbcMapping(),
								converter.getCreationContext().getSessionFactory().getTypeConfiguration()
						);
					}
				}
			}
		}
		else if ( argumentIndex == 1 ) {
			final SqmTypedNode<?> nodeToResolve = function.getArguments().get( 1 );
			if ( nodeToResolve.getExpressible() instanceof MappingModelExpressible<?> ) {
				// If the node already has suitable type, don't infer it to be treated as an array
				return null;
			}
			final SqmTypedNode<?> node = function.getArguments().get( 0 );
			if ( node instanceof SqmExpression<?> ) {
				final MappingModelExpressible<?> expressible = converter.determineValueMapping( (SqmExpression<?>) node );
				if ( expressible instanceof BasicPluralType<?, ?> ) {
					return expressible;
				}
			}
		}
		return null;
	}
}
