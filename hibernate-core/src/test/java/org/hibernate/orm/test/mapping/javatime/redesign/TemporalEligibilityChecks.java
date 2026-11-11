package org.hibernate.orm.test.mapping.javatime.redesign;

import java.util.Arrays;

import org.hibernate.annotations.TemporalPreservationPolicy.Type;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.temporaltype.spi.TemporalPreservation.TemporalPart;
import org.hibernate.testing.orm.junit.DialectFeatureCheck;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.internal.UtcTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.internal.UtcTimestampJdbcType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaType;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaType;
import org.hibernate.type.descriptor.java.OffsetTimeJavaType;
import org.hibernate.type.internal.TemporalMappingValidator;

import static org.hibernate.dialect.temporaltype.spi.TemporalPreservation.Level.SUPPORTED;
import static org.hibernate.dialect.temporaltype.spi.TemporalPreservation.TemporalPart.DATE;
import static org.hibernate.dialect.temporaltype.spi.TemporalPreservation.TemporalPart.INSTANT;
import static org.hibernate.dialect.temporaltype.spi.TemporalPreservation.TemporalPart.OFFSET;
import static org.hibernate.dialect.temporaltype.spi.TemporalPreservation.TemporalPart.TIME;

/// Pre-factory eligibility for these fixtures' exact built-in descriptor requirements.
/// Resolves dialect contributions without mapping an entity or acquiring a connection.
/// Missing assessments disable positive execution; separate tests assert rejection.
/// These checks do not infer capabilities from dialect ancestry or bootstrap failures.
///
/// @author Steve Ebersole
public final class TemporalEligibilityChecks {
	private TemporalEligibilityChecks() {
	}

	private static boolean supports(Dialect dialect, JavaType<?> javaType, Integer jdbcCode,
			Type producerPolicy, TemporalPart... required) {
		try ( var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( JdbcSettings.DIALECT, dialect )
				.applySetting( JdbcSettings.ALLOW_METADATA_ON_BOOT, false )
				.applySetting( MappingSettings.TEMPORAL_PRESERVATION, true )
				.build() ) {
			final var metadata = (MetadataImplementor) new MetadataSources( registry ).buildMetadata();
			final var types = metadata.getTypeConfiguration();
			final var jdbcType = jdbcCode == null
					? javaType.getRecommendedJdbcType( types.getCurrentBaseSqlTypeIndicators() )
					: jdbcCode == SqlTypes.TIME_UTC
							? new UtcTimeJdbcType( types.getJdbcTypeRegistry().getDescriptor( jdbcCode ) )
							: new UtcTimestampJdbcType( types.getJdbcTypeRegistry().getDescriptor( jdbcCode ) );
			final var report = TemporalMappingValidator.assess( dialect, javaType, jdbcType, "test eligibility" );
			return Arrays.stream( required ).allMatch( part -> report.preserves( part ) == SUPPORTED )
					&& (producerPolicy == null || dialect.getCurrentTemporalSupport()
					.getCurrentTimestampSelection( javaType, jdbcType, producerPolicy ) != null);
		}
	}

	public static class LiveProfile implements DialectFeatureCheck {
		@Override
		public boolean apply(Dialect dialect) {
			return TemporalLiveProfile.classify( dialect.getClass().getName() ) != TemporalLiveProfile.UNSUPPORTED;
		}
	}

	public static class LocalDateTimeProducer implements DialectFeatureCheck {
		@Override
		public boolean apply(Dialect dialect) {
			return supports( dialect, LocalDateTimeJavaType.INSTANCE, null, Type.DOMAIN_VALUE, DATE, TIME );
		}
	}

	public static class NormalizedOffsetDateTimeProducer implements DialectFeatureCheck {
		@Override
		public boolean apply(Dialect dialect) {
			return supports( dialect, OffsetDateTimeJavaType.INSTANCE, null, Type.NORMALIZED, INSTANT );
		}
	}

	public static class InlineOffsetTime implements DialectFeatureCheck {
		@Override
		public boolean apply(Dialect dialect) {
			return supports( dialect, OffsetTimeJavaType.INSTANCE, null, null, TIME, OFFSET );
		}
	}

	public static class UtcOffsetTime implements DialectFeatureCheck {
		@Override
		public boolean apply(Dialect dialect) {
			return supports( dialect, OffsetTimeJavaType.INSTANCE, SqlTypes.TIME_UTC, null, TIME );
		}
	}

	public static class AdjustedOffsetDateTime implements DialectFeatureCheck {
		@Override
		public boolean apply(Dialect dialect) {
			return supports( dialect, OffsetDateTimeJavaType.INSTANCE, SqlTypes.TIMESTAMP_UTC, null, INSTANT );
		}
	}
}
