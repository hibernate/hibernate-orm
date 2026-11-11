package org.hibernate.orm.test.mapping.javatime.redesign;

import static org.assertj.core.api.Assertions.*;

import jakarta.persistence.*;

import org.hibernate.MappingException;
import org.hibernate.annotations.*;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.dialect.*;
import org.hibernate.dialect.temporaltype.spi.TemporalPreservation;
import org.hibernate.dialect.type.internal.PostgreSQLUtcTimeJdbcType;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Component;
import org.hibernate.testing.orm.junit.*;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.*;
import org.hibernate.type.descriptor.jdbc.internal.UtcTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.internal.UtcTimestampJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.AbstractJavaTimeJdbcType;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/// Entity persistence coverage for INLINE/COLUMN, with direct-enabled fallback.
/// Each supported mapping is persisted, reloaded in a fresh session, updated,
/// reloaded again, and deleted. Unknown guarantees must fail actual bootstrap.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
@RequiresDialect(PostgreSQLDialect.class)
@RequiresDialect(OracleDialect.class)
@RequiresDialectFeature(feature = TemporalEligibilityChecks.LiveProfile.class)
class TimeZoneAwareTemporalOrmPreservationMatrixTests {
	enum Handling {
		DIRECT,
		FALLBACK
	}

	enum Form {
		PLAIN,
		WITH_TIME_ZONE
	}

	record Combination(
			Class<? extends ValueEntity> entity,
			boolean time,
			boolean column,
			boolean adjusted,
			Handling handling,
			Form form) {}

