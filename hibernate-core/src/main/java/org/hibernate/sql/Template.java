/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Function;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.spi.TypeConfiguration;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Character.isLetter;
import static org.hibernate.internal.util.StringHelper.WHITESPACE;

/**
 * Parses SQL fragments specified in mapping documents. The SQL fragment
 * should be written in the native SQL dialect of the target database,
 * with the following special exceptions:
 * <ul>
 *     <li>any backtick-quoted identifier, for example {@code `hello`},
 *     is interpreted as a quoted identifier and re-quoted using the
 *     {@linkplain Dialect#quote native quoted identifier syntax} of
 *     the database, and</li>
 *     <li>the literal identifiers {@code true} and {@code false} are
 *     interpreted as literal boolean values, and replaced with
 *     {@linkplain Dialect#toBooleanValueString dialect-specific
 *     literal values}.
 *     </li>
 * </ul>
 *
 * @implNote This is based on a simple scanner-based state machine.
 *           It is NOT in any way, shape, nor form, a parser, since
 *           we simply cannot recognize the syntax of every dialect
 *           of SQL we support.
 *
 * @author Gavin King
 */
public final class Template {

	private static final Set<String> KEYWORDS = Set.of(
			"and",
			"or",
			"not",
			"like",
			"escape",
			"is",
			"in",
			"between",
			"null",
			"select",
			"distinct",
			"from",
			"join",
			"inner",
			"outer",
			"left",
			"right",
			"on",
			"where",
			"having",
			"group",
			"order",
			"by",
			"desc",
			"asc",
			"limit",
			"any",
			"some",
			"exists",
			"all",
			"union",
			"minus",
			"except",
			"intersect",
			"partition",
			"within");
	private static final Set<String> BEFORE_TABLE_KEYWORDS
			= Set.of("from", "join");
	private static final Set<String> FUNCTION_KEYWORDS
			= Set.of("as", "leading", "trailing", "from", "case", "when", "then", "else", "end");
	private static final Set<String> FUNCTION_WITH_FROM_KEYWORDS
			= Set.of("extract", "trim");
	private static final Set<String> SOFT_KEYWORDS
			= Set.of("date", "time");
	private static final Set<String> LITERAL_PREFIXES
			= Set.of("n", "x", "varbyte", "bx", "bytea", "date", "time", "timestamp", "zone");
	private static final Set<String> FETCH_BIGRAMS
			= Set.of("first", "next");
	private static final Set<String> CURRENT_BIGRAMS
			= Set.of("date", "time", "timestamp");
	// Ordered-set aggregate function names we want to recognize
	private static final Set<String> ORDERED_SET_AGGREGATES
			= Set.of("listagg", "percentile_cont", "percentile_disc", "mode");
	// Soft keywords that are only treated as keywords in the LISTAGG extension immediately
	// following the argument list and up to and including GROUP
	private static final Set<String> LISTAGG_EXTENSION_KEYWORDS
			= Set.of("on", "overflow", "error", "truncate", "without", "count", "within", "with", "group");

	private static final String PUNCTUATION = "=><!+-*/()',|&`";

	public static final String TEMPLATE = "{@}";

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

	/**
	 * Takes the SQL fragment provided in the mapping attribute and interpolates the default
	 * {@linkplain #TEMPLATE placeholder value}, which is {@value #TEMPLATE}, using it to
	 * qualify every unqualified column name.
	 * <p>
	 * Handles subselects, quoted identifiers, quoted strings, expressions, SQL functions,
	 * named parameters, literals.
	 *
	 * @param sql The SQL string into which to interpolate the placeholder value
	 * @param dialect The dialect to apply
	 * @return The rendered SQL fragment
	 */
	public static String renderWhereStringTemplate(
			String sql,
			Dialect dialect,
			TypeConfiguration typeConfiguration) {
		return renderWhereStringTemplate( sql, TEMPLATE, dialect, typeConfiguration );
	}

