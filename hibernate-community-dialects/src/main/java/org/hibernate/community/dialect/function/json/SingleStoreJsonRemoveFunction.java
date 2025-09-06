/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.function.json;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.dialect.function.json.AbstractJsonRemoveFunction;
import org.hibernate.dialect.function.json.JsonPathHelper;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * SingleStore json_remove function.
 */
public class SingleStoreJsonRemoveFunction extends AbstractJsonRemoveFunction {

	public SingleStoreJsonRemoveFunction(TypeConfiguration typeConfiguration) {
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
		sqlAppender.appendSql( "json_delete_key(" );
		json.accept( translator );
		for ( JsonPathHelper.JsonPathElement pathElement : jsonPathElements ) {
			sqlAppender.appendSql( ',' );
			if ( pathElement instanceof JsonPathHelper.JsonAttribute attribute ) {
				sqlAppender.appendSingleQuoteEscapedString( attribute.attribute() );
			}
			else if ( pathElement instanceof JsonPathHelper.JsonParameterIndexAccess indexParameter) {
				final String parameterName = indexParameter.parameterName();
				throw new QueryException( "JSON path [" + jsonPath + "] uses parameter [" + parameterName + "] that is not passed" );
			}
			else {
				sqlAppender.appendSql( '\'' );
				sqlAppender.appendSql( ( (JsonPathHelper.JsonIndexAccess) pathElement ).index() );
				sqlAppender.appendSql( '\'' );
			}
		}
		sqlAppender.appendSql( ')' );
	}
}
