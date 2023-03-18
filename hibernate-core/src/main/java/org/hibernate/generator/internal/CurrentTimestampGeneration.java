/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.generator.internal;

import org.hibernate.AssertionFailure;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.tuple.GenerationTiming;

import java.lang.reflect.Member;
import java.sql.Time;
import java.sql.Timestamp;
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
	private final EnumSet<EventType> eventTypes;

	private final CurrentTimestampGeneratorDelegate delegate;
	private static final Map<Class<?>, CurrentTimestampGeneratorDelegate> generatorDelegates = new HashMap<>();

	static {
		generatorDelegates.put(
				Date.class,
				Date::new
		);
		generatorDelegates.put(
				Calendar.class,
				() -> {
					Calendar calendar = Calendar.getInstance();
					calendar.setTime( new Date() );
					return calendar;
				}
		);
		generatorDelegates.put(
				java.sql.Date.class,
				() -> new java.sql.Date( System.currentTimeMillis() )
		);

		generatorDelegates.put(
				Time.class,
				() -> new Time( System.currentTimeMillis() )
		);
		generatorDelegates.put(
				Timestamp.class,
				() -> new Timestamp( System.currentTimeMillis() )
		);
		generatorDelegates.put(
				Instant.class,
				Instant::now
		);
		generatorDelegates.put(
				LocalDate.class,
				LocalDate::now
		);
		generatorDelegates.put(
				LocalDateTime.class,
				LocalDateTime::now
		);
		generatorDelegates.put(
				LocalTime.class,
				LocalTime::now
		);
		generatorDelegates.put(
				MonthDay.class,
				MonthDay::now
		);
		generatorDelegates.put(
				OffsetDateTime.class,
				OffsetDateTime::now
		);
		generatorDelegates.put(
				OffsetTime.class,
				OffsetTime::now
		);
		generatorDelegates.put(
				Year.class,
				Year::now
		);
		generatorDelegates.put(
				YearMonth.class,
				YearMonth::now
		);
		generatorDelegates.put(
				ZonedDateTime.class,
				ZonedDateTime::now
		);
	}

	public CurrentTimestampGeneration(CurrentTimestamp annotation, Member member, GeneratorCreationContext context) {
		delegate = getGeneratorDelegate( annotation.source(), member );
		eventTypes = annotation.timing() == GenerationTiming.ALWAYS
				? fromArray( annotation.event() )
				: annotation.timing().getEquivalent().eventTypes();
	}

	public CurrentTimestampGeneration(CreationTimestamp annotation, Member member, GeneratorCreationContext context) {
		delegate = getGeneratorDelegate( annotation.source(), member );
		eventTypes = INSERT_ONLY;
	}

	public CurrentTimestampGeneration(UpdateTimestamp annotation, Member member, GeneratorCreationContext context) {
		delegate = getGeneratorDelegate( annotation.source(), member );
		eventTypes = INSERT_AND_UPDATE;
	}

	private static CurrentTimestampGeneratorDelegate getGeneratorDelegate(SourceType source, Member member) {
		switch (source) {
			case VM:
				// Generator is only used for in-VM generation
				return generatorDelegates.get( ReflectHelper.getPropertyType( member ) );
			case DB:
				return null;
			default:
				throw new AssertionFailure("unknown source");
		}
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

	private interface CurrentTimestampGeneratorDelegate {
		// Left out the Generator params, they're not used anyway. Since this is purely internal, this can be changed if needed
		Object generate();
	}
}
