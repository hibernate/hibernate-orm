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
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmJsonValueExpression;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JsonPathPassingClause;
import org.hibernate.sql.ast.tree.expression.JsonValueEmptyBehavior;
import org.hibernate.sql.ast.tree.expression.JsonValueErrorBehavior;
import org.hibernate.type.spi.TypeConfiguration;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.ANY;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.IMPLICIT_JSON;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.JSON;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;

/**
 * Standard json_value function.
 */
public class JsonValueFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	protected final boolean supportsJsonPathExpression;
	protected final boolean supportsJsonPathPassingClause;

	public JsonValueFunction(
			TypeConfiguration typeConfiguration,
			boolean supportsJsonPathExpression,
			boolean supportsJsonPathPassingClause) {
		super(
				"json_value",
				FunctionKind.NORMAL,
				StandardArgumentsValidators.composite(
						new ArgumentTypesValidator( StandardArgumentsValidators.between( 2, 3 ), IMPLICIT_JSON, STRING, ANY )
				),
				new CastTargetReturnTypeResolver( typeConfiguration ),
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
		return new SqmJsonValueExpression<>(
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

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		render( sqlAppender, JsonValueArguments.extract( sqlAstArguments ), returnType, walker );
	}

	protected void render(
			SqlAppender sqlAppender,
			JsonValueArguments arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "json_value(" );
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
		renderReturningClause( sqlAppender, arguments, walker );
		if ( arguments.errorBehavior() != null ) {
			if ( arguments.errorBehavior() == JsonValueErrorBehavior.ERROR ) {
				sqlAppender.appendSql( " error on error" );
			}
			else if ( arguments.errorBehavior() != JsonValueErrorBehavior.NULL ) {
				final Expression defaultExpression = arguments.errorBehavior().getDefaultExpression();
				assert defaultExpression != null;
				sqlAppender.appendSql( " default " );
				defaultExpression.accept( walker );
				sqlAppender.appendSql( " on error" );
			}
		}
		if ( arguments.emptyBehavior() != null ) {
			if ( arguments.emptyBehavior() == JsonValueEmptyBehavior.ERROR ) {
				sqlAppender.appendSql( " error on empty" );
			}
			else if ( arguments.emptyBehavior() != JsonValueEmptyBehavior.NULL ) {
				final Expression defaultExpression = arguments.emptyBehavior().getDefaultExpression();
				assert defaultExpression != null;
				sqlAppender.appendSql( " default " );
				defaultExpression.accept( walker );
				sqlAppender.appendSql( " on empty" );
			}
		}
		sqlAppender.appendSql( ')' );
	}

	protected void renderReturningClause(SqlAppender sqlAppender, JsonValueArguments arguments, SqlAstTranslator<?> walker) {
		if ( arguments.returningType() != null ) {
			sqlAppender.appendSql( " returning " );
			arguments.returningType().accept( walker );
		}
	}

	protected record JsonValueArguments(
			Expression jsonDocument,
			Expression jsonPath,
			boolean isJsonType,
			@Nullable JsonPathPassingClause passingClause,
			@Nullable CastTarget returningType,
			@Nullable JsonValueErrorBehavior errorBehavior,
			@Nullable JsonValueEmptyBehavior emptyBehavior) {
		public static JsonValueArguments extract(List<? extends SqlAstNode> sqlAstArguments) {
			int nextIndex = 2;
			JsonPathPassingClause passingClause = null;
			CastTarget castTarget = null;
			JsonValueErrorBehavior errorBehavior = null;
			JsonValueEmptyBehavior emptyBehavior = null;
			if ( nextIndex < sqlAstArguments.size() ) {
				final SqlAstNode node = sqlAstArguments.get( nextIndex );
				if ( node instanceof CastTarget cast ) {
					castTarget = cast;
					nextIndex++;
				}
			}
			if ( nextIndex < sqlAstArguments.size() ) {
				final SqlAstNode node = sqlAstArguments.get( nextIndex );
				if ( node instanceof JsonPathPassingClause jsonPathPassingClause ) {
					passingClause = jsonPathPassingClause;
					nextIndex++;
				}
			}
			if ( nextIndex < sqlAstArguments.size() ) {
				final SqlAstNode node = sqlAstArguments.get( nextIndex );
				if ( node instanceof JsonValueErrorBehavior jsonValueErrorBehavior ) {
					errorBehavior = jsonValueErrorBehavior;
					nextIndex++;
				}
			}
			if ( nextIndex < sqlAstArguments.size() ) {
				final SqlAstNode node = sqlAstArguments.get( nextIndex );
				if ( node instanceof JsonValueEmptyBehavior jsonValueEmptyBehavior ) {
					emptyBehavior = jsonValueEmptyBehavior;
				}
			}
			final Expression jsonDocument = (Expression) sqlAstArguments.get( 0 );
			return new JsonValueArguments(
					jsonDocument,
					(Expression) sqlAstArguments.get( 1 ),
					jsonDocument.getExpressionType() != null
							&& jsonDocument.getExpressionType().getSingleJdbcMapping().getJdbcType().isJson(),
					passingClause,
					castTarget,
					errorBehavior,
					emptyBehavior
			);
		}
	}
}
