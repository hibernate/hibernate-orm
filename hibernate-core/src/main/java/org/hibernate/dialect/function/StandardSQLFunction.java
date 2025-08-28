/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import java.util.List;
import java.util.function.Supplier;

import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.function.NamedSqmFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.BasicTypeReference;
import org.hibernate.type.spi.TypeConfiguration;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Simplified API allowing users to contribute
 * {@link org.hibernate.query.sqm.function.SqmFunctionDescriptor}s
 * to HQL.
 *
 * @author David Channon
 */
public class StandardSQLFunction extends NamedSqmFunctionDescriptor {
	private final BasicTypeReference<?> type;

	public StandardSQLFunction(String name) {
		this( name, null );
	}

	public StandardSQLFunction(String name, BasicTypeReference<?> type) {
		this( name, true, type );
	}

	public StandardSQLFunction(String name, boolean useParentheses, BasicTypeReference<?> type) {
		super( name, useParentheses, null, new FunctionReturnTypeResolver() {
			@Override
			public ReturnableType<?> resolveFunctionReturnType(
					ReturnableType<?> impliedType,
					@Nullable SqmToSqlAstConverter converter,
					List<? extends SqmTypedNode<?>> arguments,
					TypeConfiguration typeConfiguration) {
				return type == null ? null : typeConfiguration.getBasicTypeRegistry().resolve( type );
			}

			@Override
			public BasicValuedMapping resolveFunctionReturnType(Supplier<BasicValuedMapping> impliedTypeAccess, List<? extends SqlAstNode> arguments) {
				return type == null || impliedTypeAccess == null ? null : impliedTypeAccess.get();
			}
		} );
		this.type = type;
	}

	public BasicTypeReference<?> getType() {
		return type;
	}
}
