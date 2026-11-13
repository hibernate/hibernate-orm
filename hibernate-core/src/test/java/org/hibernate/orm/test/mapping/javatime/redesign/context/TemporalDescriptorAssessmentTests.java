package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.time.*;
import java.util.*;
import java.util.stream.Stream;

import org.hibernate.MappingException;
import org.hibernate.dialect.*;
import org.hibernate.dialect.temporaltype.spi.TemporalPreservation;
import org.hibernate.type.descriptor.java.*;
import org.hibernate.type.descriptor.jdbc.*;
import org.hibernate.type.internal.TemporalMappingValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.dialect.temporaltype.spi.TemporalPreservation.Level.*;
import static org.hibernate.dialect.temporaltype.spi.TemporalPreservation.TemporalPart.*;

/// Descriptor contracts are assessed per actual instance, independently of a registry.
/// Provider reports here are dispatch controls, not claims of live driver execution.
///
/// @author Steve Ebersole
public class TemporalDescriptorAssessmentTests {
	static final TemporalPreservation INSTANT_ONLY =
			new TemporalPreservation(UNKNOWN, UNKNOWN, SUPPORTED, UNKNOWN, UNKNOWN);

	static Stream<Dialect> dialects() {
		return Stream.of(
				new Dialect(DatabaseVersion.make(1)) {},
				new H2Dialect(),
				new PostgreSQLDialect(DatabaseVersion.make(18, 6)),
				new OracleDialect(DatabaseVersion.make(23, 26)));
	}

	@ParameterizedTest
	@MethodSource("dialects")
	void replacementAndSubclassRecognition(Dialect dialect) {
		assertThat(
						dialect.getTemporalPreservation(
										InstantJavaType.INSTANCE,
										TimestampUtcAsPlainJdbcTimestampJdbcType.INSTANCE)
								.instant())
				.isEqualTo(SUPPORTED);
		for (var javaType : List.of(new InstantJavaType() {}, new AwareInstant())) {
			for (var jdbc :
					List.of(
							TimestampUtcAsPlainJdbcTimestampJdbcType.INSTANCE,
							new PlainSubclass())) {
				assertThatThrownBy(() -> validate(dialect, javaType, jdbc, null))
						.hasMessageContaining("INSTANT")
						.hasMessageContaining("UNKNOWN");
			}
		}
		assertThatThrownBy(
						() ->
								validate(
										dialect,
										InstantJavaType.INSTANCE,
										new PlainSubclass(),
										null))
				.hasMessageContaining("UNKNOWN");
		validate(dialect, new AwareInstant(), new DeclaredJdbc(), null);
		assertThat(
						dialect.getTemporalPreservation(
										InstantJavaType.INSTANCE, new DeclinedJdbc())
								.instant())
				.isEqualTo(UNKNOWN);
	}

	@ParameterizedTest
	@MethodSource("dialects")
	void authoritativePartsAndInstanceIsolation(Dialect dialect) {
		var good = new DeclaredJdbc();
		var other = new DeclaredJdbc();
		other.report = TemporalPreservation.UNKNOWN;
		validate(dialect, InstantJavaType.INSTANCE, good, null);
		for (var level : List.of(UNKNOWN, UNSUPPORTED, IRRELEVANT)) {
			other.report =
					new TemporalPreservation(SUPPORTED, SUPPORTED, level, SUPPORTED, SUPPORTED);
			assertThat(dialect.getTemporalPreservation(InstantJavaType.INSTANCE, other))
					.isSameAs(other.report);
			assertThatThrownBy(() -> validate(dialect, InstantJavaType.INSTANCE, other, null))
					.hasMessageContaining(level.name());
			validate(dialect, InstantJavaType.INSTANCE, good, null);
		}
	}

	@Test
	void applicationDialectRemainsAuthoritativeOrDelegates() {
		var type = new DeclaredJdbc();
		var overriding =
				new H2Dialect() {
					@Override
					public TemporalPreservation getTemporalPreservation(
							JavaType<?> javaType, JdbcType jdbcType) {
						return jdbcType == type
								? TemporalPreservation.UNKNOWN
								: super.getTemporalPreservation(javaType, jdbcType);
					}
				};
		assertThatThrownBy(() -> validate(overriding, InstantJavaType.INSTANCE, type, null))
				.hasMessageContaining("UNKNOWN");
		var delegating =
				new H2Dialect() {
					@Override
					public TemporalPreservation getTemporalPreservation(
							JavaType<?> javaType, JdbcType jdbcType) {
						return super.getTemporalPreservation(javaType, jdbcType);
					}
				};
		validate(delegating, InstantJavaType.INSTANCE, type, null);
		validate(
				delegating,
				InstantJavaType.INSTANCE,
				TimestampUtcAsPlainJdbcTimestampJdbcType.INSTANCE,
				null);
	}

