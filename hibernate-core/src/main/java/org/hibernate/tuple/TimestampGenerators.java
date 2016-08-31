/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.criteria.internal.expression.function.CurrentTimestampFunction;

/**
 * Generators for obtaining the current VM timestamp in different representations.
 *
 * @author Gunnar Morling
 */
/* package */ final class TimestampGenerators {

	private static final Map<Class<?>, Supplier<ValueGenerator<?>>> generators;

	private TimestampGenerators() {
	}

	static {
		generators = new HashMap<>();
		generators.put( Date.class, CurrentDateGenerator::new );
        generators.put( Calendar.class, CurrentCalendarGenerator::new );
        generators.put( java.sql.Date.class, CurrentSqlDateGenerator::new );
        generators.put( Time.class, CurrentSqlTimeGenerator::new );
		generators.put( Timestamp.class, CurrentSqlTimestampGenerator::new );
		generators.put( Instant.class, CurrentInstantGenerator::new );
		generators.put( LocalDate.class, CurrentLocalDateGenerator::new );
		generators.put( LocalDateTime.class, CurrentLocalDateTimeGenerator::new );
		generators.put( LocalTime.class, CurrentLocalTimeGenerator::new );
		generators.put( MonthDay.class, CurrentMonthDayGenerator::new );
		generators.put( OffsetDateTime.class, CurrentOffsetDateTimeGenerator::new );
		generators.put( OffsetTime.class, CurrentOffsetTimeGenerator::new );
		generators.put( Year.class, CurrentYearGenerator::new );
		generators.put( YearMonth.class, CurrentYearMonthGenerator::new );
		generators.put( ZonedDateTime.class, CurrentZonedDateTimeGenerator::new );
	}

	static class CurrentDateGenerator implements ValueGenerator<Date> {

		@Override
		public Date generateValue(Session session, Object owner) {
			return new Date();
		}
	}

	static class CurrentCalendarGenerator implements ValueGenerator<Calendar> {

		@Override
		public Calendar generateValue(Session session, Object owner) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime( new Date() );
			return calendar;
		}
	}

	static class CurrentSqlDateGenerator implements ValueGenerator<java.sql.Date> {

		@Override
		public java.sql.Date generateValue(Session session, Object owner) {
			return new java.sql.Date( new Date().getTime() );
		}
	}

	static class CurrentSqlTimeGenerator implements ValueGenerator<Time> {

		@Override
		public Time generateValue(Session session, Object owner) {
			return new Time( new Date().getTime() );
		}
	}

	static class CurrentSqlTimestampGenerator implements ValueGenerator<Timestamp> {

		@Override
		public Timestamp generateValue(Session session, Object owner) {
			return new Timestamp( new Date().getTime() );
		}
	}

	static class CurrentInstantGenerator implements ValueGenerator<Instant> {

		@Override
		public Instant generateValue(Session session, Object owner) {
			return Instant.now();
		}
	}

	static class CurrentLocalDateGenerator implements ValueGenerator<LocalDate> {

		@Override
		public LocalDate generateValue(Session session, Object owner) {
			return LocalDate.now();
		}
	}

	static class CurrentLocalDateTimeGenerator implements ValueGenerator<LocalDateTime> {

		@Override
		public LocalDateTime generateValue(Session session, Object owner) {
			return LocalDateTime.now();
		}
	}

	static class CurrentLocalTimeGenerator implements ValueGenerator<LocalTime> {

		@Override
		public LocalTime generateValue(Session session, Object owner) {
			return LocalTime.now();
		}
	}

	static class CurrentMonthDayGenerator implements ValueGenerator<MonthDay> {

		@Override
		public MonthDay generateValue(Session session, Object owner) {
			return MonthDay.now();
		}
	}

	static class CurrentOffsetDateTimeGenerator implements ValueGenerator<OffsetDateTime> {

		@Override
		public OffsetDateTime generateValue(Session session, Object owner) {
			return OffsetDateTime.now();
		}
	}

	static class CurrentOffsetTimeGenerator implements ValueGenerator<OffsetTime> {

		@Override
		public OffsetTime generateValue(Session session, Object owner) {
			return OffsetTime.now();
		}
	}

	static class CurrentYearGenerator implements ValueGenerator<Year> {

		@Override
		public Year generateValue(Session session, Object owner) {
			return Year.now();
		}
	}

	static class CurrentYearMonthGenerator implements ValueGenerator<YearMonth> {

		@Override
		public YearMonth generateValue(Session session, Object owner) {
			return YearMonth.now();
		}
	}

	static class CurrentZonedDateTimeGenerator implements ValueGenerator<ZonedDateTime> {

		@Override
		public ZonedDateTime generateValue(Session session, Object owner) {
			return ZonedDateTime.now();
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> ValueGenerator<T> get(final Class<T> type) {
		final Supplier<ValueGenerator<?>> valueGeneratorSupplier = generators.get(type);
		if (Objects.isNull(valueGeneratorSupplier)) {
			throw new HibernateException("Unsupported property type for generator annotation @CreationTimestamp/@UpdateTimestamp");
		}

		return (ValueGenerator<T>) valueGeneratorSupplier.get();
	}
}
