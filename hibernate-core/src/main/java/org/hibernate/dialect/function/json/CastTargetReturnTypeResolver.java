/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.List;
import java.util.function.Supplier;

import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmCastTarget;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.type.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers.extractArgumentType;
import static org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers.extractArgumentValuedMapping;
import static org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers.isAssignableTo;
import static org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers.useImpliedTypeIfPossible;

public class CastTargetReturnTypeResolver implements FunctionReturnTypeResolver {

	private final BasicType<?> defaultType;

	public CastTargetReturnTypeResolver(TypeConfiguration typeConfiguration) {
		this.defaultType = typeConfiguration.getBasicTypeForJavaType( String.class );
	}

	@Override
	public ReturnableType<?> resolveFunctionReturnType(
			ReturnableType<?> impliedType,
			@Nullable SqmToSqlAstConverter converter,
			List<? extends SqmTypedNode<?>> arguments,
			TypeConfiguration typeConfiguration) {
		if ( arguments.size() > 2 ) {
			int castTargetIndex = 0;
			for ( int i = 2; i < arguments.size(); i++ ) {
				if (arguments.get( i ) instanceof SqmCastTarget<?> ) {
					castTargetIndex = i + 1;
					break;
				}
			}
			if ( castTargetIndex != 0 ) {
				ReturnableType<?> argType = extractArgumentType( arguments, castTargetIndex );
				return isAssignableTo( argType, impliedType ) ? impliedType : argType;
			}
		}
		return defaultType;
	}

	@Override
	public BasicValuedMapping resolveFunctionReturnType(
			Supplier<BasicValuedMapping> impliedTypeAccess,
			List<? extends SqlAstNode> arguments) {
		if ( arguments.size() > 2 ) {
			int castTargetIndex = 0;
			for ( int i = 2; i < arguments.size(); i++ ) {
				if (arguments.get( i ) instanceof CastTarget ) {
					castTargetIndex = i + 1;
					break;
				}
			}
			if ( castTargetIndex != 0 ) {
				final BasicValuedMapping specifiedArgType = extractArgumentValuedMapping( arguments, castTargetIndex );
				return useImpliedTypeIfPossible( specifiedArgType, impliedTypeAccess.get() );
			}
		}
		return defaultType;
	}
}
