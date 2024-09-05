/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Character.isLetter;
import static org.hibernate.internal.util.StringHelper.WHITESPACE;

/**
 * Parses SQL fragments specified in mapping documents.
 *
 * @author Gavin King
 */
public final class Template {

	private static final Set<String> KEYWORDS = new HashSet<>();
	private static final Set<String> BEFORE_TABLE_KEYWORDS = new HashSet<>();
	private static final Set<String> FUNCTION_KEYWORDS = new HashSet<>();
	private static final Set<String> LITERAL_PREFIXES = new HashSet<>();
	public static final String PUNCTUATION = "=><!+-*/()',|&`";

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
		KEYWORDS.add("except");
		KEYWORDS.add("intersect");
		KEYWORDS.add("partition");

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

		LITERAL_PREFIXES.add("n");
		LITERAL_PREFIXES.add("x");
		LITERAL_PREFIXES.add("varbyte");
		LITERAL_PREFIXES.add("bx");
		LITERAL_PREFIXES.add("bytea");
		LITERAL_PREFIXES.add("date");
		LITERAL_PREFIXES.add("time");
		LITERAL_PREFIXES.add("timestamp");
		LITERAL_PREFIXES.add("zone");
	}

	public static final String TEMPLATE = "$PlaceHolder$";

	private Template() {}

	public static String renderTransformerReadFragment(
			String fragment,
			String... columnNames) {
		// NOTE : would need access to SessionFactoryImplementor to make this configurable
		for ( String columnName : columnNames ) {
			fragment = fragment.replace( columnName, TEMPLATE + '.' + columnName );
		}
		return fragment;
	}

	public static String renderWhereStringTemplate(
			String sqlWhereString,
			Dialect dialect,
			TypeConfiguration typeConfiguration,
			SqmFunctionRegistry functionRegistry) {
		return renderWhereStringTemplate( sqlWhereString, TEMPLATE, dialect, typeConfiguration, functionRegistry );
	}

	/**
	 * Takes the where condition provided in the mapping attribute and interpolates the alias.
	 * Handles sub-selects, quoted identifiers, quoted strings, expressions, SQL functions,
	 * named parameters.
	 *
	 * @param sqlWhereString The string into which to interpolate the placeholder value
	 * @param placeholder The value to be interpolated into the sqlWhereString
	 * @param dialect The dialect to apply
	 * @param functionRegistry The registry of all sql functions
	 * @return The rendered sql fragment
	 */
	public static String renderWhereStringTemplate(
			String sqlWhereString,
			String placeholder,
			Dialect dialect,
			TypeConfiguration typeConfiguration,
			SqmFunctionRegistry functionRegistry) {

		// IMPL NOTE : The basic process here is to tokenize the incoming string and to iterate over each token
		//		in turn.  As we process each token, we set a series of flags used to indicate the type of context in
		// 		which the tokens occur.  Depending on the state of those flags we decide whether we need to qualify
		//		identifier references.

		final String symbols = PUNCTUATION + WHITESPACE + dialect.openQuote() + dialect.closeQuote();
		final StringTokenizer tokens = new StringTokenizer( sqlWhereString, symbols, true );
		final StringBuilder result = new StringBuilder();

		boolean quoted = false;
		boolean quotedIdentifier = false;
		boolean beforeTable = false;
		boolean inFromClause = false;
		boolean afterFromTable = false;

		boolean hasMore = tokens.hasMoreTokens();
		String nextToken = hasMore ? tokens.nextToken() : null;
		while ( hasMore ) {
			String token = nextToken;
			String lcToken = token.toLowerCase(Locale.ROOT);
			hasMore = tokens.hasMoreTokens();
			nextToken = hasMore ? tokens.nextToken() : null;

			boolean isQuoteCharacter = false;

			if ( !quotedIdentifier && "'".equals(token) ) {
				quoted = !quoted;
				isQuoteCharacter = true;
			}

			if ( !quoted ) {
				final boolean isOpenQuote;
				if ( "`".equals(token) ) {
					isOpenQuote = !quotedIdentifier;
					token = lcToken = isOpenQuote
							? Character.toString( dialect.openQuote() )
							: Character.toString( dialect.closeQuote() );
					quotedIdentifier = isOpenQuote;
					isQuoteCharacter = true;
				}
				else if ( !quotedIdentifier && dialect.openQuote()==token.charAt(0) ) {
					isOpenQuote = true;
					quotedIdentifier = true;
					isQuoteCharacter = true;
				}
				else if ( quotedIdentifier && dialect.closeQuote()==token.charAt(0) ) {
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

			final boolean quotedOrWhitespace =
					quoted || quotedIdentifier || isQuoteCharacter
							|| token.isBlank();
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
			else if ( isExtractFunction( lcToken, nextToken ) ) {
				// Special processing for ANSI SQL EXTRACT function
				handleExtractFunction( placeholder, dialect, typeConfiguration, functionRegistry, tokens, result );
				hasMore = tokens.hasMoreTokens();
				nextToken = hasMore ? tokens.nextToken() : null;
			}
			else if ( isTrimFunction( lcToken, nextToken ) ) {
				// Special processing for ANSI SQL TRIM function
				handleTrimFunction( placeholder, dialect, typeConfiguration, functionRegistry, tokens, result );
				hasMore = tokens.hasMoreTokens();
				nextToken = hasMore ? tokens.nextToken() : null;
			}
			else if ( isIdentifier(token)
					&& !isFunctionOrKeyword( lcToken, nextToken, dialect, typeConfiguration, functionRegistry )
					&& !isLiteral( lcToken, nextToken, sqlWhereString, symbols, tokens ) ) {
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
				if ( isBoolean( token ) ) {
					token = dialect.toBooleanValueString( parseBoolean( token ) );
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

	private static boolean isTrimFunction(String lcToken, String nextToken) {
		return "trim".equals(lcToken) && "(".equals(nextToken);
	}

	private static boolean isExtractFunction(String lcToken, String nextToken) {
		return "extract".equals(lcToken) && "(".equals(nextToken);
	}

	private static boolean isLiteral(
			String lcToken, String next,
			String sqlWhereString, String symbols, StringTokenizer tokens) {
		if ( LITERAL_PREFIXES.contains( lcToken ) && next != null ) {
			// easy cases first
			if ( "'".equals(next) ) {
				return true;
			}
			else if ( !next.isBlank() ) {
				return false;
			}
			else {
				// we need to look ahead in the token stream
				// to find the first non-blank token
				final StringTokenizer lookahead =
						new StringTokenizer( sqlWhereString, symbols, true );
				while ( lookahead.countTokens() > tokens.countTokens()+1 ) {
					lookahead.nextToken();
				}
				if ( lookahead.hasMoreTokens() ) {
					String nextToken;
					do {
						nextToken = lookahead.nextToken().toLowerCase(Locale.ROOT);
					}
					while ( nextToken.isBlank() && lookahead.hasMoreTokens() );
					return "'".equals( nextToken )
						|| lcToken.equals( "time" ) && "with".equals( nextToken )
						|| lcToken.equals( "timestamp" ) && "with".equals( nextToken )
						|| lcToken.equals( "time" ) && "zone".equals( nextToken );
				}
				else {
					return false;
				}
			}
		}
		else {
			return false;
		}
	}

	private static void handleTrimFunction(
			String placeholder, Dialect dialect,
			TypeConfiguration typeConfiguration,
			SqmFunctionRegistry functionRegistry,
			StringTokenizer tokens,
			StringBuilder result) {
		final List<String> operands = new ArrayList<>();
		final StringBuilder builder = new StringBuilder();

		boolean hasMoreOperands = true;
		String operandToken = tokens.nextToken();
		switch ( operandToken.toLowerCase( Locale.ROOT ) ) {
			case "leading":
			case "trailing":
			case "both":
				operands.add( operandToken );
				if ( hasMoreOperands = tokens.hasMoreTokens() ) {
					operandToken = tokens.nextToken();
				}
				break;
		}
		boolean quotedOperand = false;
		int parenthesis = 0;
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
			else if ( parenthesis != 0 ) {
				builder.append( operandToken );
				switch ( operandToken ) {
					case "(":
						parenthesis++;
						break;
					case ")":
						parenthesis--;
						break;
				}
			}
			else {
				builder.append( operandToken );
				switch ( operandToken.toLowerCase( Locale.ROOT ) ) {
					case "(":
						parenthesis++;
						break;
					case ")":
						parenthesis--;
						break;
					case "from":
						if ( builder.length() != 0 ) {
							operands.add( builder.substring( 0, builder.length() - 4 ) );
							builder.setLength( 0 );
							operands.add( operandToken );
						}
						break;
				}
			}
			operandToken = tokens.nextToken();
			hasMoreOperands = tokens.hasMoreTokens()
					&& ( parenthesis != 0 || ! ")".equals( operandToken ) );
		}
		if ( builder.length() != 0 ) {
			operands.add( builder.toString() );
		}

		final TrimOperands trimOperands = new TrimOperands( operands );
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
						renderWhereStringTemplate( trimOperands.trimSpec, placeholder, dialect, typeConfiguration, functionRegistry )
				);
			}
			result.append( ' ' );
		}
		if ( trimOperands.from != null ) {
			result.append( trimOperands.from ).append( ' ' );
		}
		else if ( trimOperands.trimSpec != null || trimOperands.trimChar != null ) {
			// I think ANSI SQL says that the 'from' is not optional if either trim-spec or trim-char is specified
			result.append( "from " );
		}

		result.append( renderWhereStringTemplate( trimOperands.trimSource, placeholder, dialect, typeConfiguration, functionRegistry ) )
				.append( ')' );
	}

	private static void handleExtractFunction(
			String placeholder,
			Dialect dialect,
			TypeConfiguration typeConfiguration,
			SqmFunctionRegistry functionRegistry,
			StringTokenizer tokens,
			StringBuilder result) {
		final String field = extractUntil( tokens, "from" );
		final String source = renderWhereStringTemplate(
				extractUntil( tokens, ")" ),
				placeholder,
				dialect,
				typeConfiguration,
				functionRegistry
		);
		result.append( "extract(" ).append( field ).append( " from " ).append( source ).append( ')' );
	}

	public static List<String> collectColumnNames(
			String sql,
			Dialect dialect,
			TypeConfiguration typeConfiguration,
			SqmFunctionRegistry functionRegistry) {
		return collectColumnNames( renderWhereStringTemplate( sql, dialect, typeConfiguration, functionRegistry ) );
	}

	public static List<String> collectColumnNames(String template) {
		final List<String> names = new ArrayList<>();
		int begin = 0;
		int match;
		while ( ( match = template.indexOf(TEMPLATE, begin) ) >= 0 ) {
			int start = match + TEMPLATE.length() + 1;
			for ( int loc = start;; loc++ ) {
				if ( loc == template.length() - 1 ) {
					names.add( template.substring( start ) );
					begin = template.length();
					break;
				}
				else {
					char ch = template.charAt( loc );
					if ( PUNCTUATION.indexOf(ch) >= 0 || WHITESPACE.indexOf(ch) >= 0 ) {
						names.add( template.substring( start, loc ) );
						begin = loc;
						break;
					}
				}
			}
		}
		return names;
	}

//	/**
//	 * Takes the where condition provided in the mapping attribute and interpolates the alias.
//	 * Handles sub-selects, quoted identifiers, quoted strings, expressions, SQL functions,
//	 * named parameters.
//	 *
//	 * @param sqlWhereString The string into which to interpolate the placeholder value
//	 * @param placeholder The value to be interpolated into the sqlWhereString
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
//			String lcToken = token.toLowerCase(Locale.ROOT);
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
		final StringBuilder valueBuilder = new StringBuilder();
		String token = tokens.nextToken();
		while ( ! delimiter.equalsIgnoreCase( token ) ) {
			valueBuilder.append( token );
			token = tokens.nextToken();
		}
		return valueBuilder.toString().trim();
	}

	private static boolean isNamedParameter(String token) {
		return token.startsWith( ":" );
	}

	private static boolean isFunctionOrKeyword(
			String lcToken,
			String nextToken,
			Dialect dialect,
			TypeConfiguration typeConfiguration,
			SqmFunctionRegistry functionRegistry) {
		if ( "(".equals( nextToken ) ) {
			return true;
		}
		else if ( "date".equals( lcToken ) || "time".equals( lcToken ) ) {
			// these can be column names on some databases
			// TODO: treat 'current date' as a function
			return false;
		}
		else {
			return KEYWORDS.contains( lcToken )
				|| isType( lcToken, typeConfiguration )
				|| isFunction( lcToken, nextToken, functionRegistry )
				|| dialect.getKeywords().contains( lcToken )
				|| FUNCTION_KEYWORDS.contains( lcToken );
		}
	}

	private static boolean isType(String lcToken, TypeConfiguration typeConfiguration) {
		return typeConfiguration.getDdlTypeRegistry().isTypeNameRegistered( lcToken );
	}

	private static boolean isFunction(String lcToken, String nextToken, SqmFunctionRegistry functionRegistry) {
		// checking for "(" is currently redundant because it is checked before getting here;
		// doing the check anyhow, in case that earlier check goes away;
		if ( "(".equals( nextToken ) ) {
			return true;
		}

		final SqmFunctionDescriptor function = functionRegistry.findFunctionDescriptor( lcToken );
		return function != null;
	}

	private static boolean isIdentifier(String token) {
		if ( isBoolean( token ) ) {
			return false;
		}
		return token.charAt( 0 ) == '`'
			|| ( //allow any identifier quoted with backtick
				isLetter( token.charAt( 0 ) ) && //only recognizes identifiers beginning with a letter
						token.indexOf( '.' ) < 0
			);
	}

	private static boolean isBoolean(String token) {
		return "true".equals( token ) || "false".equals( token );
	}
}
