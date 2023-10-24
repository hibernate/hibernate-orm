/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.function.array;

import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.produce.function.FunctionArgumentTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmFunction;
import org.hibernate.type.BasicPluralType;

/**
 * A {@link FunctionArgumentTypeResolver} that resolves the array argument type based on the element argument type
 * or the element argument type based on the array argument type.
 */
public class ArrayAndElementArgumentTypeResolver implements FunctionArgumentTypeResolver {

	public static final FunctionArgumentTypeResolver DEFAULT_INSTANCE = new ArrayAndElementArgumentTypeResolver( 0, 1 );

	private final int arrayIndex;
	private final int[] elementIndexes;

	public ArrayAndElementArgumentTypeResolver(int arrayIndex, int... elementIndexes) {
		this.arrayIndex = arrayIndex;
		this.elementIndexes = elementIndexes;
	}

	@Override
	public MappingModelExpressible<?> resolveFunctionArgumentType(
			SqmFunction<?> function,
			int argumentIndex,
			SqmToSqlAstConverter converter) {
		if ( argumentIndex == arrayIndex ) {
			for ( int elementIndex : elementIndexes ) {
				final SqmTypedNode<?> argument = function.getArguments().get( elementIndex );
				final DomainType<?> sqmType = argument.getExpressible().getSqmType();
				if ( sqmType instanceof ReturnableType<?> ) {
					return DdlTypeHelper.resolveArrayType(
							sqmType,
							converter.getCreationContext().getSessionFactory().getTypeConfiguration()
					);
				}
			}
		}
		else if ( ArrayHelper.contains( elementIndexes, argumentIndex ) ) {
			final SqmTypedNode<?> argument = function.getArguments().get( arrayIndex );
			final SqmExpressible<?> sqmType = argument.getNodeType();
			if ( sqmType instanceof BasicPluralType<?, ?> ) {
				return ( (BasicPluralType<?, ?>) sqmType ).getElementType();
			}
		}
		return null;
	}
}
