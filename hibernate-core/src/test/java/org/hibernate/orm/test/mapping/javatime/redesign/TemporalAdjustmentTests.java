package org.hibernate.orm.test.mapping.javatime.redesign;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.temporaltype.spi.TemporalPreservation;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.type.descriptor.converter.internal.TemporalAdjustmentConverter;
import org.hibernate.type.descriptor.converter.internal.TemporalAdjustmentMutabilityPlan;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaType;
import org.hibernate.type.descriptor.java.OffsetTimeJavaType;
import org.hibernate.type.descriptor.java.ZonedDateTimeJavaType;
import org.hibernate.type.descriptor.jdbc.OffsetDateTimeJdbcType;
import org.hibernate.usertype.internal.ContextualTemporalCompositeUserType;
import org.hibernate.usertype.internal.ZonedDateTimeCompositeUserType;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Behavioral checks for target isolation, whole-value COLUMN adjustment and cache precision.
///
/// @author Steve Ebersole
class TemporalAdjustmentTests {
	@Test
	void targetsRemainMappingSpecific() {
		final var ny = new TemporalAdjustmentConverter<>( OffsetDateTimeJavaType.INSTANCE, ZoneId.of( "America/New_York" ) );
		final var india = new TemporalAdjustmentConverter<>( OffsetDateTimeJavaType.INSTANCE, ZoneOffset.of( "+05:30" ) );
		final var value = OffsetDateTime.parse( "2026-07-01T12:00:00+02:00" );
		assertThat( ny.toRelationalValue( value ) ).isEqualTo( OffsetDateTime.parse( "2026-07-01T06:00:00-04:00" ) );
		assertThat( india.toRelationalValue( value ) ).isEqualTo( OffsetDateTime.parse( "2026-07-01T15:30:00+05:30" ) );
		assertThat( ny.toDomainValue( value ).toInstant() ).isEqualTo( value.toInstant() );
		assertThat( ny.toDomainValue( null ) ).isNull();
		assertThat( value.getOffset() ).isEqualTo( ZoneOffset.ofHours( 2 ) );
	}

	@Test
	void offsetTimeWrapsWithoutReferenceDate() {
		final var converter = new TemporalAdjustmentConverter<>( OffsetTimeJavaType.INSTANCE, ZoneOffset.ofHours( -3 ) );
		assertThat( converter.toRelationalValue( OffsetTime.parse( "01:00+02:00" ) ) )
				.isEqualTo( OffsetTime.parse( "20:00-03:00" ) );
		assertThatThrownBy( () -> new TemporalAdjustmentConverter<>( OffsetTimeJavaType.INSTANCE, ZoneId.of( "UTC" ) ) )
				.isInstanceOf( MappingException.class );
	}

	@Test
	void cacheRetainsPrecisionAndResolvesItsOwnInstant() {
		final var converter = new TemporalAdjustmentConverter<>( ZonedDateTimeJavaType.INSTANCE, ZoneId.of( "America/New_York" ) );
		final var plan = new TemporalAdjustmentMutabilityPlan<>( converter, ZonedDateTimeJavaType.INSTANCE.getMutabilityPlan() );
		final var value = Instant.parse( "2026-03-08T06:59:59.600Z" ).atZone( ZoneOffset.UTC );
		final var cached = plan.assemble( plan.disassemble( value, null ), null );
		assertThat( cached.toInstant() ).isEqualTo( value.toInstant() );
		assertThat( cached.getOffset() ).isEqualTo( ZoneOffset.ofHours( -5 ) );
		assertThat( converter.toDomainValue( value.plusNanos( 400_000_000 ) ).getOffset() ).isEqualTo( ZoneOffset.ofHours( -4 ) );
		assertThat( plan.deepCopy( value ) ).isSameAs( value );
	}

	@Test
	void columnReconstructsBeforeRegionalAdjustment() {
		final var type = new ContextualTemporalCompositeUserType<>( new ZonedDateTimeCompositeUserType(),
				ZonedDateTimeJavaType.INSTANCE, ZoneId.of( "America/New_York" ), "Event.created" );
		final var instant = Instant.parse( "2026-03-08T07:00:00Z" );
		final var result = type.instantiate( values( instant, ZoneOffset.ofHours( -5 ) ) );
		assertThat( result.getHour() ).isEqualTo( 3 );
		assertThat( result.getOffset() ).isEqualTo( ZoneOffset.ofHours( -4 ) );
		assertThat( type.instantiate( values( null, null ) ) ).isNull();
		assertThatThrownBy( () -> type.instantiate( values( instant, null ) ) ).isInstanceOf( HibernateException.class );
	}

	@Test
	void unadjustedColumnCacheCannotRetainOriginalRegion() {
		final var type = new ContextualTemporalCompositeUserType<>( new ZonedDateTimeCompositeUserType(),
				ZonedDateTimeJavaType.INSTANCE, null, "Event.created" );
		final var original = ZonedDateTime.parse( "2026-07-01T12:00:00+02:00[Europe/Paris]" );
		final var result = type.assemble( type.disassemble( original ), null );
		assertThat( result.getZone() ).isEqualTo( original.getOffset() );
		assertThat( result.toInstant() ).isEqualTo( original.toInstant() );
	}

