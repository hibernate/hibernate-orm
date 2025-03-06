/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.QueryException;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JsonPathPassingClause;
import org.hibernate.sql.ast.tree.expression.JsonValueEmptyBehavior;
import org.hibernate.sql.ast.tree.expression.JsonValueErrorBehavior;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * GaussDB json_value function.
 *
 * @author liubao
 *
 * Notes: Original code of this class is based on PostgreSQLJsonValueFunction.
 */
public class GaussDBJsonValueFunction extends JsonValueFunction {

	private final boolean supportsStandard;

	public GaussDBJsonValueFunction(boolean supportsStandard, TypeConfiguration typeConfiguration) {
		super( typeConfiguration, true, true );
		this.supportsStandard = supportsStandard;
	}

	@Override
	protected void render(
			SqlAppender sqlAppender,
			JsonValueArguments arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		if ( supportsStandard ) {
			super.render( sqlAppender, arguments, returnType, walker );
		}
		else {
			// jsonb_path_query_first errors by default
			if ( arguments.errorBehavior() != null && arguments.errorBehavior() != JsonValueErrorBehavior.ERROR ) {
				throw new QueryException( "Can't emulate on error clause on GaussDB" );
			}
			if ( arguments.emptyBehavior() != null && arguments.emptyBehavior() != JsonValueEmptyBehavior.NULL ) {
				throw new QueryException( "Can't emulate on empty clause on GaussDB" );
			}

			appendJsonValue(
					sqlAppender,
					arguments.jsonDocument(),
					arguments.jsonPath(),
					arguments.isJsonType(),
					arguments.returningType(),
					arguments.passingClause(),
					walker
			);
		}
	}

	@Override
	protected void renderReturningClause(SqlAppender sqlAppender, JsonValueArguments arguments, SqlAstTranslator<?> walker) {
		// See #render for an explanation of this behavior
		if ( supportsStandard && isString( arguments.returningType() ) ) {
			sqlAppender.appendSql( " returning jsonb" );
		}
		else {
			super.renderReturningClause( sqlAppender, arguments, walker );
		}
	}

	private boolean isString(@Nullable CastTarget castTarget) {
		return castTarget == null || castTarget.getJdbcMapping().getJdbcType().isString();
	}

	static void appendJsonValue(SqlAppender sqlAppender, Expression jsonDocument, SqlAstNode jsonPath, boolean isJsonType, @Nullable CastTarget castTarget, @Nullable JsonPathPassingClause passingClause, SqlAstTranslator<?> walker) {
		if ( castTarget != null ) {
			sqlAppender.appendSql( "cast(" );
		}
		sqlAppender.appendSql( "jsonb_path_query_first(" );
		final boolean needsCast = !isJsonType && AbstractSqlAstTranslator.isParameter( jsonDocument );
		if ( needsCast ) {
			sqlAppender.appendSql( "cast(" );
		}
		jsonDocument.accept( walker );
		if ( needsCast ) {
			sqlAppender.appendSql( " as jsonb)" );
		}
		sqlAppender.appendSql( ',' );
		if ( jsonPath instanceof Literal ) {
			jsonPath.accept( walker );
		}
		else {
			sqlAppender.appendSql( "cast(" );
			jsonPath.accept( walker );
			sqlAppender.appendSql( " as jsonpath)" );
		}
		if ( passingClause != null ) {
			sqlAppender.append( ",jsonb_build_object" );
			char separator = '(';
			for ( Map.Entry<String, Expression> entry : passingClause.getPassingExpressions().entrySet() ) {
				sqlAppender.append( separator );
				sqlAppender.appendSingleQuoteEscapedString( entry.getKey() );
				sqlAppender.append( ',' );
				entry.getValue().accept( walker );
				separator = ',';
			}
			sqlAppender.append( ')' );
		}
		// Unquote the value
		sqlAppender.appendSql( ")#>>'{}'" );
		if ( castTarget != null ) {
			sqlAppender.appendSql( " as " );
			castTarget.accept( walker );
			sqlAppender.appendSql( ')' );
		}
	}
}
