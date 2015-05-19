/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

import java.sql.Time;
import java.sql.Timestamp;
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
}
