package org.hibernate.orm.test.mapping.embeddable;

import java.sql.SQLException;
import java.time.LocalTime;
import java.util.Locale;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.type.internal.OracleLocalTimeJdbcType;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Builds the real HQL mutation SQL AST and stops at statement inspection, before JDBC.
/// This verifies converter null cast sizing even where a live XML fixture is restricted.
///
/// @author Steve Ebersole
class OracleConvertedNullCastTests {
	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void convertedNullUsesRelationalPrecision(boolean preservation) throws SQLException {
		final var guard = mock( ConnectionProvider.class );
		when( guard.getConnection() ).thenThrow( new AssertionError( "No JDBC allowed" ) );
		try ( var registry = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, new OracleDialect( DatabaseVersion.make( 19 ) ) )
				.applySetting( AvailableSettings.ALLOW_METADATA_ON_BOOT, false )
				.applySetting( AvailableSettings.TEMPORAL_PRESERVATION, preservation )
				.applySetting( AvailableSettings.ALLOW_UPDATE_OUTSIDE_TRANSACTION, true )
				.applySetting( AvailableSettings.USE_SECOND_LEVEL_CACHE, false )
				.addService( ConnectionProvider.class, guard ).build() ) {
			final var metadata = new MetadataSources( registry ).addAnnotatedClass( XmlRow.class ).buildMetadata();
			try ( var factory = metadata.getSessionFactoryBuilder()
					.applyStatementInspector( sql -> { throw new CapturedSql( sql ); } ).build();
					var session = factory.openSession() ) {
				final var captured = catchThrowableOfType( CapturedSql.class, () -> session.createMutationQuery(
						"update ConvertedNullRow e set e.payload.direct=null,e.payload.converted=null where e.id=1" ).executeUpdate() );
				assertThat( captured ).isNotNull();
				assertThat( captured.getMessage().toLowerCase( Locale.ROOT ) )
						.contains( "timestamp(0)", "to_char", "converted" ).doesNotContain( "timestamp(19)" );
			}
		}
		finally {
			verify( guard, never() ).getConnection();
		}
	}

	static class CapturedSql extends RuntimeException {
		CapturedSql(String sql) {
			super( sql );
		}
	}

	@Entity(name = "ConvertedNullRow")
	static class XmlRow {
		@Id
		int id;
		@JdbcTypeCode(SqlTypes.SQLXML)
		Clock payload;
	}
	@Embeddable
	static class Clock {
		@Column(secondPrecision = 9)
		@JdbcType(OracleLocalTimeJdbcType.class)
		LocalTime direct;
		@Column(secondPrecision = 9)
		@JdbcType(OracleLocalTimeJdbcType.class)
		@Convert(converter = TimeConverter.class)
		TimeValue converted;
	}
	record TimeValue(LocalTime time) {}
	@Converter
	public static class TimeConverter implements AttributeConverter<TimeValue, LocalTime> {
		@Override
		public LocalTime convertToDatabaseColumn(TimeValue value) {
			return value == null ? null : value.time();
		}
		@Override
		public TimeValue convertToEntityAttribute(LocalTime value) {
			return value == null ? null : new TimeValue( value );
		}
	}
}
