package org.hibernate.orm.test.mapping.javatime.redesign;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.List;

import jakarta.persistence.ParameterMode;

import org.hibernate.annotations.TemporalPreservationPolicy;
import org.hibernate.annotations.TimeZoneRepresentation;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.jdbc.spi.OracleServerConfiguration;
import org.hibernate.dialect.temporaltype.internal.StandardTemporalCallablePrecision;
import org.hibernate.dialect.temporaltype.internal.StandardTemporalPrecision;
import org.hibernate.dialect.temporaltype.spi.TemporalCallablePrecisionContext;
import org.hibernate.dialect.temporaltype.spi.TemporalCallablePrecisionContext.Access;
import org.hibernate.dialect.temporaltype.spi.TemporalCallablePrecisionContext.Boundary;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionContext;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionSupport;
import org.hibernate.orm.test.dialect.resolver.TestingDialectResolutionInfo;
import org.hibernate.type.descriptor.java.InstantJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaType;
import org.hibernate.type.descriptor.java.OffsetTimeJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.TimeUtcAsOffsetTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsPlainJdbcTimestampJdbcType;
import org.hibernate.type.descriptor.jdbc.internal.UtcTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.internal.UtcTimestampJdbcType;
import org.hibernate.type.internal.BasicTypeImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Pure Oracle profile boundaries and independent precision vectors. Constructing
/// the supplied Dialect configuration requires no driver or database connection.
///
/// @author Steve Ebersole
class OraclePrecisionContractTests {
	private static final OracleDialect ORACLE = oracle( 23, 26, 23, 26 );
	private static final JdbcType STAMP = new UtcTimestampJdbcType( TimestampUtcAsPlainJdbcTimestampJdbcType.INSTANCE );
	private static final JdbcType CLOCK = new UtcTimeJdbcType( TimeUtcAsOffsetTimeJdbcType.INSTANCE );

	@Test
	void directRoundingCarryAndReconstruction() {
		final var support = resolve( ORACLE, OffsetDateTimeJavaType.INSTANCE, STAMP, 3, "timestamp(3)", TemporalPrecisionContext.Operation.ATTRIBUTE );
		for ( String[] pair : new String[][] {
				{ "1969-12-31T23:59:59.123499999Z", "1969-12-31T23:59:59.123Z" },
				{ "1969-12-31T23:59:59.123500Z", "1969-12-31T23:59:59.124Z" },
				{ "1999-12-31T23:59:59.999500Z", "2000-01-01T00:00Z" },
				{ "2026-12-31T23:59:59.999499999+05:45:30", "2026-12-31T23:59:59.999+05:45:30" }
		} ) {
			final var expected = OffsetDateTime.parse( pair[1] );
			assertThat( support.quantize( OffsetDateTime.parse( pair[0] ) ) ).isEqualTo( expected );
			assertThat( support.quantize( expected ) ).isEqualTo( expected );
			assertThat( support.reconstruct( expected ) ).isEqualTo( expected.withOffsetSameInstant( ZoneOffset.UTC ) );
		}
		assertThat( support.quantize( null ) ).isNull();
		assertThatThrownBy( () -> support.quantize( OffsetDateTime.parse( "9999-12-31T23:59:59.999999999Z" ) ) ).hasMessageContaining( "finite" );
		assertThatThrownBy( () -> support.quantize( OffsetDateTime.MAX ) ).isInstanceOf( RuntimeException.class );
		final var instant = resolve( ORACLE, InstantJavaType.INSTANCE, TimestampUtcAsPlainJdbcTimestampJdbcType.INSTANCE,
				9, "timestamp(9)", TemporalPrecisionContext.Operation.ATTRIBUTE );
		assertThat( instant.quantize( Instant.parse( "-4712-11-24T00:00:00Z" ) ) ).isEqualTo( Instant.parse( "-4712-11-24T00:00:00Z" ) );
		assertThatThrownBy( () -> instant.quantize( Instant.parse( "-4712-11-23T00:00:00Z" ) ) ).hasMessageContaining( "finite" );
		final var clock = resolve( ORACLE, OffsetTimeJavaType.INSTANCE, CLOCK, 3, "timestamp(3) with time zone", TemporalPrecisionContext.Operation.ATTRIBUTE );
		assertThat( clock.quantize( OffsetTime.parse( "23:59:59.999500Z" ) ) ).isEqualTo( OffsetTime.parse( "00:00Z" ) );
		assertThatThrownBy( () -> clock.quantize( OffsetTime.parse( "00:00+01:00" ) ) ).hasMessageContaining( "UTC-adjusted" );
	}

