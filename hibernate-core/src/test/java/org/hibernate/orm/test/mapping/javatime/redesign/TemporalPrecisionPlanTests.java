package org.hibernate.orm.test.mapping.javatime.redesign;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.function.UnaryOperator;

import org.hibernate.MappingException;
import org.hibernate.TemporalValueException;
import org.hibernate.annotations.TemporalPreservationPolicy;
import org.hibernate.annotations.TimeZoneRepresentation;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.temporaltype.internal.DecimalTemporalPrecision;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionContext;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionSupport;
import org.hibernate.dialect.temporaltype.spi.TemporalValueSemantics.PrecisionAdjustment;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaType;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.OffsetDateTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TemporalPrecisionJdbcType;
import org.hibernate.type.internal.TemporalPrecisionPlan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Pure precision and contribution controls. These establish algorithm semantics,
/// not a claim that an unexecuted provider supports the corresponding storage grid.
///
/// @author Steve Ebersole
class TemporalPrecisionPlanTests {
	@ParameterizedTest
	@CsvSource({
			"12:34:56.1234999,12:34:56.123",
			"12:34:56.1235000,12:34:56.124",
			"12:34:56.1235001,12:34:56.124",
			"12:34:56.9995000,12:34:57",
			"23:59:59.9995000,00:00:00"
	})
	void decimalRounding(String input, String expected) {
		final var quantizer = new DecimalTemporalPrecision( 3, 9, PrecisionAdjustment.ROUND, true );
		final var value = quantizer.apply( LocalTime.parse( input ) );
		assertThat( value ).isEqualTo( LocalTime.parse( expected ) );
		assertThat( quantizer.apply( value ) ).isEqualTo( value );
	}

	@Test
	void stagedRoundingAndTruncationAreDistinctContracts() {
		final var input = LocalTime.parse( "12:34:56.12349999" );
		assertThat( new DecimalTemporalPrecision( 3, 7, PrecisionAdjustment.ROUND_MAX, true ).apply( input ) )
				.isEqualTo( LocalTime.parse( "12:34:56.124" ) );
		assertThat( new DecimalTemporalPrecision( 3, 9, PrecisionAdjustment.ROUND, true ).apply( input ) )
				.isEqualTo( LocalTime.parse( "12:34:56.123" ) );
		assertThat( new DecimalTemporalPrecision( 3, 9, PrecisionAdjustment.TRUNCATE, true )
				.apply( LocalTime.parse( "23:59:59.999999999" ) ) ).isEqualTo( LocalTime.parse( "23:59:59.999" ) );
	}

	@Test
	void preEpochCarryOffsetsAndRange() {
		final var quantizer = new DecimalTemporalPrecision( 6, 9, PrecisionAdjustment.ROUND, true );
		assertThat( quantizer.apply( Instant.parse( "1969-12-31T23:59:59.999999500Z" ) ) ).isEqualTo( Instant.EPOCH );
		assertThat( quantizer.apply( OffsetTime.parse( "23:59:59.999999500+05:45" ) ) )
				.isEqualTo( OffsetTime.parse( "00:00:00+05:45" ) );
		assertThat( quantizer.apply( null ) ).isNull();
		assertThatThrownBy( () -> quantizer.apply( LocalDateTime.MAX ) ).isInstanceOf( TemporalValueException.class )
				.hasMessageContaining( "range" );
	}

	@Test
	void strictRegionCarryRejectsOffsetChange() {
		final var value = ZonedDateTime.of( 2026, 3, 8, 1, 59, 59, 999_999_500, ZoneId.of( "America/New_York" ) );
		assertThatThrownBy( () -> new DecimalTemporalPrecision( 6, 9, PrecisionAdjustment.ROUND, true ).apply( value ) )
				.isInstanceOf( TemporalValueException.class ).hasMessageContaining( "resolved offset" );
		assertThat( new DecimalTemporalPrecision( 6, 9, PrecisionAdjustment.ROUND, false ).apply( value ) )
				.isEqualTo( ZonedDateTime.of( 2026, 3, 8, 3, 0, 0, 0, value.getZone() ) );
	}

