package org.hibernate.orm.test.mapping.javatime.redesign;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.annotations.TemporalPreservationPolicy;
import org.hibernate.annotations.TimeZoneRepresentation;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionContext;
import org.hibernate.dialect.type.internal.PostgreSQLOffsetDateTimeJdbcType;
import org.hibernate.dialect.type.internal.PostgreSQLOffsetTimeJdbcType;
import org.hibernate.type.descriptor.java.OffsetTimeJavaType;
import org.hibernate.type.descriptor.java.InstantJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaType;
import org.hibernate.type.descriptor.java.LocalTimeJavaType;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.LocalTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsPlainJdbcTimestampJdbcType;
import org.hibernate.type.descriptor.jdbc.internal.UtcTimestampJdbcType;
import org.hibernate.type.internal.TemporalPrecisionPlan;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Production controls remain independent of synthetic bootstrap prerequisites.
/// In particular, DDL/policy tests passing do not certify deferred providers.
///
/// @author Steve Ebersole
class TemporalProviderPrecisionContractTests {
	@Test
	void hsqlBoundariesAndOperationIsolation() {
		final var dialect = new HSQLDialect( DatabaseVersion.make( 2, 7, 4 ) );
		for ( int p : new int[] { 0, 3, 6, 9 } ) {
			for ( var operation : TemporalPrecisionContext.Operation.values() ) {
				final var context = context( dialect, LocalDateTimeJavaType.INSTANCE, LocalDateTimeJdbcType.INSTANCE,
						"timestamp(" + p + ")", p, operation );
				final var support = dialect.getTemporalPrecisionSupport( context );
				if ( operation == TemporalPrecisionContext.Operation.ATTRIBUTE || operation == TemporalPrecisionContext.Operation.VERSION ) {
					final var plan = new TemporalPrecisionPlan( context, support );
					for ( String text : List.of( "1969-12-31T23:59:59.999999500", "2026-12-31T23:59:59.999499999" ) ) {
						final var input = LocalDateTime.parse( text );
						int quantum = 1;
						for ( int i = p; i < 9; i++ ) { quantum *= 10; }
						final var expected = input.withNano( input.getNano() / quantum * quantum );
						assertThat( plan.prepare( input ) ).isEqualTo( expected );
						assertThat( plan.prepare( plan.prepare( input ) ) ).isEqualTo( expected );
						assertThat( plan.reconstructPrepared( expected ) ).isEqualTo( expected );
					}
					assertThat( plan.prepare( null ) ).isNull();
				}
				else { assertThat( support.isSupported() ).as( operation.name() ).isFalse(); }
			}
		}
		for ( String ddl : List.of( "timestamp(3)", "timestamp(10)", "timestamp(6) with time zone", "application_timestamp", "time(6)" ) ) {
			assertThat( dialect.getTemporalPrecisionSupport( context( dialect, LocalDateTimeJavaType.INSTANCE,
					LocalDateTimeJdbcType.INSTANCE, ddl, 6, TemporalPrecisionContext.Operation.ATTRIBUTE ) ).isSupported() ).as( ddl ).isFalse();
		}
		for ( Dialect other : List.of( new HSQLDialect(), new HSQLDialect( DatabaseVersion.make( 2, 7, 4 ) ) {} ) ) {
			assertThat( other.getTemporalPrecisionSupport( context( other, LocalDateTimeJavaType.INSTANCE,
					LocalDateTimeJdbcType.INSTANCE, "timestamp(6)", 6, TemporalPrecisionContext.Operation.ATTRIBUTE ) ).isSupported() ).isFalse();
		}
		assertThat( dialect.getTemporalPrecisionSupport( context( dialect, new LocalDateTimeJavaType() {},
				LocalDateTimeJdbcType.INSTANCE, "timestamp(6)", 6, TemporalPrecisionContext.Operation.ATTRIBUTE ) ).isSupported() ).isFalse();
		assertThat( dialect.getTemporalPrecisionSupport( context( dialect, LocalDateTimeJavaType.INSTANCE,
				new LocalDateTimeJdbcType() {}, "timestamp(6)", 6, TemporalPrecisionContext.Operation.ATTRIBUTE ) ).isSupported() ).isFalse();
	}


	@Test
	void hsqlPhysicalDefaultsAndCoarserLogicalGrid() {
		final var dialect = new HSQLDialect( DatabaseVersion.make( 2, 7 ) );
		final var input = LocalDateTime.parse( "2026-01-01T12:00:00.123456789" );
		for ( String ddl : List.of( "timestamp(3)", "timestamp(3) without time zone" ) ) {
			final var context = context( dialect, LocalDateTimeJavaType.INSTANCE, LocalDateTimeJdbcType.INSTANCE,
					ddl, null, TemporalPrecisionContext.Operation.ATTRIBUTE );
			assertThat( new TemporalPrecisionPlan( context, dialect.getTemporalPrecisionSupport( context ) ).prepare( input ) )
					.isEqualTo( input.withNano( 123_000_000 ) );
		}
		final var coarser = context( dialect, LocalDateTimeJavaType.INSTANCE, LocalDateTimeJdbcType.INSTANCE,
				"timestamp(6)", 3, TemporalPrecisionContext.Operation.ATTRIBUTE );
		assertThat( new TemporalPrecisionPlan( coarser, dialect.getTemporalPrecisionSupport( coarser ) ).prepare( input ) )
				.isEqualTo( input.withNano( 123_000_000 ) );
		final var omitted = context( dialect, LocalDateTimeJavaType.INSTANCE, LocalDateTimeJdbcType.INSTANCE,
				"timestamp", null, TemporalPrecisionContext.Operation.ATTRIBUTE );
		assertThat( new TemporalPrecisionPlan( omitted, dialect.getTemporalPrecisionSupport( omitted ) ).prepare( input ) )
				.isEqualTo( input.withNano( 123_456_000 ) );
		refused( context( dialect, InstantJavaType.INSTANCE,
				new UtcTimestampJdbcType( TimestampUtcAsPlainJdbcTimestampJdbcType.INSTANCE ),
				"timestamp(6)", 6, TemporalPrecisionContext.Operation.ATTRIBUTE ) );
	}

