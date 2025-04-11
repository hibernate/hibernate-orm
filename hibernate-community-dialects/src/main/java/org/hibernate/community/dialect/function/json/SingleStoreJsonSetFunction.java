/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.function.json;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.dialect.function.json.AbstractJsonSetFunction;
import org.hibernate.dialect.function.json.JsonPathHelper;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.UnparsedNumericLiteral;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * SingleStore json_set function.
 * <p>
 * The behavior of creating missing keypaths is available by setting the json_compatibility_level to 8.0
 */
public class SingleStoreJsonSetFunction extends AbstractJsonSetFunction {

	public SingleStoreJsonSetFunction(TypeConfiguration typeConfiguration) {
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
		final List<JsonPathHelper.JsonPathElement> jsonPathElements = JsonPathHelper.parseJsonPathElements( translator.getLiteralValue(
				jsonPath ) );
		final SqlAstNode value = arguments.get( 2 );
		sqlAppender.appendSql( "json_set_" );
		sqlAppender.appendSql( isNumeric( value ) ? "double(" : "string(" );
		json.accept( translator );
		for ( JsonPathHelper.JsonPathElement pathElement : jsonPathElements ) {
			sqlAppender.appendSql( ',' );
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
		}
		sqlAppender.appendSql( ',' );
		value.accept( translator );
		sqlAppender.appendSql( ')' );
	}

	private static boolean isNumeric(SqlAstNode value) {
		return value instanceof UnparsedNumericLiteral<?>;
	}

}