	@Test
	void mappingPrecisionIsIsolatedAndExtractionDoesNotRepair() {
		final var three = plan( context( LocalDateTimeJdbcType.INSTANCE, 3 ) );
		final var six = plan( context( LocalDateTimeJdbcType.INSTANCE, 6 ) );
		final var value = LocalDateTime.parse( "2026-03-08T12:34:56.123456789" );
		assertThat( three.prepare( value ) ).isEqualTo( value.withNano( 123_000_000 ) );
		assertThat( six.prepare( value ) ).isEqualTo( value.withNano( 123_457_000 ) );
		assertThat( three.getSignature() ).isNotEqualTo( six.getSignature() );
		assertThat( three.precisionEqual( value, value.withNano( 123_499_999 ) ) ).isTrue();
		assertThat( three.precisionEqual( value, value.withNano( 123_500_000 ) ) ).isFalse();
		assertThatThrownBy( () -> three.validateExtracted( value ) ).isInstanceOf( TemporalValueException.class )
				.hasMessageContaining( "extracted value" ).hasMessageContaining( "TestEntity.value" );
		three.validateExtracted( three.prepare( value ) );
	}

	@Test
	void precisionComparisonDoesNotHideOffsetChanges() {
		final var context = new TemporalPrecisionContext( OffsetDateTimeJavaType.INSTANCE, OffsetDateTimeJdbcType.INSTANCE,
				new H2Dialect(), SqlTypes.TIMESTAMP_WITH_TIMEZONE, "timestamp(3) with time zone", 3, 3, null, null,
				false, "TestEntity.offset", TemporalPrecisionContext.Operation.ATTRIBUTE,
				TemporalPreservationPolicy.Type.NORMALIZED, TimeZoneRepresentation.Type.INLINE, null );
		final var plan = plan( context );
		final var value = OffsetDateTime.parse( "2026-03-08T12:34:56.123456789+05:45" );
		assertThat( plan.precisionEqual( value, value.withOffsetSameInstant( ZoneOffset.UTC ) ) ).isFalse();
	}

	@Test
	void descriptorRefusalAndCyclesAreAuthoritative() {
		final var descriptor = new Contribution();
		final var context = context( descriptor, 3 );
		descriptor.result = Optional.of( TemporalPrecisionSupport.unsupported( "custom refusal" ) );
		assertThatThrownBy( () -> plan( context ) ).isInstanceOf( MappingException.class ).hasMessageContaining( "custom refusal" );
		descriptor.result = null;
		assertThatThrownBy( () -> plan( context ) ).isInstanceOf( MappingException.class ).hasMessageContaining( "null Optional" );
		descriptor.recursive = true;
		assertThatThrownBy( () -> plan( context ) ).isInstanceOf( MappingException.class ).hasMessageContaining( "recursive" );
		descriptor.recursive = false;
		descriptor.result = Optional.of( TemporalPrecisionSupport.supported( "custom-identity-v1", UnaryOperator.identity(), UnaryOperator.identity() ) );
		assertThat( plan( context ).prepare( LocalDateTime.MIN ) ).isEqualTo( LocalDateTime.MIN );
	}

	@Test
	void unknownSubclassesDoNotInheritStockSupport() {
		assertThatThrownBy( () -> plan( context( new LocalDateTimeJdbcType() {}, 3 ) ) )
				.isInstanceOf( MappingException.class ).hasMessageContaining( "Unassessed" );
	}

	private static TemporalPrecisionPlan plan(TemporalPrecisionContext context) {
		return new TemporalPrecisionPlan( context, context.dialect().getTemporalPrecisionSupport( context ) );
	}

	private static TemporalPrecisionContext context(JdbcType jdbc, int precision) {
		return new TemporalPrecisionContext( LocalDateTimeJavaType.INSTANCE, jdbc, new H2Dialect(), SqlTypes.TIMESTAMP,
				"timestamp(" + precision + ")", precision, precision, null, null, false, "TestEntity.value",
				TemporalPrecisionContext.Operation.ATTRIBUTE, TemporalPreservationPolicy.Type.DOMAIN_VALUE,
				TimeZoneRepresentation.Type.INLINE, null );
	}

	private static class Contribution extends LocalDateTimeJdbcType implements TemporalPrecisionJdbcType {
		Optional<TemporalPrecisionSupport> result = Optional.empty();
		boolean recursive;

		@Override
		public Optional<TemporalPrecisionSupport> getTemporalPrecisionSupport(TemporalPrecisionContext context) {
			return recursive ? Optional.of( context.dialect().getTemporalPrecisionSupport( context ) ) : result;
		}
	}
}
