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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.dialect.function.SQLFunctionRegistry;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.sql.ordering.antlr.ColumnMapper;
import org.hibernate.sql.ordering.antlr.OrderByAliasResolver;
import org.hibernate.sql.ordering.antlr.OrderByFragmentTranslator;
import org.hibernate.sql.ordering.antlr.OrderByTranslation;
import org.hibernate.sql.ordering.antlr.SqlValueReference;
import org.hibernate.sql.ordering.antlr.TranslationContext;

/**
 * Parses SQL fragments specified in mapping documents
 *
 * @author Gavin King
 */
public final class Template {

	private static final Set<String> KEYWORDS = new HashSet<String>();
	private static final Set<String> BEFORE_TABLE_KEYWORDS = new HashSet<String>();
	private static final Set<String> FUNCTION_KEYWORDS = new HashSet<String>();
	static {
		KEYWORDS.add("and");
		KEYWORDS.add("or");
		KEYWORDS.add("not");
		KEYWORDS.add("like");
		KEYWORDS.add("escape");
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
	@Deprecated
    @SuppressWarnings({ "JavaDoc" })
	public static String renderWhereStringTemplate(String sqlWhereString, String placeholder, Dialect dialect) {
		return renderWhereStringTemplate(
				sqlWhereString,
				placeholder,
				dialect,
				new SQLFunctionRegistry( dialect, java.util.Collections.<String, SQLFunction>emptyMap() )
		);
	}

	/**
	 * Takes the where condition provided in the mapping attribute and interpolates the alias.
	 * Handles sub-selects, quoted identifiers, quoted strings, expressions, SQL functions,
	 * named parameters.
	 *
	 * @param sqlWhereString The string into which to interpolate the placeholder value
	 * @param placeholder The value to be interpolated into the the sqlWhereString
	 * @param dialect The dialect to apply
	 * @param functionRegistry The registry of all sql functions
	 * @return The rendered sql fragment
	 */
	public static String renderWhereStringTemplate(String sqlWhereString, String placeholder, Dialect dialect, SQLFunctionRegistry functionRegistry ) {

		// IMPL NOTE : The basic process here is to tokenize the incoming string and to iterate over each token
		//		in turn.  As we process each token, we set a series of flags used to indicate the type of context in
		// 		which the tokens occur.  Depending on the state of those flags we decide whether we need to qualify
		//		identifier references.

		String symbols = new StringBuilder()
				.append( "=><!+-*/()',|&`" )
				.append( StringHelper.WHITESPACE )
				.append( dialect.openQuote() )
				.append( dialect.closeQuote() )
				.toString();
		StringTokenizer tokens = new StringTokenizer( sqlWhereString, symbols, true );
		StringBuilder result = new StringBuilder();

		boolean quoted = false;
		boolean quotedIdentifier = false;
		boolean beforeTable = false;
		boolean inFromClause = false;
		boolean afterFromTable = false;

		boolean hasMore = tokens.hasMoreTokens();
		String nextToken = hasMore ? tokens.nextToken() : null;
		while ( hasMore ) {
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
					token = lcToken = isOpenQuote
							? Character.toString( dialect.openQuote() )
							: Character.toString( dialect.closeQuote() );
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

				if ( isOpenQuote ) {
					result.append( placeholder ).append( '.' );
				}
			}

			// Special processing for ANSI SQL EXTRACT function
			if ( "extract".equals( lcToken ) && "(".equals( nextToken ) ) {
				final String field = extractUntil( tokens, "from" );
				final String source = renderWhereStringTemplate(
						extractUntil( tokens, ")" ),
						placeholder,
						dialect,
						functionRegistry
				);
				result.append( "extract(" ).append( field ).append( " from " ).append( source ).append( ')' );

				hasMore = tokens.hasMoreTokens();
				nextToken = hasMore ? tokens.nextToken() : null;

				continue;
			}

			// Special processing for ANSI SQL TRIM function
			if ( "trim".equals( lcToken ) && "(".equals( nextToken ) ) {
				List<String> operands = new ArrayList<String>();
				StringBuilder builder = new StringBuilder();

				boolean hasMoreOperands = true;
				String operandToken = tokens.nextToken();
				boolean quotedOperand = false;
				while ( hasMoreOperands ) {
					final boolean isQuote = "'".equals( operandToken );
					if ( isQuote ) {
						quotedOperand = !quotedOperand;
						if ( !quotedOperand ) {
							operands.add( builder.append( '\'' ).toString() );
							builder.setLength( 0 );
						}
						else {
							builder.append( '\'' );
						}
					}
					else if ( quotedOperand ) {
						builder.append( operandToken );
					}
					else if ( operandToken.length() == 1 && Character.isWhitespace( operandToken.charAt( 0 ) ) ) {
						// do nothing
					}
					else {
						operands.add( operandToken );
					}
					operandToken = tokens.nextToken();
					hasMoreOperands = tokens.hasMoreTokens() && ! ")".equals( operandToken );
				}

				TrimOperands trimOperands = new TrimOperands( operands );
				result.append( "trim(" );
				if ( trimOperands.trimSpec != null ) {
					result.append( trimOperands.trimSpec ).append( ' ' );
				}
				if ( trimOperands.trimChar != null ) {
					if ( trimOperands.trimChar.startsWith( "'" ) && trimOperands.trimChar.endsWith( "'" ) ) {
						result.append( trimOperands.trimChar );
					}
					else {
						result.append(
								renderWhereStringTemplate( trimOperands.trimSpec, placeholder, dialect, functionRegistry )
						);
					}
					result.append( ' ' );
				}
				if ( trimOperands.from != null ) {
					result.append( trimOperands.from ).append( ' ' );
				}
				else if ( trimOperands.trimSpec != null || trimOperands.trimChar != null ) {
					// I think ANSI SQL says that the 'from' is not optional if either trim-spec or trim-char are specified
					result.append( "from " );
				}

				result.append( renderWhereStringTemplate( trimOperands.trimSource, placeholder, dialect, functionRegistry ) )
						.append( ')' );

				hasMore = tokens.hasMoreTokens();
				nextToken = hasMore ? tokens.nextToken() : null;

				continue;
			}

			boolean quotedOrWhitespace = quoted || quotedIdentifier || isQuoteCharacter
					|| Character.isWhitespace( token.charAt(0) );

			if ( quotedOrWhitespace ) {
				result.append( token );
			}
			else if ( beforeTable ) {
				result.append( token );
				beforeTable = false;
				afterFromTable = true;
			}
			else if ( afterFromTable ) {
				if ( !"as".equals(lcToken) ) {
					afterFromTable = false;
				}
				result.append(token);
			}
			else if ( isNamedParameter(token) ) {
				result.append(token);
			}
			else if ( isIdentifier(token)
					&& !isFunctionOrKeyword(lcToken, nextToken, dialect , functionRegistry) ) {
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

			//Yuck:
			if ( inFromClause
					&& KEYWORDS.contains( lcToken ) //"as" is not in KEYWORDS
					&& !BEFORE_TABLE_KEYWORDS.contains( lcToken ) ) {
				inFromClause = false;
			}
		}

		return result.toString();
	}

//	/**
//	 * Takes the where condition provided in the mapping attribute and interpolates the alias.
//	 * Handles sub-selects, quoted identifiers, quoted strings, expressions, SQL functions,
//	 * named parameters.
//	 *
//	 * @param sqlWhereString The string into which to interpolate the placeholder value
//	 * @param placeholder The value to be interpolated into the the sqlWhereString
//	 * @param dialect The dialect to apply
//	 * @param functionRegistry The registry of all sql functions
//	 *
//	 * @return The rendered sql fragment
//	 */
//	public static String renderWhereStringTemplate(
//			String sqlWhereString,
//			String placeholder,
//			Dialect dialect,
//			SQLFunctionRegistry functionRegistry) {
//
//		// IMPL NOTE : The basic process here is to tokenize the incoming string and to iterate over each token
//		//		in turn.  As we process each token, we set a series of flags used to indicate the type of context in
//		// 		which the tokens occur.  Depending on the state of those flags we decide whether we need to qualify
//		//		identifier references.
//
//		final String dialectOpenQuote = Character.toString( dialect.openQuote() );
//		final String dialectCloseQuote = Character.toString( dialect.closeQuote() );
//
//		String symbols = new StringBuilder()
//				.append( "=><!+-*/()',|&`" )
//				.append( StringHelper.WHITESPACE )
//				.append( dialect.openQuote() )
//				.append( dialect.closeQuote() )
//				.toString();
//		StringTokenizer tokens = new StringTokenizer( sqlWhereString, symbols, true );
//		ProcessingState state = new ProcessingState();
//
//		StringBuilder quotedBuffer = new StringBuilder();
//		StringBuilder result = new StringBuilder();
//
//		boolean hasMore = tokens.hasMoreTokens();
//		String nextToken = hasMore ? tokens.nextToken() : null;
//		while ( hasMore ) {
//			String token = nextToken;
//			String lcToken = token.toLowerCase();
//			hasMore = tokens.hasMoreTokens();
//			nextToken = hasMore ? tokens.nextToken() : null;
//
//			// First, determine quoting which might be based on either:
//			// 		1) back-tick
//			// 		2) single quote (ANSI SQL standard)
//			// 		3) or dialect defined quote character(s)
//			QuotingCharacterDisposition quotingCharacterDisposition = QuotingCharacterDisposition.NONE;
//			if ( "`".equals( token ) ) {
//				state.quoted = !state.quoted;
//				quotingCharacterDisposition = state.quoted
//						? QuotingCharacterDisposition.OPEN
//						: QuotingCharacterDisposition.CLOSE;
//				// replace token with the appropriate dialect quoting char
//				token = lcToken = ( quotingCharacterDisposition == QuotingCharacterDisposition.OPEN )
//						? dialectOpenQuote
//						: dialectCloseQuote;
//			}
//			else if ( "'".equals( token ) ) {
//				state.quoted = !state.quoted;
//				quotingCharacterDisposition = state.quoted
//						? QuotingCharacterDisposition.OPEN
//						: QuotingCharacterDisposition.CLOSE;
//			}
//			else if ( !state.quoted && dialectOpenQuote.equals( token ) ) {
//				state.quoted = true;
//				quotingCharacterDisposition = QuotingCharacterDisposition.OPEN;
//			}
//			else if ( state.quoted && dialectCloseQuote.equals( token ) ) {
//				state.quoted = false;
//				quotingCharacterDisposition = QuotingCharacterDisposition.CLOSE;
//			}
//
//			if ( state.quoted ) {
//				quotedBuffer.append( token );
//				continue;
//			}
//
//			// if we were previously processing quoted state and just encountered the close quote, then handle that
//			// quoted text
//			if ( quotingCharacterDisposition == QuotingCharacterDisposition.CLOSE ) {
//				token = quotedBuffer.toString();
//				quotedBuffer.setLength( 0 );
//				result.append( placeholder ).append( '.' )
//						.append( dialectOpenQuote ).append( token ).append( dialectCloseQuote );
//				continue;
//			}
//
//			// Special processing for ANSI SQL EXTRACT function
//			if ( "extract".equals( lcToken ) && "(".equals( nextToken ) ) {
//				final String field = extractUntil( tokens, "from" );
//				final String source = renderWhereStringTemplate(
//						extractUntil( tokens, ")" ),
//						placeholder,
//						dialect,
//						functionRegistry
//				);
//				result.append( "extract(" ).append( field ).append( " from " ).append( source ).append( ')' );
//
//				hasMore = tokens.hasMoreTokens();
//				nextToken = hasMore ? tokens.nextToken() : null;
//
//				continue;
//			}
//
//			// Special processing for ANSI SQL TRIM function
//			if ( "trim".equals( lcToken ) && "(".equals( nextToken ) ) {
//				List<String> operands = new ArrayList<String>();
//				StringBuilder builder = new StringBuilder();
//
//				boolean hasMoreOperands = true;
//				String operandToken = tokens.nextToken();
//				boolean quoted = false;
//				while ( hasMoreOperands ) {
//					final boolean isQuote = "'".equals( operandToken );
//					if ( isQuote ) {
//						quoted = !quoted;
//						if ( !quoted ) {
//							operands.add( builder.append( '\'' ).toString() );
//							builder.setLength( 0 );
//						}
//						else {
//							builder.append( '\'' );
//						}
//					}
//					else if ( quoted ) {
//						builder.append( operandToken );
//					}
//					else if ( operandToken.length() == 1 && Character.isWhitespace( operandToken.charAt( 0 ) ) ) {
//						// do nothing
//					}
//					else {
//						operands.add( operandToken );
//					}
//					operandToken = tokens.nextToken();
//					hasMoreOperands = tokens.hasMoreTokens() && ! ")".equals( operandToken );
//				}
//
//				TrimOperands trimOperands = new TrimOperands( operands );
//				result.append( "trim(" );
//				if ( trimOperands.trimSpec != null ) {
//					result.append( trimOperands.trimSpec ).append( ' ' );
//				}
//				if ( trimOperands.trimChar != null ) {
//					if ( trimOperands.trimChar.startsWith( "'" ) && trimOperands.trimChar.endsWith( "'" ) ) {
//						result.append( trimOperands.trimChar );
//					}
//					else {
//						result.append(
//								renderWhereStringTemplate( trimOperands.trimSpec, placeholder, dialect, functionRegistry )
//						);
//					}
//					result.append( ' ' );
//				}
//				if ( trimOperands.from != null ) {
//					result.append( trimOperands.from ).append( ' ' );
//				}
//				else if ( trimOperands.trimSpec != null || trimOperands.trimChar != null ) {
//					// I think ANSI SQL says that the 'from' is not optional if either trim-spec or trim-char are specified
//					result.append( "from " );
//				}
//
//				result.append( renderWhereStringTemplate( trimOperands.trimSource, placeholder, dialect, functionRegistry ) )
//						.append( ')' );
//
//				hasMore = tokens.hasMoreTokens();
//				nextToken = hasMore ? tokens.nextToken() : null;
//
//				continue;
//			}
//
//
//			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//
//			if ( Character.isWhitespace( token.charAt( 0 ) ) ) {
//				result.append( token );
//			}
//			else if ( state.beforeTable ) {
//				result.append( token );
//				state.beforeTable = false;
//				state.afterFromTable = true;
//			}
//			else if ( state.afterFromTable ) {
//				if ( !"as".equals(lcToken) ) {
//					state.afterFromTable = false;
//				}
//				result.append(token);
//			}
//			else if ( isNamedParameter(token) ) {
//				result.append(token);
//			}
//			else if ( isIdentifier(token, dialect)
//					&& !isFunctionOrKeyword(lcToken, nextToken, dialect , functionRegistry) ) {
//				result.append(placeholder)
//						.append('.')
//						.append( dialect.quote(token) );
//			}
//			else {
//				if ( BEFORE_TABLE_KEYWORDS.contains(lcToken) ) {
//					state.beforeTable = true;
//					state.inFromClause = true;
//				}
//				else if ( state.inFromClause && ",".equals(lcToken) ) {
//					state.beforeTable = true;
//				}
//				result.append(token);
//			}
//
//			//Yuck:
//			if ( state.inFromClause
//					&& KEYWORDS.contains( lcToken ) //"as" is not in KEYWORDS
//					&& !BEFORE_TABLE_KEYWORDS.contains( lcToken ) ) {
//				state.inFromClause = false;
//			}
//		}
//
//		return result.toString();
//	}
//
//	private static class ProcessingState {
//		boolean quoted = false;
//		boolean quotedIdentifier = false;
//		boolean beforeTable = false;
//		boolean inFromClause = false;
//		boolean afterFromTable = false;
//	}
//
//	private static enum QuotingCharacterDisposition { NONE, OPEN, CLOSE }

	private static class TrimOperands {
		private final String trimSpec;
		private final String trimChar;
		private final String from;
		private final String trimSource;

		private TrimOperands(List<String> operands) {
			final int size = operands.size();
			if ( size == 1 ) {
				trimSpec = null;
				trimChar = null;
				from = null;
				trimSource = operands.get(0);
			}
			else if ( size == 4 ) {
				trimSpec = operands.get(0);
				trimChar = operands.get(1);
				from = operands.get(2);
				trimSource = operands.get(3);
			}
			else {
				if ( size < 1 || size > 4 ) {
					throw new HibernateException( "Unexpected number of trim function operands : " + size );
				}

				// trim-source will always be the last operand
				trimSource = operands.get( size - 1 );

				// ANSI SQL says that more than one operand means that the FROM is required
				if ( ! "from".equals( operands.get( size - 2 ) ) ) {
					throw new HibernateException( "Expecting FROM, found : " + operands.get( size - 2 ) );
				}
				from = operands.get( size - 2 );

				// trim-spec, if there is one will always be the first operand
				if ( "leading".equalsIgnoreCase( operands.get(0) )
						|| "trailing".equalsIgnoreCase( operands.get(0) )
						|| "both".equalsIgnoreCase( operands.get(0) ) ) {
					trimSpec = operands.get(0);
					trimChar = null;
				}
				else {
					trimSpec = null;
					if ( size - 2 == 0 ) {
						trimChar = null;
					}
					else {
						trimChar = operands.get( 0 );
					}
				}
			}
		}
	}

	private static String extractUntil(StringTokenizer tokens, String delimiter) {
		StringBuilder valueBuilder = new StringBuilder();
		String token = tokens.nextToken();
		while ( ! delimiter.equalsIgnoreCase( token ) ) {
			valueBuilder.append( token );
			token = tokens.nextToken();
		}
		return valueBuilder.toString().trim();
	}

	public static class NoOpColumnMapper implements ColumnMapper {
		public static final NoOpColumnMapper INSTANCE = new NoOpColumnMapper();
		public SqlValueReference[] map(String reference) {
//			return new String[] { reference };
			return null;
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
	 * @deprecated Use {@link #translateOrderBy} instead
	 */
	@Deprecated
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

	public static String renderOrderByStringTemplate(
			String orderByFragment,
			final ColumnMapper columnMapper,
			final SessionFactoryImplementor sessionFactory,
			final Dialect dialect,
			final SQLFunctionRegistry functionRegistry) {
		return translateOrderBy(
				orderByFragment,
				columnMapper,
				sessionFactory,
				dialect,
				functionRegistry
		).injectAliases( LEGACY_ORDER_BY_ALIAS_RESOLVER );
	}

	public static OrderByAliasResolver LEGACY_ORDER_BY_ALIAS_RESOLVER = new OrderByAliasResolver() {
		@Override
		public String resolveTableAlias(String columnReference) {
			return TEMPLATE;
		}
	};

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
	public static OrderByTranslation translateOrderBy(
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

		return OrderByFragmentTranslator.translate( context, orderByFragment );
	}

	private static boolean isNamedParameter(String token) {
		return token.startsWith(":");
	}

	private static boolean isFunctionOrKeyword(String lcToken, String nextToken, Dialect dialect, SQLFunctionRegistry functionRegistry) {
		return "(".equals(nextToken) ||
			KEYWORDS.contains(lcToken) ||
			isFunction(lcToken, nextToken, functionRegistry ) ||
			dialect.getKeywords().contains(lcToken) ||
			FUNCTION_KEYWORDS.contains(lcToken);
	}

	private static boolean isFunction(String lcToken, String nextToken, SQLFunctionRegistry functionRegistry) {
		// checking for "(" is currently redundant because it is checked before getting here;
		// doing the check anyhow, in case that earlier check goes away;
		if ( "(".equals( nextToken ) ) {
			return true;
		}
		SQLFunction function = functionRegistry.findSQLFunction(lcToken);
		if ( function == null ) {
			// lcToken does not refer to a function
			return false;
		}
		// if function.hasParenthesesIfNoArguments() is true, then assume
		// lcToken is not a function (since it is not followed by '(')
		return ! function.hasParenthesesIfNoArguments();
	}

	private static boolean isIdentifier(String token) {
		return token.charAt(0)=='`' || ( //allow any identifier quoted with backtick
			Character.isLetter( token.charAt(0) ) && //only recognizes identifiers beginning with a letter
			token.indexOf('.') < 0
		);
	}

}
