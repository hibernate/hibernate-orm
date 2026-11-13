package org.hibernate.orm.test.mapping.javatime.redesign;

import org.hibernate.cfg.Environment;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// Providers whose session SQL and fixture DDL are established for the live matrices.
/// Dialect inheritance alone does not establish compatibility with these operations.
///
/// @author Steve Ebersole
public enum TemporalLiveProfile {
	H2, POSTGRESQL, ORACLE, UNSUPPORTED;

	public static TemporalLiveProfile classify(String dialect) {
		if ( dialect == null ) return UNSUPPORTED;
		return switch ( dialect ) {
			case "org.hibernate.dialect.H2Dialect" -> H2;
			case "org.hibernate.dialect.PostgreSQLDialect", "org.hibernate.dialect.PostgresPlusDialect",
			"org.hibernate.spatial.dialect.postgis.PostgisPG93Dialect",
			"org.hibernate.spatial.dialect.postgis.PostgisPG94Dialect",
			"org.hibernate.spatial.dialect.postgis.PostgisPG95Dialect",
			"org.hibernate.spatial.dialect.postgis.PostgisPG10Dialect" -> POSTGRESQL;
			case "org.hibernate.dialect.OracleDialect" -> ORACLE;
			default -> UNSUPPORTED;
		};
	}

	public static TemporalLiveProfile requireSupported() {
		final String dialect = Environment.getProperties().getProperty( "hibernate.dialect" );
		final var profile = classify( dialect );
		assumeTrue( profile != UNSUPPORTED, "Temporal live harness has no established session/DDL profile for " + dialect );
		return profile;
	}
}
