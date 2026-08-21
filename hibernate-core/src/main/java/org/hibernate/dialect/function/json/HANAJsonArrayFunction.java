/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.JsonNullBehavior;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.internal.util.NumberHelper.digitCount;

/**
 * SAP HANA json_array function.
 */
public class HANAJsonArrayFunction extends JsonArrayFunction {

	public HANAJsonArrayFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		if ( sqlAstArguments.isEmpty() ) {
			sqlAppender.appendSql( "'[]'" );
			return;
		}
		final SqlAstNode lastArgument = sqlAstArguments.get( sqlAstArguments.size() - 1 );
		final JsonNullBehavior nullBehavior;
		final int argumentsCount;
		if ( lastArgument instanceof JsonNullBehavior jsonNullBehavior ) {
			nullBehavior = jsonNullBehavior;
			argumentsCount = sqlAstArguments.size() - 1;
		}
		else {
			nullBehavior = JsonNullBehavior.ABSENT;
			argumentsCount = sqlAstArguments.size();
		}
		final int digits = digitCount( argumentsCount );
		final String prefix = "0".repeat( digits );

		final List<String> jsonArgumentFields = getJsonArgumentFields( prefix, sqlAstArguments, argumentsCount );
		sqlAppender.appendSql( "(select json_query(t.x,'$[0].*' with wrapper) from (" );
		replaceJsonArgumentsEscaping(
				sqlAppender,
				sqlAstArguments,
				walker,
				0,
				jsonArgumentFields,
				prefix,
				argumentsCount,
				nullBehavior
		);
		sqlAppender.appendSql( ") t)" );
	}

	private static void replaceJsonArgumentsEscaping(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			SqlAstTranslator<?> walker,
			int i,
			List<String> jsonArgumentFields,
			String prefix,
			int argumentsCount,
			JsonNullBehavior nullBehavior) {

		if ( i < jsonArgumentFields.size() ) {
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
					i + 1,
					jsonArgumentFields,
					prefix,
					argumentsCount,
					nullBehavior
			);
			sqlAppender.appendSql( ") t" );

			sqlAppender.appendSql( ",(select '" );
			sqlAppender.appendSql( valueExtractionPattern( jsonArgumentFields.get( i ) ) );
			sqlAppender.appendSql( "' x from sys.dummy) r" );
		}
		else {
			sqlAppender.appendSql( "select t.jsonresult x from (select " );
			renderArrayArguments( sqlAppender, sqlAstArguments, walker, prefix, argumentsCount );
			sqlAppender.appendSql( " from sys.dummy for json" );
			if ( nullBehavior == JsonNullBehavior.NULL ) {
				sqlAppender.appendSql( "('omitnull'='no')" );
			}
			sqlAppender.appendSql( ") t" );
		}
	}

	private static void renderArrayArguments(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			SqlAstTranslator<?> walker,
			String prefix,
			int argumentsCount) {
		sqlAstArguments.get( 0 ).accept( walker );
		sqlAppender.appendSql( " C" );
		sqlAppender.append( prefix, 1, prefix.length() );
		sqlAppender.appendSql( "0" );
		for ( int i = 1; i < argumentsCount; i++ ) {
			sqlAppender.appendSql( ',' );
			sqlAstArguments.get( i ).accept( walker );
			sqlAppender.appendSql( " C" );
			final String position = Integer.toString( i );
			sqlAppender.append( prefix, position.length(), prefix.length() );
			sqlAppender.appendSql( position );
		}
	}

	private List<String> getJsonArgumentFields(
			String zeroPrefix,
			List<? extends SqlAstNode> sqlAstArguments,
			int argumentsCount) {
		final ArrayList<String> jsonArgumentIndexes = new ArrayList<>();
		for ( int i = 0; i < argumentsCount; i++ ) {
			if ( ExpressionTypeHelper.isJson( sqlAstArguments.get( i ) ) ) {
				final String position = Integer.toString( i );
				jsonArgumentIndexes.add( "C" + zeroPrefix.substring( position.length() ) + position );
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
		return "(?<!\\\\)(?<=\"" + attributeName + "\":\").*?(?<!\\\\)(?=\")";
	}
}
