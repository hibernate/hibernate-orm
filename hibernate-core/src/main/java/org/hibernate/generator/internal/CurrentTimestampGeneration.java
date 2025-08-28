/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.generator.internal;

import java.lang.reflect.Member;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.SessionFactory;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
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
	private final GeneratorDelegate delegate;

	private static final ConcurrentHashMap<Key, GeneratorDelegate> GENERATOR_DELEGATES = new ConcurrentHashMap<>();

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

	private static GeneratorDelegate getGeneratorDelegate(
			SourceType source,
			Member member,
			GeneratorCreationContext context) {
		return getGeneratorDelegate( source, ReflectHelper.getPropertyType( member ), context );
	}

	static GeneratorDelegate getGeneratorDelegate(
			SourceType source,
			Class<?> propertyType,
			GeneratorCreationContext context) {
		return switch (source) {
			case DB -> null;
			case VM -> getGeneratorDelegate( propertyType, getBaseClock( context ), getPrecision( context ) );
		};
	}

	private static GeneratorDelegate getGeneratorDelegate(Class<?> propertyType, Clock baseClock, int precision) {
		final var key = new Key( propertyType, baseClock, precision );
		final var delegate = GENERATOR_DELEGATES.get( key );
		if ( delegate != null ) {
			return delegate;
		}
		else {
			final var generatorDelegate = generatorDelegate( propertyType, baseClock, precision );
			if ( generatorDelegate == null ) {
				return null;
			}
			else {
				final var old = GENERATOR_DELEGATES.putIfAbsent( key, generatorDelegate );
				return old != null ? old : generatorDelegate;
			}
		}
	}

	private static int getPrecision(GeneratorCreationContext context) {
		final var basicValue = (BasicValue) context.getProperty().getValue();
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

	static Timestamp getCurrentTimestamp(SharedSessionContractImplementor session) {
		final var dialect = session.getJdbcServices().getJdbcEnvironment().getDialect();
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
		final var coordinator = session.getJdbcCoordinator();
		final var statementPreparer = coordinator.getStatementPreparer();
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
		final var resultSet = coordinator.getResultSetReturn().extract( statement, sql );
		resultSet.next();
		return resultSet.getTimestamp( 1 );
	}

	static Timestamp extractCalledResult(PreparedStatement statement, JdbcCoordinator coordinator, String sql)
			throws SQLException {
		final var callable = (CallableStatement) statement;
		callable.registerOutParameter( 1, TIMESTAMP );
		coordinator.getResultSetReturn().execute( callable, sql );
		return callable.getTimestamp( 1 );
	}

	@FunctionalInterface
	interface GeneratorDelegate {
		// Left out the Generator params, they're not used anyway.
		// Since this is purely internal, this can be changed if needed.
		Object generate();
	}

	private record Key(Class<?> clazz, @Nullable Clock clock, int precision) {
	}

	private static GeneratorDelegate generatorDelegate(Class<?> clazz, @Nullable Clock baseClock, int precision) {
		if ( clazz == Instant.class ) {
			final var clock = ClockHelper.forPrecision( baseClock, precision, 9 );
			return clock::instant;
		}
		else if ( clazz == LocalDateTime.class ) {
			final var clock = ClockHelper.forPrecision( baseClock, precision, 9 );
			return () -> LocalDateTime.now( clock );
		}
		else if ( clazz == LocalDate.class ) {
			return () -> LocalDate.now( baseClock == null ? Clock.systemDefaultZone() : baseClock );
		}
		else if ( clazz == LocalTime.class ) {
			final var clock = ClockHelper.forPrecision( baseClock, precision, 9 );
			return () -> LocalTime.now( clock );
		}
		else if ( clazz == OffsetDateTime.class ) {
			final var clock = ClockHelper.forPrecision( baseClock, precision, 9 );
			return () -> OffsetDateTime.now( clock );
		}
		else if ( clazz == OffsetTime.class ) {
			final var clock = ClockHelper.forPrecision( baseClock, precision, 9 );
			return () -> OffsetTime.now( clock );
		}
		else if ( clazz == ZonedDateTime.class ) {
			final var clock = ClockHelper.forPrecision( baseClock, precision, 9 );
			return () -> ZonedDateTime.now( clock );
		}
		else if ( clazz == Year.class ) {
			return () -> Year.now( baseClock == null ? Clock.systemDefaultZone() : baseClock );
		}
		else if ( clazz == YearMonth.class ) {
			return () -> YearMonth.now( baseClock == null ? Clock.systemDefaultZone() : baseClock );
		}
		else if ( clazz == MonthDay.class ) {
			return () -> MonthDay.now( baseClock == null ? Clock.systemDefaultZone() : baseClock );
		}
		// DEPRECATED:
		else if ( clazz == Date.class ) {
			final var clock = ClockHelper.forPrecision( baseClock, precision, 3 );
			return () -> new Date( clock.millis() );
		}
		else if ( clazz == Calendar.class ) {
			final var clock = ClockHelper.forPrecision( baseClock, precision, 3 );
			return () -> {
				var calendar = Calendar.getInstance();
				calendar.setTimeInMillis( clock.millis() );
				return calendar;
			};
		}
		else if ( clazz == java.sql.Timestamp.class ) {
			final var clock = ClockHelper.forPrecision( baseClock, precision, 9 );
			return () -> java.sql.Timestamp.from( clock.instant() );
		}
		else if ( clazz == java.sql.Date.class ) {
			return () -> new java.sql.Date( baseClock == null ? System.currentTimeMillis() : baseClock.millis() );
		}
		else if ( clazz == java.sql.Time.class ) {
			final var clock = ClockHelper.forPrecision( baseClock, precision, 3 );
			return () -> new java.sql.Time( clock.millis() );
		}
		else {
			return null;
		}
	}
}
