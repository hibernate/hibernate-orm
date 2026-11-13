package org.hibernate.orm.test.mapping.javatime.redesign;

import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.mapping.BasicValue;
import org.hibernate.type.internal.TemporalPrecisionBasicType;
import org.hibernate.annotations.TemporalPreservationPolicy;
import org.hibernate.annotations.TimeZoneRepresentation;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.jdbc.spi.PostgreSQLDriverKind;
import org.hibernate.dialect.temporaltype.internal.StandardTemporalPrecision;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionContext;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionSupport;
import org.hibernate.dialect.type.internal.PostgreSQLOffsetDateTimeJdbcType;
import org.hibernate.dialect.type.internal.PostgreSQLOffsetTimeJdbcType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaType;
import org.hibernate.type.descriptor.java.OffsetTimeJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.OffsetDateTimeJdbcType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Pure, independently stated precision vectors and conservative profile boundaries.
/// No database, metadata lookup or synthetic executing descriptor is needed.
///
/// @author Steve Ebersole
class PostgreSQLPrecisionContractTests {
	private static final PostgreSQLDialect PG = new PostgreSQLDialect( DatabaseVersion.make( 18 ) );

	@Test
	void timestampEpochTiesDoubleRoundingAndIdempotence() {
		final var support = timestamp( PG, 3, "timestamp(3) with time zone", TemporalPrecisionContext.Operation.ATTRIBUTE );
		for ( String[] pair : new String[][] {
				{ "1999-12-31T23:59:59.123500Z", "1999-12-31T23:59:59.123Z" },
				{ "2000-01-01T00:00:00.123500Z", "2000-01-01T00:00:00.124Z" },
				{ "2026-12-31T23:59:59.999499999Z", "2027-01-01T00:00:00Z" },
				{ "1999-12-31T23:59:59.999999500Z", "2000-01-01T00:00:00Z" },
				{ "2000-01-01T01:59:59.123500+02:00", "2000-01-01T01:59:59.123+02:00" }
		} ) {
			final var expected = OffsetDateTime.parse( pair[1] );
			assertThat( support.quantize( OffsetDateTime.parse( pair[0] ) ) ).isEqualTo( expected );
			assertThat( support.quantize( expected ) ).isEqualTo( expected );
			assertThat( support.reconstruct( expected ) ).isEqualTo( expected.withOffsetSameInstant( ZoneOffset.UTC ) );
		}
		assertThat( support.quantize( null ) ).isNull();
		assertThatThrownBy( () -> support.quantize( OffsetDateTime.MAX ) ).isInstanceOf( RuntimeException.class );
		assertThatThrownBy( () -> support.quantize( OffsetDateTime.parse( "+294276-12-31T23:59:59.999999999Z" ) ) )
				.hasMessageContaining( "finite" );
		assertThatThrownBy( () -> support.quantize( OffsetDateTime.parse( "2026-01-01T00:00+16:00" ) ) )
				.hasMessageContaining( "-15:59:59" );
	}

	@Test
	void nativeTimeTruncationAndMidnightAreDistinctFromUtcTime() {
		final var support = resolve( OffsetTimeJavaType.INSTANCE, PostgreSQLOffsetTimeJdbcType.INSTANCE,
				PG, 3, "time(3) with time zone", TemporalPrecisionContext.Operation.ATTRIBUTE );
		assertThat( support.quantize( OffsetTime.parse( "01:00:00.123499999+05:45:30" ) ) )
				.isEqualTo( OffsetTime.parse( "01:00:00.123+05:45:30" ) );
		assertThat( support.quantize( OffsetTime.parse( "23:59:59.999999500+05:45:30" ) ) )
				.isEqualTo( OffsetTime.parse( "00:00+05:45:30" ) );
	}

