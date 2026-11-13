package org.hibernate.orm.test.mapping.javatime.redesign;

import java.time.*;
import java.util.stream.Stream;

import jakarta.persistence.*;
import org.hibernate.MappingException;
import org.hibernate.annotations.JdbcType;
import org.hibernate.boot.MetadataSources;
import org.hibernate.dialect.*;
import org.hibernate.dialect.temporaltype.spi.TemporalPreservation;
import org.hibernate.mapping.BasicValue;
import org.hibernate.testing.orm.junit.*;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.type.descriptor.jdbc.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.*;

/// Non-timezone-aware domain mappings must preserve local fields (or the instant
/// for Instant), with fresh-session persistence and actual bootstrap rejection.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
@RequiresDialect(PostgreSQLDialect.class)
@RequiresDialect(OracleDialect.class)
class NonTimeZoneAwareTemporalPreservationMatrixTests {
	record Combination(Class<? extends ValueEntity> entity, boolean fallback) {}

	static Stream<Combination> combinations() {
		return Stream.of( DateDirect.class, DateFallback.class, TimeDirect.class, TimeFractionalDirect.class, TimeFallback.class,
						TimestampDirect.class, TimestampFallback.class, InstantDirect.class, InstantFallback.class )
				.map( type -> new Combination( type, type.getSimpleName().endsWith( "Fallback" ) ) );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("combinations")
	void preservesThroughHibernate(Combination c) throws Exception {
		try (var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( "hibernate.type.java_time_use_direct_jdbc", true )
				.applySetting( "hibernate.hbm2ddl.auto", "create-drop" ).build()) {
			final var builder = new MetadataSources( registry ).addAnnotatedClass( c.entity ).getMetadataBuilder();
			if ( c.fallback && c.entity != InstantFallback.class ) {
				assertThatThrownBy( builder::build )
						.isInstanceOf( MappingException.class )
						.hasMessageContaining( "preservation" );
				return;
			}
			final var metadata = builder.build();
			final var value = (BasicValue) metadata.getEntityBinding( c.entity.getName() ).getProperty( "value" ).getValue();
			final var resolution = value.resolve();
			final var dialect = registry.requireService( org.hibernate.engine.jdbc.spi.JdbcServices.class ).getDialect();
			final var report = dialect.getTemporalPreservation( resolution.getDomainJavaType(),	resolution.getJdbcType() );
			final boolean date = c.entity == DateDirect.class;
			final boolean time = c.entity == TimeDirect.class || c.entity == TimeFractionalDirect.class;
			final boolean timestamp = c.entity == TimestampDirect.class;

			final TemporalPreservation.TemporalPart part;
			final Class<?> preferredJavaType;
			final Object input2;
			if ( date ) {
				part = TemporalPreservation.TemporalPart.DATE;
				preferredJavaType = LocalDate.class;
				input2 = LocalDate.of( 2024, 2, 29 );
			}
			else if ( time ) {
				part = TemporalPreservation.TemporalPart.TIME;
				preferredJavaType = LocalTime.class;
				input2 = LocalTime.of( 23, 59, 58, c.entity == TimeFractionalDirect.class ? 123456000 : 0 );
			}
			else if ( timestamp ) {
				part = TemporalPreservation.TemporalPart.DATE;
				preferredJavaType = LocalDateTime.class;
				input2 = LocalDateTime.parse( "2024-02-29T23:59:58.123456" );
			}
			else {
				part = TemporalPreservation.TemporalPart.INSTANT;
				preferredJavaType = Instant.class;
				input2 = Instant.parse( "2024-02-29T23:59:58.123456Z" );
			}

			assertThat( report.preserves( part ) ).isEqualTo( TemporalPreservation.Level.SUPPORTED );
		if ( timestamp ) {
		assertThat( report.time() ).isEqualTo( TemporalPreservation.Level.SUPPORTED );
		}
			assertThat( resolution.getJdbcType().getPreferredJavaTypeClass( null ) ).isEqualTo(
					c.fallback ? Instant.class
							: date ? LocalDate.class
									: time ? LocalTime.class : timestamp ? LocalDateTime.class : OffsetDateTime.class );
			Object input = date ? LocalDate.of( 2024, 2, 29 ) : time ? LocalTime.of( 23, 59, 58, c.entity == TimeFractionalDirect.class ? 123456000 : 0 )
					: timestamp ? LocalDateTime.parse( "2024-02-29T23:59:58.123456" )
							: Instant.parse( "2024-02-29T23:59:58.123456Z" );
			try (var factory = metadata.buildSessionFactory()) {
				for ( Object sample : new Object[] {input, null} ) {
					var entity = c.entity.getDeclaredConstructor().newInstance();
					entity.id = 1L;
					entity.value( sample );
					try (var session = factory.openSession()) {
						var tx = session.beginTransaction();
						TemporalMatrixEnvironment.prepareSession( session, true );
						session.persist( entity );
						tx.commit();
					}
					try (var session = factory.openSession()) {
						var tx = session.beginTransaction();
						TemporalMatrixEnvironment.prepareSession( session, false );
						var actual = session.get( c.entity, 1L );
						assertThat( actual.value() ).isEqualTo( sample );
						actual.value( null );
						tx.commit();
					}
					try (var session = factory.openSession()) {
						var tx = session.beginTransaction();
						TemporalMatrixEnvironment.prepareSession( session, false );
						var actual = session.get( c.entity, 1L );
						assertThat( actual.value() ).isNull();
						session.remove( actual );
						tx.commit();
					}
				}
			}
		}
	}

	@MappedSuperclass
	static abstract class ValueEntity {
		@Id
		Long id;

		abstract Object value();

		abstract void value(Object value);
	}

	@Entity
	@Table(name = "local_matrix")
	static class DateDirect extends ValueEntity {
		@JdbcType(LocalDateJdbcType.class)
		@Column(name = "temporal_value")
		LocalDate value;

		Object value() {
			return value;
		}

		void value(Object value) {
			this.value = (LocalDate) value;
		}
	}

	@Entity
	@Table(name = "local_matrix")
	static class DateFallback extends ValueEntity {
		@JdbcType(DateJdbcType.class)
		@Column(name = "temporal_value")
		LocalDate value;

		Object value() {
			return value;
		}

		void value(Object value) {
			this.value = (LocalDate) value;
		}
	}

	@Entity
	@Table(name = "local_matrix")
	static class TimeDirect extends ValueEntity {
		@org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.LOCAL_TIME)
		@Column(name = "temporal_value", secondPrecision = 0)
		LocalTime value;

		Object value() {
			return value;
		}

		void value(Object value) {
			this.value = (LocalTime) value;
		}
	}

