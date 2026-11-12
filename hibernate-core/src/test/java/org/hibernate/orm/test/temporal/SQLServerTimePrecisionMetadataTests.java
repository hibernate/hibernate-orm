package org.hibernate.orm.test.temporal;

import java.sql.SQLException;
import java.sql.Time;
import java.util.Date;
import java.util.Calendar;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.function.Consumer;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import org.hibernate.annotations.FractionalSeconds;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.type.spi.StandardDdlTypes;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.mapping.BasicValue;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.LocalTimeJavaType;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaType;
import org.hibernate.type.descriptor.java.InstantJavaType;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsPlainJdbcTimestampJdbcType;
import org.hibernate.orm.test.mapping.javatime.redesign.BootstrapOnlyTemporalPrecision;
import org.hibernate.type.descriptor.jdbc.LocalTimeJdbcType;
import org.hibernate.type.internal.BasicTypeImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Actual SQL Server contributions must distinguish omitted TIME precision from zero,
/// without acquiring a JDBC connection or weakening legacy handler validation.
/// Fixture-only precision prerequisites isolate DDL assertions; they throw on
/// execution and do not certify SQL Server or synthetic custom_time storage.
///
/// @author Steve Ebersole
class SQLServerTimePrecisionMetadataTests {
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void declarationsAndCasts(boolean enabled) throws SQLException {
		metadata( new SQLServerDialect(), enabled, Model.class, model -> {
			assertColumn( model, "omitted", "time", null );
			assertColumn( model, "legacy", "time", null );
			assertColumn( model, "implicitLocalTime", "time", null );
			assertColumn( model, "legacyDate", "time", null );
			assertColumn( model, "legacyCalendar", "time", null );
			assertColumn( model, "converted", "time", null );
			assertColumn( model, "explicitSql", "time(7)", null );
			for ( int p = 0; p <= 7; p++ ) {
				assertColumn( model, "jpa" + p, "time(" + p + ")", p );
				assertColumn( model, "fraction" + p, "time(" + p + ")", p );
			}
			assertColumn( model, "timestamp", "datetime2(3)", 3 );
			assertColumn( model, "instant", enabled ? "datetime2(3)" : "datetimeoffset(3)", 3 );
			final var registry = model.getTypeConfiguration().getDdlTypeRegistry();
			final var time = registry.getDescriptor( SqlTypes.TIME );
			final var mapping = new BasicTypeImpl<>( LocalTimeJavaType.INSTANCE, LocalTimeJdbcType.INSTANCE );
			assertThat( time.getCastTypeName( Size.nil(), mapping, registry ) ).isEqualTo( "time" );
			assertThat( time.getNarrowCastTypeName( Size.nil(), mapping, registry ) ).isEqualTo( "time" );
			for ( int p = 0; p <= 7; p++ ) {
				assertThat( time.getCastTypeName( Size.precision( p ), mapping, registry ) ).isEqualTo( "time(" + p + ")" );
			}
			assertThat( registry.getTypeName( SqlTypes.TIME_UTC, Size.precision( 3 ), null ) ).isEqualTo( "datetimeoffset(3)" );
			assertThat( registry.getTypeName( SqlTypes.TIME_WITH_TIMEZONE, Size.precision( 3 ), null ) ).isEqualTo( "datetimeoffset(3)" );
		} );
	}

	@ParameterizedTest
	@ValueSource(ints = {8, 9, -2})
	void invalidPrecisionDoesNotFallBack(int precision) {
		final var dialect = new SQLServerDialect();
		assertThatThrownBy( () -> dialect.getSizeStrategy().resolveSize(
				LocalTimeJdbcType.INSTANCE, LocalTimeJavaType.INSTANCE, precision, null, null ) )
				.isInstanceOf( IllegalArgumentException.class ).hasMessageContaining( "SQL Server TIME" ).hasMessageContaining( "0 and 7" );
	}

	@Test
	void invalidMappingDeclarationsFailBeforeJdbc() {
		for ( Class<?> model : new Class<?>[] { InvalidJpa.class, InvalidFraction.class } ) {
			assertThatThrownBy( () -> metadata( new SQLServerDialect(), true, model, ignored -> {} ) )
					.hasMessageContaining( "SQL Server TIME" ).hasMessageContaining( "0 and 7" );
		}
	}

