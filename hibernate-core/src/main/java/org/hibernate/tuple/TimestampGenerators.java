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

import org.hibernate.Session;

/**
 * Generators for obtaining the current VM timestamp in different representations.
 *
 * @author Gunnar Morling
 */
/* package */ interface TimestampGenerators {

	class CurrentDateGenerator implements ValueGenerator<Date> {

		@Override
		public Date generateValue(Session session, Object owner) {
			return new Date();
		}
	}

	class CurrentCalendarGenerator implements ValueGenerator<Calendar> {

		@Override
		public Calendar generateValue(Session session, Object owner) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime( new Date() );
			return calendar;
		}
	}

	class CurrentSqlDateGenerator implements ValueGenerator<java.sql.Date> {

		@Override
		public java.sql.Date generateValue(Session session, Object owner) {
			return new java.sql.Date( new Date().getTime() );
		}
	}

	class CurrentSqlTimeGenerator implements ValueGenerator<Time> {

		@Override
		public Time generateValue(Session session, Object owner) {
			return new Time( new Date().getTime() );
		}
	}

	class CurrentSqlTimestampGenerator implements ValueGenerator<Timestamp> {

		@Override
		public Timestamp generateValue(Session session, Object owner) {
			return new Timestamp( new Date().getTime() );
		}
	}

	class CurrentInstantGenerator implements ValueGenerator<Instant> {

		@Override
		public Instant generateValue(Session session, Object owner) {
			return Instant.now();
		}
	}

	class CurrentLocalDateGenerator implements ValueGenerator<LocalDate> {

		@Override
		public LocalDate generateValue(Session session, Object owner) {
			return LocalDate.now();
		}
	}

	class CurrentLocalDateTimeGenerator implements ValueGenerator<LocalDateTime> {

		@Override
		public LocalDateTime generateValue(Session session, Object owner) {
			return LocalDateTime.now();
		}
	}

	class CurrentLocalTimeGenerator implements ValueGenerator<LocalTime> {

		@Override
		public LocalTime generateValue(Session session, Object owner) {
			return LocalTime.now();
		}
	}

	class CurrentMonthDayGenerator implements ValueGenerator<MonthDay> {

		@Override
		public MonthDay generateValue(Session session, Object owner) {
			return MonthDay.now();
		}
	}

	class CurrentOffsetDateTimeGenerator implements ValueGenerator<OffsetDateTime> {

		@Override
		public OffsetDateTime generateValue(Session session, Object owner) {
			return OffsetDateTime.now();
		}
	}

	class CurrentOffsetTimeGenerator implements ValueGenerator<OffsetTime> {

		@Override
		public OffsetTime generateValue(Session session, Object owner) {
			return OffsetTime.now();
		}
	}

	class CurrentYearGenerator implements ValueGenerator<Year> {

		@Override
		public Year generateValue(Session session, Object owner) {
			return Year.now();
		}
	}

	class CurrentYearMonthGenerator implements ValueGenerator<YearMonth> {

		@Override
		public YearMonth generateValue(Session session, Object owner) {
			return YearMonth.now();
		}
	}

	class CurrentZonedDateTimeGenerator implements ValueGenerator<ZonedDateTime> {

		@Override
		public ZonedDateTime generateValue(Session session, Object owner) {
			return ZonedDateTime.now();
		}
	}

}
