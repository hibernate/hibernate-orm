package org.hibernate.orm.test.function.json;

import org.hibernate.community.dialect.MySQLLegacyDialect;
import org.hibernate.community.dialect.SingleStoreDialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.jdbc.spi.MySQLServerConfiguration;

import org.junit.jupiter.api.Test;

import static org.hibernate.orm.test.function.json.JsonTestHelper.containsOutsideQuotes;
import static org.hibernate.orm.test.function.json.JsonTestHelper.usesBackslashEscapes;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the SQL scanner used by {@link JsonTestHelper}, so that the injection tests can't pass vacuously.
 */
public class JsonTestHelperTest {

	@Test
	public void testBackslashEscapingDetection() {
		assertTrue( usesBackslashEscapes( new MySQLDialect() ) );
		assertTrue( usesBackslashEscapes( new MySQLLegacyDialect() ) );
		assertTrue( usesBackslashEscapes( new SingleStoreDialect() ) );
		assertTrue( usesBackslashEscapes( new SpannerDialect() ) );

		assertFalse( usesBackslashEscapes( new MySQLDialect(
				DatabaseVersion.make( 8 ),
				new MySQLServerConfiguration( 4, true, null )
		) ) );
		assertFalse( usesBackslashEscapes( new MySQLLegacyDialect( DatabaseVersion.make( 8 ), 4, true ) ) );
		assertFalse( usesBackslashEscapes( new H2Dialect() ) );
		assertFalse( usesBackslashEscapes( new PostgreSQLDialect() ) );
		assertFalse( usesBackslashEscapes( new OracleDialect() ) );
		assertFalse( usesBackslashEscapes( new SQLServerDialect() ) );
	}

	@Test
	public void testStandardEscaping() {
		// Properly escaped single quote
		assertFalse( containsOutsideQuotes( "select json_value(x,'$''--') from t", "--", false ) );
		// Unescaped single quote
		assertTrue( containsOutsideQuotes( "select json_value(x,'$'--') from t", "--", false ) );
		// Properly escaped double quote within quoted identifier
		assertFalse( containsOutsideQuotes( "select json_object(\"\"\"--\" value 1)", "--", false ) );
		// Unescaped double quote
		assertTrue( containsOutsideQuotes( "select json_object(\"\"--\" value 1)", "--", false ) );
		// A backslash is a regular character without backslash escaping
		assertTrue( containsOutsideQuotes( "select json_value(x,'$\\'--') from t", "--", false ) );
		assertFalse( containsOutsideQuotes( "select json_value(x,'$\\''--') from t", "--", false ) );
	}

	@Test
	public void testBackslashEscaping() {
		// Properly escaped backslash and single quote
		assertFalse( containsOutsideQuotes( "select json_value(x,'$\\\\''--') from t", "--", true ) );
		assertFalse( containsOutsideQuotes( "select json_value(x,'$\\\\\\'--') from t", "--", true ) );
		// The backslash escapes the first quote, so the second quote closes the literal
		assertTrue( containsOutsideQuotes( "select json_value(x,'$\\''--') from t", "--", true ) );
	}
}
