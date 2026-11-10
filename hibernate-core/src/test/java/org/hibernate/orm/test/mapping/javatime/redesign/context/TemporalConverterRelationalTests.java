package org.hibernate.orm.test.mapping.javatime.redesign.context;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.RequiresDialect;

import java.time.*;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.persistence.*;
import org.hibernate.MappingException;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.mapping.BasicValue;
import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;
import org.hibernate.type.descriptor.jdbc.TimestampJdbcType;
import org.hibernate.type.internal.TemporalMappingValidator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

/// Application conversion owns the domain value; preservation is assessed for
/// the relational Java value and JDBC descriptor, including identity converters.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
@RequiresDialect(PostgreSQLDialect.class)
@RequiresDialect(OracleDialect.class)
class TemporalConverterRelationalTests {
	@ParameterizedTest
	@ValueSource( booleans = { true, false } )
	void explicitAndAutoConvertersUseRelationalPairs( boolean metadata ) {
		try ( var scope = ContextScope.open( metadata, Map.of( "hibernate.cache.use_second_level_cache", false ),
				Converted.class, Text.class ) ) {
			final var model = scope.metadata.getEntityBinding( Converted.class.getName() );
			for ( String member : new String[] { "text", "identity", "different", "parsed" } ) {
				final var value = ( BasicValue ) model.getProperty( member ).getValue();
				assertThat( value.resolve().getValueConverter() ).isInstanceOf( JpaAttributeConverter.class );
				assertThat( value.getColumnSpan() ).isEqualTo( 1 );
				assertThatCode( () -> TemporalMappingValidator.validate( scope.factory.getJdbcServices().getDialect(),
						value.resolve().getLegacyResolvedBasicType(), member ) ).doesNotThrowAnyException();
			}
			final var identity = ( ( BasicValue ) model.getProperty( "identity" ).getValue() ).resolve();
			assertThat( identity.getDomainJavaType().getJavaTypeClass() ).isEqualTo( LocalDateTime.class );
			assertThat( identity.getRelationalJavaType().getJavaTypeClass() ).isEqualTo( LocalDateTime.class );
			assertThat( identity.getJdbcType() )
					.isExactlyInstanceOf( org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType.class );
			assertThat( scope.factory.getJdbcServices().getDialect()
					.getTemporalPreservation( identity.getRelationalJavaType(), identity.getJdbcType() ).time() )
					.isEqualTo( org.hibernate.dialect.temporaltype.spi.TemporalPreservation.Level.SUPPORTED );
			final var different = ( ( BasicValue ) model.getProperty( "different" ).getValue() ).resolve();
			assertThat( different.getDomainJavaType().getJavaTypeClass() ).isEqualTo( LocalDate.class );
			assertThat( different.getRelationalJavaType().getJavaTypeClass() ).isEqualTo( LocalDateTime.class );
			assertThat( ( ( BasicValue ) model.getProperty( "text" ).getValue() ).resolve().getRelationalJavaType()
					.getJavaTypeClass() ).isEqualTo( String.class );
			writes.set( 0 );
			reads.set( 0 );
			final var row = new Converted();
			row.id = 1;
			row.text = OffsetDateTime.parse( "2026-07-01T12:00+05:30" );
			row.identity = LocalDateTime.of( 2026, 7, 1, 12, 0 );
			row.different = LocalDate.of( 2026, 7, 1 );
			row.parsed = "2026-07-01T12:00";
			scope.tx( session -> session.persist( row ) );
			assertThat( writes.get() ).isGreaterThanOrEqualTo( 4 );
			scope.measure();
			scope.tx( session -> {
				var actual = session.find( Converted.class, 1 );
				assertThat( actual.text ).isEqualTo( row.text );
				assertThat( actual.identity ).isEqualTo( row.identity );
				assertThat( actual.different ).isEqualTo( row.different );
				assertThat( actual.parsed ).isEqualTo( row.parsed );
			} );
			assertThat( reads.get() ).isGreaterThanOrEqualTo( 4 );
			assertThat( scope.sql ).anyMatch( sql -> sql.startsWith( "select " ) );
		}
	}

