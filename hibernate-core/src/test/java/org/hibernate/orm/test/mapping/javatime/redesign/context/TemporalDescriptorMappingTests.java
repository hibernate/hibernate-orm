package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.util.function.UnaryOperator;
import org.hibernate.type.descriptor.jdbc.TemporalPrecisionJdbcType;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionContext;
import org.hibernate.dialect.temporaltype.spi.TemporalCallablePrecisionContext;
import org.hibernate.dialect.temporaltype.spi.TemporalCallablePrecisionContext.Access;
import org.hibernate.dialect.temporaltype.spi.TemporalCallablePrecisionContext.Boundary;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionSupport;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.persistence.*;

import org.hibernate.annotations.JavaTypeRegistration;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.JdbcTypeRegistration;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.temporaltype.spi.TemporalPreservation;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.*;
import org.hibernate.type.descriptor.jdbc.*;
import org.hibernate.type.internal.BasicTypeImpl;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.dialect.temporaltype.spi.TemporalPreservation.Level.*;
import static org.hibernate.dialect.temporaltype.spi.TemporalPreservation.TemporalPart.*;

/// Live custom pairs supplement descriptor reports with independent stored values,
/// mapped queries, converter boundaries and metadata-disabled bootstrap guards.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
public class TemporalDescriptorMappingTests {
	static final int DAY_CODE = 78001, INSTANT_CODE = 78002;
	static final Day DAY = new Day(LocalDate.of(2026, 7, 1));
	static final Instant INSTANT = Instant.parse("2026-07-01T10:00:00.123456Z");
	static final Map<String, Object> OPTIONS =
			Map.of("hibernate.test.temporal.reuse_connection", true);

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void explicitAndRegisteredPairsExecute(boolean metadata) {
		try (var scope = ContextScope.open(metadata, OPTIONS, Event.class)) {
			DayJavaType.wraps.set(0);
			DayJavaType.unwraps.set(0);
			scope.tx(
					s -> {
						var e = new Event();
						e.id = 1;
						e.day = DAY;
						e.registered = DAY;
						e.stamp = INSTANT;
						e.converted = "2026-07-01";
						e.days.add(DAY);
						s.persist(e);
						var n = new Event();
						n.id = 2;
						s.persist(n);
					});
			scope.tx(
					s -> {
						var e = s.find(Event.class, 1);
						assertThat(e.day).isEqualTo(DAY);
						assertThat(e.registered).isEqualTo(DAY);
						assertThat(e.stamp).isEqualTo(INSTANT);
						assertThat(e.converted).isEqualTo("2026-07-01");
						assertThat(e.days).containsExactly(DAY);
						var n = s.find(Event.class, 2);
						assertThat(n.day).isNull();
						assertThat(n.registered).isNull();
						assertThat(n.stamp).isNull();
						assertThat(n.converted).isNull();
						assertThat(
										s.createSelectionQuery(
														"select e.id from SpiTemporalEvent e where"
															+ " e.day=:d and e.stamp=:i",
														Integer.class)
												.setParameter("d", DAY)
												.setParameter("i", INSTANT)
												.getResultList())
								.containsExactly(1);
					});
			scope.tx(
					s ->
							s.doWork(
									c -> {
										try (var st = c.createStatement();
												var rs =
														st.executeQuery(
																"select day_value,registered,stamp"
																	+ " from SpiTemporalEvent where"
																	+ " id=1")) {
											assertThat(rs.next()).isTrue();
											assertThat(rs.getObject(1, LocalDate.class))
													.isEqualTo(LocalDate.of(2026, 7, 1));
											assertThat(rs.getObject(2, LocalDate.class))
													.isEqualTo(LocalDate.of(2026, 7, 1));
											assertThat(rs.getObject(3, LocalDateTime.class))
													.isEqualTo(
															LocalDateTime.of(
																	2026, 7, 1, 10, 0, 0,
																	123456000));
										}
									}));
			scope.tx(
					s ->
							assertThat(
											s.createMutationQuery(
															"update SpiTemporalEvent e set e.day=:d"
																+ " where e.id=1")
													.setParameter(
															"d", new Day(LocalDate.of(2026, 7, 2)))
													.executeUpdate())
									.isEqualTo(1));
			scope.tx(
					s ->
							assertThat(s.find(Event.class, 1).day)
									.isEqualTo(new Day(LocalDate.of(2026, 7, 2))));
			assertThat(DayJavaType.wraps.get()).isPositive();
			assertThat(DayJavaType.unwraps.get()).isPositive();
			var model = scope.factory.getMappingMetamodel().getEntityDescriptor(Event.class);
			assertThat(((BasicType<?>) model.getPropertyType("day")).getJdbcType())
					.isInstanceOf(DayJdbc.class);
			assertThat(((BasicType<?>) model.getPropertyType("registered")).getJdbcType())
					.isInstanceOf(DayJdbc.class);
			assertThat(((BasicType<?>) model.getPropertyType("converted")).getJdbcJavaType())
					.isInstanceOf(DayJavaType.class);
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void unknownPairsAndIneligibleFeaturesReject(boolean metadata) {
		for (var entity : List.of(UnknownDay.class, UnknownConverter.class)) {
			assertThatThrownBy(
							() -> {
								try (var ignored = ContextScope.boot(metadata, true, entity)) {}
							})
					.hasMessageContaining("DATE")
					.hasMessageContaining("UNKNOWN")
					.hasMessageContaining("TemporalJdbcType");
		}
		for (var entity : List.of(BadRepresentation.class, BadAdjustment.class))
			assertThatThrownBy(
							() -> {
								try (var ignored = ContextScope.boot(metadata, true, entity)) {}
							})
					.hasMessageContaining("not applicable");
		try (var scope = ContextScope.boot(metadata, true, OPTIONS, Event.class);
				var session = scope.factory.openSession()) {
			var insufficient = new BasicTypeImpl<>(new DayJavaType(), LocalDateJdbcType.INSTANCE);
			try (var call = session.createStoredProcedureCall("spi_invalid_function")) {
				assertThatThrownBy(() -> call.markAsFunctionCall(insufficient))
						.hasMessageContaining("function return")
						.hasMessageContaining("DATE")
						.hasMessageContaining("UNKNOWN");
			}
			try (var call = session.createStoredProcedureCall("spi_invalid_parameter")) {
				var p = call.registerParameter(1, insufficient, ParameterMode.IN);
				call.setParameter(p, DAY);
				assertThatThrownBy(call::getOutputs)
						.hasMessageContaining("parameter 1")
						.hasMessageContaining("UNKNOWN");
			}
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void legacyExecutionAndOldTemporalJavaTypeDoNotOptIn(boolean metadata) {
		try (var scope =
				ContextScope.open(
						metadata,
						Map.of("hibernate.type.temporal_preservation", false),
						DisabledEvent.class)) {
			scope.tx(
					s -> {
						var e = new DisabledEvent();
						e.id = 1;
						e.day = DAY;
						s.persist(e);
					});
			scope.tx(
					s -> {
						assertThat(s.find(DisabledEvent.class, 1).day).isEqualTo(DAY);
						assertThat(
										s.createSelectionQuery(
														"from SpiDisabledEvent e where e.day=:day",
														DisabledEvent.class)
												.setParameter("day", DAY)
												.getResultList())
								.hasSize(1);
					});
		}
		try (var scope = ContextScope.open(metadata, OPTIONS, OldEvent.class)) {
			scope.tx(
					s -> {
						var e = new OldEvent();
						e.id = 1;
						e.day = DAY;
						s.persist(e);
					});
			scope.tx(s -> assertThat(s.find(OldEvent.class, 1).day).isEqualTo(DAY));
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void converterDomainRequirementsDoNotReplaceRelationalType(boolean metadata) {
		try (var scope = ContextScope.open(metadata, OPTIONS, TextEncodedEvent.class)) {
			scope.tx(
					s -> {
						var e = new TextEncodedEvent();
						e.id = 1;
						e.day = DAY;
						s.persist(e);
					});
			scope.tx(s -> assertThat(s.find(TextEncodedEvent.class, 1).day).isEqualTo(DAY));
			var type =
					(BasicType<?>)
							scope.factory
									.getMappingMetamodel()
									.getEntityDescriptor(TextEncodedEvent.class)
									.getPropertyType("day");
			assertThat(type.getJavaTypeDescriptor()).isInstanceOf(DayJavaType.class);
			assertThat(type.getJdbcJavaType()).isInstanceOf(StringJavaType.class);
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void factoryContributionsRemainIndependent(boolean metadata) {
		var goodSettings =
				Map.<String, Object>of(
						"hibernate.dialect", ConfiguredDialect.class, "spi.test.supported", true);
		var legacySettings =
				Map.<String, Object>of(
						"hibernate.dialect",
						ConfiguredDialect.class,
						"spi.test.supported",
						false,
						"hibernate.type.temporal_preservation",
						false);
		try (var good = ContextScope.open(metadata, goodSettings, ConfiguredEvent.class);
				var legacy =
						ContextScope.boot(metadata, true, legacySettings, ConfiguredEvent.class)) {
			var first =
					(BasicType<?>)
							good.factory
									.getMappingMetamodel()
									.getEntityDescriptor(ConfiguredEvent.class)
									.getPropertyType("stamp");
			var second =
					(BasicType<?>)
							legacy.factory
									.getMappingMetamodel()
									.getEntityDescriptor(ConfiguredEvent.class)
									.getPropertyType("stamp");
			assertThat(first.getJdbcType().getClass()).isEqualTo(second.getJdbcType().getClass());
			assertThat(first.getJdbcType()).isNotSameAs(second.getJdbcType());
			assertThat(
							good.factory
									.getJdbcServices()
									.getDialect()
									.getTemporalPreservation(
											first.getJavaTypeDescriptor(), first.getJdbcType())
									.instant())
					.isEqualTo(SUPPORTED);
			assertThat(
							legacy.factory
									.getJdbcServices()
									.getDialect()
									.getTemporalPreservation(
											second.getJavaTypeDescriptor(), second.getJdbcType())
									.instant())
					.isEqualTo(UNKNOWN);
			assertThatThrownBy(
							() -> {
								try (var ignored =
										ContextScope.boot(
												metadata,
												true,
												Map.of(
														"hibernate.dialect",
														ConfiguredDialect.class,
														"spi.test.supported",
														false),
												ConfiguredEvent.class)) {}
							})
					.hasMessageContaining("UNKNOWN");
			good.tx(
					s -> {
						var e = new ConfiguredEvent();
						e.id = 1;
						e.stamp = INSTANT;
						s.persist(e);
					});
			good.tx(s -> assertThat(s.find(ConfiguredEvent.class, 1).stamp).isEqualTo(INSTANT));
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	@SuppressWarnings("unchecked")
	void customFunctionRetainsExplicitDescriptor(boolean metadata) {
		try (var scope = ContextScope.open(metadata, OPTIONS, Event.class)) {
			var type =
					(BasicType<Day>)
							scope.factory
									.getMappingMetamodel()
									.getEntityDescriptor(Event.class)
									.getPropertyType("day");
			scope.tx(
					s ->
							s.doWork(
									c -> {
										try (var st = c.createStatement()) {
											st.execute(
													"create alias spi_day_echo for '"
															+ getClass().getName()
															+ ".echoDay'");
										}
									}));
			try {
				for (Day value : new Day[] {DAY, null})
					scope.tx(
							s -> {
								try (var call = s.createStoredProcedureCall("spi_day_echo")) {
									call.markAsFunctionCall(type);
									var p = call.registerParameter(1, type, ParameterMode.IN);
									call.setParameter(p, value);
									assertThat(call.getSingleResult()).isEqualTo(value);
								}
							});
			} finally {
				scope.tx(
						s ->
								s.doWork(
										c -> {
											try (var st = c.createStatement()) {
												st.execute("drop alias spi_day_echo");
											}
										}));
			}
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void columnComponentsAreAssessedIndependently(boolean metadata) {
		var settings =
				Map.<String, Object>of(
						"hibernate.dialect",
						CompositionDialect.class,
						"hibernate.type.preferred_instant_jdbc_type",
						INSTANT_CODE);
		try (var scope = ContextScope.open(metadata, settings, ColumnEvent.class)) {
			var value = OffsetDateTime.parse("2026-07-01T12:00:00.123456+02:00");
			scope.tx(
					s -> {
						var e = new ColumnEvent();
						e.id = 1;
						e.stamp = value;
						s.persist(e);
					});
			scope.tx(s -> assertThat(s.find(ColumnEvent.class, 1).stamp).isEqualTo(value));
		}
		for (var part : List.of("primary", "offset", "malformed")) {
			var invalid = new HashMap<>(settings);
			invalid.put("spi.test.loss", part);
			assertThatThrownBy(
							() -> {
								try (var ignored =
										ContextScope.boot(
												metadata, true, invalid, ColumnEvent.class)) {}
							})
					.hasMessageContaining(
							part.equals("primary")
									? "INSTANT"
									: part.equals("offset") ? "OFFSET" : "null Optional")
					.hasMessageContaining(part.equals("primary") ? "instant" : "stamp");
		}
	}

	public static LocalDate echoDay(LocalDate value) {
		return value;
	}

	public record Day(LocalDate value) {}

	public static class OldDayJavaType extends AbstractTemporalJavaType<Day> {
		public OldDayJavaType() {
			super(Day.class, ImmutableMutabilityPlan.instance());
		}

		@Override
		public TemporalType getPrecision() {
			return TemporalType.DATE;
		}

		@Override
		protected TemporalJavaType<Day> forDatePrecision(
				org.hibernate.type.spi.TypeConfiguration configuration) {
			return this;
		}

		@Override
		public JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
			return new DayJdbc();
		}

		@Override
		public Day fromString(CharSequence value) {
			return new Day(LocalDate.parse(value));
		}

		@Override
		public String toString(Day value) {
			return value == null ? null : value.value().toString();
		}

		@Override
		public <X> X unwrap(Day value, Class<X> type, WrapperOptions options) {
			return value == null
					? null
					: type == Day.class
							? type.cast(value)
							: LocalDateJavaType.INSTANCE.unwrap(value.value(), type, options);
		}

		@Override
		public <X> Day wrap(X value, WrapperOptions options) {
			return value == null
					? null
					: value instanceof Day day
							? day
							: new Day(LocalDateJavaType.INSTANCE.wrap(value, options));
		}
	}

	public static class DayJavaType extends OldDayJavaType
			implements PreservationAwareTemporalJavaType<Day> {
		static final AtomicInteger wraps = new AtomicInteger(), unwraps = new AtomicInteger();

		@Override
		public Set<TemporalPreservation.TemporalPart> getRequiredTemporalParts() {
			return Set.of(DATE);
		}

		@Override
		public <X> X unwrap(Day value, Class<X> type, WrapperOptions options) {
			unwraps.incrementAndGet();
			return super.unwrap(value, type, options);
		}

		@Override
		public <X> Day wrap(X value, WrapperOptions options) {
			wraps.incrementAndGet();
			return super.wrap(value, options);
		}
	}

	public static class DayJdbc extends LocalDateJdbcType implements TemporalJdbcType, TemporalPrecisionJdbcType, TemporalCallablePrecisionJdbcType {
		@Override
		public Optional<TemporalPrecisionSupport> getTemporalCallablePrecisionSupport(TemporalCallablePrecisionContext context) {
			// Independently exercised by TemporalCallablePrecisionTests.customDayRawTransport.
			boolean route = context.mode() == ParameterMode.IN && context.boundary() == Boundary.INPUT
					&& context.valueAccess() == Access.INDEXED
					|| context.functionReturn() && context.boundary() == Boundary.OUTPUT
							&& context.valueAccess() == Access.RESULT_SET;
			return context.dialect().getClass() == H2Dialect.class
					&& context.type().getJdbcJavaType().getClass() == DayJavaType.class
					&& context.registrationAccess() == Access.NONE && route
					? Optional.of( TemporalPrecisionSupport.supported( "test-day-callable-v1:" + context.boundary(),
							UnaryOperator.identity(), UnaryOperator.identity() ) ) : Optional.empty();
		}

		@Override
		public Optional<TemporalPrecisionSupport> getTemporalPrecisionSupport(TemporalPrecisionContext context) {
			return Optional.of( TemporalPrecisionSupport.supported( "test-day-v1", UnaryOperator.identity(), UnaryOperator.identity() ) );
		}

		@Override
		public Optional<TemporalPreservation> getTemporalPreservation(
				JavaType<?> javaType, Dialect dialect) {
			return javaType.getClass() == DayJavaType.class && dialect instanceof H2Dialect
					? Optional.of(
							new TemporalPreservation(SUPPORTED, UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN))
					: Optional.empty();
		}
	}

	public static class ForbiddenSpiJdbc extends DayJdbc {
		@Override
		public Optional<TemporalPreservation> getTemporalPreservation(
				JavaType<?> javaType, Dialect dialect) {
			throw new AssertionError("Legacy execution must not invoke the SPI");
		}
	}

	public static class KnownInstantJdbc extends TimestampUtcAsJdbcTimestampJdbcType
			implements TemporalJdbcType, TemporalPrecisionJdbcType {
		@Override
		public Optional<TemporalPrecisionSupport> getTemporalPrecisionSupport(TemporalPrecisionContext context) {
			return TemporalTestPrecision.decimal( context, UnaryOperator.identity() );
		}

		@Override
		public int getDdlTypeCode() {
			return java.sql.Types.TIMESTAMP;
		}

		private final boolean supported;

		public KnownInstantJdbc() {
			this(true);
		}

		KnownInstantJdbc(boolean supported) {
			this.supported = supported;
		}

		@Override
		public Optional<TemporalPreservation> getTemporalPreservation(
				JavaType<?> javaType, Dialect dialect) {
			return javaType.getClass() == TemporalDescriptorAssessmentTests.AwareInstant.class
							&& dialect instanceof H2Dialect
					? Optional.of(
							supported
									? TemporalDescriptorAssessmentTests.INSTANT_ONLY
									: TemporalPreservation.UNKNOWN)
					: Optional.empty();
		}
	}

	public static class ConfiguredDialect extends H2Dialect {
		@Override
		public void contributeTypes(TypeContributions types, ServiceRegistry services) {
			super.contributeTypes(types, services);
			types.getTypeConfiguration()
					.getJdbcTypeRegistry()
					.addDescriptor(
							INSTANT_CODE,
							new KnownInstantJdbc(
									Boolean.TRUE.equals(
											services.getService(ConfigurationService.class)
													.getSettings()
													.get("spi.test.supported"))));
		}
	}

	public static class PrimaryJdbc extends TimestampUtcAsJdbcTimestampJdbcType
			implements TemporalJdbcType, TemporalPrecisionJdbcType {
		@Override
		public Optional<TemporalPrecisionSupport> getTemporalPrecisionSupport(TemporalPrecisionContext context) {
			return TemporalTestPrecision.decimal( context, UnaryOperator.identity() );
		}

		private final boolean loss;

		PrimaryJdbc(boolean loss) {
			this.loss = loss;
		}

		@Override
		public int getDdlTypeCode() {
			return java.sql.Types.TIMESTAMP;
		}

		@Override
		public Optional<TemporalPreservation> getTemporalPreservation(
				JavaType<?> javaType, Dialect dialect) {
			return javaType.getClass() == InstantJavaType.class && dialect instanceof H2Dialect
					? Optional.of(
							loss
									? TemporalPreservation.UNKNOWN
									: TemporalDescriptorAssessmentTests.INSTANT_ONLY)
					: Optional.empty();
		}
	}

	public static class OffsetJdbc extends IntegerJdbcType implements TemporalJdbcType {
		private final String loss;

		OffsetJdbc(String loss) {
			this.loss = loss;
		}

		@Override
		public Optional<TemporalPreservation> getTemporalPreservation(
				JavaType<?> javaType, Dialect dialect) {
			if (javaType.getClass() != ZoneOffsetJavaType.class || !(dialect instanceof H2Dialect))
				return Optional.empty();
			if ("malformed".equals(loss)) return null;
			return Optional.of(
					new TemporalPreservation(
							UNKNOWN,
							UNKNOWN,
							UNKNOWN,
							"offset".equals(loss) ? UNKNOWN : SUPPORTED,
							UNKNOWN));
		}
	}

	public static class CompositionDialect extends H2Dialect {
		@Override
		public void contributeTypes(TypeContributions types, ServiceRegistry services) {
			super.contributeTypes(types, services);
			var loss =
					(String)
							services.getService(ConfigurationService.class)
									.getSettings()
									.get("spi.test.loss");
			var registry = types.getTypeConfiguration().getJdbcTypeRegistry();
			registry.addDescriptor(INSTANT_CODE, new PrimaryJdbc("primary".equals(loss)));
			registry.addDescriptor(java.sql.Types.INTEGER, new OffsetJdbc(loss));
		}
	}

	@Converter
	public static class TextDayConverter implements AttributeConverter<String, Day> {
		@Override
		public Day convertToDatabaseColumn(String value) {
			return value == null ? null : new Day(LocalDate.parse(value));
		}

		@Override
		public String convertToEntityAttribute(Day value) {
			return value == null ? null : value.value().toString();
		}
	}

	@Converter
	public static class DayTextConverter implements AttributeConverter<Day, String> {
		@Override
		public String convertToDatabaseColumn(Day value) {
			return value == null ? null : value.value().toString();
		}

		@Override
		public Day convertToEntityAttribute(String value) {
			return value == null ? null : new Day(LocalDate.parse(value));
		}
	}

	@Entity(name = "SpiTextEncodedEvent")
	@JavaTypeRegistration(javaType = Day.class, descriptorClass = DayJavaType.class)
	static class TextEncodedEvent {
		@Id int id;

		@Convert(converter = DayTextConverter.class)
		Day day;
	}

	@MappedSuperclass
	static class Base {
		@org.hibernate.annotations.JavaType(DayJavaType.class)
		@org.hibernate.annotations.JdbcType(DayJdbc.class)
		@Column(name = "day_value")
		Day day;
	}

	@Entity(name = "SpiTemporalEvent")
	@JavaTypeRegistration(javaType = Day.class, descriptorClass = DayJavaType.class)
	@JdbcTypeRegistration(value = DayJdbc.class, registrationCode = DAY_CODE)
	static class Event extends Base {
		@Id int id;

		@org.hibernate.annotations.JavaType(DayJavaType.class)
		@JdbcTypeCode(DAY_CODE)
		Day registered;

		@org.hibernate.annotations.JavaType(TemporalDescriptorAssessmentTests.AwareInstant.class)
		@org.hibernate.annotations.JdbcType(KnownInstantJdbc.class)
		Instant stamp;

		@Convert(converter = TextDayConverter.class)
		@org.hibernate.annotations.JdbcType(DayJdbc.class)
		String converted;

		@ElementCollection
		@org.hibernate.annotations.JavaType(DayJavaType.class)
		@org.hibernate.annotations.JdbcType(DayJdbc.class)
		List<Day> days = new ArrayList<>();
	}

	@Entity(name = "SpiColumnEvent")
	static class ColumnEvent {
		@Id int id;

		@org.hibernate.annotations.TimeZoneRepresentation(
				org.hibernate.annotations.TimeZoneRepresentation.Type.COLUMN)
		OffsetDateTime stamp;
	}

	@Entity(name = "SpiUnknownDay")
	static class UnknownDay {
		@Id int id;

		@org.hibernate.annotations.JavaType(DayJavaType.class)
		@org.hibernate.annotations.JdbcType(LocalDateJdbcType.class)
		Day day;
	}

	@Entity(name = "SpiUnknownConverter")
	@JavaTypeRegistration(javaType = Day.class, descriptorClass = DayJavaType.class)
	static class UnknownConverter {
		@Id int id;

		@Convert(converter = TextDayConverter.class)
		@org.hibernate.annotations.JdbcType(LocalDateJdbcType.class)
		String converted;
	}

	@Entity(name = "SpiBadRepresentation")
	static class BadRepresentation {
		@Id int id;

		@org.hibernate.annotations.JavaType(DayJavaType.class)
		@org.hibernate.annotations.JdbcType(DayJdbc.class)
		@org.hibernate.annotations.TimeZoneRepresentation(
				org.hibernate.annotations.TimeZoneRepresentation.Type.COLUMN)
		Day day;
	}

	@Entity(name = "SpiBadAdjustment")
	static class BadAdjustment {
		@Id int id;

		@org.hibernate.annotations.JavaType(DayJavaType.class)
		@org.hibernate.annotations.JdbcType(DayJdbc.class)
		@org.hibernate.annotations.TimeZoneAdjustment("UTC")
		Day day;
	}

	@Entity(name = "SpiDisabledEvent")
	static class DisabledEvent {
		@Id int id;

		@org.hibernate.annotations.JavaType(DayJavaType.class)
		@org.hibernate.annotations.JdbcType(ForbiddenSpiJdbc.class)
		Day day;
	}

	@Entity(name = "SpiOldEvent")
	static class OldEvent {
		@Id int id;

		@org.hibernate.annotations.JavaType(OldDayJavaType.class)
		@org.hibernate.annotations.JdbcType(ForbiddenSpiJdbc.class)
		Day day;
	}

	@Entity(name = "SpiConfiguredEvent")
	static class ConfiguredEvent {
		@Id int id;

		@org.hibernate.annotations.JavaType(TemporalDescriptorAssessmentTests.AwareInstant.class)
		@JdbcTypeCode(INSTANT_CODE)
		Instant stamp;
	}
}
