package org.hibernate.orm.test.function.json;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.JDBCException;
import org.hibernate.QueryException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.sql.spi.StringBuilderDialectSqlAppender;

import org.hibernate.testing.jdbc.CollectingStatementObserver;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

/**
 * Helper for asserting that values passed to JSON functions can't break out of SQL string literals.
 * <p>
 * Test classes using this helper must enable
 * {@link org.hibernate.testing.orm.junit.SessionFactory#useCollectingStatementObserver()}.
 */
public class JsonTestHelper {

	/**
	 * The marker that must never appear outside a quoted SQL token.
	 * Injection payloads are expected to contain this marker right after the attempted break-out.
	 */
	private static final String INJECTION_MARKER = "--";

	/**
	 * Runs the given work and asserts that the injection marker contained in the passed values
	 * did not end up outside a quoted SQL token in any of the executed SQL statements.
	 * <p>
	 * The generated SQL is inspected regardless of whether the query succeeds or fails.
	 * The only acceptable failures are
	 * <ul>
	 *     <li>a rejection by the database i.e. a {@link JDBCException}, in which case the SQL is still inspected</li>
	 *     <li>a rejection of the JSON path by the limited JSON path parser that Hibernate uses for emulations,
	 *     in which case no SQL is generated at all, so there is nothing that could be injected</li>
	 * </ul>
	 * Any other failure, or not executing any SQL at all, fails the test,
	 * because escaping could not be verified in that case.
	 */
	public static void assertNoInjection(SessionFactoryScope scope, Consumer<SessionImplementor> work) {
		final CollectingStatementObserver observer = scope.getCollectingStatementObserver();
		observer.clear();
		RuntimeException failure = null;
		try {
			scope.inSession( work );
		}
		catch (RuntimeException e) {
			if ( isJsonPathParserRejection( e ) ) {
				if ( !observer.getSqlQueries().isEmpty() ) {
					throw withFailure( new AssertionError( "JSON path was rejected, but SQL was executed anyway" ), e );
				}
				return;
			}
			if ( findCause( e, JDBCException.class ) == null ) {
				throw e;
			}
			failure = e;
		}
		final List<String> sqls = observer.getSqlQueries();
		if ( sqls.isEmpty() ) {
			throw withFailure( new AssertionError( "No SQL was executed, so escaping could not be verified" ), failure );
		}
		final boolean backslashEscapes = usesBackslashEscapes( scope.getSessionFactory().getJdbcServices().getDialect() );
		for ( String sql : sqls ) {
			if ( containsOutsideQuotes( sql, INJECTION_MARKER, backslashEscapes ) ) {
				throw withFailure( new AssertionError( "Injection marker escaped quoting in SQL: " + sql ), failure );
			}
		}
	}

	private static boolean isJsonPathParserRejection(RuntimeException e) {
		final QueryException queryException = findCause( e, QueryException.class );
		return queryException != null
			&& queryException.getMessage().startsWith( "Can't emulate non-simple json path expression" );
	}

	/**
	 * Finds the first throwable in the cause chain (including the given throwable itself)
	 * that is an instance of the given class. Unlike {@link org.hibernate.testing.util.ExceptionUtil#findCause},
	 * this also matches subclasses e.g. {@link org.hibernate.exception.SQLGrammarException} for {@link JDBCException}.
	 */
	private static <T extends Throwable> T findCause(Throwable t, Class<T> causeClass) {
		for ( Throwable current = t; current != null; current = current.getCause() ) {
			if ( causeClass.isInstance( current ) ) {
				return causeClass.cast( current );
			}
			if ( current.getCause() == current ) {
				break;
			}
		}
		return null;
	}

	private static AssertionError withFailure(AssertionError error, RuntimeException failure) {
		if ( failure != null ) {
			error.addSuppressed( failure );
		}
		return error;
	}

	/**
	 * Determines whether the dialect treats the backslash as escape character within string literals,
	 * by asking the dialect's literal rendering strategy, so that custom strategies and modes like
	 * MySQL's {@code NO_BACKSLASH_ESCAPES} are taken into account.
	 */
	public static boolean usesBackslashEscapes(Dialect dialect) {
		final StringBuilderDialectSqlAppender appender = new StringBuilderDialectSqlAppender( dialect );
		appender.appendLiteralContent( "\\" );
		return appender.toString().equals( "\\\\" );
	}

	/**
	 * Tokenizes single-quoted string literals and double-quoted tokens (identifiers, or strings on MySQL)
	 * and returns whether the marker occurs outside such tokens.
	 * On databases that treat the backslash as escape character (e.g. MySQL by default),
	 * the SQL {@code '\''--'} contains the {@code --} outside a string literal,
	 * which is why the escaping rules of the dialect must be respected.
	 */
	static boolean containsOutsideQuotes(String sql, String marker, boolean backslashEscapes) {
		char quote = 0;
		for ( int i = 0; i < sql.length(); i++ ) {
			final char c = sql.charAt( i );
			if ( quote != 0 ) {
				if ( c == '\\' && backslashEscapes ) {
					// Skip the escaped character
					i++;
				}
				else if ( c == quote ) {
					if ( i + 1 < sql.length() && sql.charAt( i + 1 ) == quote ) {
						// Skip the escaped quote
						i++;
					}
					else {
						quote = 0;
					}
				}
			}
			else if ( c == '\'' || c == '"' ) {
				quote = c;
			}
			else if ( sql.startsWith( marker, i ) ) {
				return true;
			}
		}
		return false;
	}
}
