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
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmJsonExistsExpression;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JsonExistsErrorBehavior;
import org.hibernate.sql.ast.tree.expression.JsonPathPassingClause;
import org.hibernate.type.spi.TypeConfiguration;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.IMPLICIT_JSON;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.JSON;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;

/**
 * Standard json_exists function.
 */
public class JsonExistsFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	protected final boolean supportsJsonPathExpression;
	protected final boolean supportsJsonPathPassingClause;

	public JsonExistsFunction(
			TypeConfiguration typeConfiguration,
			boolean supportsJsonPathExpression,
			boolean supportsJsonPathPassingClause) {
		super(
				"json_exists",
				FunctionKind.NORMAL,
				new ArgumentTypesValidator( null, IMPLICIT_JSON, STRING ),
				StandardFunctionReturnTypeResolvers.invariant( typeConfiguration.standardBasicTypeForJavaType( Boolean.class ) ),
				StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, JSON, STRING )
		);
		this.supportsJsonPathExpression = supportsJsonPathExpression;
		this.supportsJsonPathPassingClause = supportsJsonPathPassingClause;
	}

	@Override
	public boolean isPredicate() {
		return true;
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		//noinspection unchecked
		return (SelfRenderingSqmFunction<T>) new SqmJsonExistsExpression(
				this,
				this,
				arguments,
				(ReturnableType<Boolean>) impliedResultType,
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
		render( sqlAppender, JsonExistsArguments.extract( sqlAstArguments ), returnType, walker );
	}

	protected void render(
			SqlAppender sqlAppender,
			JsonExistsArguments arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "json_exists(" );
		arguments.jsonDocument().accept( walker );
		sqlAppender.appendSql( ',' );
		final Expression jsonPath = arguments.jsonPath();
		final JsonPathPassingClause passingClause = arguments.passingClause();
		if ( supportsJsonPathPassingClause || passingClause == null ) {
			if ( supportsJsonPathExpression ) {
				jsonPath.accept( walker );
			}
			else {
				walker.getSessionFactory().getJdbcServices().getDialect().appendLiteral(
						sqlAppender,
						walker.getLiteralValue( jsonPath )
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
		final JsonExistsErrorBehavior errorBehavior = arguments.errorBehavior();
		if ( errorBehavior != null && errorBehavior != JsonExistsErrorBehavior.FALSE ) {
			if ( errorBehavior == JsonExistsErrorBehavior.TRUE ) {
				sqlAppender.appendSql( " true on error" );
			}
			else {
				sqlAppender.appendSql( " error on error" );
			}
		}
		sqlAppender.appendSql( ')' );
	}

	protected record JsonExistsArguments(
			Expression jsonDocument,
			Expression jsonPath,
			boolean isJsonType,
			@Nullable JsonPathPassingClause passingClause,
			@Nullable JsonExistsErrorBehavior errorBehavior) {
		public static JsonExistsArguments extract(List<? extends SqlAstNode> sqlAstArguments) {
			int nextIndex = 2;
			JsonPathPassingClause passingClause = null;
			JsonExistsErrorBehavior errorBehavior = null;
			if ( nextIndex < sqlAstArguments.size() ) {
				final SqlAstNode node = sqlAstArguments.get( nextIndex );
				if ( node instanceof JsonPathPassingClause pathPassingClause ) {
					passingClause = pathPassingClause;
					nextIndex++;
				}
			}
			if ( nextIndex < sqlAstArguments.size() ) {
				final SqlAstNode node = sqlAstArguments.get( nextIndex );
				if ( node instanceof JsonExistsErrorBehavior existsErrorBehavior ) {
					errorBehavior = existsErrorBehavior;
					nextIndex++;
				}
			}
			final Expression jsonDocument = (Expression) sqlAstArguments.get( 0 );
			return new JsonExistsArguments(
					jsonDocument,
					(Expression) sqlAstArguments.get( 1 ),
					jsonDocument.getExpressionType() != null
							&& jsonDocument.getExpressionType().getSingleJdbcMapping().getJdbcType().isJson(),
					passingClause,
					errorBehavior
			);
		}
	}

}
