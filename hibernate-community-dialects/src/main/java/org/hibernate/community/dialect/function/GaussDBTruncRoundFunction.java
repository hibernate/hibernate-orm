/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.function;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.AbstractSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionRenderer;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.INTEGER;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.NUMERIC;

/**
 * GaussDB only supports the two-argument {@code trunc} and {@code round} functions
 * with the following signatures:
 * <ul>
 *     <li>{@code trunc(numeric, integer)}</li>
 *     <li>{@code round(numeric, integer)}</li>
 * </ul>
 * <p>
 * This custom function falls back to using {@code floor} as a workaround only when necessary,
 * e.g. when there are 2 arguments to the function and either:
 * <ul>
 *     <li>The first argument is not of type {@code numeric}</li>
 *     or
 *     <li>The dialect doesn't support the two-argument {@code trunc} function</li>
 * </ul>
 *
 * @author liubao
 *
 * Notes: Original code of this class is based on PostgreSQLTruncRoundFunction.
 */
public class GaussDBTruncRoundFunction extends AbstractSqmFunctionDescriptor implements FunctionRenderer {
	private final boolean supportsTwoArguments;

	public GaussDBTruncRoundFunction(String name, boolean supportsTwoArguments) {
		super(
				name,
				new ArgumentTypesValidator( StandardArgumentsValidators.between( 1, 2 ), NUMERIC, INTEGER ),
				StandardFunctionReturnTypeResolvers.useArgType( 1 ),
				StandardFunctionArgumentTypeResolvers.invariant( NUMERIC, INTEGER )
		);
		this.supportsTwoArguments = supportsTwoArguments;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final int numberOfArguments = arguments.size();
		final Expression firstArg = (Expression) arguments.get( 0 );
		final JdbcType jdbcType = firstArg.getExpressionType().getSingleJdbcMapping().getJdbcType();
		if ( numberOfArguments == 1 || supportsTwoArguments && jdbcType.isDecimal() ) {
			// use native two-argument function
			sqlAppender.appendSql( getName() );
			sqlAppender.appendSql( "(" );
			firstArg.accept( walker );
			if ( numberOfArguments > 1 ) {
				sqlAppender.appendSql( ", " );
				arguments.get( 1 ).accept( walker );
			}
			sqlAppender.appendSql( ")" );
		}
		else {
			// workaround using floor
			if ( getName().equals( "trunc" ) ) {
				sqlAppender.appendSql( "sign(" );
				firstArg.accept( walker );
				sqlAppender.appendSql( ")*floor(abs(" );
				firstArg.accept( walker );
				sqlAppender.appendSql( ")*1e" );
				arguments.get( 1 ).accept( walker );
			}
			else {
				sqlAppender.appendSql( "floor(" );
				firstArg.accept( walker );
				sqlAppender.appendSql( "*1e" );
				arguments.get( 1 ).accept( walker );
				sqlAppender.appendSql( "+0.5" );
			}
			sqlAppender.appendSql( ")/1e" );
			arguments.get( 1 ).accept( walker );
		}
	}

	@Override
	public String getArgumentListSignature() {
		return "(NUMERIC number[, INTEGER places])";
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		return new SelfRenderingSqmFunction<>(
				this,
				this,
				arguments,
				impliedResultType,
				getArgumentsValidator(),
				getReturnTypeResolver(),
				queryEngine.getCriteriaBuilder(),
				getName()
		);
	}
}