	@Test
	void onlyTheEvidencedProviderDescriptorAndOperationAreAdmitted() {
		for ( Dialect dialect : List.of( new PostgreSQLDialect( DatabaseVersion.make( 15 ) ),
				new PostgreSQLDialect( DatabaseVersion.make( 19 ) ),
				new PostgreSQLDialect( DatabaseVersion.make( 18 ), PostgreSQLDriverKind.VERT_X ),
				new PostgreSQLDialect( DatabaseVersion.make( 18 ), PostgreSQLDriverKind.OTHER ),
				new PostgreSQLDialect( DatabaseVersion.make( 18 ) ) {} ) ) {
			assertThat( timestamp( dialect, 3, "timestamp(3) with time zone", TemporalPrecisionContext.Operation.ATTRIBUTE ).isSupported() ).isFalse();
		}
		for ( var operation : TemporalPrecisionContext.Operation.values() ) {
			assertThat( timestamp( PG, 3, "timestamp(3) with time zone", operation ).isSupported() )
					.isEqualTo( operation == TemporalPrecisionContext.Operation.ATTRIBUTE );
		}
		for ( JdbcType jdbc : List.of( OffsetDateTimeJdbcType.INSTANCE, new OffsetDateTimeJdbcType() {} ) ) {
			assertThat( resolve( OffsetDateTimeJavaType.INSTANCE, jdbc, PG, 3, "timestamp(3) with time zone",
					TemporalPrecisionContext.Operation.ATTRIBUTE ).isSupported() ).isFalse();
		}
		for ( String ddl : List.of( "timestamp(9) with time zone", "timestamp(3)", "time(3) with time zone", "custom_timestamp" ) ) {
			assertThat( timestamp( PG, 3, ddl, TemporalPrecisionContext.Operation.ATTRIBUTE ).isSupported() ).isFalse();
		}
		assertThat( timestamp( PG, 6, "timestamp(3) with time zone", TemporalPrecisionContext.Operation.ATTRIBUTE ).isSupported() ).isFalse();
		for ( Integer precision : new Integer[] { null, 0, 3, 6 } ) {
			assertThat( timestamp( PG, precision, "timestamp with time zone", TemporalPrecisionContext.Operation.ATTRIBUTE ).isSupported() ).isTrue();
		}
	}

	@Test
	void automaticAndExplicitPrecisionBootstrapWithoutConnections() {
		for ( boolean preservation : new boolean[] { true, false } ) {
			final var guard = new TemporalPrecisionBootstrapTests.NoConnections();
			try ( var registry = new StandardServiceRegistryBuilder()
					.applySetting( "hibernate.dialect", PG )
					.applySetting( "hibernate.boot.allow_jdbc_metadata_access", false )
					.applySetting( "hibernate.type.temporal_preservation", preservation )
					.applySetting( "hibernate.cache.use_second_level_cache", false )
					.addService( ConnectionProvider.class, guard ).build() ) {
				final var metadata = new MetadataSources( registry ).addAnnotatedClass( Sized.class ).buildMetadata();
				for ( String property : List.of( "automatic", "seconds", "micros" ) ) {
					final var value = (BasicValue) metadata.getEntityBinding( Sized.class.getName() ).getProperty( property ).getValue();
					final var type = TemporalPrecisionBasicType.find( value.resolve().getJdbcMapping() );
					if ( preservation ) {
						assertThat( type ).isNotNull();
						assertThat( type.getPrecisionPlan().getContext().precision() ).isEqualTo( property.equals( "micros" ) ? 6 : 0 );
						assertThat( type.getPrecisionPlan().getContext().declaredPrecision() )
								.isEqualTo( property.equals( "automatic" ) ? null : property.equals( "micros" ) ? 6 : 0 );
					}
					else { assertThat( type ).isNull(); }
				}
				try ( var factory = metadata.buildSessionFactory() ) { assertThat( factory.isOpen() ).isTrue(); }
				assertThat( guard.attempts ).hasValue( 0 );
			}
		}
	}

	@Entity
	static class Sized {
		@Id int id;
		OffsetTime automatic;
		@Column(secondPrecision = 0) OffsetTime seconds;
		@Column(secondPrecision = 6) OffsetTime micros;
	}

	private static TemporalPrecisionSupport timestamp(Dialect dialect, Integer precision, String ddl, TemporalPrecisionContext.Operation operation) {
		return resolve( OffsetDateTimeJavaType.INSTANCE, PostgreSQLOffsetDateTimeJdbcType.INSTANCE, dialect, precision, ddl, operation );
	}

	private static TemporalPrecisionSupport resolve(JavaType<?> java, JdbcType jdbc, Dialect dialect, Integer precision,
			String ddl, TemporalPrecisionContext.Operation operation) {
		return StandardTemporalPrecision.resolve( new TemporalPrecisionContext( java, jdbc, dialect, jdbc.getDdlTypeCode(),
				ddl, precision, precision, null, null, false, "test", operation, TemporalPreservationPolicy.Type.NORMALIZED,
				TimeZoneRepresentation.Type.INLINE, null ) );
	}
}
