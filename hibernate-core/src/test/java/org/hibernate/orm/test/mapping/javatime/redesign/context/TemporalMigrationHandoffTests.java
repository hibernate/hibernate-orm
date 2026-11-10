package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Stream;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/// Stops compatibility writers before explicit fixture conversion and final preserving readers.
/// The runtime source protocol is identified separately from the historical baseline evidence.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
@RequiresDialect(PostgreSQLDialect.class)
@RequiresDialect(OracleDialect.class)
class TemporalMigrationHandoffTests {
	static Stream<Arguments> versions() {
		String dialect = Environment.getProperties().getProperty( "hibernate.dialect" );
		var cells = new ArrayList<Arguments>();
		for ( boolean database : new boolean[] {false, true} ) {
			for ( boolean metadata : new boolean[] {false, true} ) {
				cells.add( Arguments.of( "M02", "Local", database, metadata ) );
				cells.add( Arguments.of( "M03", "Local", database, metadata ) );
				if ( dialect.contains( "PostgreSQL" ) ) cells.add( Arguments.of( "M01", "Instant", database, metadata ) );
				if ( dialect.contains( "Oracle" ) ) {
					cells.add( Arguments.of( "M06", "Offset", database, metadata ) );
					cells.add( Arguments.of( "M06", "Zoned", database, metadata ) );
				}
			}
		}
		if ( !dialect.contains( "Oracle" ) ) {
			cells.add( Arguments.of( "M07", "Zoned", false, false ) );
			if ( dialect.contains( "PostgreSQL" ) ) cells.add( Arguments.of( "M07", "Offset", false, false ) );
		}
		return cells.stream();
	}

	static Stream<Arguments> periods() {
		boolean postgres = Environment.getProperties().getProperty( "hibernate.dialect" ).contains( "PostgreSQL" );
		var cells = new ArrayList<Arguments>();
		for ( boolean metadata : new boolean[] {false, true} ) {
			for ( boolean server : new boolean[] {false, true} ) cells.add( Arguments.of( postgres ? "M01" : "M04", server, metadata ) );
			if ( !postgres ) cells.add( Arguments.of( "M05", false, metadata ) );
		}
		return cells.stream();
	}

	@ParameterizedTest(name = "{0} periods server={1} metadata={2}")
	@MethodSource("periods")
	void periodsRetainSourceInterpretation(String profile, boolean server, boolean metadata,
			@TempDir(cleanup = org.junit.jupiter.api.io.CleanupMode.ON_SUCCESS) Path directory) throws Exception {
		TemporalHandoffProcess.versions( directory, profile, "Period", server, metadata );
	}

	@ParameterizedTest(name = "{0} {1} database={2} metadata={3}")
	@MethodSource("versions")
	void versionsRetainSourceInterpretation(String profile, String kind, boolean database, boolean metadata,
			@TempDir(cleanup = org.junit.jupiter.api.io.CleanupMode.ON_SUCCESS) Path directory) throws Exception {
		TemporalHandoffProcess.versions( directory, profile, kind, database, metadata );
	}
}
