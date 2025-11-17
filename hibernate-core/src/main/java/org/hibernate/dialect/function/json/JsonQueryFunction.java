/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmJsonQueryExpression;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JsonPathPassingClause;
import org.hibernate.sql.ast.tree.expression.JsonQueryEmptyBehavior;
import org.hibernate.sql.ast.tree.expression.JsonQueryErrorBehavior;
import org.hibernate.sql.ast.tree.expression.JsonQueryWrapMode;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.ANY;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.IMPLICIT_JSON;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.JSON;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;

/**
 * Standard json_query function.
 */
public class JsonQueryFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	protected final boolean supportsJsonPathExpression;
	protected final boolean supportsJsonPathPassingClause;

	public JsonQueryFunction(
			TypeConfiguration typeConfiguration,
			boolean supportsJsonPathExpression,
			boolean supportsJsonPathPassingClause) {
		super(
				"json_query",
				FunctionKind.NORMAL,
				StandardArgumentsValidators.composite(
						new ArgumentTypesValidator( StandardArgumentsValidators.between( 2, 3 ), IMPLICIT_JSON, STRING, ANY )
				),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( String.class, SqlTypes.JSON )
				),
				StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, JSON, STRING )
		);
		this.supportsJsonPathExpression = supportsJsonPathExpression;
		this.supportsJsonPathPassingClause = supportsJsonPathPassingClause;
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		//noinspection unchecked
		return (SelfRenderingSqmFunction<T>) new SqmJsonQueryExpression(
				this,
				this,
				arguments,
				(ReturnableType<String>) impliedResultType,
				getArgumentsValidator(),
				getReturnTypeResolver(),
				queryEngine.getCriteriaBuilder(),
				getName()
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		render( sqlAppender, JsonQueryArguments.extract( sqlAstArguments ), returnType, walker );
	}

	protected void render(
			SqlAppender sqlAppender,
			JsonQueryArguments arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "json_query(" );
		arguments.jsonDocument().accept( walker );
		sqlAppender.appendSql( ',' );
		final JsonPathPassingClause passingClause = arguments.passingClause();
		if ( supportsJsonPathPassingClause || passingClause == null ) {
			if ( supportsJsonPathExpression ) {
				arguments.jsonPath().accept( walker );
			}
			else {
				walker.getSessionFactory().getJdbcServices().getDialect().appendLiteral(
						sqlAppender,
						walker.getLiteralValue( arguments.jsonPath() )
				);
			}
			if ( passingClause != null ) {
				sqlAppender.appendSql( " passing " );
				final Map<String, Expression> passingExpressions = passingClause.getPassingExpressions();
				final Iterator<Map.Entry<String, Expression>> iterator = passingExpressions.entrySet().iterator();
				Map.Entry<String, Expression> entry = iterator.next();
				entry.getValue().accept( walker );
				sqlAppender.appendSql( " as " );
				sqlAppender.appendDoubleQuoteEscapedString( entry.getKey() );
				while ( iterator.hasNext() ) {
					entry = iterator.next();
					sqlAppender.appendSql( ',' );
					entry.getValue().accept( walker );
					sqlAppender.appendSql( " as " );
					sqlAppender.appendDoubleQuoteEscapedString( entry.getKey() );
				}
			}
		}
		else {
			JsonPathHelper.appendInlinedJsonPathIncludingPassingClause(
					sqlAppender,
					"",
					arguments.jsonPath(),
					passingClause,
					walker
			);
		}
		if ( arguments.wrapMode() != null ) {
			switch ( arguments.wrapMode() ) {
				case WITH_WRAPPER -> sqlAppender.appendSql( " with wrapper" );
				case WITHOUT_WRAPPER -> sqlAppender.appendSql( " without wrapper" );
				case WITH_CONDITIONAL_WRAPPER -> sqlAppender.appendSql( " with conditional wrapper" );
			}
		}
		if ( arguments.errorBehavior() != null ) {
			switch ( arguments.errorBehavior() ) {
				case ERROR -> sqlAppender.appendSql( " error on error" );
				case NULL -> sqlAppender.appendSql( " null on error" );
				case EMPTY_OBJECT -> sqlAppender.appendSql( " empty object on error" );
				case EMPTY_ARRAY -> sqlAppender.appendSql( " empty array on error" );
			}
		}
		if ( arguments.emptyBehavior() != null ) {
			switch ( arguments.emptyBehavior() ) {
				case ERROR -> sqlAppender.appendSql( " error on empty" );
				case NULL -> sqlAppender.appendSql( " null on empty" );
				case EMPTY_OBJECT -> sqlAppender.appendSql( " empty object on empty" );
				case EMPTY_ARRAY -> sqlAppender.appendSql( " empty array on empty" );
			}
		}
		sqlAppender.appendSql( ')' );
	}

	protected record JsonQueryArguments(
			Expression jsonDocument,
			Expression jsonPath,
			boolean isJsonType,
			@Nullable JsonPathPassingClause passingClause,
			@Nullable JsonQueryWrapMode wrapMode,
			@Nullable JsonQueryErrorBehavior errorBehavior,
			@Nullable JsonQueryEmptyBehavior emptyBehavior) {
		public static JsonQueryArguments extract(List<? extends SqlAstNode> sqlAstArguments) {
			int nextIndex = 2;
			JsonPathPassingClause passingClause = null;
			JsonQueryWrapMode wrapMode = null;
			JsonQueryErrorBehavior errorBehavior = null;
			JsonQueryEmptyBehavior emptyBehavior = null;
			if ( nextIndex < sqlAstArguments.size() ) {
				final SqlAstNode node = sqlAstArguments.get( nextIndex );
				if ( node instanceof JsonPathPassingClause jsonPathPassingClause ) {
					passingClause = jsonPathPassingClause;
					nextIndex++;
				}
			}
			if ( nextIndex < sqlAstArguments.size() ) {
				final SqlAstNode node = sqlAstArguments.get( nextIndex );
				if ( node instanceof JsonQueryWrapMode jsonQueryWrapMode ) {
					wrapMode = jsonQueryWrapMode;
					nextIndex++;
				}
			}
			if ( nextIndex < sqlAstArguments.size() ) {
				final SqlAstNode node = sqlAstArguments.get( nextIndex );
				if ( node instanceof JsonQueryErrorBehavior jsonQueryErrorBehavior ) {
					errorBehavior = jsonQueryErrorBehavior;
					nextIndex++;
				}
			}
			if ( nextIndex < sqlAstArguments.size() ) {
				final SqlAstNode node = sqlAstArguments.get( nextIndex );
				if ( node instanceof JsonQueryEmptyBehavior jsonQueryEmptyBehavior ) {
					emptyBehavior = jsonQueryEmptyBehavior;
				}
			}
			final Expression jsonDocument = (Expression) sqlAstArguments.get( 0 );
			return new JsonQueryArguments(
					jsonDocument,
					(Expression) sqlAstArguments.get( 1 ),
					jsonDocument.getExpressionType() != null
							&& jsonDocument.getExpressionType().getSingleJdbcMapping().getJdbcType().isJson(),
					passingClause,
					wrapMode,
					errorBehavior,
					emptyBehavior
			);
		}
	}
}