	@Test
	void exactVersionsPairsDefinitionsAndOperationsOnly() {
		for ( var dialect : List.of( oracle( 19, 0, 23, 26 ), oracle( 23, 27, 23, 26 ), oracle( 23, 26, 19, 0 ),
				new OracleDialect( DatabaseVersion.make( 23, 26 ) ),
				new OracleDialect( TestingDialectResolutionInfo.forDatabaseInfo( "Oracle", 23, 26 ), new OracleServerConfiguration( false, false, 23, 26 ) ) {} ) ) {
			assertThat( resolve( dialect, OffsetDateTimeJavaType.INSTANCE, STAMP, 3, "timestamp(3)", TemporalPrecisionContext.Operation.ATTRIBUTE ).isSupported() ).isFalse();
		}
		for ( var operation : TemporalPrecisionContext.Operation.values() ) {
			assertThat( resolve( ORACLE, OffsetDateTimeJavaType.INSTANCE, STAMP, 3, "timestamp(3)", operation ).isSupported() )
					.isEqualTo( operation == TemporalPrecisionContext.Operation.ATTRIBUTE );
		}
		for ( var ddl : List.of( "timestamp(10)", "time(3)", "timestamp(3) with time zone", "opaque" ) ) {
			assertThat( resolve( ORACLE, OffsetDateTimeJavaType.INSTANCE, STAMP, 3, ddl, TemporalPrecisionContext.Operation.ATTRIBUTE ).isSupported() ).isFalse();
		}
		assertThat( resolve( ORACLE, OffsetDateTimeJavaType.INSTANCE, STAMP, 6, "timestamp(3)", TemporalPrecisionContext.Operation.ATTRIBUTE ).isSupported() ).isFalse();
		assertThat( resolve( ORACLE, new OffsetDateTimeJavaType() {}, STAMP, 3, "timestamp(3)", TemporalPrecisionContext.Operation.ATTRIBUTE ).isSupported() ).isFalse();
		assertThat( resolve( ORACLE, OffsetTimeJavaType.INSTANCE, new UtcTimeJdbcType( new TimeUtcAsOffsetTimeJdbcType() {} ),
				3, "timestamp(3) with time zone", TemporalPrecisionContext.Operation.ATTRIBUTE ).isSupported() ).isFalse();
		for ( Integer precision : new Integer[] { null, 0, 3, 6, 9 } ) {
			assertThat( resolve( ORACLE, OffsetDateTimeJavaType.INSTANCE, STAMP, precision, "timestamp(9)", TemporalPrecisionContext.Operation.ATTRIBUTE ).isSupported() ).isTrue();
		}
	}

	@Test
	void callableGridIsIndependentOfColumnAndRoutinePrecision() {
		final var type = new BasicTypeImpl<>( OffsetDateTimeJavaType.INSTANCE, STAMP );
		for ( var access : List.of( Access.INDEXED, Access.NAMED ) ) {
			final var support = StandardTemporalCallablePrecision.resolve( new TemporalCallablePrecisionContext(
					type, ORACLE, STAMP.getJdbcTypeCode(), ParameterMode.IN, Boundary.INPUT, access, Access.NONE,
					false, "test", TemporalPreservationPolicy.Type.DOMAIN_VALUE, ZoneOffset.UTC ) );
			assertThat( support.isSupported() ).isEqualTo( access == Access.INDEXED );
			if ( support.isSupported() ) {
				final var value = OffsetDateTime.parse( "1969-12-31T23:59:59.123456789Z" );
				assertThat( support.quantize( value ) ).isEqualTo( value );
			}
		}
	}

	private static OracleDialect oracle(int major, int minor, int driverMajor, int driverMinor) {
		return new OracleDialect( TestingDialectResolutionInfo.forDatabaseInfo( "Oracle", major, minor ),
				new OracleServerConfiguration( false, false, driverMajor, driverMinor ) );
	}

	private static TemporalPrecisionSupport resolve(OracleDialect dialect, JavaType<?> java, JdbcType jdbc,
			Integer precision, String ddl, TemporalPrecisionContext.Operation operation) {
		return StandardTemporalPrecision.resolve( new TemporalPrecisionContext( java, jdbc, dialect, jdbc.getDdlTypeCode(),
				ddl, precision, precision, null, null, false, "test", operation, TemporalPreservationPolicy.Type.DOMAIN_VALUE,
				TimeZoneRepresentation.Type.INLINE, ZoneOffset.UTC ) );
	}
}
