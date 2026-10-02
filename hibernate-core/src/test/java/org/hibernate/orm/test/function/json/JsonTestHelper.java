package org.hibernate.orm.test.function.json;

import org.hibernate.exception.SQLGrammarException;
import org.hibernate.testing.util.ExceptionUtil;

import java.util.Locale;

public class JsonTestHelper {
	public static <T extends Throwable> void assertNoJsonInjection(T e) throws T {
		assertNoJsonInjection( e, "'--" );
	}

	public static <T extends Throwable> void assertNoJsonInjection(T e, String injectionString) throws T {
		final SQLGrammarException grammarException = e instanceof SQLGrammarException ex
				? ex : (SQLGrammarException) ExceptionUtil.findCause( e, SQLGrammarException.class );
		if ( grammarException != null ) {
			final String sql = grammarException.getSQL().toLowerCase( Locale.ROOT );
			final char escapeChar = injectionString.charAt( 0 );
			int index = -1;
			// Check if the SQL contains the injected string without a proper escaping
			while ( ( index = sql.indexOf( injectionString, index + 1 ) ) != -1 ) {
				if ( sql.charAt( index - 1 ) != escapeChar ) {
					throw e;
				}
			}
		}
	}
}