	@Test
	void explicitTimeCarrierLimitRemains() {
		assertThatThrownBy( () -> metadata( new SQLServerDialect(), true, MillisecondCarrier.class, model -> {} ) )
				.hasMessageContaining( "millisecond precision" );
	}

	@Test
	void otherDialectAndCustomContributionRemainIndependent() throws SQLException {
		assertThat( new H2Dialect().getSizeStrategy().resolveSize(
				LocalTimeJdbcType.INSTANCE, LocalTimeJavaType.INSTANCE, null, null, null ).getPrecision() ).isZero();
		metadata( new SQLServerDialect() {
			@Override
			protected void registerColumnTypes(TypeContributions contributions, ServiceRegistry services) {
				super.registerColumnTypes( contributions, services );
				contributions.getTypeConfiguration().getDdlTypeRegistry()
						.addDescriptor( StandardDdlTypes.simple( SqlTypes.TIME, "custom_time", this ) );
			}
		}, true, Model.class, model -> assertColumn( model, "omitted", "custom_time", null ) );
	}

	@Test
	void subclassColumnAndCastPatternsRemainAuthoritative() throws SQLException {
		metadata( new SQLServerDialect() {
			@Override
			protected String columnType(int code) {
				return code == SqlTypes.TIME ? "custom_time" : super.columnType( code );
			}
		}, true, Model.class, model -> assertColumn( model, "omitted", "custom_time", null ) );
		metadata( new SQLServerDialect() {
			@Override
			protected String castType(int code) {
				return code == SqlTypes.TIME ? "custom_time_cast" : super.castType( code );
			}
		}, true, Model.class, model -> {
			final var registry = model.getTypeConfiguration().getDdlTypeRegistry();
			final var mapping = new BasicTypeImpl<>( LocalTimeJavaType.INSTANCE, LocalTimeJdbcType.INSTANCE );
			assertThat( registry.getDescriptor( SqlTypes.TIME ).getCastTypeName( Size.nil(), mapping, registry ) )
					.isEqualTo( "custom_time_cast" );
		} );
	}

	private static void assertColumn(MetadataImplementor model, String property, String ddl, Integer precision) {
		final var value = (BasicValue) model.getEntityBinding( Model.class.getName() ).getProperty( property ).getValue();
		final var column = value.getColumns().get( 0 );
		assertThat( column.getSqlType( model ) ).as( property ).isEqualTo( ddl );
		assertThat( column.getColumnSize( model.getDatabase().getDialect(), model ).getPrecision() ).as( property ).isEqualTo( precision );
		if ( property.startsWith( "jpa" ) || property.startsWith( "fraction" ) ) {
			assertThat( value.resolve().getJdbcType() ).isInstanceOf( LocalTimeJdbcType.class );
		}
	}

	private static void metadata(Dialect dialect, boolean enabled, Class<?> entity, Consumer<MetadataImplementor> check) throws SQLException {
		final var guard = mock( ConnectionProvider.class );
		when( guard.getConnection() ).thenThrow( new AssertionError( "Boot JDBC is disabled" ) );
		try ( var registry = new StandardServiceRegistryBuilder()
				.applySetting( JdbcSettings.DIALECT, BootstrapOnlyTemporalPrecision.isolate( dialect, Model.class, context ->
						context.javaType().getClass() == LocalTimeJavaType.class
								&& context.jdbcType().getClass() == LocalTimeJdbcType.class
								&& context.ddlTypeName().matches( "time(?:\\([0-7]\\))?|custom_time" )
						|| context.javaType().getClass() == LocalDateTimeJavaType.class
								&& context.jdbcType().getClass() == LocalDateTimeJdbcType.class
								&& context.ddlTypeName().equals( "datetime2(3)" )
						|| context.javaType().getClass() == InstantJavaType.class
								&& context.jdbcType().getClass() == TimestampUtcAsPlainJdbcTimestampJdbcType.class
								&& context.ddlTypeName().equals( "datetime2(3)" ) ) )
				.applySetting( JdbcSettings.ALLOW_METADATA_ON_BOOT, false )
				.applySetting( JdbcSettings.JDBC_TIME_ZONE, "America/Los_Angeles" )
				.applySetting( MappingSettings.TEMPORAL_PRESERVATION, enabled )
				.applySetting( "hibernate.cache.use_second_level_cache", false )
				.addService( ConnectionProvider.class, guard ).build() ) {
			final var model = (MetadataImplementor) new MetadataSources( registry ).addAnnotatedClass( entity ).buildMetadata();
			model.validate();
			check.accept( model );
			try ( var factory = model.buildSessionFactory() ) {
				assertThat( factory.getSessionFactoryOptions().isTemporalPreservationEnabled() ).isEqualTo( enabled );
			}
		}
		finally { verify( guard, never() ).getConnection(); }
	}

