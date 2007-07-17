//$Id: Template.java 9922 2006-05-10 16:58:09Z steve.ebersole@jboss.com $
package org.hibernate.sql;

import java.util.HashSet;
import java.util.StringTokenizer;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.SQLFunctionRegistry;
import org.hibernate.util.StringHelper;

/**
 * Parses SQL fragments specified in mapping documents
 *
 * @author Gavin King
 */
public final class Template {

	private static final java.util.Set KEYWORDS = new HashSet();
	private static final java.util.Set BEFORE_TABLE_KEYWORDS = new HashSet();
	private static final java.util.Set FUNCTION_KEYWORDS = new HashSet();
	static {
		KEYWORDS.add("and");
		KEYWORDS.add("or");
		KEYWORDS.add("not");
		KEYWORDS.add("like");
		KEYWORDS.add("is");
		KEYWORDS.add("in");
		KEYWORDS.add("between");
		KEYWORDS.add("null");
		KEYWORDS.add("select");
		KEYWORDS.add("distinct");
		KEYWORDS.add("from");
		KEYWORDS.add("join");
		KEYWORDS.add("inner");
		KEYWORDS.add("outer");
		KEYWORDS.add("left");
		KEYWORDS.add("right");
		KEYWORDS.add("on");
		KEYWORDS.add("where");
		KEYWORDS.add("having");
		KEYWORDS.add("group");
		KEYWORDS.add("order");
		KEYWORDS.add("by");
		KEYWORDS.add("desc");
		KEYWORDS.add("asc");
		KEYWORDS.add("limit");
		KEYWORDS.add("any");
		KEYWORDS.add("some");
		KEYWORDS.add("exists");
		KEYWORDS.add("all");
		
		BEFORE_TABLE_KEYWORDS.add("from");
		BEFORE_TABLE_KEYWORDS.add("join");
		
		FUNCTION_KEYWORDS.add("as");
		FUNCTION_KEYWORDS.add("leading");
		FUNCTION_KEYWORDS.add("trailing");
		FUNCTION_KEYWORDS.add("from");
		FUNCTION_KEYWORDS.add("case");
		FUNCTION_KEYWORDS.add("when");
		FUNCTION_KEYWORDS.add("then");
		FUNCTION_KEYWORDS.add("else");
		FUNCTION_KEYWORDS.add("end");
	}

	public static final String TEMPLATE = "$PlaceHolder$";

	private Template() {}

	public static String renderWhereStringTemplate(String sqlWhereString, Dialect dialect, SQLFunctionRegistry functionRegistry) {
		return renderWhereStringTemplate(sqlWhereString, TEMPLATE, dialect, functionRegistry);
	}

	/**
	 * Same functionality as {@link #renderWhereStringTemplate(String, String, Dialect, SQLFunctionRegistry)},
	 * except that a SQLFunctionRegistry is not provided (i.e., only the dialect-defined functions are
	 * considered).  This is only intended for use by the annotations project until the
	 * many-to-many/map-key-from-target-table feature is pulled into core.
	 *
	 * @deprecated Only intended for annotations usage; use {@link #renderWhereStringTemplate(String, String, Dialect, SQLFunctionRegistry)} instead
	 */
	public static String renderWhereStringTemplate(String sqlWhereString, String placeholder, Dialect dialect) {
		return renderWhereStringTemplate( sqlWhereString, placeholder, dialect, new SQLFunctionRegistry( dialect, java.util.Collections.EMPTY_MAP ) );
	}

	/**
	 * Takes the where condition provided in the mapping attribute and interpolates the alias. 
	 * Handles subselects, quoted identifiers, quoted strings, expressions, SQL functions, 
	 * named parameters.
	 *
	 * @param sqlWhereString The string into which to interpolate the placeholder value
	 * @param placeholder The value to be interpolated into the the sqlWhereString
	 * @param dialect The dialect to apply
	 * @param functionRegistry The registry of all sql functions
	 * @return The rendered sql fragment
	 */
	public static String renderWhereStringTemplate(String sqlWhereString, String placeholder, Dialect dialect, SQLFunctionRegistry functionRegistry ) {
		//TODO: make this a bit nicer
		String symbols = new StringBuffer()
			.append("=><!+-*/()',|&`")
			.append(StringHelper.WHITESPACE)
			.append( dialect.openQuote() )
			.append( dialect.closeQuote() )
			.toString();
		StringTokenizer tokens = new StringTokenizer(sqlWhereString, symbols, true);
		
		StringBuffer result = new StringBuffer();
		boolean quoted = false;
		boolean quotedIdentifier = false;
		boolean beforeTable = false;
		boolean inFromClause = false;
		boolean afterFromTable = false;
		
		boolean hasMore = tokens.hasMoreTokens();
		String nextToken = hasMore ? tokens.nextToken() : null;
		while (hasMore) {
			String token = nextToken;
			String lcToken = token.toLowerCase();
			hasMore = tokens.hasMoreTokens();
			nextToken = hasMore ? tokens.nextToken() : null;
			
			boolean isQuoteCharacter = false;
			
			if ( !quotedIdentifier && "'".equals(token) ) {
				quoted = !quoted;
				isQuoteCharacter = true;
			}
			
			if ( !quoted ) {
				
				boolean isOpenQuote;
				if ( "`".equals(token) ) {
					isOpenQuote = !quotedIdentifier;
					token = lcToken = isOpenQuote ? 
						new Character( dialect.openQuote() ).toString() :
						new Character( dialect.closeQuote() ).toString();
					quotedIdentifier = isOpenQuote;	
					isQuoteCharacter = true;
				}
				else if ( !quotedIdentifier && ( dialect.openQuote()==token.charAt(0) ) ) {
					isOpenQuote = true;
					quotedIdentifier = true;	
					isQuoteCharacter = true;
				}
				else if ( quotedIdentifier && ( dialect.closeQuote()==token.charAt(0) ) ) {
					quotedIdentifier = false;
					isQuoteCharacter = true;
					isOpenQuote = false;
				}
				else {
					isOpenQuote = false;
				}
				
				if (isOpenQuote) {
					result.append(placeholder).append('.');
				}
				
			}
	
			boolean quotedOrWhitespace = quoted || 
				quotedIdentifier || 
				isQuoteCharacter || 
				Character.isWhitespace( token.charAt(0) );
			
			if (quotedOrWhitespace) {
				result.append(token);
			}
			else if (beforeTable) {
				result.append(token);
				beforeTable = false;
				afterFromTable = true;
			}
			else if (afterFromTable) {
				if ( !"as".equals(lcToken) ) afterFromTable = false;
				result.append(token);
			}
			else if ( isNamedParameter(token) ) {
				result.append(token);
			}
			else if (
				isIdentifier(token, dialect) &&
				!isFunctionOrKeyword(lcToken, nextToken, dialect , functionRegistry)
			) {
				result.append(placeholder)
					.append('.')
					.append( dialect.quote(token) );
			}
			else {
				if ( BEFORE_TABLE_KEYWORDS.contains(lcToken) ) {
					beforeTable = true;
					inFromClause = true;
				}
				else if ( inFromClause && ",".equals(lcToken) ) {
					beforeTable = true;
				}
				result.append(token);
			}
			
			if ( //Yuck:
					inFromClause && 
					KEYWORDS.contains(lcToken) && //"as" is not in KEYWORDS
					!BEFORE_TABLE_KEYWORDS.contains(lcToken)
			) { 
				inFromClause = false;
			}

		}
		return result.toString();
	}

