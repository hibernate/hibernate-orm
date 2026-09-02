/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jdbc.env;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.jdbc.spi.JdbcMetadataOverrides;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentImpl;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.dialect.jdbc.spi.JdbcMetadataOverrides.SupportOverride.SUPPORTED;
import static org.hibernate.dialect.jdbc.spi.JdbcMetadataOverrides.SupportOverride.UNSUPPORTED;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Verifies separation and resolution of raw and effective JDBC metadata.
///
/// @author Steve Ebersole
public class JdbcMetadataResolutionTest {
	@Test
	void overridesNeverRewriteRawReports() throws Exception {
		final JdbcEnvironmentImpl environment = environment(
				metadata( true, false, true, "Fixture JDBC driver", 1 ),
				profile( SUPPORTED, SUPPORTED, UNSUPPORTED )
		);
		final var effective = environment.getJdbcMetadata();
		final var raw = effective.getExtractedDatabaseMetaData();

		assertThat( raw ).isNotSameAs( effective );
		assertThat( raw.supportsNamedParameters() ).isTrue();
		assertThat( raw.supportsBatchUpdates() ).isFalse();
		assertThat( raw.supportsRefCursors() ).isTrue();
		assertThat( effective.supportsNamedParameters() ).isTrue();
		assertThat( effective.supportsBatchUpdates() ).isTrue();
		assertThat( effective.supportsRefCursors() ).isFalse();
		assertThat( environment.getJdbcMetadata() ).isSameAs( effective );
		assertThat( effective.getExtractedDatabaseMetaData() ).isSameAs( raw );
	}

	@Test
	void everyOverrideResolvesBothRawAnswersInEveryDimension() throws Exception {
		for ( boolean reported : new boolean[] { false, true } ) {
			for ( JdbcMetadataOverrides.SupportOverride override :
					JdbcMetadataOverrides.SupportOverride.values() ) {
				assertDimension( reported, override, Dimension.NAMED_PARAMETERS );
				assertDimension( reported, override, Dimension.BATCH_UPDATES );
				assertDimension( reported, override, Dimension.REF_CURSORS );
			}
		}
	}

	@Test
	void oracleCompatibilityInterpretationIsEffectiveOnly() throws Exception {
		assertOracle( 18, true, false );
		assertOracle( 19, true, true );
		assertOracle( 20, true, true );
		assertOracle( 20, false, false );

		final JdbcEnvironmentImpl otherDriver = environment(
				metadata( false, true, true, "Not Oracle", 18 ),
				JdbcMetadataOverrides.STANDARD
		);
		assertThat( otherDriver.getJdbcMetadata().supportsRefCursors() ).isTrue();
	}

	@Test
	void failedSupportProbesUseRawDefaultsBeforeForcedResolution() throws Exception {
		final DatabaseMetaData metadata = metadata( false, false, false, "Fixture JDBC driver", 1 );
		when( metadata.supportsNamedParameters() ).thenThrow( new SQLException( "named" ) );
		when( metadata.supportsBatchUpdates() ).thenThrow( new SQLException( "batch" ) );
		when( metadata.supportsRefCursors() ).thenThrow( new SQLException( "cursor" ) );

		final JdbcEnvironmentImpl environment = environment(
				metadata,
				profile( SUPPORTED, UNSUPPORTED, SUPPORTED )
		);
		final var raw = environment.getJdbcMetadata().getExtractedDatabaseMetaData();
		assertThat( raw.supportsNamedParameters() ).isFalse();
		assertThat( raw.supportsBatchUpdates() ).isTrue();
		assertThat( raw.supportsRefCursors() ).isFalse();
		assertThat( environment.getJdbcMetadata().supportsNamedParameters() ).isTrue();
		assertThat( environment.getJdbcMetadata().supportsBatchUpdates() ).isFalse();
		assertThat( environment.getJdbcMetadata().supportsRefCursors() ).isTrue();
	}

	@Test
	void failedOracleIdentificationPreservesDefensiveFalse() throws Exception {
		final DatabaseMetaData driverNameFailure = metadata( false, true, true, "Oracle JDBC driver", 19 );
		when( driverNameFailure.getDriverName() ).thenThrow( new SQLException( "driver" ) );
		assertThat( environment( driverNameFailure, JdbcMetadataOverrides.STANDARD )
				.getJdbcMetadata().supportsRefCursors() ).isFalse();

		final DatabaseMetaData driverVersionFailure = metadata( false, true, true, "Oracle JDBC driver", 19 );
		when( driverVersionFailure.getDriverMajorVersion() ).thenThrow( new IllegalStateException( "version" ) );
		assertThat( environment( driverVersionFailure, JdbcMetadataOverrides.STANDARD )
				.getJdbcMetadata().supportsRefCursors() ).isFalse();
	}