	@Entity(name = "TimePrecisionModel")
	static class Model {
		@Id int id;
		@JdbcTypeCode(SqlTypes.LOCAL_TIME) LocalTime omitted;
		Time legacy;
		LocalTime implicitLocalTime;
		@Temporal(TemporalType.TIME) Date legacyDate;
		@Temporal(TemporalType.TIME) Calendar legacyCalendar;
		@Convert(converter = Identity.class) @JdbcTypeCode(SqlTypes.LOCAL_TIME) LocalTime converted;
		@Column(columnDefinition = "time(7)") @JdbcTypeCode(SqlTypes.LOCAL_TIME) LocalTime explicitSql;
		@Column(secondPrecision = 3) @JdbcTypeCode(SqlTypes.LOCAL_DATE_TIME) LocalDateTime timestamp;
		@Column(secondPrecision = 3) Instant instant;
		@Column(secondPrecision = 0) @JdbcTypeCode(SqlTypes.LOCAL_TIME) LocalTime jpa0;
		@FractionalSeconds(0) @JdbcTypeCode(SqlTypes.LOCAL_TIME) LocalTime fraction0;
		@Column(secondPrecision = 1) @JdbcTypeCode(SqlTypes.LOCAL_TIME) LocalTime jpa1;
		@FractionalSeconds(1) @JdbcTypeCode(SqlTypes.LOCAL_TIME) LocalTime fraction1;
		@Column(secondPrecision = 2) @JdbcTypeCode(SqlTypes.LOCAL_TIME) LocalTime jpa2;
		@FractionalSeconds(2) @JdbcTypeCode(SqlTypes.LOCAL_TIME) LocalTime fraction2;
		@Column(secondPrecision = 3) @JdbcTypeCode(SqlTypes.LOCAL_TIME) LocalTime jpa3;
		@FractionalSeconds(3) @JdbcTypeCode(SqlTypes.LOCAL_TIME) LocalTime fraction3;
		@Column(secondPrecision = 4) @JdbcTypeCode(SqlTypes.LOCAL_TIME) LocalTime jpa4;
		@FractionalSeconds(4) @JdbcTypeCode(SqlTypes.LOCAL_TIME) LocalTime fraction4;
		@Column(secondPrecision = 5) @JdbcTypeCode(SqlTypes.LOCAL_TIME) LocalTime jpa5;
		@FractionalSeconds(5) @JdbcTypeCode(SqlTypes.LOCAL_TIME) LocalTime fraction5;
		@Column(secondPrecision = 6) @JdbcTypeCode(SqlTypes.LOCAL_TIME) LocalTime jpa6;
		@FractionalSeconds(6) @JdbcTypeCode(SqlTypes.LOCAL_TIME) LocalTime fraction6;
		@Column(secondPrecision = 7) @JdbcTypeCode(SqlTypes.LOCAL_TIME) LocalTime jpa7;
		@FractionalSeconds(7) @JdbcTypeCode(SqlTypes.LOCAL_TIME) LocalTime fraction7;
	}

	@Entity
	static class InvalidJpa {
		@Id int id;
		@Column(secondPrecision = 8) @JdbcTypeCode(SqlTypes.LOCAL_TIME) LocalTime value;
	}

	@Entity
	static class InvalidFraction {
		@Id int id;
		@FractionalSeconds(9) @JdbcTypeCode(SqlTypes.LOCAL_TIME) LocalTime value;
	}

	@Entity
	static class MillisecondCarrier {
		@Id int id;
		@Column(secondPrecision = 4) @JdbcTypeCode(SqlTypes.TIME) LocalTime value;
	}

	@Converter
	static class Identity implements AttributeConverter<LocalTime, LocalTime> {
		@Override public LocalTime convertToDatabaseColumn(LocalTime value) { return value; }
		@Override public LocalTime convertToEntityAttribute(LocalTime value) { return value; }
	}
}
