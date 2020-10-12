/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.internal;

import java.util.HashSet;
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

	private static final Set<String> BEGIN_CLAUSES = new HashSet<>();
	private static final Set<String> END_CLAUSES = new HashSet<>();
	private static final Set<String> LOGICAL = new HashSet<>();
	private static final Set<String> QUANTIFIERS = new HashSet<>();
	private static final Set<String> DML = new HashSet<>();
	private static final Set<String> MISC = new HashSet<>();

	static {
		BEGIN_CLAUSES.add( "left" );
		BEGIN_CLAUSES.add( "right" );
		BEGIN_CLAUSES.add( "inner" );
		BEGIN_CLAUSES.add( "outer" );
		BEGIN_CLAUSES.add( "group" );
		BEGIN_CLAUSES.add( "order" );

		END_CLAUSES.add( "where" );
		END_CLAUSES.add( "set" );
		END_CLAUSES.add( "having" );
		END_CLAUSES.add( "from" );
		END_CLAUSES.add( "by" );
		END_CLAUSES.add( "join" );
		END_CLAUSES.add( "into" );
		END_CLAUSES.add( "union" );

		LOGICAL.add( "and" );
		LOGICAL.add( "or" );
		LOGICAL.add( "when" );
		LOGICAL.add( "else" );
		LOGICAL.add( "end" );

		QUANTIFIERS.add( "in" );
		QUANTIFIERS.add( "all" );
		QUANTIFIERS.add( "exists" );
		QUANTIFIERS.add( "some" );
		QUANTIFIERS.add( "any" );

		DML.add( "insert" );
		DML.add( "update" );
		DML.add( "delete" );

		MISC.add( "select" );
		MISC.add( "on" );
	}

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
		boolean afterInsert;
		int inFunction;
		int parensSinceSelect;
		private LinkedList<Integer> parenCounts = new LinkedList<>();
		private LinkedList<Boolean> afterByOrFromOrSelects = new LinkedList<>();

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
				lcToken = token.toLowerCase( Locale.ROOT );

				switch ( token ) {
					case "'":
						// cannot handle single quotes
						concatUntil( "'" );
						break;
					case "\"": {
						concatUntil( "\"" );
						break;
					}
					// SQL Server uses "[" and "]" to escape reserved words
					// see SQLServerDialect.openQuote and SQLServerDialect.closeQuote
					case "[": {
						concatUntil( "]" );
						break;
					}
				}

				doProcess();

				if ( !isWhitespace( token ) ) {
					lastToken = lcToken;
				}

			}
			return result.toString();
		}

		private void concatUntil(String closure) {
			String t;
			StringBuilder s = new StringBuilder( token );
			do {
				t = tokens.nextToken();
				s.append( t );
			}
			while ( !closure.equals( t ) && tokens.hasMoreTokens() );
			token = s.toString();
		}

		private void doProcess() {
			handleCommaAfterByFromSelect();
		}

		private void handleCommaAfterByFromSelect() {
			if ( afterByOrSetOrFromOrSelect && ",".equals( token ) ) {
				out();
				newline();
				return;
			}
			handleCommaAfterOn();
		}

		private void handleCommaAfterOn() {
			if ( afterOn && ",".equals( token ) ) {
				out();
				indent--;
				newline();
				afterOn = false;
				afterByOrSetOrFromOrSelect = true;
				return;
			}
			handleOpenParen();
		}

		private void handleOpenParen() {
			if ( "(".equals( token ) ) {
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
				return;
			}
			handleCloseParen();
		}

		private void handleCloseParen() {
			if ( ")".equals( token ) ) {
				parensSinceSelect--;
				if ( parensSinceSelect < 0 ) {
					indent--;
					parensSinceSelect = parenCounts.removeLast();
					afterByOrSetOrFromOrSelect = afterByOrFromOrSelects.removeLast();
				}
				if ( inFunction > 0 ) {
					inFunction--;
				}
				else {
					if ( !afterByOrSetOrFromOrSelect ) {
						indent--;
						newline();
					}
				}
				out();
				beginLine = false;
				return;
			}
			handleBeginNewClause();
		}

		private void handleBeginNewClause() {
			if ( BEGIN_CLAUSES.contains( lcToken ) ) {
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
				return;
			}
			handleEndNewClause();
		}

		private void handleEndNewClause() {
			if ( END_CLAUSES.contains( lcToken ) ) {
				if ( !afterBeginBeforeEnd ) {
					indent--;
					if ( afterOn ) {
						indent--;
						afterOn = false;
					}
					newline();
				}
				out();
				if ( !"union".equals( lcToken ) ) {
					indent++;
				}
				newline();
				afterBeginBeforeEnd = false;
				afterByOrSetOrFromOrSelect = "by".equals( lcToken )
						|| "set".equals( lcToken )
						|| "from".equals( lcToken );
				return;
			}
			handleSelect();
		}

		private void handleSelect() {
			if ( "select".equals( lcToken ) ) {
				out();
				indent++;
				newline();
				parenCounts.addLast( parensSinceSelect );
				afterByOrFromOrSelects.addLast( afterByOrSetOrFromOrSelect );
				parensSinceSelect = 0;
				afterByOrSetOrFromOrSelect = true;
				return;
			}
			handleUpdateInsertDelete();
		}

		private void handleUpdateInsertDelete() {
			if ( DML.contains( lcToken ) ) {
				out();
				indent++;
				beginLine = false;
				if ( "update".equals( lcToken ) ) {
					newline();
				}
				if ( "insert".equals( lcToken ) ) {
					afterInsert = true;
				}
				return;
			}
			handleValues();
		}

		private void handleValues() {
			if ( "values".equals( lcToken ) ) {
				indent--;
				newline();
				out();
				indent++;
				newline();
				return;
			}
			handleOn();
		}

		private void handleOn() {
			if ( "on".equals( lcToken ) ) {
				indent++;
				afterOn = true;
				newline();
				out();
				beginLine = false;
				return;
			}
			handleAfterBetweenAnd();
		}

		private void handleAfterBetweenAnd() {
			if ( afterBetween && "and".equals( lcToken ) ) {
				misc();
				afterBetween = false;
				return;
			}
			handelLogical();
		}

		private void handelLogical() {
			if ( LOGICAL.contains( lcToken ) ) {
				if ( "end".equals( lcToken ) ) {
					indent--;
				}
				newline();
				out();
				beginLine = false;
				return;
			}
			handleWhiteSpace();
		}

		private void handleWhiteSpace() {
			if ( isWhitespace( token ) ) {
				if ( !beginLine ) {
					result.append( " " );
				}
				return;
			}
			misc();
		}

		private void misc() {
			out();
			if ( "between".equals( lcToken ) ) {
				afterBetween = true;
			}
			if ( afterInsert ) {
				newline();
				afterInsert = false;
			}
			else {
				beginLine = false;
				if ( "case".equals( lcToken ) ) {
					indent++;
				}
			}
		}

		private void out() {
			result.append( token );
		}

		private static boolean isFunctionName(String tok) {
			if ( tok == null || tok.length() == 0 ) {
				return false;
			}

			final char begin = tok.charAt( 0 );
			final boolean isIdentifier = Character.isJavaIdentifierStart( begin ) || '"' == begin;
			return isIdentifier &&
					!LOGICAL.contains( tok ) &&
					!END_CLAUSES.contains( tok ) &&
					!QUANTIFIERS.contains( tok ) &&
					!DML.contains( tok ) &&
					!MISC.contains( tok );
		}

		private static boolean isWhitespace(String token) {
			return StringHelper.WHITESPACE.contains( token );
		}

		private void newline() {
			result.append( System.lineSeparator() );
			for ( int i = 0; i < indent; i++ ) {
				result.append( INDENT_STRING );
			}
			beginLine = true;
		}
	}

}
