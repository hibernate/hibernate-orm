/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.sql;

import java.util.HashSet;
import java.util.StringTokenizer;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.SQLFunctionRegistry;
import org.hibernate.util.StringHelper;
import org.hibernate.sql.ordering.antlr.ColumnMapper;
import org.hibernate.sql.ordering.antlr.TranslationContext;
import org.hibernate.sql.ordering.antlr.OrderByFragmentTranslator;
import org.hibernate.engine.SessionFactoryImplementor;

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
		KEYWORDS.add("union");
		KEYWORDS.add("minus");

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

	public static class NoOpColumnMapper implements ColumnMapper {
		public static final NoOpColumnMapper INSTANCE = new NoOpColumnMapper();
		public String[] map(String reference) {
			return new String[] { reference };
		}
	}

	/**
	 * Performs order-by template rendering without {@link ColumnMapper column mapping}.  An <tt>ORDER BY</tt> template
	 * has all column references "qualified" with a placeholder identified by {@link Template#TEMPLATE}
	 *
	 * @param orderByFragment The order-by fragment to render.
	 * @param dialect The SQL dialect being used.
	 * @param functionRegistry The SQL function registry
	 *
	 * @return The rendered <tt>ORDER BY</tt> template.
	 * 
	 * @see #renderOrderByStringTemplate(String,ColumnMapper,SessionFactoryImplementor,Dialect,SQLFunctionRegistry)
	 */
	public static String renderOrderByStringTemplate(
			String orderByFragment,
			Dialect dialect,
			SQLFunctionRegistry functionRegistry) {
		return renderOrderByStringTemplate(
				orderByFragment,
				NoOpColumnMapper.INSTANCE,
				null,
				dialect,
				functionRegistry
		);
	}

	/**
	 * Performs order-by template rendering allowing {@link ColumnMapper column mapping}.  An <tt>ORDER BY</tt> template
	 * has all column references "qualified" with a placeholder identified by {@link Template#TEMPLATE} which can later
	 * be used to easily inject the SQL alias.
	 *
	 * @param orderByFragment The order-by fragment to render.
	 * @param columnMapper The column mapping strategy to use.
	 * @param sessionFactory The session factory.
	 * @param dialect The SQL dialect being used.
	 * @param functionRegistry The SQL function registry
	 *
	 * @return The rendered <tt>ORDER BY</tt> template.
	 */
	public static String renderOrderByStringTemplate(
			String orderByFragment,
			final ColumnMapper columnMapper,
			final SessionFactoryImplementor sessionFactory,
			final Dialect dialect,
			final SQLFunctionRegistry functionRegistry) {
		TranslationContext context = new TranslationContext() {
			public SessionFactoryImplementor getSessionFactory() {
				return sessionFactory;
			}

			public Dialect getDialect() {
				return dialect;
			}

			public SQLFunctionRegistry getSqlFunctionRegistry() {
				return functionRegistry;
			}

			public ColumnMapper getColumnMapper() {
				return columnMapper;
			}
		};

		OrderByFragmentTranslator translator = new OrderByFragmentTranslator( context );
		return translator.render( orderByFragment );
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
