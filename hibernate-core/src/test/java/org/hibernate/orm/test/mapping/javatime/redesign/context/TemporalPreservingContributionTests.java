package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.util.Optional;
import java.util.function.UnaryOperator;
import org.hibernate.type.descriptor.jdbc.TemporalPrecisionJdbcType;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionContext;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionSupport;

import java.util.Map;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.JdbcTypeRegistration;
import org.hibernate.annotations.Temporal;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.temporal.internal.H2TemporalTableSupport;
import org.hibernate.dialect.temporal.spi.TemporalTableSupport;
import org.hibernate.dialect.temporal.spi.TemporalPeriodPrecisionContext;
import org.hibernate.dialect.temporal.spi.TemporalPeriodPrecisionSupport;
import org.hibernate.type.descriptor.DateTimeUtils;
import org.hibernate.dialect.temporaltype.spi.StandardTemporalPreservation;
import org.hibernate.dialect.temporaltype.spi.TemporalPreservation;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsJdbcTimestampJdbcType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// A custom JDBC contribution needs preservation facts and a separate server producer.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
class TemporalPreservingContributionTests {
	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void unknownPairAndUnsupportedProducerAreDistinct(boolean metadata) {
		assertThatThrownBy( () -> {
			try ( var ignored = ContextScope.boot( metadata, false,
					Map.of( MappingSettings.TEMPORAL_PRESERVATION, true ), CustomPeriod.class ) ) { }
		} ).hasMessageContaining( "INSTANT" ).hasMessageContaining( "UNKNOWN" );

		assertThatThrownBy( () -> {
			try ( var ignored = ContextScope.boot( metadata, false,
					Map.of( MappingSettings.TEMPORAL_PRESERVATION, true, "hibernate.dialect", PreservationOnly.class ), CustomPeriod.class ) ) {}
		} ).hasMessageContaining( "Unsupported temporal period precision" ).hasMessageContaining( "CustomPeriod.rowStart" );

		try ( var scope = ContextScope.boot( metadata, false,
				Map.of( MappingSettings.TEMPORAL_PRESERVATION, true, "hibernate.dialect", Contributed.class ), CustomPeriod.class ) ) {
			final var value = (org.hibernate.mapping.BasicValue) scope.metadata.getEntityBinding( CustomPeriod.class.getName() )
					.getAuxiliaryColumn( "rowStart" ).getValue();
			assertThat( value.resolve().getJdbcType() ).isExactlyInstanceOf( CustomUtc.class );
		}
		assertThatThrownBy( () -> {
			try ( var ignored = ContextScope.boot( metadata, false,
					Map.of( MappingSettings.TEMPORAL_PRESERVATION, true, "hibernate.dialect", Contributed.class,
							"hibernate.temporal.use_server_transaction_timestamps", true ), CustomPeriod.class ) ) { }
		} ).hasMessageContaining( "inline system-period producer" );
	}

	public static class CustomUtc extends TimestampUtcAsJdbcTimestampJdbcType  implements TemporalPrecisionJdbcType {
		@Override
		public Optional<TemporalPrecisionSupport> getTemporalPrecisionSupport(TemporalPrecisionContext context) {
			return TemporalTestPrecision.decimal( context, UnaryOperator.identity() );
		}

		@Override public int getDdlTypeCode() { return SqlTypes.TIMESTAMP; }
	}
	public static class PreservationOnly extends H2Dialect {
		@Override
		public TemporalPreservation getTemporalPreservation(JavaType<?> javaType, JdbcType jdbcType) {
			return jdbcType.getClass() == CustomUtc.class
					? StandardTemporalPreservation.instantOnly( javaType ) : super.getTemporalPreservation( javaType, jdbcType );
		}
	}
	public static class Contributed extends PreservationOnly {
		@Override
		public TemporalTableSupport getTemporalTableSupport() {
			return new H2TemporalTableSupport( this ) {
				@Override
				public TemporalPeriodPrecisionSupport getTemporalPeriodPrecisionSupport(TemporalPeriodPrecisionContext context) {
					if ( context.jdbcType().getClass() == CustomUtc.class
							&& context.source() == TemporalPeriodPrecisionContext.Source.JAVA
							&& context.precision() == 6 && context.ddlType().equals( "timestamp(6)" ) ) {
						return TemporalPeriodPrecisionSupport.supported( "custom-utc-micros-java",
								value -> DateTimeUtils.roundToSecondPrecision( value, 6 ), null );
					}
					return super.getTemporalPeriodPrecisionSupport( context );
				}
			};
		}
	}
	@Entity(name = "ContributedPreservingPeriod")
	@Temporal
	@JdbcTypeRegistration(value = CustomUtc.class, registrationCode = SqlTypes.TIMESTAMP)
	static class CustomPeriod { @Id int id; }
}