	@ParameterizedTest
	@ValueSource( booleans = { true, false } )
	void unknownRelationalPairsRejectWithActiveDisabledAndAbsentConverters( boolean metadata ) {
		for ( Class<?> type : new Class<?>[] { UnknownIdentity.class, UnknownDifferent.class, UnknownString.class,
				Disabled.class, Unconverted.class } )
			assertThatThrownBy( () -> {
				try ( var ignored = ContextScope.boot( metadata, true, type ) ) {
				}
			} ).isInstanceOf( MappingException.class ).hasMessageContaining( "preservation" )
					.hasMessageContaining( "stamp" );
		try ( var scope = ContextScope.boot( metadata, false, UnknownString.class, Disabled.class,
				Unconverted.class ) ) {
			final var value = ( BasicValue ) scope.metadata.getEntityBinding( UnknownString.class.getName() )
					.getProperty( "stamp" ).getValue();
			assertThat( value.resolve().getDomainJavaType().getJavaTypeClass() ).isEqualTo( String.class );
			assertThat( value.resolve().getRelationalJavaType().getJavaTypeClass() ).isEqualTo( LocalDateTime.class );
			assertThatThrownBy( () -> TemporalMappingValidator.validate( scope.factory.getJdbcServices().getDialect(),
					value.resolve().getLegacyResolvedBasicType(), "UnknownString.stamp" ) )
					.isInstanceOf( MappingException.class ).hasMessageContaining( "preservation" );
		}
	}

	@ParameterizedTest
	@ValueSource( booleans = { true, false } )
	void nonTemporalDomainStillRequiresOffsetPreservation( boolean metadata ) {
		if ( ContextEnvironment.is( PostgreSQLDialect.class ) ) {
			assertThatThrownBy( () -> {
				try ( var ignored = ContextScope.boot( metadata, true, OffsetCarrier.class ) ) {
				}
			} ).isInstanceOf( MappingException.class ).hasMessageContaining( "OFFSET preservation" );
		} else
			try ( var scope = ContextScope.open( metadata, Map.of( "hibernate.cache.use_second_level_cache", false ),
					OffsetCarrier.class ) ) {
				final var row = new OffsetCarrier();
				row.id = 1;
				row.stamp = "2026-07-01T12:00+02:00";
				scope.tx( session -> session.persist( row ) );
				scope.tx(
						session -> assertThat( session.find( OffsetCarrier.class, 1 ).stamp ).isEqualTo( row.stamp ) );
			}
	}

	@ParameterizedTest
	@ValueSource( booleans = { true, false } )
	void converterDoesNotDiscardNativeConstraint( boolean metadata ) {
		assertThatThrownBy( () -> {
			try ( var ignored = ContextScope.boot( metadata, true, NativeText.class ) ) {
			}
		} ).isInstanceOf( MappingException.class ).hasMessageContaining( "native timezone storage" )
				.hasMessageContaining( "stamp" );
		try ( var ignored = ContextScope.boot( metadata, false, NativeText.class ) ) {
		}
	}

	@ParameterizedTest
	@ValueSource( booleans = { true, false } )
	@org.hibernate.testing.orm.junit.RequiresDialect( org.hibernate.dialect.H2Dialect.class )
	void legacySqlTimeCarrierUsesExistingPrecisionRules( boolean metadata ) {
		try ( var scope = ContextScope.open( metadata, Map.of( "hibernate.cache.use_second_level_cache", false ),
				ConvertedTime.class ) ) {
			final var row = new ConvertedTime();
			row.id = 1;
			row.stamp = LocalTime.parse( "12:34:56.123456" );
			scope.tx( session -> session.persist( row ) );
			scope.tx( session -> assertThat( session.find( ConvertedTime.class, 1 ).stamp )
					.isEqualTo( LocalTime.of( 12, 34, 56 ) ) );
		}
	}

	@ParameterizedTest
	@ValueSource( booleans = { true, false } )
	@org.hibernate.testing.orm.junit.RequiresDialect( org.hibernate.dialect.H2Dialect.class )
	void javaTimeRelationalCarrierRetainsPrecisionChecks( boolean metadata ) {
		assertThatThrownBy( () -> {
			try ( var ignored = ContextScope.boot( metadata, true, ImpreciseTime.class ) ) {
			}
		} ).isInstanceOf( MappingException.class ).hasMessageContaining( "millisecond precision" )
				.hasMessageContaining( "stamp" );
	}

	static final AtomicInteger writes = new AtomicInteger(), reads = new AtomicInteger();

	@Converter( autoApply = true )
	public static class Text implements AttributeConverter<OffsetDateTime, String> {
		public String convertToDatabaseColumn( OffsetDateTime value ) {
			writes.incrementAndGet();
			return value == null ? null : value.toString();
		}

		public OffsetDateTime convertToEntityAttribute( String value ) {
			reads.incrementAndGet();
			return value == null ? null : OffsetDateTime.parse( value );
		}
	}

	@Converter
	public static class Identity implements AttributeConverter<LocalDateTime, LocalDateTime> {
		public LocalDateTime convertToDatabaseColumn( LocalDateTime value ) {
			writes.incrementAndGet();
			return value;
		}

