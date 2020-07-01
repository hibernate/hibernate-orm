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

				ProcessEnum.start( this );

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

		private enum ProcessEnum {

			PROCESS_COMMA_AFTER_BY_FROM_SELECT {
				@Override
				public void process(FormatProcess p) {
					if ( p.afterByOrSetOrFromOrSelect && ",".equals( p.token ) ) {
						p.out();
						p.newline();
						return;
					}
					PROCESS_COMMA_AFTER_ON.process( p );
				}
			},

			PROCESS_COMMA_AFTER_ON {
				@Override
				public void process(FormatProcess p) {
					if ( p.afterOn && ",".equals( p.token ) ) {
						p.out();
						p.indent--;
						p.newline();
						p.afterOn = false;
						p.afterByOrSetOrFromOrSelect = true;
						return;
					}
					PROCESS_OPEN_PAREN.process( p );
				}
			},

			PROCESS_OPEN_PAREN {
				@Override
				public void process(FormatProcess p) {
					if ( "(".equals( p.token ) ) {
						if ( isFunctionName( p.lastToken ) || p.inFunction > 0 ) {
							p.inFunction++;
						}
						p.beginLine = false;
						if ( p.inFunction > 0 ) {
							p.out();
						}
						else {
							p.out();
							if ( !p.afterByOrSetOrFromOrSelect ) {
								p.indent++;
								p.newline();
								p.beginLine = true;
							}
						}
						p.parensSinceSelect++;
						return;
					}
					PROCESS_CLOSE_PAREN.process( p );
				}
			},

			PROCESS_CLOSE_PAREN {
				@Override
				public void process(FormatProcess p) {
					if ( ")".equals( p.token ) ) {
						p.parensSinceSelect--;
						if ( p.parensSinceSelect < 0 ) {
							p.indent--;
							p.parensSinceSelect = p.parenCounts.removeLast();
							p.afterByOrSetOrFromOrSelect = p.afterByOrFromOrSelects.removeLast();
						}
						if ( p.inFunction > 0 ) {
							p.inFunction--;
						}
						else {
							if ( !p.afterByOrSetOrFromOrSelect ) {
								p.indent--;
								p.newline();
							}
						}
						p.out();
						p.beginLine = false;
						return;
					}
					PROCESS_BEGIN_NEW_CLAUSE.process( p );
				}
			},

			PROCESS_BEGIN_NEW_CLAUSE {
				@Override
				public void process(FormatProcess p) {
					if ( BEGIN_CLAUSES.contains( p.lcToken ) ) {
						if ( !p.afterBeginBeforeEnd ) {
							if ( p.afterOn ) {
								p.indent--;
								p.afterOn = false;
							}
							p.indent--;
							p.newline();
						}
						p.out();
						p.beginLine = false;
						p.afterBeginBeforeEnd = true;
						return;
					}
					PROCESS_END_NEW_CLAUSE.process( p );
				}
			},

			PROCESS_END_NEW_CLAUSE {
				@Override
				public void process(FormatProcess p) {
					if ( END_CLAUSES.contains( p.lcToken ) ) {
						if ( !p.afterBeginBeforeEnd ) {
							p.indent--;
							if ( p.afterOn ) {
								p.indent--;
								p.afterOn = false;
							}
							p.newline();
						}
						p.out();
						if ( !"union".equals( p.lcToken ) ) {
							p.indent++;
						}
						p.newline();
						p.afterBeginBeforeEnd = false;
						p.afterByOrSetOrFromOrSelect = "by".equals( p.lcToken )
								|| "set".equals( p.lcToken )
								|| "from".equals( p.lcToken );
						return;
					}
					PROCESS_SELECT.process( p );
				}
			},

			PROCESS_SELECT {
				@Override
				public void process(FormatProcess p) {
					if ( "select".equals( p.lcToken ) ) {
						p.out();
						p.indent++;
						p.newline();
						p.parenCounts.addLast( p.parensSinceSelect );
						p.afterByOrFromOrSelects.addLast( p.afterByOrSetOrFromOrSelect );
						p.parensSinceSelect = 0;
						p.afterByOrSetOrFromOrSelect = true;
						return;
					}
					PROCESS_UPDATE_INSERT_DELETE.process( p );
				}
			},

			PROCESS_UPDATE_INSERT_DELETE {
				@Override
				public void process(FormatProcess p) {
					if ( DML.contains( p.lcToken ) ) {
						p.out();
						p.indent++;
						p.beginLine = false;
						if ( "update".equals( p.lcToken ) ) {
							p.newline();
						}
						if ( "insert".equals( p.lcToken ) ) {
							p.afterInsert = true;
						}
						return;
					}
					PROCESS_VALUES.process( p );
				}
			},

			PROCESS_VALUES {
				@Override
				public void process(FormatProcess p) {
					if ( "values".equals( p.lcToken ) ) {
						p.indent--;
						p.newline();
						p.out();
						p.indent++;
						p.newline();
						return;
					}
					PROCESS_ON.process( p );
				}
			},

			PROCESS_ON {
				@Override
				public void process(FormatProcess p) {
					if ( "on".equals( p.lcToken ) ) {
						p.indent++;
						p.afterOn = true;
						p.newline();
						p.out();
						p.beginLine = false;
						return;
					}
					PROCESS_AFTER_BETWEEN_AND.process( p );
				}
			},

			PROCESS_AFTER_BETWEEN_AND {
				@Override
				public void process(FormatProcess p) {
					if ( p.afterBetween && "and".equals( p.lcToken ) ) {
						p.misc();
						p.afterBetween = false;
						return;
					}
					PROCESS_LOGICAL.process( p );
				}
			},

			PROCESS_LOGICAL {
				@Override
				public void process(FormatProcess p) {
					if ( LOGICAL.contains( p.lcToken ) ) {
						if ( "end".equals( p.lcToken ) ) {
							p.indent--;
						}
						p.newline();
						p.out();
						p.beginLine = false;
						return;
					}
					PROCESS_WHITE_SPACE.process( p );
				}
			},

			PROCESS_WHITE_SPACE {
				@Override
				public void process(FormatProcess p) {
					if ( isWhitespace( p.token ) ) {
						if ( !p.beginLine ) {
							p.result.append( " " );
						}
						return;
					}
					PROCESS_MISC.process( p );
				}
			},

			PROCESS_MISC {
				@Override
				public void process(FormatProcess p) {
					p.misc();
				}
			};

			/**
			 * Method that actually doing the process work.
			 *
			 * @param p current process
			 */
			abstract public void process(FormatProcess p);

			/**
			 * Start the process using the first ProcessEnum.
			 * If it's not the right guy, will handover to the next...
			 *
			 * @param p current process
			 */
			public static void start(FormatProcess p) {
				PROCESS_COMMA_AFTER_BY_FROM_SELECT.process( p );
			}
		}
	}

}
