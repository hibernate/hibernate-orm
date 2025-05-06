/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JsonNullBehavior;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * SAP HANA json_object function.
 */
public class HANAJsonObjectFunction extends JsonObjectFunction {

	public HANAJsonObjectFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, true );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		if ( sqlAstArguments.isEmpty() ) {
			sqlAppender.appendSql( "'{}'" );
			return;
		}
		final JsonNullBehavior nullBehavior;
		final int argumentsCount;
		if ( ( sqlAstArguments.size() & 1 ) == 1 ) {
			nullBehavior = (JsonNullBehavior) sqlAstArguments.get( sqlAstArguments.size() - 1 );
			argumentsCount = sqlAstArguments.size() - 1;
		}
		else {
			nullBehavior = JsonNullBehavior.NULL;
			argumentsCount = sqlAstArguments.size();
		}
		final List<String> jsonArgumentFields = getJsonArgumentFields( sqlAstArguments, argumentsCount, walker );
		sqlAppender.appendSql( '(' );
		replaceJsonArgumentsEscaping(
				sqlAppender,
				sqlAstArguments,
				walker,
				0,
				jsonArgumentFields,
				argumentsCount,
				nullBehavior
		);
		sqlAppender.appendSql( ')' );
	}

	private static void replaceJsonArgumentsEscaping(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			SqlAstTranslator<?> walker,
			int jsonArg,
			List<String> jsonArgumentFields,
			int argumentsCount,
			JsonNullBehavior nullBehavior) {

		if ( jsonArg < jsonArgumentFields.size() ) {
			// Take the substring before the match
			sqlAppender.appendSql( "select substring(t.x, 1, locate_regexpr(r.x in t.x) - 2)" );

			// The match itself after replacing double backslashes and backslash escaped quotes
			sqlAppender.appendSql( "|| replace(replace(substr_regexpr(r.x in t.x),'\\\\','\\'),'\\\"','\"')" );

			// And the rest of the string after the match
			sqlAppender.appendSql( "|| substring(t.x, locate_regexpr(r.x in t.x) + length(substr_regexpr(r.x in t.x)) + 1) x");

			sqlAppender.appendSql( " from (" );
			replaceJsonArgumentsEscaping(
					sqlAppender,
					sqlAstArguments,
					walker,
					jsonArg + 1,
					jsonArgumentFields,
					argumentsCount,
					nullBehavior
			);
			sqlAppender.appendSql( ") t" );

			sqlAppender.appendSql( ",(select '" );
			sqlAppender.appendSql( valueExtractionPattern( jsonArgumentFields.get( jsonArg ) ) );
			sqlAppender.appendSql( "' x from sys.dummy) r" );
		}
		else {
			sqlAppender.appendSql( "select t.jsonresult x from (select" );
			char separator = ' ';
			for ( int i = 0; i < argumentsCount; i += 2 ) {
				sqlAppender.appendSql( separator );
				final SqlAstNode key = sqlAstArguments.get( i );
				final SqlAstNode value = sqlAstArguments.get( i + 1 );
				value.accept( walker );
				sqlAppender.appendSql( ' ' );
				final String literalValue = walker.getLiteralValue( (Expression) key );
				sqlAppender.appendDoubleQuoteEscapedString( literalValue );
				separator = ',';
			}
			sqlAppender.appendSql( " from sys.dummy for json('arraywrap'='no'" );
			if ( nullBehavior == JsonNullBehavior.NULL ) {
				sqlAppender.appendSql( ",'omitnull'='no'" );
			}
			sqlAppender.appendSql( ")) t" );
		}
	}

	private List<String> getJsonArgumentFields(
			List<? extends SqlAstNode> sqlAstArguments,
			int argumentsCount,
			SqlAstTranslator<?> walker) {
		final ArrayList<String> jsonArgumentIndexes = new ArrayList<>();
		for ( int i = 0; i < argumentsCount; i += 2 ) {
			if ( ExpressionTypeHelper.isJson( sqlAstArguments.get( i + 1 ) ) ) {
				jsonArgumentIndexes.add( walker.getLiteralValue( (Expression) sqlAstArguments.get( i ) )  );
			}
		}
		return jsonArgumentIndexes;
	}

	private static String valueExtractionPattern(String attributeName) {
		// (?<!\\) ensures the next character is not preceded by a backslash
		// (?<=\"" + attributeName + "\":\") ensures the match is preceded by `"attributeName":"`
		// .*? is a non-greedy match for all chars
		// (?<!\\) ensures that the next character is not preceded by a backslash
		// (?=") ensures that the character after our match is a double quote
		return "(?<!\\\\)(?<=\"" + Pattern.quote( attributeName ) + "\":\").*?(?<!\\\\)(?=\")";
	}
}
