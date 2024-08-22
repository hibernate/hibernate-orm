/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.json;

import java.util.ArrayList;
import java.util.List;

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
			Object literalValue = walker.getLiteralValue( expression );
			if ( literalValue instanceof String ) {
				appendLiteral( sqlAppender, 0, (String) literalValue );
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

	public sealed interface JsonPathElement {}
	public record JsonAttribute(String attribute) implements JsonPathElement {}
	public record JsonIndexAccess(int index) implements JsonPathElement {}
	public record JsonParameterIndexAccess(String parameterName) implements JsonPathElement {}
}
