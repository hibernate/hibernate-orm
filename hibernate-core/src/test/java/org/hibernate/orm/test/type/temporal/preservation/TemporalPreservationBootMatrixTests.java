package org.hibernate.orm.test.type.temporal.preservation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.orm.test.type.temporal.preservation.TemporalPreservationBootMatrix.Cell;

import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import static org.assertj.core.api.Assertions.assertThat;

/// Holds the published support manifest to what actually boots.
///
/// The manifest, `temporal-preservation-support.csv`, is included verbatim in
/// the User Guide. For the database and JDBC driver of the current test
/// profile, the cells booted here must be exactly the rows the manifest
/// publishes for the Dialect. A Dialect absent from the manifest must have
/// every Java Time class rejected, so that nothing is supported without
/// being published, and nothing is published without being supported.
///
/// The booted cells and the versions of the database and driver are written
/// to `target/temporal-preservation`, from where a manifest row is copied
/// when support changes.
///
/// @author Steve Ebersole
class TemporalPreservationBootMatrixTests {
	private static final String MANIFEST = "temporal-preservation-support.csv";

	@Test
	void manifestMatchesWhatBoots() throws IOException {
		final List<Cell> cells = TemporalPreservationBootMatrix.bootWithPolicies(
				Map.of( MappingSettings.TEMPORAL_PRESERVATION, true )
		);
		assertThat( cells ).hasSize( TemporalPreservationBootMatrix.JAVA_TIME_CLASSES.size() + 12 );
		record( cells );

		final String dialect = cells.get( 0 ).dialect();
		final List<String> published = manifestRows( dialect );
		if ( published.isEmpty() ) {
			assertThat( cells )
					.as( "%s is not in the manifest, so nothing may be supported", dialect )
					.allSatisfy( cell -> assertThat( cell.outcome() ).isEqualTo( "REJECTED" ) );
		}
		else {
			assertThat( cells.stream().map( Cell::toCsv ) )
					.as( "manifest rows for %s", dialect )
					.containsExactlyElementsOf( published );
		}
	}

	/// Asking for direct JDBC access as well changes nothing.
	@Test
	void directJdbcSettingIsImplied() {
		assertThat( TemporalPreservationBootMatrix.boot( Map.of(
				MappingSettings.TEMPORAL_PRESERVATION, true,
				MappingSettings.JAVA_TIME_USE_DIRECT_JDBC, true
		) ) ).isEqualTo( TemporalPreservationBootMatrix.boot( Map.of( MappingSettings.TEMPORAL_PRESERVATION, true ) ) );
	}

	/// Direct JDBC access alone assesses and rejects nothing. For the types
	/// without an offset or a zone it selects the same JDBC types and column
	/// types as temporal preservation does; for the others, temporal
	/// preservation may add a second column, which direct JDBC access never does.
	@Test
	@DisabledIfSystemProperty(
			named = "hibernate.type.temporal_preservation",
			matches = "true",
			disabledReason = "Verifies the behavior with the setting left unset"
	)
	void directJdbcSettingAloneDoesNotEnable() {
		final List<Cell> direct = TemporalPreservationBootMatrix.boot(
				Map.of( MappingSettings.JAVA_TIME_USE_DIRECT_JDBC, true )
		);
		final List<Cell> preserving = TemporalPreservationBootMatrix.boot(
				Map.of( MappingSettings.TEMPORAL_PRESERVATION, true )
		);
		assertThat( direct ).allSatisfy( cell -> assertThat( cell.outcome() ).isEqualTo( "DISABLED" ) );
		assertThat( direct ).allSatisfy( cell -> assertThat( cell.jdbcType() ).doesNotContain( "+" ) );
		for ( int i = 0; i < direct.size(); i++ ) {
			final String javaClass = direct.get( i ).javaClass();
			// where the offset or zone is stored is decided differently with preservation enabled
			// and a LocalTime on Oracle is exchanged with the driver differently
			if ( !javaClass.equals( "OffsetTime" )
					&& !javaClass.equals( "OffsetDateTime" )
					&& !javaClass.equals( "ZonedDateTime" )
					&& !( javaClass.equals( "LocalTime" ) && direct.get( i ).dialect().equals( "OracleDialect" ) ) ) {
				assertThat( direct.get( i ).jdbcType() ).as( javaClass ).isEqualTo( preserving.get( i ).jdbcType() );
				assertThat( direct.get( i ).columnType() ).as( javaClass ).isEqualTo( preserving.get( i ).columnType() );
			}
		}
	}

	@Test
	@DisabledIfSystemProperty(
			named = "hibernate.type.temporal_preservation",
			matches = "true",
			disabledReason = "Verifies the behavior with the setting left unset"
	)
	void unsetBehavesAsExplicitlyDisabled() {
		final List<Cell> unset = TemporalPreservationBootMatrix.boot( Map.of() );
		assertThat( unset ).allSatisfy( cell -> assertThat( cell.outcome() ).isEqualTo( "DISABLED" ) );
		assertThat( TemporalPreservationBootMatrix.boot( Map.of( MappingSettings.TEMPORAL_PRESERVATION, false ) ) )
				.isEqualTo( unset );
		assertThat( TemporalPreservationBootMatrix.boot( Map.of( MappingSettings.JAVA_TIME_USE_DIRECT_JDBC, false ) ) )
				.isEqualTo( unset );
	}

	private static List<String> manifestRows(String dialect) throws IOException {
		try ( InputStream stream = TemporalPreservationBootMatrixTests.class.getResourceAsStream( MANIFEST ) ) {
			assertThat( stream ).as( MANIFEST ).isNotNull();
			final List<String> lines = new String( stream.readAllBytes(), StandardCharsets.UTF_8 ).lines().toList();
			assertThat( lines.get( 0 ) ).isEqualTo( TemporalPreservationBootMatrix.CSV_HEADER );
			return lines.stream().filter( line -> line.startsWith( dialect + "," ) ).toList();
		}
	}

	private static void record(List<Cell> cells) throws IOException {
		final Path directory = Path.of( "target", "temporal-preservation" );
		Files.createDirectories( directory );
		Files.writeString(
				directory.resolve( "boot-matrix.csv" ),
				TemporalPreservationBootMatrix.CSV_HEADER + "\n"
					+ cells.stream().map( Cell::toCsv ).collect( Collectors.joining( "\n" ) ) + "\n"
		);
		try ( var registry = ServiceRegistryUtil.serviceRegistryBuilder().build();
				var factory = (SessionFactoryImplementor) new MetadataSources( registry )
						.buildMetadata()
						.buildSessionFactory() ) {
			factory.inSession( session -> session.doWork( connection -> {
				final var metaData = connection.getMetaData();
				try {
					Files.writeString(
							directory.resolve( "environment.txt" ),
							"database: " + metaData.getDatabaseProductName() + " "
								+ metaData.getDatabaseProductVersion().replace( '\n', ' ' ) + "\n"
								+ "driver: " + metaData.getDriverName() + " " + metaData.getDriverVersion() + "\n"
								+ "java: " + System.getProperty( "java.runtime.version" ) + "\n"
					);
				}
				catch (IOException e) {
					throw new IllegalStateException( e );
				}
			} ) );
		}
	}
}
