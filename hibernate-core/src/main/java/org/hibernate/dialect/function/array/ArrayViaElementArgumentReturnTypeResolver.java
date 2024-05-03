/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.function.array;

import java.util.List;
import java.util.function.Supplier;

import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.spi.TypeConfiguration;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link FunctionReturnTypeResolver} that resolves an array type based on the arguments,
 * which are supposed to be of the element type. The inferred type and implied type have precedence though.
 */
public class ArrayViaElementArgumentReturnTypeResolver implements FunctionReturnTypeResolver {

	public static final FunctionReturnTypeResolver DEFAULT_INSTANCE = new ArrayViaElementArgumentReturnTypeResolver( false, 0  );

	public static final FunctionReturnTypeResolver DEFAULT_LIST_INSTANCE = new ArrayViaElementArgumentReturnTypeResolver( true, 0  );
	public static final FunctionReturnTypeResolver VARARGS_INSTANCE = new ArrayViaElementArgumentReturnTypeResolver( false, -1  );
	public static final FunctionReturnTypeResolver VARARGS_LIST_INSTANCE = new ArrayViaElementArgumentReturnTypeResolver( true, -1  );

	private final boolean list;
	private final int elementIndex;

	private ArrayViaElementArgumentReturnTypeResolver(boolean list, int elementIndex) {
		this.list = list;
		this.elementIndex = elementIndex;
	}

	@Override
	public ReturnableType<?> resolveFunctionReturnType(
			ReturnableType<?> impliedType,
			@Nullable SqmToSqlAstConverter converter,
			List<? extends SqmTypedNode<?>> arguments,
			TypeConfiguration typeConfiguration) {
		final MappingModelExpressible<?> inferredType = converter == null
				? null
				: converter.resolveFunctionImpliedReturnType();
		if ( inferredType != null ) {
			if ( inferredType instanceof ReturnableType<?> ) {
				return (ReturnableType<?>) inferredType;
			}
			else if ( inferredType instanceof BasicValuedMapping ) {
				return (ReturnableType<?>) ( (BasicValuedMapping) inferredType ).getJdbcMapping();
			}
		}
		if ( impliedType != null ) {
			return impliedType;
		}
		if ( elementIndex == -1 ) {
			for ( SqmTypedNode<?> argument : arguments ) {
				final DomainType<?> sqmType = argument.getExpressible().getSqmType();
				if ( sqmType instanceof ReturnableType<?> ) {
					return list
							? DdlTypeHelper.resolveListType( sqmType, typeConfiguration )
							: DdlTypeHelper.resolveArrayType( sqmType, typeConfiguration );
				}
			}
		}
		else {
			final DomainType<?> sqmType = arguments.get( elementIndex ).getExpressible().getSqmType();
			if ( sqmType instanceof ReturnableType<?> ) {
				return list
						? DdlTypeHelper.resolveListType( sqmType, typeConfiguration )
						: DdlTypeHelper.resolveArrayType( sqmType, typeConfiguration );
			}
		}
		return null;
	}

	@Override
	public BasicValuedMapping resolveFunctionReturnType(
			Supplier<BasicValuedMapping> impliedTypeAccess,
			List<? extends SqlAstNode> arguments) {
		return null;
	}

}