	/**
	 * Takes order by clause provided in the mapping attribute and interpolates the alias.
	 * Handles asc, desc, SQL functions, quoted identifiers.
	 */
	public static String renderOrderByStringTemplate(String sqlOrderByString, Dialect dialect, SQLFunctionRegistry functionRegistry) {
		//TODO: make this a bit nicer
		String symbols = new StringBuffer()
			.append("=><!+-*/()',|&`")
			.append(StringHelper.WHITESPACE)
			.append( dialect.openQuote() )
			.append( dialect.closeQuote() )
			.toString();
		StringTokenizer tokens = new StringTokenizer(sqlOrderByString, symbols, true);
		
		StringBuffer result = new StringBuffer();
		boolean quoted = false;
		boolean quotedIdentifier = false;
		
		boolean hasMore = tokens.hasMoreTokens();
		String nextToken = hasMore ? tokens.nextToken() : null;
		while (hasMore) {
			String token = nextToken;
			String lcToken = token.toLowerCase();
			hasMore = tokens.hasMoreTokens();
			nextToken = hasMore ? tokens.nextToken() : null;
			
			boolean isQuoteCharacter = false;
			
			if ( !quotedIdentifier && "'".equals(token) ) {
				quoted = !quoted;
				isQuoteCharacter = true;
			}
			
			if ( !quoted ) {
				
				boolean isOpenQuote;
				if ( "`".equals(token) ) {
					isOpenQuote = !quotedIdentifier;
					token = lcToken = isOpenQuote ? 
						new Character( dialect.openQuote() ).toString() :
						new Character( dialect.closeQuote() ).toString();
					quotedIdentifier = isOpenQuote;	
					isQuoteCharacter = true;
				}
				else if ( !quotedIdentifier && ( dialect.openQuote()==token.charAt(0) ) ) {
					isOpenQuote = true;
					quotedIdentifier = true;	
					isQuoteCharacter = true;
				}
				else if ( quotedIdentifier && ( dialect.closeQuote()==token.charAt(0) ) ) {
					quotedIdentifier = false;
					isQuoteCharacter = true;
					isOpenQuote = false;
				}
				else {
					isOpenQuote = false;
				}
				
				if (isOpenQuote) {
					result.append(TEMPLATE).append('.');
				}
				
			}
	
			boolean quotedOrWhitespace = quoted || 
				quotedIdentifier || 
				isQuoteCharacter || 
				Character.isWhitespace( token.charAt(0) );
			
			if (quotedOrWhitespace) {
				result.append(token);
			}
			else if (
				isIdentifier(token, dialect) &&
				!isFunctionOrKeyword(lcToken, nextToken, dialect, functionRegistry)
			) {
				result.append(TEMPLATE)
					.append('.')
					.append( dialect.quote(token) );
			}
			else {
				result.append(token);
			}
		}
		return result.toString();
	}
	
	private static boolean isNamedParameter(String token) {
		return token.startsWith(":");
	}

	private static boolean isFunctionOrKeyword(String lcToken, String nextToken, Dialect dialect, SQLFunctionRegistry functionRegistry) {
		return "(".equals(nextToken) ||
			KEYWORDS.contains(lcToken) ||
			functionRegistry.hasFunction(lcToken) ||
			dialect.getKeywords().contains(lcToken) ||
			FUNCTION_KEYWORDS.contains(lcToken);
	}

	private static boolean isIdentifier(String token, Dialect dialect) {
		return token.charAt(0)=='`' || ( //allow any identifier quoted with backtick
			Character.isLetter( token.charAt(0) ) && //only recognizes identifiers beginning with a letter
			token.indexOf('.') < 0
		);
	}

	
}
