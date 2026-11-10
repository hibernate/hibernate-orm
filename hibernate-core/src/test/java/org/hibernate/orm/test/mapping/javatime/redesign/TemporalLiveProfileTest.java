package org.hibernate.orm.test.mapping.javatime.redesign;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hibernate.orm.test.mapping.javatime.redesign.TemporalLiveProfile.*;

/// A PostgreSQL subclass is not necessarily a PostgreSQL live-test environment.
///
/// @author Steve Ebersole
class TemporalLiveProfileTest {
	@Test
	void establishedProfiles() {
		assertEquals( H2, classify( H2Dialect.class.getName() ) );
		assertEquals( ORACLE, classify( OracleDialect.class.getName() ) );
		assertEquals( POSTGRESQL, classify( PostgreSQLDialect.class.getName() ) );
		assertEquals( POSTGRESQL, classify( PostgresPlusDialect.class.getName() ) );
		assertEquals( POSTGRESQL, classify( "org.hibernate.spatial.dialect.postgis.PostgisPG10Dialect" ) );
	}

	@Test
	void ancestryDoesNotAuthorizeSessionSql() {
		assertEquals( UNSUPPORTED, classify( SpannerPostgreSQLDialect.class.getName() ) );
		assertEquals( UNSUPPORTED, classify( UnknownPostgreSQLDialect.class.getName() ) );
		assertEquals( UNSUPPORTED, classify( null ) );
	}

	private static class UnknownPostgreSQLDialect extends PostgreSQLDialect {}
}