	/**
	 * Takes the SQL fragment provided in the mapping attribute and interpolates the given
	 * alias, using it to qualify every unqualified column name.
	 * <p>
	 * Handles subselects, quoted identifiers, quoted strings, expressions, SQL functions,
	 * named parameters, literals.
	 *
	 * @param sql The SQL string into which to interpolate the alias value
	 * @param alias The alias to be interpolated into the SQL
	 * @param dialect The dialect to apply
	 * @return The rendered SQL fragment
	 */
	public static String renderWhereStringTemplate(
			String sql,
			String alias,
			Dialect dialect,
			TypeConfiguration typeConfiguration) {

		// IMPL NOTE: The basic process here is to tokenize the incoming string and to iterate over each token
		//		in turn. As we process each token, we set a series of flags used to indicate the type of context in
		// 		which the tokens occur. Depending on the state of those flags, we decide whether we need to qualify
		//		identifier references.

		// WARNING TO MAINTAINERS: This is a simple scanner-based state machine. Please don't attempt to turn it into
		//      a parser for SQL, no matter how "special" your case is. What I mean by this is: don't write code which
		//      attempts to recognize the grammar of SQL, not even little bits of SQL. Previous "enhancements" to this
		//      function did not respect this concept and resulted in code which was fragile and unmaintainable. If
		//      lookahead is truly necessary, use the lookahead() function provided below.

		final String symbols = PUNCTUATION + WHITESPACE + dialect.openQuote() + dialect.closeQuote();
		final var tokens = new StringTokenizer( sql, symbols, true );
		final var result = new StringBuilder();

		boolean quoted = false;
		boolean quotedIdentifier = false;
		boolean beforeTable = false;
		boolean inFromClause = false;
		boolean afterFromTable = false;
		boolean afterCastAs = false;
		boolean afterFetch = false;
		boolean afterCurrent = false;
		int inExtractOrTrim = -1;
		int inCast = -1;
		int nestingLevel = 0;
		// State for ordered-set aggregates / LISTAGG extension handling
		boolean inOrderedSetFunction = false;
		int orderedSetParenDepth = 0;
		boolean afterOrderedSetArgs = false;
		boolean inListaggExtension = false;
		boolean lastWasListagg = false;

		boolean hasMore = tokens.hasMoreTokens();
		String nextToken = hasMore ? tokens.nextToken() : null;
		String token = null;
		String previousToken;
		while ( hasMore ) {
			previousToken = token;
			token = nextToken;
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
				if ( isOpenQuote
						&& !inFromClause // don't want to append alias to tokens inside the FROM clause
						&& !endsWithDot( previousToken ) ) {
					result.append( alias ).append( '.' );
				}
			}

			final boolean isWhitespace = token.isBlank();

			// handle bigrams here
			final boolean wasAfterFetch = afterFetch;
			afterFetch = afterFetch && isWhitespace;
			final boolean wasAfterCurrent = afterCurrent;
			afterCurrent = afterCurrent && isWhitespace;

			final String processedToken;
			final boolean isQuoted =
					quoted || quotedIdentifier || isQuoteCharacter;
			if ( isQuoted || isWhitespace ) {
				processedToken = token;
			}
			else if ( beforeTable ) {
				processedToken = token;
				beforeTable = false;
				afterFromTable = true;
			}
			else if ( afterFromTable ) {
				afterFromTable = "as".equals(lcToken);
				processedToken = token;
			}
			else if ( "(".equals(lcToken) ) {
				if ( inOrderedSetFunction ) {
					orderedSetParenDepth++;
				}
				nestingLevel ++;
				processedToken = token;
			}
			else if ( ")".equals(lcToken) ) {
				nestingLevel --;
				if ( nestingLevel == inExtractOrTrim ) {
					inExtractOrTrim = -1;
				}
				if ( nestingLevel == inCast ) {
					inCast = -1;
					afterCastAs = false;
				}
				if ( inOrderedSetFunction ) {
					orderedSetParenDepth--;
					if ( orderedSetParenDepth == 0 ) {
						inOrderedSetFunction = false;
						afterOrderedSetArgs = true;
						inListaggExtension = lastWasListagg;
					}
				}
				processedToken = token;
			}
			else if ( ",".equals(lcToken) ) {
				if ( inFromClause ) {
					beforeTable = true;
				}
				processedToken = token;
			}
			else if ( lcToken.length()==1 && symbols.contains(lcToken) ) {
				processedToken = token;
			}
			else if ( BEFORE_TABLE_KEYWORDS.contains(lcToken) ) {
				if ( inExtractOrTrim == -1 ) {
					beforeTable = true;
					inFromClause = true;
				}
				processedToken = token;
			}
			else if ( inFromClause || afterCastAs ) {
				// Don't want to append alias to:
				// 1. tokens inside the FROM clause
				// 2. type names after 'CAST(expression AS'
				processedToken = token;
			}
			else if ( isNamedParameter(token) ) {
				processedToken = token;
			}
			else if ( "as".equals( lcToken ) ) {
				processedToken = token;
				afterCastAs = inCast>-1;
			}
			else if ( isFetch( dialect, lcToken ) ) {
				processedToken = token;
				afterFetch = true;
			}
			else if ( wasAfterFetch && FETCH_BIGRAMS.contains( lcToken ) ) {
				processedToken = token;
			}
			else if ( isCurrent( lcToken, nextToken, sql, symbols, tokens ) ) {
				processedToken = token;
				afterCurrent = true;
			}
			else if ( isBoolean( lcToken ) ) {
				processedToken = dialect.toBooleanValueString( parseBoolean( token ) );
			}
			else if ( isFunctionCall( nextToken, sql, symbols, tokens ) ) {
				if ( FUNCTION_WITH_FROM_KEYWORDS.contains( lcToken ) ) {
					inExtractOrTrim = nestingLevel;
				}
				if ( "cast".equals( lcToken ) ) {
					inCast = nestingLevel;
				}
				if ( ORDERED_SET_AGGREGATES.contains( lcToken ) ) {
					inOrderedSetFunction = true;
					orderedSetParenDepth = 0;
					lastWasListagg = "listagg".equals( lcToken );
				}
				processedToken = token;
			}
			else if ( afterOrderedSetArgs && (inListaggExtension
					? ( LISTAGG_EXTENSION_KEYWORDS.contains( lcToken ) )
					: "within".equals( lcToken )) ) {
				if ( "group".equals( lcToken ) ) {
					// end special handling after GROUP (inclusive)
					afterOrderedSetArgs = false;
					inListaggExtension = false;
				}
				processedToken = token;
			}
			else if ( isAliasableIdentifier( token, lcToken, nextToken,
							sql, symbols, tokens, wasAfterCurrent,
							dialect, typeConfiguration ) ) {
				// Any aliasable identifier here cannot be one of the soft keywords allowed in the
				// ordered-set/LISTAGG post-args region. We've left that region so must end special handling.
				// (It's irrelevant at this point whether the dialect supports ordered-set/LISTAGG.)
				afterOrderedSetArgs = false;
				inListaggExtension = false;
				processedToken = alias + '.' +  dialect.quote(token);
			}
			else {
				processedToken = token;
			}

			result.append( processedToken );

			//Yuck:
			if ( inFromClause
					&& KEYWORDS.contains( lcToken ) // "as" is not in KEYWORDS
					&& !BEFORE_TABLE_KEYWORDS.contains( lcToken ) ) {
				inFromClause = false;
			}
		}