	@Entity
	@Table(name = "local_matrix")
	static class TimeFractionalDirect extends ValueEntity {
		@org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.LOCAL_TIME)
		@Column(name = "temporal_value", secondPrecision = 6)
		LocalTime value;

		Object value() {
			return value;
		}

		void value(Object value) {
			this.value = (LocalTime) value;
		}
	}

	@Entity
	@Table(name = "local_matrix")
	static class TimeFallback extends ValueEntity {
		@JdbcType(TimeJdbcType.class)
		@Column(name = "temporal_value", secondPrecision = 0)
		LocalTime value;

		Object value() {
			return value;
		}

		void value(Object value) {
			this.value = (LocalTime) value;
		}
	}

	@Entity
	@Table(name = "local_matrix")
	static class TimestampDirect extends ValueEntity {
		@JdbcType(LocalDateTimeJdbcType.class)
		@Column(name = "temporal_value", secondPrecision = 6)
		LocalDateTime value;

		Object value() {
			return value;
		}

		void value(Object value) {
			this.value = (LocalDateTime) value;
		}
	}

	@Entity
	@Table(name = "local_matrix")
	static class TimestampFallback extends ValueEntity {
		@JdbcType(TimestampJdbcType.class)
		@Column(name = "temporal_value", secondPrecision = 6)
		LocalDateTime value;

		Object value() {
			return value;
		}

		void value(Object value) {
			this.value = (LocalDateTime) value;
		}
	}

	@Entity
	@Table(name = "local_matrix")
	static class InstantDirect extends ValueEntity {
		@JdbcType(TimestampUtcAsOffsetDateTimeJdbcType.class)
		@Column(name = "temporal_value", secondPrecision = 6)
		Instant value;

		Object value() {
			return value;
		}

		void value(Object value) {
			this.value = (Instant) value;
		}
	}

	@Entity
	@Table(name = "local_matrix")
	static class InstantFallback extends ValueEntity {
		@JdbcType(TimestampUtcAsPlainJdbcTimestampJdbcType.class)
		@Column(name = "temporal_value", secondPrecision = 6)
		Instant value;

		Object value() {
			return value;
		}

		void value(Object value) {
			this.value = (Instant) value;
		}
	}
}