	static Stream<Combination> combinations() {
		return Stream.of(
						new Object[][] {
							{OffsetTimeInline.class, true, false, false},
							{OffsetTimeInlineAdjusted.class, true, false, true},
							{OffsetTimeColumn.class, true, true, false},
							{OffsetTimeColumnAdjusted.class, true, true, true},
							{OffsetDateTimeInline.class, false, false, false},
							{OffsetDateTimeInlineAdjusted.class, false, false, true},
							{OffsetDateTimeColumn.class, false, true, false},
							{OffsetDateTimeColumnAdjusted.class, false, true, true},
							{ZonedDateTimeInline.class, false, false, false},
							{ZonedDateTimeInlineAdjusted.class, false, false, true},
							{ZonedDateTimeColumn.class, false, true, false},
							{ZonedDateTimeColumnAdjusted.class, false, true, true},
						})
				.flatMap(
						row ->
								Stream.of(Handling.values())
										.flatMap(
												handling ->
														Stream.of(Form.values())
																.map(
																		form ->
																				new Combination(
																						(Class<
																										? extends
																												ValueEntity>)
																								row[
																										0],
																						(boolean)
																								row[
																										1],
																						(boolean)
																								row[
																										2],
																						(boolean)
																								row[
																										3],
																						handling,
																						form))))
				.filter(
						combination ->
								combination.column || combination.form == Form.WITH_TIME_ZONE);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("combinations")
	void preservesThroughEntityLifecycle(Combination c) throws Exception {
		final var contributor = new AtomicReference<TypeContributor>();
		final var loaders =
				new ClassLoaderServiceImpl() {
					@Override
					public <S> java.util.Collection<S> loadJavaServices(Class<S> contract) {
						return contract == TypeContributor.class && contributor.get() != null
								? java.util.List.of(contract.cast(contributor.get()))
								: super.loadJavaServices(contract);
					}
				};
		try (var registry =
				ServiceRegistryUtil.serviceRegistryBuilder(
								new BootstrapServiceRegistryBuilder()
										.applyClassLoaderService(loaders)
										.build())
						.applySetting("hibernate.type.java_time_use_direct_jdbc", true)
						.applySetting("hibernate.hbm2ddl.auto", "create-drop")
						.build()) {
			final var dialect = registry.requireService(JdbcServices.class).getDialect();
			boolean pg = dialect instanceof PostgreSQLDialect;
			boolean oracle = dialect instanceof OracleDialect;
			boolean zonedDomain = c.entity.getSimpleName().startsWith("Zoned");
			final org.hibernate.type.descriptor.jdbc.JdbcType fallback = c.form == Form.PLAIN
					? TimestampUtcAsPlainJdbcTimestampJdbcType.INSTANCE
					: pg ? org.hibernate.dialect.type.internal.PostgreSQLTimestampUtcAsJdbcTimestampJdbcType.INSTANCE
							: oracle ? new org.hibernate.dialect.type.internal.OracleTimestampUtcAsJdbcTimestampJdbcType( registry.requireService( org.hibernate.boot.registry.classloading.spi.ClassLoaderService.class ) )
							: TimestampUtcAsZonedJdbcTimestampJdbcType.INSTANCE;

			boolean supported;
			if (!c.column && !c.adjusted)
				supported =
						c.handling == Handling.DIRECT
								&& (c.time || zonedDomain && oracle || !zonedDomain && !pg);
			else if (c.time)
				supported =
						c.handling == Handling.DIRECT
								&& (pg ? c.form == Form.PLAIN : c.form == Form.WITH_TIME_ZONE);
			else supported = c.handling == Handling.FALLBACK || c.form == Form.WITH_TIME_ZONE;
			final var builder =
					new MetadataSources(registry).addAnnotatedClass(c.entity).getMetadataBuilder();
			contributor.set(
					(contributions, services) -> {
						final var types = contributions.getTypeConfiguration();
						final var jdbc = types.getJdbcTypeRegistry();
						jdbc.addDescriptor(
								SqlTypes.TIMESTAMP_UTC,
								c.handling == Handling.FALLBACK
										? fallback
										: c.form == Form.PLAIN
												? InstantJdbcType.INSTANCE
												: TimestampUtcAsOffsetDateTimeJdbcType.INSTANCE);
						jdbc.addDescriptor(
								SqlTypes.TIME_UTC,
								c.handling == Handling.FALLBACK
										? TimeUtcAsJdbcTimeJdbcType.INSTANCE
										: c.form == Form.PLAIN
												? pg
														? PostgreSQLUtcTimeJdbcType.INSTANCE
														: LocalTimeJdbcType.INSTANCE
												: pg
														? TimeWithTimeZoneJdbcType.INSTANCE
														: TimeUtcAsOffsetTimeJdbcType.INSTANCE);
						if (c.handling == Handling.FALLBACK) {
							jdbc.addDescriptor(
									SqlTypes.OFFSET_DATE_TIME,
									fallback);
							jdbc.addDescriptor(
									SqlTypes.ZONED_DATE_TIME,
									fallback);
							jdbc.addDescriptor(
									SqlTypes.OFFSET_TIME, TimeUtcAsJdbcTimeJdbcType.INSTANCE);
							jdbc.addDescriptor(
									SqlTypes.TIMESTAMP_WITH_TIMEZONE,
									fallback);
						}
						final var ddl = types.getDdlTypeRegistry();
						ddl.addDescriptor(
								new DdlTypeImpl(
										SqlTypes.TIMESTAMP_UTC,
										"timestamp(6)"
												+ (c.form == Form.PLAIN ? "" : " with time zone"),
										dialect));
						ddl.addDescriptor(
								new DdlTypeImpl(
										SqlTypes.TIME_UTC,
										(oracle ? "timestamp(6)" : "time(6)")
												+ (c.form == Form.PLAIN ? "" : " with time zone"),
										dialect));
					});
			if (!supported) {
				assertThatThrownBy(builder::build)
						.as("%s must reject missing guarantees", c)
						.isInstanceOf(MappingException.class)
						.satisfies(
								ex ->
										assertThat(ex.getMessage())
												.containsAnyOf("preservation", "precision"));
				return;
			}
			final var metadata = builder.build();
			final var value =
					metadata.getEntityBinding(c.entity.getName()).getProperty("value").getValue();
			if (value instanceof Component component) {
				assertThat(component.getColumnSpan()).isEqualTo(2);
				for (var property : component.getProperties()) {
					final var basic = (BasicValue) property.getValue();
					final var resolved = basic.resolve();
					final var report =
							dialect.getTemporalPreservation(
									resolved.getDomainJavaType(), resolved.getJdbcType());
					final var part =
							property.getName().equals("zoneOffset")
									? TemporalPreservation.TemporalPart.OFFSET
									: c.time
											? TemporalPreservation.TemporalPart.TIME
											: TemporalPreservation.TemporalPart.INSTANT;
					assertThat(report.preserves(part))
							.as("%s / %s", c, property.getName())
							.isEqualTo(TemporalPreservation.Level.SUPPORTED);
				}
			} else assertThat(value.getColumnSpan()).isEqualTo(1);
			final var primaryValue =
					value instanceof Component component
							? (BasicValue)
									component.getProperties().stream()
											.filter(
													property ->
															!property.getName()
																	.equals("zoneOffset"))
											.findFirst()
											.orElseThrow()
											.getValue()
							: (BasicValue) value;
			var primaryJdbc = primaryValue.resolve().getJdbcType();
			if (primaryJdbc instanceof UtcTimestampJdbcType utc) primaryJdbc = utc.getDelegate();
			if (primaryJdbc instanceof UtcTimeJdbcType utc) primaryJdbc = utc.getDelegate();
			if (c.handling == Handling.FALLBACK)
				assertThat(primaryJdbc.getClass())
						.isEqualTo(fallback.getClass());
			else if (c.column || c.adjusted)
				assertThat(primaryJdbc.getClass())
						.isEqualTo(
								c.time
										? pg
												? PostgreSQLUtcTimeJdbcType.class
												: TimeUtcAsOffsetTimeJdbcType.class
										: TimestampUtcAsOffsetDateTimeJdbcType.class);
			else assertThat(primaryJdbc).isInstanceOf(AbstractJavaTimeJdbcType.class);
			final var resolution = primaryValue.resolve();
			final var report =
					dialect.getTemporalPreservation(
							resolution.getDomainJavaType(), resolution.getJdbcType());
			assertThat(
							report.preserves(
									c.time
											? TemporalPreservation.TemporalPart.TIME
											: TemporalPreservation.TemporalPart.INSTANT))
					.isEqualTo(TemporalPreservation.Level.SUPPORTED);
			if (!c.column && !c.adjusted) {
				assertThat(report.offset()).isEqualTo(TemporalPreservation.Level.SUPPORTED);
				if (zonedDomain)
					assertThat(report.zoneId()).isEqualTo(TemporalPreservation.Level.SUPPORTED);
			}
			if (c.column || c.adjusted) {
				final var sqlType =
						((org.hibernate.mapping.Column) primaryValue.getColumn())
								.getSqlType(metadata);
				assertThat(sqlType.contains("with time zone"))
						.as("%s physical primary SQL type: %s", c, sqlType)
						.isEqualTo(c.form == Form.WITH_TIME_ZONE);
			}

			try (var factory = metadata.buildSessionFactory()) {
				final Object[] samples =
						c.time
								? new Object[] {
									OffsetTime.parse("00:15:16.123456+05:30"),
									OffsetTime.parse("23:45:16.654321-03:00"),
									null
								}
								: new Object[] {
									ZonedDateTime.parse(
											"2026-11-01T01:30:16.123456-04:00[America/New_York]"),
									ZonedDateTime.parse(
											"2026-11-01T01:30:16.654321-05:00[America/New_York]"),
									null
								};
				for (Object sample : samples) {
					Object input =
							sample instanceof ZonedDateTime z && !zonedDomain
									? z.toOffsetDateTime()
									: sample;
					var entity = c.entity.getDeclaredConstructor().newInstance();
					entity.id = 1L;
					entity.value(input);
					try (var session = factory.openSession()) {
						var tx = session.beginTransaction();
						TemporalMatrixEnvironment.prepareSession( session, true );
						session.persist(entity);
						tx.commit();
					}
					try (var session = factory.openSession()) {
						var tx = session.beginTransaction();
						TemporalMatrixEnvironment.prepareSession( session, false );
						var loaded = session.get(c.entity, 1L);
						assertValue(c, input, loaded.value());
						loaded.value(null);
						tx.commit();
					}
					try (var session = factory.openSession()) {
						var tx = session.beginTransaction();
						TemporalMatrixEnvironment.prepareSession( session, false );
						var loaded = session.get(c.entity, 1L);
						assertThat(loaded.value()).isNull();
						session.remove(loaded);
						tx.commit();
					}
				}
			}
		}
	}

	private static void assertValue(Combination c, Object input, Object actual) {
		if (input == null) {
			assertThat(actual).isNull();
			return;
		}
		if (input instanceof OffsetTime t) {
			assertThat(actual)
					.isEqualTo(c.adjusted ? t.withOffsetSameInstant(ZoneOffset.ofHours(-3)) : t);
			return;
		}
		if (input instanceof OffsetDateTime t) {
			final var a = (OffsetDateTime) actual;
			assertThat(a.toInstant()).isEqualTo(t.toInstant());
			assertThat(a.getOffset())
					.isEqualTo(
							c.adjusted
									? t.atZoneSameInstant(ZoneId.of("America/New_York")).getOffset()
									: t.getOffset());
			assertThat(a.toLocalDateTime())
					.isEqualTo(
							c.adjusted
									? t.atZoneSameInstant(ZoneId.of("America/New_York"))
											.toLocalDateTime()
									: t.toLocalDateTime());
		} else {
			final var t = (ZonedDateTime) input;
			final var a = (ZonedDateTime) actual;
			ZoneId expected =
					c.adjusted
							? ZoneId.of("America/New_York")
							: c.column ? t.getOffset() : t.getZone();
			assertThat(a.toInstant()).isEqualTo(t.toInstant());
			assertThat(a.getZone()).isEqualTo(expected);
			assertThat(a.getOffset()).isEqualTo(t.withZoneSameInstant(expected).getOffset());
			assertThat(a.toLocalDateTime())
					.isEqualTo(t.withZoneSameInstant(expected).toLocalDateTime());
		}
	}

	@MappedSuperclass
	abstract static class ValueEntity {
		@Id Long id;

		abstract Object value();

		abstract void value(Object value);
	}

	@Entity
	@Table(name = "aware_matrix")
	static class OffsetTimeInline extends ValueEntity {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE)
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetTime value;

		Object value() {
			return value;
		}

		void value(Object value) {
			this.value = (OffsetTime) value;
		}
	}