	@Test
	void inheritedContractsAreDeliberate() {
		var dialect = new H2Dialect();
		validate(dialect, new AwareInstant() {}, new FamilyChild(), null);
		assertThatThrownBy(
						() -> validate(dialect, InstantJavaType.INSTANCE, new ExactChild(), null))
				.hasMessageContaining("UNKNOWN");
		assertThatThrownBy(
						() -> validate(dialect, InstantJavaType.INSTANCE, new LosingChild(), null))
				.hasMessageContaining("UNSUPPORTED");
	}

	@Test
	void malformedRequirementsAndKnownDomainContracts() {
		var javaType = new AwareInstant();
		var jdbc = new DeclaredJdbc();
		var dialect = new H2Dialect();
		for (Set<TemporalPreservation.TemporalPart> parts :
				Arrays.<Set<TemporalPreservation.TemporalPart>>asList(
						null,
						Set.of(),
						new HashSet<>(Arrays.asList(INSTANT, null)),
						Set.of(DATE))) {
			javaType.parts = parts;
			assertThatThrownBy(() -> validate(dialect, javaType, jdbc, null))
					.isInstanceOf(MappingException.class)
					.hasMessageContaining("SPI requirements")
					.hasMessageContaining("probe.value")
					.hasMessageContaining(AwareInstant.class.getName())
					.hasMessageContaining(DeclaredJdbc.class.getName());
		}
		javaType.parts = new HashSet<>(Set.of(INSTANT));
		validate(dialect, javaType, jdbc, null);
		assertThat(javaType.parts).containsExactly(INSTANT);
	}

	@Test
	void allCanonicalDeclarationsAndAdjustmentCopy() {
		var dialect = new H2Dialect();
		var all = new DeclaredJdbc();
		all.report =
				new TemporalPreservation(SUPPORTED, SUPPORTED, SUPPORTED, SUPPORTED, SUPPORTED);
		for (var javaType :
				List.<JavaType<?>>of(
						new AwareDate(),
						new AwareTime(),
						new AwareDateTime(),
						new AwareInstant(),
						new AwareOffsetTime(),
						new AwareOffsetDateTime(),
						new AwareZoned())) {
			validate(dialect, javaType, all, null);
		}
		var offset = new AwareOffsetDateTime();
		validate(dialect, offset, new DeclaredJdbc(), ZoneOffset.UTC);
		assertThat(offset.parts).containsExactlyInAnyOrder(INSTANT, OFFSET);
		assertThatThrownBy(() -> validate(dialect, offset, new DeclaredJdbc(), null))
				.hasMessageContaining("OFFSET");
	}

	@Test
	void callbackFailuresAndRecursionReleaseState() {
		var dialect = new H2Dialect();
		var javaType = InstantJavaType.INSTANCE;
		var nullResult =
				new DeclaredJdbc() {
					@Override
					public Optional<TemporalPreservation> getTemporalPreservation(
							JavaType<?> j, Dialect d) {
						return null;
					}
				};
		assertThatThrownBy(() -> validate(dialect, javaType, nullResult, null))
				.hasMessageContaining("null Optional")
				.hasMessageContaining("probe.value");
		var broken =
				new DeclaredJdbc() {
					@Override
					public Optional<TemporalPreservation> getTemporalPreservation(
							JavaType<?> j, Dialect d) {
						throw new IllegalStateException("original contributor failure");
					}
				};
		assertThatThrownBy(() -> validate(dialect, javaType, broken, null))
				.hasRootCauseInstanceOf(IllegalStateException.class)
				.hasRootCauseMessage("original contributor failure");
		var first = new RecursiveJdbc();
		var second = new RecursiveJdbc();
		first.delegate = first;
		assertThatThrownBy(() -> validate(dialect, javaType, first, null))
				.hasMessageContaining("recursive assessment");
		first.delegate = second;
		second.delegate = first;
		assertThatThrownBy(() -> validate(dialect, javaType, first, null))
				.hasMessageContaining("recursive assessment");
		second.delegate = TimestampUtcAsPlainJdbcTimestampJdbcType.INSTANCE;
		validate(dialect, javaType, first, null);
		validate(dialect, javaType, new DeclaredJdbc(), null);
	}