		public LocalDateTime convertToEntityAttribute( LocalDateTime value ) {
			reads.incrementAndGet();
			return value;
		}
	}

	@Converter
	public static class Different implements AttributeConverter<LocalDate, LocalDateTime> {
		public LocalDateTime convertToDatabaseColumn( LocalDate value ) {
			writes.incrementAndGet();
			return value == null ? null : value.atStartOfDay();
		}

		public LocalDate convertToEntityAttribute( LocalDateTime value ) {
			reads.incrementAndGet();
			return value == null ? null : value.toLocalDate();
		}
	}

	@Converter
	public static class SqlTime implements AttributeConverter<LocalTime, java.sql.Time> {
		public java.sql.Time convertToDatabaseColumn( LocalTime value ) {
			return value == null ? null : java.sql.Time.valueOf( value );
		}

		public LocalTime convertToEntityAttribute( java.sql.Time value ) {
			return value == null ? null : value.toLocalTime();
		}
	}

	@Entity( name = "RelationalConverted" )
	static class Converted {
		@Id
		int id;
		OffsetDateTime text;
		@Column( name = "identity_stamp" )
		@Convert( converter = Identity.class )
		LocalDateTime identity;
		@Convert( converter = Different.class )
		LocalDate different;
		@Convert( converter = Parsed.class )
		String parsed;
	}

	@Entity( name = "RelationalDisabled" )
	static class Disabled {
		@Id
		int id;
		@Convert( converter = Identity.class, disableConversion = true )
		@JdbcType( TimestampJdbcType.class )
		LocalDateTime stamp;
	}

	@Entity( name = "RelationalUnconverted" )
	static class Unconverted {
		@Id
		int id;
		@JdbcType( TimestampJdbcType.class )
		LocalDateTime stamp;
	}

	@Entity( name = "RelationalNativeText" )
	static class NativeText {
		@Id
		int id;
		@Convert( converter = Text.class )
		@TimeZoneStorage( TimeZoneStorageType.NATIVE )
		OffsetDateTime stamp;
	}

	@Entity( name = "RelationalConvertedTime" )
	static class ConvertedTime {
		@Id
		int id;
		@Column( secondPrecision = 6 )
		@Convert( converter = SqlTime.class )
		LocalTime stamp;
	}

	@Converter
	public static class Parsed implements AttributeConverter<String, LocalDateTime> {
		public LocalDateTime convertToDatabaseColumn( String value ) {
			writes.incrementAndGet();
			return value == null ? null : LocalDateTime.parse( value );
		}

		public String convertToEntityAttribute( LocalDateTime value ) {
			reads.incrementAndGet();
			return value == null ? null : value.toString();
		}
	}

	@Converter
	public static class OffsetParsed implements AttributeConverter<String, OffsetDateTime> {
		public OffsetDateTime convertToDatabaseColumn( String value ) {
			return value == null ? null : OffsetDateTime.parse( value );
		}

		public String convertToEntityAttribute( OffsetDateTime value ) {
			return value == null ? null : value.toString();
		}
	}

	@Entity( name = "RelationalUnknownIdentity" )
	static class UnknownIdentity {
		@Id
		int id;
		@Convert( converter = Identity.class )
		@JdbcType( TimestampJdbcType.class )
		LocalDateTime stamp;
	}

	@Entity( name = "RelationalUnknownDifferent" )
	static class UnknownDifferent {
		@Id
		int id;
		@Convert( converter = Different.class )
		@JdbcType( TimestampJdbcType.class )
		LocalDate stamp;
	}

	@Entity( name = "RelationalUnknownString" )
	static class UnknownString {
		@Id
		int id;
		@Convert( converter = Parsed.class )
		@JdbcType( TimestampJdbcType.class )
		String stamp;
	}

	@Entity( name = "RelationalOffsetCarrier" )
	static class OffsetCarrier {
		@Id
		int id;
		@Convert( converter = OffsetParsed.class )
		String stamp;
	}

	@Converter
	public static class TimeCarrier implements AttributeConverter<LocalDateTime, LocalTime> {
		public LocalTime convertToDatabaseColumn( LocalDateTime value ) {
			return value == null ? null : value.toLocalTime();
		}

		public LocalDateTime convertToEntityAttribute( LocalTime value ) {
			return value == null ? null : LocalDate.of( 2026, 7, 1 ).atTime( value );
		}
	}

	@Entity( name = "RelationalImpreciseTime" )
	static class ImpreciseTime {
		@Id
		int id;
		@Column( secondPrecision = 6 )
		@Convert( converter = TimeCarrier.class )
		@JdbcType( org.hibernate.type.descriptor.jdbc.TimeJdbcType.class )
		LocalDateTime stamp;
	}
}
