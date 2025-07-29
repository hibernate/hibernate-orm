/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.generator.internal;

import java.lang.reflect.Member;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

import org.hibernate.SessionFactory;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.StatementPreparer;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.mapping.BasicValue;
import org.hibernate.type.descriptor.java.ClockHelper;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.type.descriptor.java.JavaType;

import static java.sql.Types.TIMESTAMP;
import static org.hibernate.engine.jdbc.JdbcLogging.JDBC_MESSAGE_LOGGER;
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
	private final JavaType<Object> propertyType;
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
		eventTypes = fromArray( annotation.event() );
		propertyType = getPropertyType( context );
	}

	public CurrentTimestampGeneration(CreationTimestamp annotation, Member member, GeneratorCreationContext context) {
		delegate = getGeneratorDelegate( annotation.source(), member, context );
		eventTypes = INSERT_ONLY;
		propertyType = getPropertyType( context );
	}

	public CurrentTimestampGeneration(UpdateTimestamp annotation, Member member, GeneratorCreationContext context) {
		delegate = getGeneratorDelegate( annotation.source(), member, context );
		eventTypes = INSERT_AND_UPDATE;
		propertyType = getPropertyType( context );
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
		return switch (source) {
			case DB -> null;
			case VM -> {
				// Generator is only used for in-VM generation
				final Key key = new Key( propertyType, getBaseClock( context ), getPrecision( context ) );
				final var delegate = GENERATOR_DELEGATES.get( key );
				if ( delegate != null ) {
					yield delegate;
				}
				else {
					final var producer = GENERATOR_PRODUCERS.get( key.clazz );
					if ( producer == null ) {
						yield null;
					}
					else {
						final var generatorDelegate = producer.apply( key.clock, key.precision );
						final var old = GENERATOR_DELEGATES.putIfAbsent( key, generatorDelegate );
						yield old != null ? old : generatorDelegate;
					}
				}
			}
		};
	}

	private static int getPrecision(GeneratorCreationContext context) {
		final BasicValue basicValue = (BasicValue) context.getProperty().getValue();
		final Size size =
				basicValue.getColumns().get( 0 )
						.getColumnSize( context.getDatabase().getDialect(),
								basicValue.getMetadata() );
		return size.getPrecision() == null ? 0 : size.getPrecision();
	}

	private static Clock getBaseClock(GeneratorCreationContext context) {
		return context.getServiceRegistry().requireService( ConfigurationService.class )
				.getSetting( CLOCK_SETTING_NAME, value -> (Clock) value );
	}

	public static <T extends Clock> T getClock(SessionFactory sessionFactory) {
		return (T) sessionFactory.getProperties().get( CLOCK_SETTING_NAME );
	}

	private static JavaType<Object> getPropertyType(GeneratorCreationContext context) {
		return context.getDatabase().getTypeConfiguration().getJavaTypeRegistry()
				.getDescriptor( context.getProperty().getType().getReturnedClass() );
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
		if ( delegate == null ) {
			if ( eventType != EventType.FORCE_INCREMENT ) {
				throw new UnsupportedOperationException( "CurrentTimestampGeneration.generate() should not have been called" );
			}
			return propertyType.wrap( getCurrentTimestamp( session ), session );
		}
		else {
			return delegate.generate();
		}
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

	private record Key(Class<?> clazz, @Nullable Clock clock, int precision) {
	}

	static Timestamp getCurrentTimestamp(SharedSessionContractImplementor session) {
		final Dialect dialect = session.getJdbcServices().getJdbcEnvironment().getDialect();
		return getCurrentTimestampFromDatabase(
				dialect.getCurrentTimestampSelectString(),
				dialect.isCurrentTimestampSelectStringCallable(),
				session
		);
	}

	static Timestamp getCurrentTimestampFromDatabase(
			String timestampSelectString,
			boolean callable,
			SharedSessionContractImplementor session) {
		final JdbcCoordinator coordinator = session.getJdbcCoordinator();
		final StatementPreparer statementPreparer = coordinator.getStatementPreparer();
		PreparedStatement statement = null;
		try {
			statement = statementPreparer.prepareStatement( timestampSelectString, callable );
			final Timestamp ts = callable
					? extractCalledResult( statement, coordinator, timestampSelectString )
					: extractResult( statement, coordinator, timestampSelectString );
			if ( JDBC_MESSAGE_LOGGER.isTraceEnabled() ) {
				JDBC_MESSAGE_LOGGER.currentTimestampRetrievedFromDatabase( ts, ts.getNanos(), ts.getTime() );
			}
			return ts;
		}
		catch (SQLException e) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					e,
					"could not obtain current timestamp from database",
					timestampSelectString
			);
		}
		finally {
			if ( statement != null ) {
				coordinator.getLogicalConnection().getResourceRegistry().release( statement );
				coordinator.afterStatementExecution();
			}
		}
	}

	static Timestamp extractResult(PreparedStatement statement, JdbcCoordinator coordinator, String sql)
			throws SQLException {
		final ResultSet resultSet = coordinator.getResultSetReturn().extract( statement, sql );
		resultSet.next();
		return resultSet.getTimestamp( 1 );
	}

	static Timestamp extractCalledResult(PreparedStatement statement, JdbcCoordinator coordinator, String sql)
			throws SQLException {
		final CallableStatement callable = (CallableStatement) statement;
		callable.registerOutParameter( 1, TIMESTAMP );
		coordinator.getResultSetReturn().execute( callable, sql );
		return callable.getTimestamp( 1 );
	}
}
