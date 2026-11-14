package org.hibernate.orm.test.mapping.javatime.redesign;

import java.util.Arrays;
import java.sql.Connection;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import static org.assertj.core.api.Assertions.assertThat;

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
import org.hibernate.type.descriptor.java.LocalDateJavaType;
import org.hibernate.type.descriptor.java.LocalTimeJavaType;
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
		final var guard = new NoConnections();
		try ( var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.addService( ConnectionProvider.class, guard )
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
							: jdbcCode == SqlTypes.TIMESTAMP_UTC
							? new UtcTimestampJdbcType( types.getJdbcTypeRegistry().getDescriptor( jdbcCode ) )
							: types.getJdbcTypeRegistry().getDescriptor( jdbcCode );
			final var report = TemporalMappingValidator.assess( dialect, javaType, jdbcType, "test eligibility" );
			return Arrays.stream( required ).allMatch( part -> report.preserves( part ) == SUPPORTED )
					&& (producerPolicy == null || dialect.getCurrentTemporalSupport()
					.getCurrentTimestampSelection( javaType, jdbcType, producerPolicy ) != null);
		}
		finally {
			assertThat( guard.attempts ).as( "eligibility bootstrap acquisitions" ).isZero();
		}
	}

	public static class LocalDateMapping implements DialectFeatureCheck {
		@Override
		public boolean apply(Dialect dialect) {
			return supports( dialect, LocalDateJavaType.INSTANCE, null, null, DATE );
		}
	}

	public static class LocalTimeMapping implements DialectFeatureCheck {
		@Override
		public boolean apply(Dialect dialect) {
			return supports( dialect, LocalTimeJavaType.INSTANCE, null, null, TIME );
		}
	}

	public static class LocalDateTimeMapping implements DialectFeatureCheck {
		@Override
		public boolean apply(Dialect dialect) {
			return supports( dialect, LocalDateTimeJavaType.INSTANCE, null, null, DATE, TIME );
		}
	}

	public static class ExplicitLocalFields implements DialectFeatureCheck {
		@Override
		public boolean apply(Dialect dialect) {
			return supports( dialect, LocalDateTimeJavaType.INSTANCE, SqlTypes.LOCAL_DATE_TIME, null, DATE, TIME )
					&& supports( dialect, LocalTimeJavaType.INSTANCE, SqlTypes.LOCAL_TIME, null, TIME );
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

	private static class NoConnections implements ConnectionProvider {
		int attempts;
		@Override public Connection getConnection() {
			attempts++;
			throw new AssertionError( "Eligibility must resolve contributions without JDBC" );
		}
		@Override public void closeConnection(Connection connection) { throw new AssertionError( "No loan" ); }
		@Override public boolean supportsAggressiveRelease() { return false; }
		@Override public boolean isUnwrappableAs(Class<?> type) { return type.isInstance( this ); }
		@Override public <T> T unwrap(Class<T> type) { return type.cast( this ); }
	}
}
