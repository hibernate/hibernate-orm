package org.hibernate.orm.test.type.temporal.preservation;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.MappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Tests that mappings are accepted and rejected at boot without a
/// connection to the database: what is supported is a matter of the Dialect
/// alone, and nothing is asked of the driver or the database.
///
/// @author Steve Ebersole
class TemporalPreservationOfflineBootTests {
	@Test
	@RequiresDialect(H2Dialect.class)
	@RequiresDialect(HSQLDialect.class)
	@RequiresDialect(value = PostgreSQLDialect.class, matchSubTypes = false)
	@RequiresDialect(MySQLDialect.class)
	@RequiresDialect(SQLServerDialect.class)
	@RequiresDialect(OracleDialect.class)
	@RequiresDialect(value = DB2Dialect.class, matchSubTypes = false)
	void supportedMappingIsAcceptedWithoutAConnection() {
		assertThatCode( () -> boot( WithLocalDate.class ) ).doesNotThrowAnyException();
	}

	@Test
	void unsupportedMappingIsRejectedWithoutAConnection() {
		assertThatThrownBy( () -> boot( WithArray.class ) )
				.isInstanceOf( MappingException.class )
				.hasMessageContaining( "WithArray.theValues" )
				.hasMessageContaining( MappingSettings.TEMPORAL_PRESERVATION );
	}

	private static void boot(Class<?> entityClass) {
		final var registryBuilder = ServiceRegistryUtil.serviceRegistryBuilder();
		registryBuilder.applySetting( MappingSettings.TEMPORAL_PRESERVATION, true );
		registryBuilder.applySetting( JdbcSettings.ALLOW_METADATA_ON_BOOT, false );
		registryBuilder.applySetting( JdbcSettings.DIALECT, DialectContext.getDialect().getClass().getName() );
		registryBuilder.applySetting( JdbcSettings.CONNECTION_PROVIDER, new NoConnections() );
		registryBuilder.applySetting( "hibernate.hbm2ddl.auto", "none" );
		try ( var registry = registryBuilder.build() ) {
			final var metadata = new MetadataSources( registry ).addAnnotatedClass( entityClass ).buildMetadata();
			metadata.buildSessionFactory().close();
		}
	}

	@Entity(name = "WithLocalDate")
	static class WithLocalDate {
		@Id
		Integer id;
		LocalDate theValue;
	}

	@Entity(name = "WithArray")
	static class WithArray {
		@Id
		Integer id;
		LocalDate[] theValues;
	}

	public static class NoConnections implements ConnectionProvider {
		@Override
		public Connection getConnection() throws SQLException {
			throw new SQLException( "No connection may be opened by this test" );
		}

		@Override
		public void closeConnection(Connection connection) {
		}

		@Override
		public boolean supportsAggressiveRelease() {
			return false;
		}

		@Override
		public boolean isUnwrappableAs(Class<?> unwrapType) {
			return false;
		}

		@Override
		public <T> T unwrap(Class<T> unwrapType) {
			throw new UnsupportedOperationException();
		}
	}
}