		return result.toString();
	}

	private static boolean isAliasableIdentifier(
			String token, String lcToken, String nextToken,
			String sql, String symbols, StringTokenizer tokens,
			boolean wasAfterCurrent,
			Dialect dialect, TypeConfiguration typeConfiguration) {
		return isUnqualifiedIdentifier( token )
			&& !isKeyword( lcToken, wasAfterCurrent, dialect, typeConfiguration )
			&& !isLiteral( lcToken, nextToken, sql, symbols, tokens );
	}

	private static boolean isFunctionCall(
			String nextToken,
			String sql, String symbols, StringTokenizer tokens) {
		if ( nextToken == null ) {
			return false;
		}
		else {
			return nextToken.isBlank()
					? lookPastBlankTokens( sql, symbols, tokens, 1, "("::equals )
					: "(".equals( nextToken );
		}
	}

	private static boolean isCurrent(
			String lcToken, String nextToken,
			String sql, String symbols, StringTokenizer tokens) {
		return "current".equals( lcToken )
			&& nextToken.isBlank()
			&& lookPastBlankTokens( sql, symbols, tokens, 1, CURRENT_BIGRAMS::contains );
	}

	private static boolean isFetch(Dialect dialect, String lcToken) {
		return "fetch".equals( lcToken )
			&& dialect.getKeywords().contains( "fetch" );
	}

	private static boolean endsWithDot(String token) {
		return token != null && token.endsWith( "." );
	}

	private static boolean isLiteral(
			String lcToken, String next,
			String sqlWhereString, String symbols, StringTokenizer tokens) {
		if ( next == null ) {
			return false;
		}
		else if ( LITERAL_PREFIXES.contains( lcToken ) ) {
			if ( next.isBlank() ) {
				// we need to look ahead in the token stream
				// to find the first non-blank token
				return lookPastBlankTokens( sqlWhereString, symbols, tokens, 1,
						nextToken -> "'".equals(nextToken)
								|| lcToken.equals("time") && "with".equals(nextToken)
								|| lcToken.equals("timestamp") && "with".equals(nextToken)
								|| lcToken.equals("time") && "zone".equals(nextToken) );
			}
			else {
				return "'".equals(next);
			}
		}
		else {
			return false;
		}
	}

	private static boolean lookPastBlankTokens(
			String sqlWhereString, String symbols, StringTokenizer tokens,
			@SuppressWarnings("SameParameterValue") int skip,
			Function<String, Boolean> check) {
		final var lookahead = lookahead( sqlWhereString, symbols, tokens, skip );
		if ( lookahead.hasMoreTokens() ) {
			String nextToken;
			do {
				nextToken = lookahead.nextToken().toLowerCase(Locale.ROOT);
			}
			while ( nextToken.isBlank() && lookahead.hasMoreTokens() );
			return check.apply( nextToken );
		}
		else {
			return false;
		}
	}

	/**
	 * Clone the given token stream, returning a token stream which begins
	 * from the next token.
	 *
	 * @param sql the full SQL we are scanning
	 * @param symbols the delimiter symbols
	 * @param tokens the current token stream
	 * @param skip the number of tokens to skip
	 * @return a cloned token stream
	 */
	private static StringTokenizer lookahead(String sql, String symbols, StringTokenizer tokens, int skip) {
		final var lookahead = new StringTokenizer( sql, symbols, true );
		while ( lookahead.countTokens() > tokens.countTokens() + skip ) {
			lookahead.nextToken();
		}
		return lookahead;
	}

	public static List<String> collectColumnNames(String sql, Dialect dialect, TypeConfiguration typeConfiguration) {
		return collectColumnNames( renderWhereStringTemplate( sql, dialect, typeConfiguration ) );
	}

	public static List<String> collectColumnNames(String template) {
		final List<String> names = new ArrayList<>();
		int begin = 0;
		int match;
		while ( ( match = template.indexOf(TEMPLATE, begin) ) >= 0 ) {
			final int start = match + TEMPLATE.length() + 1;
			for ( int loc = start;; loc++ ) {
				if ( loc == template.length() - 1 ) {
					names.add( template.substring( start ) );
					begin = template.length();
					break;
				}
				else {
					final char ch = template.charAt( loc );
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

	private static boolean isNamedParameter(String token) {
		return token.charAt(0) == ':';
	}

	private static boolean isKeyword(
			String lcToken,
			boolean afterCurrent,
			Dialect dialect,
			TypeConfiguration typeConfiguration) {
		if ( SOFT_KEYWORDS.contains( lcToken ) ) {
			// these can be column names on some databases
			// but treat 'current date', 'current time' bigrams as keywords
			return afterCurrent;
		}
		else {
			return KEYWORDS.contains( lcToken )
				|| isType( lcToken, typeConfiguration )
				|| dialect.getKeywords().contains( lcToken )
				|| FUNCTION_KEYWORDS.contains( lcToken );
		}
	}

	private static boolean isType(String lcToken, TypeConfiguration typeConfiguration) {
		return typeConfiguration.getDdlTypeRegistry().isTypeNameRegistered( lcToken );
	}

	private static boolean isUnqualifiedIdentifier(String token) {
		final char initialChar = token.charAt( 0 );
		return initialChar == '`'             // allow any identifier quoted with backtick
			|| isLetter( initialChar )        // only recognizes identifiers beginning with a letter
				&& token.indexOf( '.' ) < 0;  // don't qualify already-qualified identifiers
	}

	private static boolean isBoolean(String lcToken) {
		return switch ( lcToken ) {
			case "true", "false" -> true;
			default -> false;
		};
	}
}