	@Test
	void unknownPairIsConservativeAndPartsAreIndependent() {
		assertThat( new Dialect( org.hibernate.dialect.DatabaseVersion.make( 1 ) ) {}.getTemporalPreservation( OffsetDateTimeJavaType.INSTANCE, OffsetDateTimeJdbcType.INSTANCE ) )
				.satisfies( report -> {
					assertThat( report.instant() ).isEqualTo( TemporalPreservation.Level.UNKNOWN );
					assertThat( report.offset() ).isEqualTo( TemporalPreservation.Level.UNKNOWN );
					assertThat( report.zoneId() ).isEqualTo( TemporalPreservation.Level.IRRELEVANT );
				} );
		assertThatThrownBy( () -> new TemporalPreservation( null, null, null, null, null ) )
				.isInstanceOf( IllegalArgumentException.class );
	}

	@Test
	void adjustmentDoesNotRequireOriginalOffsetButStillRequiresInstant() {
		final var dialect = new Dialect( org.hibernate.dialect.DatabaseVersion.make( 1 ) ) {
			@Override
			public TemporalPreservation getTemporalPreservation(
					org.hibernate.type.descriptor.java.JavaType<?> javaType,
					org.hibernate.type.descriptor.jdbc.JdbcType jdbcType) {
				return new TemporalPreservation( TemporalPreservation.Level.UNKNOWN,
						TemporalPreservation.Level.UNKNOWN, TemporalPreservation.Level.SUPPORTED,
						TemporalPreservation.Level.UNKNOWN, TemporalPreservation.Level.IRRELEVANT );
			}
		};
		assertThatThrownBy( () -> org.hibernate.type.internal.TemporalMappingValidator.validate(
				dialect, OffsetDateTimeJavaType.INSTANCE, OffsetDateTimeJdbcType.INSTANCE, null, "Event.timestamp" ) )
				.isInstanceOf( MappingException.class ).hasMessageContaining( "OFFSET" );
		org.hibernate.type.internal.TemporalMappingValidator.validate(
				dialect, OffsetDateTimeJavaType.INSTANCE, OffsetDateTimeJdbcType.INSTANCE,
				ZoneId.of( "America/New_York" ), "Event.timestamp" );
	}

	@Test
	void missingRequiredInstantAlwaysRejects() {
		for ( var level : java.util.List.of( TemporalPreservation.Level.UNKNOWN,
				TemporalPreservation.Level.UNSUPPORTED, TemporalPreservation.Level.IRRELEVANT ) ) {
			final var dialect = new Dialect( org.hibernate.dialect.DatabaseVersion.make( 1 ) ) {
				@Override
				public TemporalPreservation getTemporalPreservation(
						org.hibernate.type.descriptor.java.JavaType<?> javaType,
						org.hibernate.type.descriptor.jdbc.JdbcType jdbcType) {
					return new TemporalPreservation( TemporalPreservation.Level.SUPPORTED,
							TemporalPreservation.Level.SUPPORTED, level,
							TemporalPreservation.Level.SUPPORTED, TemporalPreservation.Level.SUPPORTED );
				}
			};
			for ( var javaType : java.util.List.<org.hibernate.type.descriptor.java.JavaType<?>>of( org.hibernate.type.descriptor.java.InstantJavaType.INSTANCE,
					OffsetDateTimeJavaType.INSTANCE, ZonedDateTimeJavaType.INSTANCE ) ) {
				for ( ZoneId target : javaType.getJavaTypeClass() == Instant.class ? new ZoneId[] { null }
						: new ZoneId[] { null, ZoneOffset.UTC, ZoneId.of( "America/New_York" ) } ) {
					assertThatThrownBy( () -> org.hibernate.type.internal.TemporalMappingValidator.validate(
							dialect, javaType, OffsetDateTimeJdbcType.INSTANCE, target, "Event.timestamp" ) )
							.isInstanceOf( MappingException.class ).hasMessageContaining( "INSTANT" ).hasMessageContaining( level.name() );
				}
			}
		}
	}

	@Test
	void directCallableRegistrationUsesPhysicalJdbcCodes() throws java.sql.SQLException {
		for ( org.hibernate.type.descriptor.jdbc.JdbcType type : java.util.List.of(
				org.hibernate.type.descriptor.jdbc.LocalDateJdbcType.INSTANCE,
				org.hibernate.type.descriptor.jdbc.LocalTimeJdbcType.INSTANCE,
				org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType.INSTANCE,
				org.hibernate.type.descriptor.jdbc.OffsetTimeJdbcType.INSTANCE,
				org.hibernate.type.descriptor.jdbc.OffsetDateTimeJdbcType.INSTANCE,
				org.hibernate.type.descriptor.jdbc.ZonedDateTimeJdbcType.INSTANCE ) ) {
			final var statement = org.mockito.Mockito.mock( java.sql.CallableStatement.class );
			type.registerOutParameter( statement, 1 );
			type.registerOutParameter( statement, "result" );
			org.mockito.Mockito.verify( statement ).registerOutParameter( 1, type.getDdlTypeCode() );
			org.mockito.Mockito.verify( statement ).registerOutParameter( "result", type.getDdlTypeCode() );
		}
	}

	private static ValueAccess values(Object... values) {
		return () -> values;
	}
}
