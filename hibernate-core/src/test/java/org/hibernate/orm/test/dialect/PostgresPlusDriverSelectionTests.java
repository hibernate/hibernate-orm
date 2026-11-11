package org.hibernate.orm.test.dialect;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.dialect.type.spi.PostgreSQLJdbcTypes;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeConstructor;
import org.hibernate.type.spi.TypeConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.dialect.jdbc.spi.PostgreSQLDriverKind.OTHER;
import static org.hibernate.dialect.jdbc.spi.PostgreSQLDriverKind.PG_JDBC;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Unknown EDB driver identity must not select PGobject just because pgJDBC is installed.
/// Constructor and contribution controls are offline; live aggregate tests verify execution.
///
/// @author Steve Ebersole
class PostgresPlusDriverSelectionTests {
	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "EDB JDBC Driver", "another JDBC driver" })
	void edbDefaultsToCasting(String driverName) {
		assertThat( new PostgresPlusDialect( info( driverName ) ).getDriverKind() ).isEqualTo( OTHER );
	}

	@Test
	void preservesKnownPgJdbcAndPostgresqlDefaults() {
		assertThat( new PostgresPlusDialect().getDriverKind() ).isEqualTo( OTHER );
		assertThat( new PostgresPlusDialect( DatabaseVersion.make( 17 ) ).getDriverKind() ).isEqualTo( OTHER );
		assertThat( new PostgresPlusDialect( info( "PostgreSQL JDBC Driver" ) ).getDriverKind() ).isEqualTo( PG_JDBC );
		assertThat( new PostgreSQLDialect().getDriverKind() ).isEqualTo( PG_JDBC );
		assertThat( new PostgreSQLDialect( info( null ) ).getDriverKind() ).isEqualTo( PG_JDBC );
	}

	@Test
	void metadataDisabledBootstrapUsesCastingWithoutConnections() throws Exception {
		final var provider = mock( ConnectionProvider.class );
		when( provider.getConnection() ).thenThrow( new AssertionError( "No bootstrap JDBC access" ) );
		try ( var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( JdbcSettings.DIALECT, PostgresPlusDialect.class.getName() )
				.applySetting( JdbcSettings.ALLOW_METADATA_ON_BOOT, false )
				.applySetting( JdbcSettings.JAKARTA_HBM2DDL_DB_VERSION, "17.0" )
				.applySetting( "hibernate.hbm2ddl.auto", "none" )
				.addService( ConnectionProvider.class, provider ).build() ) {
			// Both drivers are intentionally available; neither class identifies the runtime connection.
			assertThat( Class.forName( "com.edb.Driver" ) ).isNotNull();
			assertThat( PostgreSQLJdbcTypes.isDriverUsable( registry ) ).isTrue();
			final var metadata = (MetadataImplementor) new MetadataSources( registry ).buildMetadata();
			try ( var factory = metadata.buildSessionFactory() ) {
				final var jdbc = metadata.getTypeConfiguration().getJdbcTypeRegistry();
				assertThat( jdbc.getDescriptor( SqlTypes.JSON ) ).isSameAs( PostgreSQLJdbcTypes.castingJsonb() );
				assertThat( jdbc.getDescriptor( SqlTypes.STRUCT ) ).isSameAs( PostgreSQLJdbcTypes.castingStruct() );
				assertThat( jdbc.getDescriptor( SqlTypes.INET ) ).isSameAs( PostgreSQLJdbcTypes.castingInet() );
				assertThat( jdbc.getDescriptor( SqlTypes.INTERVAL_SECOND ) ).isSameAs( PostgreSQLJdbcTypes.castingIntervalSecond() );
				assertThat( jdbc.getConstructor( SqlTypes.JSON_ARRAY ) ).isSameAs( PostgreSQLJdbcTypes.castingJsonbArrayConstructor() );
			}
		}
		verify( provider, never() ).getConnection();
	}

	@ParameterizedTest
	@ValueSource(strings = { "PostgreSQL JDBC Driver", "EDB JDBC Driver" })
	void knownDriverContributionsRemainUnchanged(String driverName) {
		final var dialect = new PostgresPlusDialect( info( driverName ) );
		try ( var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( JdbcSettings.DIALECT, dialect )
				.applySetting( JdbcSettings.ALLOW_METADATA_ON_BOOT, false ).build() ) {
			final var metadata = (MetadataImplementor) new MetadataSources( registry ).buildMetadata();
			final var jdbc = metadata.getTypeConfiguration().getJdbcTypeRegistry();
			final boolean pg = dialect.getDriverKind() == PG_JDBC;
			assertThat( jdbc.getDescriptor( SqlTypes.JSON ).getClass() ).isEqualTo(
					(pg ? PostgreSQLJdbcTypes.driverJsonb( registry ) : PostgreSQLJdbcTypes.castingJsonb()).getClass() );
			assertThat( jdbc.getDescriptor( SqlTypes.STRUCT ).getClass() ).isEqualTo(
					(pg ? PostgreSQLJdbcTypes.driverStruct( registry ) : PostgreSQLJdbcTypes.castingStruct()).getClass() );
		}
	}

	@Test
	void existingContributionsRemainAuthoritative() {
		try ( var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( JdbcSettings.DIALECT, PostgresPlusDialect.class.getName() )
				.applySetting( JdbcSettings.ALLOW_METADATA_ON_BOOT, false ).build() ) {
			final var types = new TypeConfiguration();
			final var jdbc = types.getJdbcTypeRegistry();
			final var custom = mock( JdbcType.class );
			final var constructor = mock( JdbcTypeConstructor.class );
			when( constructor.getDefaultSqlTypeCode() ).thenReturn( SqlTypes.JSON_ARRAY );
			for ( int code : new int[] { SqlTypes.JSON, SqlTypes.STRUCT, SqlTypes.INET, SqlTypes.INTERVAL_SECOND } ) {
				jdbc.addDescriptor( code, custom );
			}
			jdbc.addTypeConstructor( constructor );
			final TypeContributions contributions = () -> types;
			new PostgresPlusDialect().contributeTypes( contributions, registry );
			for ( int code : new int[] { SqlTypes.JSON, SqlTypes.STRUCT, SqlTypes.INET, SqlTypes.INTERVAL_SECOND } ) {
				assertThat( jdbc.getDescriptor( code ) ).isSameAs( custom );
			}
			assertThat( jdbc.getConstructor( SqlTypes.JSON_ARRAY ) ).isSameAs( constructor );
		}
	}

	private static DialectResolutionInfo info(String driverName) {
		final var info = mock( DialectResolutionInfo.class );
		when( info.getDriverName() ).thenReturn( driverName );
		when( info.makeCopyOrDefault( any( DatabaseVersion.class ) ) ).thenReturn( DatabaseVersion.make( 17 ) );
		return info;
	}
}
