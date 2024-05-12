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
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.spi.TypeConfiguration;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link FunctionReturnTypeResolver} that resolves the array element type based on an argument.
 * The inferred type and implied type have precedence though.
 */
public class ElementViaArrayArgumentReturnTypeResolver implements FunctionReturnTypeResolver {

	public static final FunctionReturnTypeResolver DEFAULT_INSTANCE = new ElementViaArrayArgumentReturnTypeResolver( 0 );

	private final int arrayIndex;

	private ElementViaArrayArgumentReturnTypeResolver(int arrayIndex) {
		this.arrayIndex = arrayIndex;
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
		final SqmExpressible<?> expressible = arguments.get( arrayIndex ).getExpressible();
		final DomainType<?> type;
		if ( expressible != null && ( type = expressible.getSqmType() ) instanceof BasicPluralType<?, ?> ) {
			return ( (BasicPluralType<?, ?>) type ).getElementType();
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
