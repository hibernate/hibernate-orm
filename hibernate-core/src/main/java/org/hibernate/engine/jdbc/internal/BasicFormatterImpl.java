/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.internal;

import java.util.LinkedList;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;

import org.hibernate.internal.util.StringHelper;

/**
 * Performs formatting of basic SQL statements (DML + query).
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class BasicFormatterImpl implements Formatter {

	private static final Set<String> NON_FUNCTION_NAMES = Set.of(
			"select", "from", "on", "set", "and", "or", "where", "having", "by"
	);

	private static final String INDENT_STRING = "    ";
	private static final String INITIAL = System.lineSeparator() + INDENT_STRING;

	@Override
	public String format(String source) {
		return new FormatProcess( source ).perform();
	}

	private static class FormatProcess {
		boolean beginLine = true;
		boolean afterBeginBeforeEnd;
		boolean afterByOrSetOrFromOrSelect;
		boolean afterOn;
		boolean afterBetween;
		boolean afterExtract;
		boolean afterInsert;
		int inFunction;
		int parensSinceSelect;
		private final LinkedList<Integer> parenCounts = new LinkedList<>();
		private final LinkedList<Boolean> afterByOrFromOrSelects = new LinkedList<>();

		int indent = 1;

		StringBuilder result = new StringBuilder();
		StringTokenizer tokens;
		String lastToken;
		String token;
		String lcToken;

		public FormatProcess(String sql) {
			assert sql != null : "SQL to format should not be null";

			tokens = new StringTokenizer(
					sql,
					"()+*/-=<>'`\"[]," + StringHelper.WHITESPACE,
					true
			);
		}

		public String perform() {

			result.append( INITIAL );

			while ( tokens.hasMoreTokens() ) {
				token = tokens.nextToken();
				lcToken = token.toLowerCase(Locale.ROOT);

				switch (lcToken) {

					case "'":
					case "\"":
						String t;
						do {
							t = tokens.nextToken();
							token += t;
						}
						while ( !lcToken.equals( t ) && tokens.hasMoreTokens() );
						lcToken = token;
						misc();
						break;
					// SQL Server uses "[" and "]" to escape reserved words
					// see SQLServerDialect.openQuote and SQLServerDialect.closeQuote
					case "[":
						String tt;
						do {
							tt = tokens.nextToken();
							token += tt;
						}
						while ( !"]".equals( tt ) && tokens.hasMoreTokens() );
						lcToken = token;
						misc();
						break;

					case ",":
						if ( afterByOrSetOrFromOrSelect && inFunction==0 ) {
							commaAfterByOrFromOrSelect();
						}
						else if ( afterOn && inFunction==0 ) {
							commaAfterOn();
						}
						else {
							misc();
						}
						break;

					case "(":
						openParen();
						break;
					case ")":
						closeParen();
						break;

					case "select":
						select();
						break;
					case "insert":
					case "update":
					case "delete":
						updateOrInsertOrDelete();
						break;

					case "values":
						values();
						break;

					case "on":
						on();
						break;

					case "between":
						afterBetween = true;
						misc();
						break;
					case "trim":
					case "extract":
						afterExtract = true;
						misc();
						break;

					//TODO: detect when 'left', 'right' are function names
					case "left":
					case "right":
					case "full":
					case "inner":
					case "outer":
					case "cross":
					case "group":
					case "order":
					case "returning":
						beginNewClause();
						break;

					case "from":
						if ( afterExtract ) {
							misc();
							afterExtract = false;
							break;
						}
						//else fall through:
					case "where":
					case "set":
					case "having":
					case "by":
					case "join":
					case "into":
					case "union":
					case "intersect":
					case "offset":
					case "limit":
					case "fetch":
						endNewClause();
						break;

					case "case":
						beginCase();
						break;
					case "end":
						endCase();
						break;

					case "and":
						if ( afterBetween ) {
							misc();
							afterBetween = false;
							break;
						}
						//else fall through:
					case "or":
					case "when":
					case "else":
						logical();
						break;
					default:
						if ( isWhitespace( token ) ) {
							white();
						}

						else {
							misc();
						}
				}

				if ( !isWhitespace( token ) ) {
					lastToken = lcToken;
				}

			}
			return result.toString();
		}

		private void commaAfterOn() {
			out();
			indent--;
			newline();
			afterOn = false;
			afterByOrSetOrFromOrSelect = true;
		}

		private void commaAfterByOrFromOrSelect() {
			out();
			newline();
		}

		private void logical() {
			newline();
			out();
			beginLine = false;
		}

		private void endCase() {
			indent--;
			logical();
		}

		private void on() {
			indent++;
			afterOn = true;
			newline();
			out();
			beginLine = false;
		}

		private void beginCase() {
			out();
			beginLine = false;
			indent++;
		}

		private void misc() {
			out();
			if ( afterInsert && inFunction==0 ) {
				newline();
				afterInsert = false;
			}
			else {
				beginLine = false;
			}
		}

		private void white() {
			if ( !beginLine ) {
				result.append( " " );
			}
		}

		private void updateOrInsertOrDelete() {
			if ( indent>1  ) {
				//probably just the insert SQL function
				out();
			}
			else {
				out();
				indent++;
				beginLine = false;
				if ( "update".equals( lcToken ) ) {
					newline();
				}
				if ( "insert".equals( lcToken ) ) {
					afterInsert = true;
				}
			}
		}

		private void select() {
			out();
			indent++;
			newline();
			parenCounts.addLast( parensSinceSelect );
			afterByOrFromOrSelects.addLast( afterByOrSetOrFromOrSelect );
			parensSinceSelect = 0;
			afterByOrSetOrFromOrSelect = true;
		}

		private void out() {
			result.append( token );
		}

		private void endNewClause() {
			if ( !afterBeginBeforeEnd ) {
				indent--;
				if ( afterOn ) {
					indent--;
					afterOn = false;
				}
				newline();
			}
			out();
			if ( !"union".equals( lcToken ) && !"intersect".equals( lcToken ) ) {
				indent++;
			}
			newline();
			afterBeginBeforeEnd = false;
			afterByOrSetOrFromOrSelect = "by".equals( lcToken )
					|| "set".equals( lcToken )
					|| "from".equals( lcToken );
		}

		private void beginNewClause() {
			if ( !afterBeginBeforeEnd ) {
				if ( afterOn ) {
					indent--;
					afterOn = false;
				}
				indent--;
				newline();
			}
			out();
			beginLine = false;
			afterBeginBeforeEnd = true;
		}

		private void values() {
			if ( parensSinceSelect == 0 ) {
				indent--;
				newline();
				out();
				indent++;
				newline();
			}
			else {
				out();
			}
		}

		private void closeParen() {
			parensSinceSelect--;
			if ( parensSinceSelect < 0 ) {
				indent--;
				parensSinceSelect = parenCounts.removeLast();
				afterByOrSetOrFromOrSelect = afterByOrFromOrSelects.removeLast();
			}
			if ( inFunction > 0 ) {
				inFunction--;
				out();
			}
			else {
				if ( !afterByOrSetOrFromOrSelect ) {
					indent--;
					newline();
				}
				out();
			}
			beginLine = false;
		}

		private void openParen() {
			if ( isFunctionName( lastToken ) || inFunction > 0 ) {
				inFunction++;
			}
			beginLine = false;
			if ( inFunction > 0 ) {
				out();
			}
			else {
				out();
				if ( !afterByOrSetOrFromOrSelect ) {
					indent++;
					newline();
					beginLine = true;
				}
			}
			parensSinceSelect++;
		}

		private static boolean isFunctionName(String tok) {
			if ( tok == null || tok.length() == 0 ) {
				return false;
			}

			final char begin = tok.charAt( 0 );
			final boolean isIdentifier = Character.isJavaIdentifierStart( begin ) || '"' == begin;
			return isIdentifier && !NON_FUNCTION_NAMES.contains( tok );
		}

		private static boolean isWhitespace(String token) {
			return StringHelper.WHITESPACE.contains( token );
		}

		private void newline() {
			result.append( System.lineSeparator() )
					.append( INDENT_STRING.repeat(indent) );
			beginLine = true;
		}
	}

}
