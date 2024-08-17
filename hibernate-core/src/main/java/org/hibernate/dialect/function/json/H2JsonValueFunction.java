/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.json;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JsonValueEmptyBehavior;
import org.hibernate.sql.ast.tree.expression.JsonValueErrorBehavior;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * H2 json_value function.
 */
public class H2JsonValueFunction extends JsonValueFunction {

	public H2JsonValueFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, false );
	}

	@Override
	protected void render(
			SqlAppender sqlAppender,
			JsonValueArguments arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		// Json dereference errors by default if the JSON is invalid
		if ( arguments.errorBehavior() != null && arguments.errorBehavior() != JsonValueErrorBehavior.ERROR ) {
			throw new QueryException( "Can't emulate on error clause on H2" );
		}
		if ( arguments.emptyBehavior() == JsonValueEmptyBehavior.ERROR ) {
			throw new QueryException( "Can't emulate error on empty clause on H2" );
		}
		final Expression defaultExpression = arguments.emptyBehavior() == null
				? null
				: arguments.emptyBehavior().getDefaultExpression();
		if ( arguments.returningType() != null ) {
			sqlAppender.appendSql( "cast(" );
		}
		final String jsonPath;
		try {
			jsonPath = walker.getLiteralValue( arguments.jsonPath() );
		}
		catch (Exception ex) {
			throw new QueryException( "H2 json_value only support literal json paths, but got " + arguments.jsonPath() );
		}

		sqlAppender.appendSql( "stringdecode(btrim(nullif(" );
		if ( defaultExpression != null ) {
			sqlAppender.appendSql( "coalesce(" );
		}
		renderJsonPath( sqlAppender, arguments.jsonDocument(), walker, jsonPath );
		if ( defaultExpression != null ) {
			sqlAppender.appendSql( ",cast(" );
			defaultExpression.accept( walker );
			sqlAppender.appendSql( " as varchar))" );
		}
		sqlAppender.appendSql( ",'null'),'\"'))");

		if ( arguments.returningType() != null ) {
			sqlAppender.appendSql( " as " );
			arguments.returningType().accept( walker );
			sqlAppender.appendSql( ')' );
		}
	}

	private void renderJsonPath(
			SqlAppender sqlAppender,
			SqlAstNode jsonDocument,
			SqlAstTranslator<?> walker,
			String jsonPath) {
		sqlAppender.appendSql( "cast(" );

		final List<JsonPathHelper.JsonPathElement> jsonPathElements = JsonPathHelper.parseJsonPathElements( jsonPath );
		final boolean needsWrapping = jsonPathElements.get( 0 ) instanceof JsonPathHelper.JsonAttribute;
		if ( needsWrapping ) {
			sqlAppender.appendSql( '(' );
		}
		jsonDocument.accept( walker );
		if ( needsWrapping ) {
			sqlAppender.appendSql( ')' );
		}
		for ( int i = 0; i < jsonPathElements.size(); i++ ) {
			final JsonPathHelper.JsonPathElement jsonPathElement = jsonPathElements.get( i );
			if ( jsonPathElement instanceof JsonPathHelper.JsonAttribute attribute ) {
				final String attributeName = attribute.attribute();
				appendInDoubleQuotes( sqlAppender, attributeName );
			}
			else {
				sqlAppender.appendSql( '[' );
				sqlAppender.appendSql( ( (JsonPathHelper.JsonIndexAccess) jsonPathElement ).index() + 1 );
				sqlAppender.appendSql( ']' );
			}
		}
		sqlAppender.appendSql( " as varchar)" );
	}

	private static void appendInDoubleQuotes(SqlAppender sqlAppender, String attributeName) {
		sqlAppender.appendSql( ".\"" );
		for ( int j = 0; j < attributeName.length(); j++ ) {
			final char c = attributeName.charAt( j );
			if ( c == '"' ) {
				sqlAppender.appendSql( '"' );
			}
			sqlAppender.appendSql( c );
		}
		sqlAppender.appendSql( '"' );
	}
}