	static void validate(Dialect dialect, JavaType<?> javaType, JdbcType jdbc, ZoneId adjustment) {
		TemporalMappingValidator.validate(dialect, javaType, jdbc, adjustment, "probe.value");
	}

	public static class PlainSubclass extends TimestampUtcAsJdbcTimestampJdbcType {}

	public static class AwareInstant extends InstantJavaType
			implements PreservationAwareTemporalJavaType<Instant> {
		Set<TemporalPreservation.TemporalPart> parts = Set.of(INSTANT);

		@Override
		public Set<TemporalPreservation.TemporalPart> getRequiredTemporalParts() {
			return parts;
		}
	}

	public static class AwareDate extends LocalDateJavaType
			implements PreservationAwareTemporalJavaType<LocalDate> {
		@Override
		public Set<TemporalPreservation.TemporalPart> getRequiredTemporalParts() {
			return Set.of(DATE);
		}
	}

	public static class AwareTime extends LocalTimeJavaType
			implements PreservationAwareTemporalJavaType<LocalTime> {
		@Override
		public Set<TemporalPreservation.TemporalPart> getRequiredTemporalParts() {
			return Set.of(TIME);
		}
	}

	public static class AwareDateTime extends LocalDateTimeJavaType
			implements PreservationAwareTemporalJavaType<LocalDateTime> {
		@Override
		public Set<TemporalPreservation.TemporalPart> getRequiredTemporalParts() {
			return Set.of(DATE, TIME);
		}
	}

	public static class AwareOffsetTime extends OffsetTimeJavaType
			implements PreservationAwareTemporalJavaType<OffsetTime> {
		@Override
		public Set<TemporalPreservation.TemporalPart> getRequiredTemporalParts() {
			return Set.of(TIME, OFFSET);
		}
	}

	public static class AwareOffsetDateTime extends OffsetDateTimeJavaType
			implements PreservationAwareTemporalJavaType<OffsetDateTime> {
		final Set<TemporalPreservation.TemporalPart> parts = new HashSet<>(Set.of(INSTANT, OFFSET));

		@Override
		public Set<TemporalPreservation.TemporalPart> getRequiredTemporalParts() {
			return parts;
		}
	}

	public static class AwareZoned extends ZonedDateTimeJavaType
			implements PreservationAwareTemporalJavaType<ZonedDateTime> {
		@Override
		public Set<TemporalPreservation.TemporalPart> getRequiredTemporalParts() {
			return Set.of(INSTANT, OFFSET, ZONE_ID);
		}
	}

	public static class DeclaredJdbc extends TimestampUtcAsJdbcTimestampJdbcType
			implements TemporalJdbcType {
		TemporalPreservation report = INSTANT_ONLY;

		@Override
		public Optional<TemporalPreservation> getTemporalPreservation(
				JavaType<?> javaType, Dialect dialect) {
			return Optional.of(report);
		}
	}

	public static class DeclinedJdbc extends DeclaredJdbc {
		@Override
		public Optional<TemporalPreservation> getTemporalPreservation(
				JavaType<?> javaType, Dialect dialect) {
			return Optional.empty();
		}
	}

	public static class FamilyChild extends DeclaredJdbc {}

	public static class ExactJdbc extends DeclaredJdbc {
		@Override
		public Optional<TemporalPreservation> getTemporalPreservation(
				JavaType<?> javaType, Dialect dialect) {
			return getClass() == ExactJdbc.class
					? super.getTemporalPreservation(javaType, dialect)
					: Optional.empty();
		}
	}

	public static class ExactChild extends ExactJdbc {}

	public static class LosingChild extends DeclaredJdbc {
		@Override
		public Optional<TemporalPreservation> getTemporalPreservation(
				JavaType<?> javaType, Dialect dialect) {
			return Optional.of(
					new TemporalPreservation(UNKNOWN, UNKNOWN, UNSUPPORTED, UNKNOWN, UNKNOWN));
		}
	}

	public static class RecursiveJdbc extends DeclaredJdbc {
		JdbcType delegate;

		@Override
		public Optional<TemporalPreservation> getTemporalPreservation(
				JavaType<?> javaType, Dialect dialect) {
			return Optional.of(dialect.getTemporalPreservation(javaType, delegate));
		}
	}
}
