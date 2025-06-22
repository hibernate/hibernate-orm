/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.QueryException;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JsonPathPassingClause;

public class JsonPathHelper {

	public static List<JsonPathElement> parseJsonPathElements(String jsonPath) {
		if ( jsonPath.charAt( 0 ) != '$' ) {
			throw new QueryException( "Json path expression expression emulation only supports absolute paths i.e. must start with a '$' but got: " + jsonPath );
		}
		final var jsonPathElements = new ArrayList<JsonPathElement>();
		int startIndex;
		int dotIndex;

		if ( jsonPath.length() > 1 ) {
			if ( jsonPath.charAt( 1 ) == '.' ) {
				startIndex = 2;
			}
			else {
				final int bracketEndIndex = jsonPath.indexOf( ']' );
				parseBracket( jsonPath, 1, bracketEndIndex, jsonPathElements );
				startIndex = bracketEndIndex + 2;
			}

			try {
				while ( ( dotIndex = jsonPath.indexOf( '.', startIndex ) ) != -1 ) {
					parseAttribute( jsonPath, startIndex, dotIndex, jsonPathElements );
					startIndex = dotIndex + 1;
				}
				if ( startIndex < jsonPath.length() ) {
					parseAttribute( jsonPath, startIndex, jsonPath.length(), jsonPathElements );
				}
			}
			catch (Exception ex) {
				throw new QueryException( "Can't emulate non-simple json path expression: " + jsonPath, ex );
			}
		}
		return jsonPathElements;
	}

	public static void appendJsonPathConcatPassingClause(
			SqlAppender sqlAppender,
			Expression jsonPathExpression,
			JsonPathPassingClause passingClause, SqlAstTranslator<?> walker) {
		appendJsonPathConcatenatedPassingClause( sqlAppender, jsonPathExpression, passingClause, walker, "concat", "," );
	}

	public static void appendJsonPathDoublePipePassingClause(
			SqlAppender sqlAppender,
			Expression jsonPathExpression,
			JsonPathPassingClause passingClause,
			SqlAstTranslator<?> walker) {
		appendJsonPathConcatenatedPassingClause( sqlAppender, jsonPathExpression, passingClause, walker, "", "||" );
	}

	public static String inlinedJsonPathIncludingPassingClause(
			Expression jsonPathExpression,
			JsonPathPassingClause passingClause,
			SqlAstTranslator<?> walker) {
		return inlinedJsonPathIncludingPassingClause(
				walker.<String>getLiteralValue( jsonPathExpression ),
				passingClause,
				walker
		);
	}

	public static String inlinedJsonPathIncludingPassingClause(
			String jsonPath,
			JsonPathPassingClause passingClause,
			SqlAstTranslator<?> walker) {
		for ( Map.Entry<String, Expression> entry : passingClause.getPassingExpressions().entrySet() ) {
			jsonPath = jsonPath.replace( "$" + entry.getKey(), walker.getLiteralValue( entry.getValue() ).toString() );
		}
		return jsonPath;
	}

	public static void appendInlinedJsonPathIncludingPassingClause(
			SqlAppender sqlAppender,
			String prefix,
			Expression jsonPathExpression,
			JsonPathPassingClause passingClause,
			SqlAstTranslator<?> walker) {
		final String jsonPath = walker.getLiteralValue( jsonPathExpression );
		final String[] parts = jsonPath.split( "\\$" );
		sqlAppender.append( '\'' );
		sqlAppender.append( prefix );
		final int start;
		if ( parts[0].isEmpty() ) {
			start = 2;
			sqlAppender.append( '$' );
			sqlAppender.append( parts[1] );
		}
		else {
			start = 0;
		}
		for ( int i = start; i < parts.length; i++ ) {
			final String part = parts[i];

			final int parameterNameEndIndex = indexOfNonIdentifier( part, 0 );
			final String parameterName = part.substring( 0, parameterNameEndIndex );
			final Expression expression = passingClause.getPassingExpressions().get( parameterName );
			if ( expression == null ) {
				throw new QueryException( "JSON path [" + jsonPath + "] uses parameter [" + parameterName + "] that is not passed" );
			}
			final Object literalValue = walker.getLiteralValue( expression );
			if ( literalValue instanceof String string ) {
				appendLiteral( sqlAppender, 0, string );
			}
			else {
				sqlAppender.appendSql( String.valueOf( literalValue ) );
			}
			appendLiteral( sqlAppender, parameterNameEndIndex, part );
		}
		sqlAppender.appendSql( '\'' );
	}

	private static void appendLiteral(SqlAppender sqlAppender, int parameterNameEndIndex, String part) {
		for ( int j = parameterNameEndIndex; j < part.length(); j++ ) {
			final char c = part.charAt( j );
			if ( c == '\'') {
				sqlAppender.appendSql( '\'' );
			}
			sqlAppender.appendSql( c );
		}
	}