	@Test
	void capturesImmutableNormalizedIdentifierMetadata() throws Exception {
		final DatabaseMetaData metadata = metadata( false, true, false, "Fixture JDBC driver", 1 );
		when( metadata.storesLowerCaseIdentifiers() ).thenReturn( true );
		when( metadata.storesUpperCaseIdentifiers() ).thenReturn( true );
		when( metadata.storesMixedCaseIdentifiers() ).thenReturn( true );
		when( metadata.storesLowerCaseQuotedIdentifiers() ).thenReturn( true );
		when( metadata.storesUpperCaseQuotedIdentifiers() ).thenReturn( true );
		when( metadata.storesMixedCaseQuotedIdentifiers() ).thenReturn( true );
		when( metadata.getSQLKeywords() ).thenReturn( " Driver_Word, ,SECOND,driver_word " );

		final var jdbcMetadata = environment( metadata, JdbcMetadataOverrides.STANDARD ).getJdbcMetadata();
		final var raw = jdbcMetadata.getExtractedDatabaseMetaData();
		assertThat( jdbcMetadata.isJdbcMetadataAccessible() ).isTrue();
		assertThat( raw.isJdbcMetadataAccessible() ).isTrue();
		assertThat( jdbcMetadata.getUnquotedIdentifierCaseStrategy() ).isEqualTo( IdentifierCaseStrategy.UPPER );
		assertThat( jdbcMetadata.getQuotedIdentifierCaseStrategy() ).isEqualTo( IdentifierCaseStrategy.MIXED );
		assertThat( jdbcMetadata.getSqlKeywords() ).containsExactlyInAnyOrder( "driver_word", "second" );
		assertThat( raw.getSqlKeywords() ).isSameAs( jdbcMetadata.getSqlKeywords() );
		assertThatThrownBy( () -> jdbcMetadata.getSqlKeywords().add( "later" ) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	private static void assertOracle(int driverMajorVersion, boolean reported, boolean effective) throws Exception {
		final JdbcEnvironmentImpl environment = environment(
				metadata( false, true, reported, "Oracle JDBC driver", driverMajorVersion ),
				JdbcMetadataOverrides.STANDARD
		);
		assertThat( environment.getJdbcMetadata().getExtractedDatabaseMetaData().supportsRefCursors() )
				.isEqualTo( reported );
		assertThat( environment.getJdbcMetadata().supportsRefCursors() ).isEqualTo( effective );
	}

	private static void assertDimension(
			boolean reported,
			JdbcMetadataOverrides.SupportOverride override,
			Dimension dimension) throws Exception {
		final JdbcMetadataOverrides.Builder builder = JdbcMetadataOverrides.builder();
		switch ( dimension ) {
			case NAMED_PARAMETERS -> builder.namedParameterSupport( override );
			case BATCH_UPDATES -> builder.batchUpdateSupport( override );
			case REF_CURSORS -> builder.standardRefCursorSupport( override );
		}
		final var metadata = environment(
				metadata( reported, reported, reported, "Fixture JDBC driver", 1 ),
				builder.build()
		).getJdbcMetadata();
		final var raw = metadata.getExtractedDatabaseMetaData();
		switch ( dimension ) {
			case NAMED_PARAMETERS -> {
				assertThat( raw.supportsNamedParameters() ).isEqualTo( reported );
				assertThat( metadata.supportsNamedParameters() ).isEqualTo( override.resolve( reported ) );
			}
			case BATCH_UPDATES -> {
				assertThat( raw.supportsBatchUpdates() ).isEqualTo( reported );
				assertThat( metadata.supportsBatchUpdates() ).isEqualTo( override.resolve( reported ) );
			}
			case REF_CURSORS -> {
				assertThat( raw.supportsRefCursors() ).isEqualTo( reported );
				assertThat( metadata.supportsRefCursors() ).isEqualTo( override.resolve( reported ) );
			}
		}
	}

	private static JdbcMetadataOverrides profile(
			JdbcMetadataOverrides.SupportOverride named,
			JdbcMetadataOverrides.SupportOverride batch,
			JdbcMetadataOverrides.SupportOverride refCursor) {
		return JdbcMetadataOverrides.builder()
				.namedParameterSupport( named )
				.batchUpdateSupport( batch )
				.standardRefCursorSupport( refCursor )
				.build();
	}

	private static JdbcEnvironmentImpl environment(
			DatabaseMetaData metadata,
			JdbcMetadataOverrides overrides) throws SQLException {
		final Dialect dialect = new Dialect( DatabaseVersion.make( 1 ) ) {
			@Override
			public JdbcMetadataOverrides getJdbcMetadataOverrides() {
				return overrides;
			}
		};
		return new JdbcEnvironmentImpl( metadata, dialect, mock( JdbcConnectionAccess.class ) );
	}

	private static DatabaseMetaData metadata(
			boolean named,
			boolean batch,
			boolean refCursor,
			String driverName,
			int driverMajorVersion) throws SQLException {
		final DatabaseMetaData metadata = mock( DatabaseMetaData.class );
		final Connection connection = mock( Connection.class );
		final Statement statement = mock( Statement.class );
		when( metadata.getConnection() ).thenReturn( connection );
		when( metadata.getDatabaseProductName() ).thenReturn( "Fixture database" );
		when( metadata.getDatabaseProductVersion() ).thenReturn( "1" );
		when( metadata.getDriverName() ).thenReturn( driverName );
		when( metadata.getDriverMajorVersion() ).thenReturn( driverMajorVersion );
		when( metadata.supportsNamedParameters() ).thenReturn( named );
		when( metadata.supportsBatchUpdates() ).thenReturn( batch );
		when( metadata.supportsRefCursors() ).thenReturn( refCursor );
		when( metadata.getCatalogSeparator() ).thenReturn( "." );
		when( metadata.isCatalogAtStart() ).thenReturn( true );
		when( connection.createStatement() ).thenReturn( statement );
		return metadata;
	}

	private enum Dimension {
		NAMED_PARAMETERS,
		BATCH_UPDATES,
		REF_CURSORS
	}
}
