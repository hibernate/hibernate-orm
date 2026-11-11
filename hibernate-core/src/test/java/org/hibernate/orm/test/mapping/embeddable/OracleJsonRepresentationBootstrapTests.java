package org.hibernate.orm.test.mapping.embeddable;

import java.sql.SQLException;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.DialectSpecificSettings;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.type.internal.OracleJsonJdbcType;
import org.hibernate.dialect.type.spi.OracleJdbcTypes;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Representation follows actual contributions, including optional-driver absence,
/// without boot JDBC or mutable state in the shared Oracle aggregate support.
///
/// @author Steve Ebersole
class OracleJsonRepresentationBootstrapTests {
	@ParameterizedTest
	@ValueSource(ints = {19, 21, 23})
	void contributionsAreIsolatedWithoutJdbc(int version) throws SQLException {
		for ( boolean preservation : new boolean[] { true, false } ) {
			final var disabled = boot( version, preservation, true, false );
			final var enabled = boot( version, preservation, false, false );
			final String enabledSql = read( version, enabled );
			final var absent = boot( version, preservation, false, true );
			assertThat( read( version, disabled ) ).contains( "1970-01-01" );
			assertThat( read( version, absent ) ).contains( "1970-01-01" );
			// Recheck earlier factories after subsequent contributions to the shared support instance.
			assertThat( read( version, enabled ) ).isEqualTo( enabledSql );
		}
	}

	private static TypeConfiguration boot(int version, boolean preservation, boolean disabled, boolean hideDriver) throws SQLException {
		final var guard = mock( ConnectionProvider.class );
		when( guard.getConnection() ).thenThrow( new AssertionError( "No boot JDBC" ) );
		final var loaders = new ClassLoaderServiceImpl() {
			@Override public <T> Class<T> classForName(String name) {
				if ( hideDriver && name.startsWith( "oracle." ) ) {
					throw new ClassLoadingException( "Optional Oracle driver hidden by fixture" );
				}
				return super.classForName( name );
			}
		};
		final var bootstrap = new BootstrapServiceRegistryBuilder().applyClassLoaderService( loaders ).build();
		try ( var registry = new StandardServiceRegistryBuilder( bootstrap )
				.applySetting( AvailableSettings.DIALECT, new OracleDialect( DatabaseVersion.make( version ) ) )
				.applySetting( AvailableSettings.ALLOW_METADATA_ON_BOOT, false )
				.applySetting( AvailableSettings.TEMPORAL_PRESERVATION, preservation )
				.applySetting( AvailableSettings.USE_SECOND_LEVEL_CACHE, false )
				.applySetting( DialectSpecificSettings.ORACLE_OSON_DISABLED, disabled )
				.addService( ConnectionProvider.class, guard ).build() ) {
			final boolean oson = version >= 21 && !disabled && OracleJdbcTypes.isOsonAvailable( registry );
			final var metadata = (MetadataImplementor) new MetadataSources( registry ).addAnnotatedClass( Row.class ).buildMetadata();
			final var types = metadata.getTypeConfiguration();
			final var json = types.getJdbcTypeRegistry().getDescriptor( SqlTypes.JSON );
			assertThat( json instanceof OracleJsonJdbcType oracle && oracle.usesOsonTemporalValues() ).isEqualTo( oson );
			assertThat( types.getJdbcTypeRegistry().getConstructor( SqlTypes.JSON_ARRAY ).getClass().getSimpleName() )
					.isEqualTo( oson ? "OracleOsonArrayJdbcTypeConstructor" : "OracleJsonArrayJdbcTypeConstructor" );
			if ( oson ) assertThat( read( version, types ) ).contains( "returning timestamp(9)" ).doesNotContain( "to_timestamp" );
			else assertThat( read( version, types ) ).contains( "1970-01-01" );
			assertThat( OracleLocalTimeAggregateSqlTests.support( version ).supportsComponentCheckConstraints() ).isEqualTo( version >= 23 );
			try ( var factory = metadata.buildSessionFactory() ) {
				assertThat( factory.getSessionFactoryOptions().isTemporalPreservationEnabled() ).isEqualTo( preservation );
			}
			return types;
		}
		finally {
			verify( guard, never() ).getConnection();
			BootstrapServiceRegistryBuilder.destroy( bootstrap );
		}
	}

	private static String read(int version, TypeConfiguration types) {
		return OracleLocalTimeAggregateSqlTests.read( version, SqlTypes.JSON, "doc", "value", OracleLocalTimeAggregateSqlTests.TIME, types );
	}

	@Entity(name = "RepresentationBootRow")
	static class Row {
		@Id
		int id;
	}
}
