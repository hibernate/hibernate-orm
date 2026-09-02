/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import org.hibernate.dialect.jdbc.spi.JdbcMetadataOverrides;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.hibernate.dialect.jdbc.spi.JdbcMetadataOverrides.SupportOverride.REPORTED;
import static org.hibernate.dialect.jdbc.spi.JdbcMetadataOverrides.SupportOverride.SUPPORTED;
import static org.hibernate.dialect.jdbc.spi.JdbcMetadataOverrides.SupportOverride.UNSUPPORTED;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Tests the immutable effective-JDBC-metadata override profile.
///
/// @author Steve Ebersole
public class JdbcMetadataOverridesTests {
	@Test
	void standardAndResolutionSemantics() {
		assertProfile( JdbcMetadataOverrides.STANDARD, REPORTED, SUPPORTED, REPORTED );

		assertThat( REPORTED.resolve( false ) ).isFalse();
		assertThat( REPORTED.resolve( true ) ).isTrue();
		assertThat( SUPPORTED.resolve( false ) ).isTrue();
		assertThat( SUPPORTED.resolve( true ) ).isTrue();
		assertThat( UNSUPPORTED.resolve( false ) ).isFalse();
		assertThat( UNSUPPORTED.resolve( true ) ).isFalse();
	}

	@Test
	void buildersCopyAllDimensionsAndCaptureSnapshots() {
		final JdbcMetadataOverrides.Builder builder = JdbcMetadataOverrides.builder()
				.namedParameterSupport( SUPPORTED )
				.batchUpdateSupport( REPORTED )
				.standardRefCursorSupport( UNSUPPORTED );
		final JdbcMetadataOverrides first = builder.build();
		builder.namedParameterSupport( UNSUPPORTED )
				.batchUpdateSupport( UNSUPPORTED )
				.standardRefCursorSupport( SUPPORTED );
		final JdbcMetadataOverrides second = builder.build();

		assertProfile( first, SUPPORTED, REPORTED, UNSUPPORTED );
		assertProfile( second, UNSUPPORTED, UNSUPPORTED, SUPPORTED );
		assertProfile(
				JdbcMetadataOverrides.builder( first )
						.namedParameterSupport( REPORTED )
						.namedParameterSupport( SUPPORTED )
						.build(),
				SUPPORTED,
				REPORTED,
				UNSUPPORTED
		);
	}

	@Test
	@SuppressWarnings("NullAway")
	void rejectsEveryNullInput() {
		assertThatIllegalArgumentException().isThrownBy( () -> JdbcMetadataOverrides.builder( null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> JdbcMetadataOverrides.builder().namedParameterSupport( null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> JdbcMetadataOverrides.builder().batchUpdateSupport( null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> JdbcMetadataOverrides.builder().standardRefCursorSupport( null ) );
	}

	@Test
	void maintainedDb2AndSybaseProfilesPreserveOverrides() {
		final JdbcMetadataOverrides db2 = new DB2Dialect().getJdbcMetadataOverrides();
		assertThat( db2 ).isSameAs( new DB2Dialect().getJdbcMetadataOverrides() );
		assertProfile( db2, REPORTED, SUPPORTED, UNSUPPORTED );

		final SybaseDialect jtds = new SybaseDialect( info(
				"jTDS Type 4 JDBC Driver for MS SQL Server and Sybase" ) );
		assertThat( jtds.getJdbcMetadataOverrides() ).isSameAs( jtds.getJdbcMetadataOverrides() );
		assertProfile( jtds.getJdbcMetadataOverrides(), REPORTED, SUPPORTED, REPORTED );
		final SybaseDialect jconnect = new SybaseDialect( info( "jConnect (TM) for JDBC (TM)" ) );
		assertProfile( jconnect.getJdbcMetadataOverrides(), UNSUPPORTED, SUPPORTED, REPORTED );
		assertProfile( new SybaseDialect().getJdbcMetadataOverrides(), UNSUPPORTED, SUPPORTED, REPORTED );
	}

	private static DialectResolutionInfo info(String driverName) {
		final DialectResolutionInfo info = mock( DialectResolutionInfo.class );
		when( info.getDriverName() ).thenReturn( driverName );
		when( info.getDatabaseVersion() ).thenReturn( "16.0" );
		when( info.getDatabaseMajorVersion() ).thenReturn( 16 );
		return info;
	}

	private static void assertProfile(
			JdbcMetadataOverrides profile,
			JdbcMetadataOverrides.SupportOverride named,
			JdbcMetadataOverrides.SupportOverride batch,
			JdbcMetadataOverrides.SupportOverride refCursor) {
		assertThat( profile.getNamedParameterSupport() ).isSameAs( named );
		assertThat( profile.getBatchUpdateSupport() ).isSameAs( batch );
		assertThat( profile.getStandardRefCursorSupport() ).isSameAs( refCursor );
	}
}
