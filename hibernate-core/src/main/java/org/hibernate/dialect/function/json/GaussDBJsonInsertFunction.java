/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * GaussDB json_insert function.
 *
 * @author liubao
 *
 * Notes: Original code of this class is based on PostgreSQLJsonInsertFunction.
 */
public class GaussDBJsonInsertFunction extends AbstractJsonInsertFunction {

	public GaussDBJsonInsertFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> translator) {
		final Expression json = (Expression) arguments.get( 0 );
		final Expression jsonPath = (Expression) arguments.get( 1 );
		final SqlAstNode value = arguments.get( 2 );
		sqlAppender.appendSql( "(select case when (t.d)#>>t.p is not null then t.d else jsonb_insert(t.d,t.p," );
		if ( value instanceof Literal && ( (Literal) value ).getLiteralValue() == null ) {
			sqlAppender.appendSql( "null::jsonb" );
		}
		else {
			sqlAppender.appendSql( "to_jsonb(" );
			value.accept( translator );
			if ( value instanceof Literal literal && literal.getJdbcMapping().getJdbcType().isString() ) {
				// GaussDB until version 16 is not smart enough to infer the type of a string literal
				sqlAppender.appendSql( "::text" );
			}
			sqlAppender.appendSql( ')' );
		}
		sqlAppender.appendSql( ",true) end from (values(" );
		final boolean needsCast = !isJsonType( json );
		if ( needsCast ) {
			sqlAppender.appendSql( "cast(" );
		}
		json.accept( translator );
		if ( needsCast ) {
			sqlAppender.appendSql( " as jsonb)" );
		}
		sqlAppender.appendSql( ',' );
		List<JsonPathHelper.JsonPathElement> jsonPathElements =
				JsonPathHelper.parseJsonPathElements( translator.getLiteralValue( jsonPath ) );
		sqlAppender.appendSql( "array" );
		char separator = '[';
		for ( JsonPathHelper.JsonPathElement pathElement : jsonPathElements ) {
			sqlAppender.appendSql( separator );
			if ( pathElement instanceof JsonPathHelper.JsonAttribute attribute ) {
				sqlAppender.appendSingleQuoteEscapedString( attribute.attribute() );
			}
			else if ( pathElement instanceof JsonPathHelper.JsonParameterIndexAccess ) {
				final String parameterName = ( (JsonPathHelper.JsonParameterIndexAccess) pathElement ).parameterName();
				throw new QueryException( "JSON path [" + jsonPath + "] uses parameter [" + parameterName + "] that is not passed" );
			}
			else {
				sqlAppender.appendSql( '\'' );
				sqlAppender.appendSql( ( (JsonPathHelper.JsonIndexAccess) pathElement ).index() );
				sqlAppender.appendSql( '\'' );
			}
			separator = ',';
		}
		sqlAppender.appendSql( "]::text[]" );
		sqlAppender.appendSql( ")) t(d,p))" );
	}

	private static boolean isJsonType(Expression expression) {
		final JdbcMappingContainer expressionType = expression.getExpressionType();
		return expressionType != null && expressionType.getSingleJdbcMapping().getJdbcType().isJson();
	}
}