	@Test
	void deferredProductionProfilesRemainExplicitlyUnsupported() {
		for ( Dialect dialect : List.of( new PostgreSQLDialect(), new PostgresPlusDialect() ) ) {
			refused( context( dialect, OffsetDateTimeJavaType.INSTANCE, PostgreSQLOffsetDateTimeJdbcType.INSTANCE,
					"timestamp(6) with time zone", 6, TemporalPrecisionContext.Operation.ATTRIBUTE ) );
			refused( context( dialect, OffsetTimeJavaType.INSTANCE, PostgreSQLOffsetTimeJdbcType.INSTANCE,
					"time(0) with time zone", 0, TemporalPrecisionContext.Operation.ATTRIBUTE ) );
		}
		for ( Dialect dialect : List.of( new SybaseASEDialect(), new SpannerDialect(), new SpannerPostgreSQLDialect() ) ) {
			final String ddl = dialect instanceof SybaseASEDialect ? "bigdatetime"
					: dialect instanceof SpannerPostgreSQLDialect ? "timestamp with time zone" : "timestamp";
			for ( int p : new int[] { 0, 6 } ) {
				refused( context( dialect, InstantJavaType.INSTANCE, TimestampUtcAsPlainJdbcTimestampJdbcType.INSTANCE,
						ddl, p, TemporalPrecisionContext.Operation.ATTRIBUTE ) );
			}
		}
		for ( String ddl : List.of( "time", "time(3)", "custom_time" ) ) {
			refused( context( new SQLServerDialect(), LocalTimeJavaType.INSTANCE, LocalTimeJdbcType.INSTANCE,
					ddl, null, TemporalPrecisionContext.Operation.ATTRIBUTE ) );
		}
		refused( context( new SQLServerDialect(), LocalDateTimeJavaType.INSTANCE, LocalDateTimeJdbcType.INSTANCE,
				"datetime2(3)", 3, TemporalPrecisionContext.Operation.ATTRIBUTE ) );
		refused( context( new SQLServerDialect(), InstantJavaType.INSTANCE, TimestampUtcAsPlainJdbcTimestampJdbcType.INSTANCE,
				"datetime2(3)", 3, TemporalPrecisionContext.Operation.ATTRIBUTE ) );
	}

	@Test
	void syntheticBootstrapContributionIsScopedAndCannotExecute() {
		final var foreignFixture = BootstrapOnlyTemporalPrecision.isolate( new SQLServerDialect(), String.class, context -> true );
		refused( context( foreignFixture, LocalTimeJavaType.INSTANCE, LocalTimeJdbcType.INSTANCE,
				"time", null, TemporalPrecisionContext.Operation.ATTRIBUTE ) );
		final var dialect = BootstrapOnlyTemporalPrecision.isolate( new SQLServerDialect(), Fixture.class,
				context -> context.javaType().getClass() == LocalTimeJavaType.class
						&& context.jdbcType().getClass() == LocalTimeJdbcType.class && context.ddlTypeName().equals( "time" ) );
		final var allowed = context( dialect, LocalTimeJavaType.INSTANCE, LocalTimeJdbcType.INSTANCE,
				"time", null, TemporalPrecisionContext.Operation.ATTRIBUTE );
		final var support = dialect.getTemporalPrecisionSupport( allowed );
		assertThat( support.isSupported() ).isTrue();
		assertThatThrownBy( () -> support.quantize( LocalTime.NOON ) ).isInstanceOf( AssertionError.class );
		assertThatThrownBy( () -> support.reconstruct( LocalTime.NOON ) ).isInstanceOf( AssertionError.class );
		refused( context( dialect, LocalTimeJavaType.INSTANCE, LocalTimeJdbcType.INSTANCE,
				"time", null, TemporalPrecisionContext.Operation.CALLABLE ) );
		refused( context( dialect, LocalTimeJavaType.INSTANCE, LocalTimeJdbcType.INSTANCE,
				"custom_time", null, TemporalPrecisionContext.Operation.ATTRIBUTE ) );
	}

	private static void refused(TemporalPrecisionContext context) {
		assertThatThrownBy( () -> new TemporalPrecisionPlan( context, context.dialect().getTemporalPrecisionSupport( context ) ) )
				.isInstanceOf( MappingException.class ).hasMessageContaining( context.role() )
				.hasMessageContaining( context.jdbcType().getClass().getName() );
	}

	private static TemporalPrecisionContext context(Dialect dialect, JavaType<?> java, JdbcType jdbc, String ddl,
			Integer precision, TemporalPrecisionContext.Operation operation) {
		return new TemporalPrecisionContext( java, jdbc, dialect, jdbc.getDdlTypeCode(), ddl, precision, precision,
				null, null, false, Fixture.class.getName() + ".stamp", operation,
				TemporalPreservationPolicy.Type.NORMALIZED, TimeZoneRepresentation.Type.INLINE, null );
	}

	private static class Fixture {
	}
}
