/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.json;

import java.util.List;

import org.hibernate.query.ReturnableType;
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

	public JsonValueFunction(TypeConfiguration typeConfiguration, boolean supportsJsonPathExpression) {
		super(
				"json_value",
				FunctionKind.NORMAL,
				StandardArgumentsValidators.composite(
						new ArgumentTypesValidator( StandardArgumentsValidators.between( 2, 5 ), IMPLICIT_JSON, STRING, ANY, ANY, ANY )
				),
				new CastTargetReturnTypeResolver( typeConfiguration ),
				StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, JSON, STRING )
		);
		this.supportsJsonPathExpression = supportsJsonPathExpression;
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
		if ( supportsJsonPathExpression ) {
			arguments.jsonPath().accept( walker );
		}
		else {
			walker.getSessionFactory().getJdbcServices().getDialect().appendLiteral(
					sqlAppender,
					walker.getLiteralValue( arguments.jsonPath() )
			);
		}

		if ( arguments.returningType() != null ) {
			sqlAppender.appendSql( " returning " );
			arguments.returningType().accept( walker );
		}
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

	protected record JsonValueArguments(
			Expression jsonDocument,
			Expression jsonPath,
			boolean isJsonType,
			@Nullable CastTarget returningType,
			@Nullable JsonValueErrorBehavior errorBehavior,
			@Nullable JsonValueEmptyBehavior emptyBehavior) {
		public static JsonValueArguments extract(List<? extends SqlAstNode> sqlAstArguments) {
			int nextIndex = 2;
			CastTarget castTarget = null;
			JsonValueErrorBehavior errorBehavior = null;
			JsonValueEmptyBehavior emptyBehavior = null;
			if ( nextIndex < sqlAstArguments.size() ) {
				final SqlAstNode node = sqlAstArguments.get( nextIndex );
				if ( node instanceof CastTarget ) {
					castTarget = (CastTarget) node;
					nextIndex++;
				}
			}
			if ( nextIndex < sqlAstArguments.size() ) {
				final SqlAstNode node = sqlAstArguments.get( nextIndex );
				if ( node instanceof JsonValueErrorBehavior ) {
					errorBehavior = (JsonValueErrorBehavior) node;
					nextIndex++;
				}
			}
			if ( nextIndex < sqlAstArguments.size() ) {
				final SqlAstNode node = sqlAstArguments.get( nextIndex );
				if ( node instanceof JsonValueEmptyBehavior ) {
					emptyBehavior = (JsonValueEmptyBehavior) node;
				}
			}
			final Expression jsonDocument = (Expression) sqlAstArguments.get( 0 );
			return new JsonValueArguments(
					jsonDocument,
					(Expression) sqlAstArguments.get( 1 ),
					jsonDocument.getExpressionType() != null
							&& jsonDocument.getExpressionType().getSingleJdbcMapping().getJdbcType().isJson(),
					castTarget,
					errorBehavior,
					emptyBehavior
			);
		}
	}
}