	@Entity
	@Table(name = "aware_matrix")
	static class OffsetTimeInlineAdjusted extends ValueEntity {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE)
		@TimeZoneAdjustment("-03:00")
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetTime value;

		Object value() {
			return value;
		}

		void value(Object value) {
			this.value = (OffsetTime) value;
		}
	}

	@Entity
	@Table(name = "aware_matrix")
	static class OffsetTimeColumn extends ValueEntity {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetTime value;

		Object value() {
			return value;
		}

		void value(Object value) {
			this.value = (OffsetTime) value;
		}
	}

	@Entity
	@Table(name = "aware_matrix")
	static class OffsetTimeColumnAdjusted extends ValueEntity {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@TimeZoneAdjustment("-03:00")
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetTime value;

		Object value() {
			return value;
		}

		void value(Object value) {
			this.value = (OffsetTime) value;
		}
	}

	@Entity
	@Table(name = "aware_matrix")
	static class OffsetDateTimeInline extends ValueEntity {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE)
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetDateTime value;

		Object value() {
			return value;
		}

		void value(Object value) {
			this.value = (OffsetDateTime) value;
		}
	}

	@Entity
	@Table(name = "aware_matrix")
	static class OffsetDateTimeInlineAdjusted extends ValueEntity {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE)
		@TimeZoneAdjustment("America/New_York")
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetDateTime value;

		Object value() {
			return value;
		}

		void value(Object value) {
			this.value = (OffsetDateTime) value;
		}
	}

	@Entity
	@Table(name = "aware_matrix")
	static class OffsetDateTimeColumn extends ValueEntity {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetDateTime value;

		Object value() {
			return value;
		}

		void value(Object value) {
			this.value = (OffsetDateTime) value;
		}
	}

	@Entity
	@Table(name = "aware_matrix")
	static class OffsetDateTimeColumnAdjusted extends ValueEntity {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@TimeZoneAdjustment("America/New_York")
		@Column(name = "temporal_value", secondPrecision = 6)
		OffsetDateTime value;

		Object value() {
			return value;
		}

		void value(Object value) {
			this.value = (OffsetDateTime) value;
		}
	}

	@Entity
	@Table(name = "aware_matrix")
	static class ZonedDateTimeInline extends ValueEntity {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE)
		@Column(name = "temporal_value", secondPrecision = 6)
		ZonedDateTime value;

		Object value() {
			return value;
		}

		void value(Object value) {
			this.value = (ZonedDateTime) value;
		}
	}

	@Entity
	@Table(name = "aware_matrix")
	static class ZonedDateTimeInlineAdjusted extends ValueEntity {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE)
		@TimeZoneAdjustment("America/New_York")
		@Column(name = "temporal_value", secondPrecision = 6)
		ZonedDateTime value;

		Object value() {
			return value;
		}

		void value(Object value) {
			this.value = (ZonedDateTime) value;
		}
	}

	@Entity
	@Table(name = "aware_matrix")
	static class ZonedDateTimeColumn extends ValueEntity {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@Column(name = "temporal_value", secondPrecision = 6)
		ZonedDateTime value;

		Object value() {
			return value;
		}

		void value(Object value) {
			this.value = (ZonedDateTime) value;
		}
	}

	@Entity
	@Table(name = "aware_matrix")
	static class ZonedDateTimeColumnAdjusted extends ValueEntity {
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@TimeZoneAdjustment("America/New_York")
		@Column(name = "temporal_value", secondPrecision = 6)
		ZonedDateTime value;

		Object value() {
			return value;
		}

		void value(Object value) {
			this.value = (ZonedDateTime) value;
		}
	}
}