	private static void appendJsonPathConcatenatedPassingClause(
			SqlAppender sqlAppender,
			Expression jsonPathExpression,
			JsonPathPassingClause passingClause,
			SqlAstTranslator<?> walker,
			String concatStart,
			String concatCombine) {
		final String jsonPath = walker.getLiteralValue( jsonPathExpression );
		final String[] parts = jsonPath.split( "\\$" );
		sqlAppender.append( concatStart );
		final int start;
		String separator = "(";
		if ( parts[0].isEmpty() ) {
			start = 2;
			sqlAppender.append( separator );
			sqlAppender.append( "'$'" );
			sqlAppender.append( concatCombine );
			sqlAppender.appendSingleQuoteEscapedString( parts[1] );
			separator = concatCombine;
		}
		else {
			start = 0;
		}
		for ( int i = start; i < parts.length; i++ ) {
			final String part = parts[i];
			sqlAppender.append( separator );

			final int parameterNameEndIndex = indexOfNonIdentifier( part, 0 );
			final String parameterName = part.substring( 0, parameterNameEndIndex );
			final Expression expression = passingClause.getPassingExpressions().get( parameterName );
			if ( expression == null ) {
				throw new QueryException( "JSON path [" + jsonPath + "] uses parameter [" + parameterName + "] that is not passed" );
			}
			expression.accept( walker );
			sqlAppender.append( ',' );
			sqlAppender.appendSingleQuoteEscapedString( part.substring( parameterNameEndIndex ) );
			separator = concatCombine;
		}
		sqlAppender.appendSql( ')' );
	}

	private static void parseAttribute(String jsonPath, int startIndex, int endIndex, ArrayList<JsonPathElement> jsonPathElements) {
		final int bracketIndex = jsonPath.indexOf( '[', startIndex );
		if ( bracketIndex != -1 && bracketIndex < endIndex ) {
			jsonPathElements.add( new JsonAttribute( jsonPath.substring( startIndex, bracketIndex ) ) );
			parseBracket( jsonPath, bracketIndex, endIndex, jsonPathElements );
		}
		else {
			jsonPathElements.add( new JsonAttribute( jsonPath.substring( startIndex, endIndex ) ) );
		}
	}

	private static void parseBracket(String jsonPath, int bracketStartIndex, int endIndex, ArrayList<JsonPathElement> jsonPathElements) {
		assert jsonPath.charAt( bracketStartIndex ) == '[';
		final int bracketEndIndex = jsonPath.lastIndexOf( ']', endIndex );
		if ( bracketEndIndex < bracketStartIndex ) {
			throw new QueryException( "Can't emulate non-simple json path expression: " + jsonPath );
		}
		final int contentStartIndex = indexOfNonWhitespace( jsonPath, bracketStartIndex + 1 );
		final int contentEndIndex = lastIndexOfWhitespace( jsonPath, bracketEndIndex - 1 );
		if ( jsonPath.charAt( contentStartIndex ) == '$' ) {
			jsonPathElements.add( new JsonParameterIndexAccess( jsonPath.substring( contentStartIndex + 1, contentEndIndex ) ) );
		}
		else {
			final int index = Integer.parseInt( jsonPath, contentStartIndex, contentEndIndex, 10 );
			jsonPathElements.add( new JsonIndexAccess( index ) );
		}
	}

	public static int indexOfNonIdentifier(String jsonPath, int i) {
		while ( i < jsonPath.length() && Character.isJavaIdentifierPart( jsonPath.charAt( i ) ) ) {
			i++;
		}
		return i;
	}

	private static int indexOfNonWhitespace(String jsonPath, int i) {
		while ( i < jsonPath.length() && Character.isWhitespace( jsonPath.charAt( i ) ) ) {
			i++;
		}
		return i;
	}

	private static int lastIndexOfWhitespace(String jsonPath, int i) {
		while ( i > 0 && Character.isWhitespace( jsonPath.charAt( i ) ) ) {
			i--;
		}
		return i + 1;
	}

	public static void inlinePassingClause(
			List<JsonPathElement> jsonPathElements,
			JsonPathPassingClause passingClause,
			SqlAstTranslator<?> walker) {
		for ( int i = 0; i < jsonPathElements.size(); i++ ) {
			final JsonPathElement jsonPathElement = jsonPathElements.get( i );
			if ( jsonPathElement instanceof JsonParameterIndexAccess parameterIndexAccess ) {
				final Expression expression = passingClause.getPassingExpressions()
						.get( parameterIndexAccess.parameterName() );
				if ( expression == null ) {
					throw new QueryException( "JSON path [" + toJsonPath( jsonPathElements ) + "] uses parameter [" + parameterIndexAccess.parameterName() + "] that is not passed" );
				}
				jsonPathElements.set( i, new JsonIndexAccess( walker.getLiteralValue( expression ) ) );
			}
		}
	}

	public static String toJsonPath(List<JsonPathElement> pathElements) {
		return toJsonPath( pathElements, 0, pathElements.size() );
	}

	public static String toJsonPath(List<JsonPathElement> pathElements, int start, int end) {
		final StringBuilder jsonPath = new StringBuilder();
		jsonPath.append( "$" );
		for ( int i = start; i < end; i++ ) {
			final JsonPathElement jsonPathElement = pathElements.get( i );
			if ( jsonPathElement instanceof JsonAttribute pathAttribute ) {
				jsonPath.append( '.' );
				jsonPath.append( pathAttribute.attribute() );
			}
			else if ( jsonPathElement instanceof JsonParameterIndexAccess parameterIndexAccess ) {
				jsonPath.append( "[$" );
				jsonPath.append( parameterIndexAccess.parameterName() );
				jsonPath.append( "]" );
			}
			else {
				assert jsonPathElement instanceof JsonIndexAccess;
				jsonPath.append( "[" );
				jsonPath.append( ( (JsonIndexAccess) jsonPathElement ).index() );
				jsonPath.append( "]" );
			}
		}
		return jsonPath.toString();
	}

	public sealed interface JsonPathElement {}
	public record JsonAttribute(String attribute) implements JsonPathElement {}
	public record JsonIndexAccess(int index) implements JsonPathElement {}
	public record JsonParameterIndexAccess(String parameterName) implements JsonPathElement {}
}
