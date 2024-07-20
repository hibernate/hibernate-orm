/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.generator.internal;

import org.hibernate.AssertionFailure;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.mapping.BasicValue;
import org.hibernate.tuple.GenerationTiming;
import org.hibernate.type.descriptor.java.ClockHelper;

import java.lang.reflect.Member;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.generator.EventTypeSets.INSERT_AND_UPDATE;
import static org.hibernate.generator.EventTypeSets.INSERT_ONLY;
import static org.hibernate.generator.EventTypeSets.fromArray;

/**
 * Value generation strategy which produces a timestamp using the database
 * {@link Dialect#currentTimestamp() current_timestamp} function or the JVM
 * {@linkplain java.time.Clock#instant() current instant}.
 * <p>
 * Underlies the {@link CurrentTimestamp}, {@link CreationTimestamp}, and
 * {@link UpdateTimestamp} annotations.
 *
 * @see CurrentTimestamp
 * @see CreationTimestamp
 * @see UpdateTimestamp
 *
 * @since 6.0
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public class CurrentTimestampGeneration implements BeforeExecutionGenerator, OnExecutionGenerator {

	/**
	 * Configuration property name to set a custom {@link Clock} for Hibernate ORM to use when generating VM based
	 * timestamp values for e.g. {@link CurrentTimestamp}, {@link CreationTimestamp}, {@link UpdateTimestamp}
	 * and {@link org.hibernate.type.descriptor.java.VersionJavaType} methods.
	 *
	 * @since 6.6
	 */
	public static final String CLOCK_SETTING_NAME = "hibernate.testing.clock";

	private final EnumSet<EventType> eventTypes;

	private final CurrentTimestampGeneratorDelegate delegate;
	private static final Map<Class<?>, BiFunction<@Nullable Clock, Integer, CurrentTimestampGeneratorDelegate>> GENERATOR_PRODUCERS = new HashMap<>();
	private static final Map<Key, CurrentTimestampGeneratorDelegate> GENERATOR_DELEGATES = new ConcurrentHashMap<>();

	static {
		GENERATOR_PRODUCERS.put(
				Date.class,
				(baseClock, precision) -> {
					final Clock clock = ClockHelper.forPrecision( baseClock, precision, 3 );
					return () -> new Date( clock.millis() );
				}
		);
		GENERATOR_PRODUCERS.put(
				Calendar.class,
				(baseClock, precision) -> {
					final Clock clock = ClockHelper.forPrecision( baseClock, precision, 3 );
					return () -> {
						Calendar calendar = Calendar.getInstance();
						calendar.setTimeInMillis( clock.millis() );
						return calendar;
					};
				}
		);
		GENERATOR_PRODUCERS.put(
				java.sql.Date.class,
				(baseClock, precision) -> () -> new java.sql.Date( baseClock == null ? System.currentTimeMillis() : baseClock.millis() )
		);

		GENERATOR_PRODUCERS.put(
				Time.class,
				(baseClock, precision) -> {
					final Clock clock = ClockHelper.forPrecision( baseClock, precision, 3 );
					return () -> new Time( clock.millis() );
				}
		);
		GENERATOR_PRODUCERS.put(
				Timestamp.class,
				(baseClock, precision) -> {
					final Clock clock = ClockHelper.forPrecision( baseClock, precision, 9 );
					return () -> Timestamp.from( clock.instant() );
				}
		);
		GENERATOR_PRODUCERS.put(
				Instant.class,
				(baseClock, precision) -> {
					final Clock clock = ClockHelper.forPrecision( baseClock, precision, 9 );
					return clock::instant;
				}
		);
		GENERATOR_PRODUCERS.put(
				LocalDate.class,
				(baseClock, precision) -> () -> LocalDate.now( baseClock == null ? Clock.systemDefaultZone() : baseClock )
		);
		GENERATOR_PRODUCERS.put(
				LocalDateTime.class,
				(baseClock, precision) -> {
					final Clock clock = ClockHelper.forPrecision( baseClock, precision, 9 );
					return () -> LocalDateTime.now( clock );
				}
		);
		GENERATOR_PRODUCERS.put(
				LocalTime.class,
				(baseClock, precision) -> {
					final Clock clock = ClockHelper.forPrecision( baseClock, precision, 9 );
					return () -> LocalTime.now( clock );
				}
		);
		GENERATOR_PRODUCERS.put(
				MonthDay.class,
				(baseClock, precision) -> () -> MonthDay.now( baseClock == null ? Clock.systemDefaultZone() : baseClock )
		);
		GENERATOR_PRODUCERS.put(
				OffsetDateTime.class,
				(baseClock, precision) -> {
					final Clock clock = ClockHelper.forPrecision( baseClock, precision, 9 );
					return () -> OffsetDateTime.now( clock );
				}
		);
		GENERATOR_PRODUCERS.put(
				OffsetTime.class,
				(baseClock, precision) -> {
					final Clock clock = ClockHelper.forPrecision( baseClock, precision, 9 );
					return () -> OffsetTime.now( clock );
				}
		);
		GENERATOR_PRODUCERS.put(
				Year.class,
				(baseClock, precision) -> () -> Year.now( baseClock == null ? Clock.systemDefaultZone() : baseClock )
		);
		GENERATOR_PRODUCERS.put(
				YearMonth.class,
				(baseClock, precision) -> () -> YearMonth.now( baseClock == null ? Clock.systemDefaultZone() : baseClock )
		);
		GENERATOR_PRODUCERS.put(
				ZonedDateTime.class,
				(baseClock, precision) -> {
					final Clock clock = ClockHelper.forPrecision( baseClock, precision, 9 );
					return () -> ZonedDateTime.now( clock );
				}
		);
	}

	public CurrentTimestampGeneration(CurrentTimestamp annotation, Member member, GeneratorCreationContext context) {
		delegate = getGeneratorDelegate( annotation.source(), member, context );
		eventTypes = annotation.timing() == GenerationTiming.ALWAYS
				? fromArray( annotation.event() )
				: annotation.timing().getEquivalent().eventTypes();
	}

	public CurrentTimestampGeneration(CreationTimestamp annotation, Member member, GeneratorCreationContext context) {
		delegate = getGeneratorDelegate( annotation.source(), member, context );
		eventTypes = INSERT_ONLY;
	}

	public CurrentTimestampGeneration(UpdateTimestamp annotation, Member member, GeneratorCreationContext context) {
		delegate = getGeneratorDelegate( annotation.source(), member, context );
		eventTypes = INSERT_AND_UPDATE;
	}

	private static CurrentTimestampGeneratorDelegate getGeneratorDelegate(
			SourceType source,
			Member member,
			GeneratorCreationContext context) {
		return getGeneratorDelegate( source, ReflectHelper.getPropertyType( member ), context );
	}

	static CurrentTimestampGeneratorDelegate getGeneratorDelegate(
			SourceType source,
			Class<?> propertyType,
			GeneratorCreationContext context) {
		switch (source) {
			case VM:
				// Generator is only used for in-VM generation
				final BasicValue basicValue = (BasicValue) context.getProperty().getValue();
				final Size size = basicValue.getColumns().get( 0 ).getColumnSize(
						context.getDatabase().getDialect(),
						basicValue.getMetadata()
				);
				final Clock baseClock = context.getServiceRegistry()
						.requireService( ConfigurationService.class )
						.getSetting( CLOCK_SETTING_NAME, value -> (Clock) value );
				final Key key = new Key( propertyType, baseClock, size.getPrecision() == null ? 0 : size.getPrecision() );
				final CurrentTimestampGeneratorDelegate delegate = GENERATOR_DELEGATES.get( key );
				if ( delegate != null ) {
					return delegate;
				}
				final BiFunction<@Nullable Clock, Integer, CurrentTimestampGeneratorDelegate> producer = GENERATOR_PRODUCERS.get( key.clazz );
				if ( producer == null ) {
					return null;
				}
				final CurrentTimestampGeneratorDelegate generatorDelegate = producer.apply( key.clock, key.precision );
				final CurrentTimestampGeneratorDelegate old = GENERATOR_DELEGATES.putIfAbsent(
						key,
						generatorDelegate
				);
				return old != null ? old : generatorDelegate;
			case DB:
				return null;
			default:
				throw new AssertionFailure("unknown source");
		}
	}

	public static <T extends Clock> T getClock(SessionFactory sessionFactory) {
		return (T) sessionFactory.getProperties().get( CLOCK_SETTING_NAME );
	}

	@Override
	public boolean generatedOnExecution() {
		return delegate == null;
	}

	@Override
	public EnumSet<EventType> getEventTypes() {
		return eventTypes;
	}

	@Override
	public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue, EventType eventType) {
		return delegate.generate();
	}

	@Override
	public boolean writePropertyValue() {
		return false;
	}

	@Override
	public boolean referenceColumnsInSql(Dialect dialect) {
		return true;
	}

	@Override
	public String[] getReferencedColumnValues(Dialect dialect) {
		return new String[] { dialect.currentTimestamp() };
	}

	interface CurrentTimestampGeneratorDelegate {
		// Left out the Generator params, they're not used anyway. Since this is purely internal, this can be changed if needed
		Object generate();
	}

	private static class Key {
		private final Class<?> clazz;
		private final @Nullable Clock clock;
		private final int precision;

		public Key(Class<?> clazz, @Nullable Clock clock, int precision) {
			this.clazz = clazz;
			this.clock = clock;
			this.precision = precision;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			Key key = (Key) o;

			if ( precision != key.precision ) {
				return false;
			}
			return clock == key.clock && clazz.equals( key.clazz );
		}

		@Override
		public int hashCode() {
			int result = clazz.hashCode();
			result = 31 * result + ( clock == null ? 0 : clock.hashCode() );
			result = 31 * result + precision;
			return result;
		}
	}
}
