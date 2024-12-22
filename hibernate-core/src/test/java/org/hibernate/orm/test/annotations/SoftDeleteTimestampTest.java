/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.SoftDeleteTimestamp;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

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

import static org.hibernate.testing.transaction.TransactionUtil.*;

/**
 * @author Yongjun Hong
 */
public class SoftDeleteTimestampTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Event.class
		};
	}

	@Entity(name = "Event")
	private static class Event {

		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "`date`")
		@SoftDeleteTimestamp
		private Date date;

		@Column(name = "`calendar`")
		@SoftDeleteTimestamp
		private Calendar calendar;

		@Column(name = "`sqlDate`")
		@SoftDeleteTimestamp
		private java.sql.Date sqlDate;

		@Column(name = "`time`")
		@SoftDeleteTimestamp
		private Time time;

		@Column(name = "`timestamp`")
		@SoftDeleteTimestamp
		private Timestamp timestamp;

		@Column(name = "`instant`")
		@SoftDeleteTimestamp
		private Instant instant;

		@Column(name = "`localDate`")
		@SoftDeleteTimestamp
		private LocalDate localDate;

		@Column(name = "`localDateTime`")
		@SoftDeleteTimestamp
		private LocalDateTime localDateTime;

		@Column(name = "`localTime`")
		@SoftDeleteTimestamp
		private LocalTime localTime;

		@Column(name = "`monthDay`")
		@SoftDeleteTimestamp
		private MonthDay monthDay;

		@Column(name = "`offsetDateTime`")
		@SoftDeleteTimestamp
		private OffsetDateTime offsetDateTime;

		@Column(name = "`offsetTime`")
		@SoftDeleteTimestamp
		private OffsetTime offsetTime;

		@Column(name = "`year`")
		@SoftDeleteTimestamp
		private Year year;

		@Column(name = "`yearMonth`")
		@SoftDeleteTimestamp
		private YearMonth yearMonth;

		@Column(name = "`zonedDateTime`")
		@SoftDeleteTimestamp
		private ZonedDateTime zonedDateTime;

		public Event() {
		}

		public Long getId() {
			return id;
		}

		public Date getDate() {
			return date;
		}

		public Calendar getCalendar() {
			return calendar;
		}

		public java.sql.Date getSqlDate() {
			return sqlDate;
		}

		public Time getTime() {
			return time;
		}

		public Timestamp getTimestamp() {
			return timestamp;
		}

		public Instant getInstant() {
			return instant;
		}

		public LocalDate getLocalDate() {
			return localDate;
		}

		public LocalDateTime getLocalDateTime() {
			return localDateTime;
		}

		public LocalTime getLocalTime() {
			return localTime;
		}

		public MonthDay getMonthDay() {
			return monthDay;
		}

		public OffsetDateTime getOffsetDateTime() {
			return offsetDateTime;
		}

		public OffsetTime getOffsetTime() {
			return offsetTime;
		}

		public Year getYear() {
			return year;
		}

		public YearMonth getYearMonth() {
			return yearMonth;
		}

		public ZonedDateTime getZonedDateTime() {
			return zonedDateTime;
		}
	}

	@Test
	public void generatesCurrentTimestamp() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Event event = new Event();
			entityManager.persist(event);
			entityManager.flush();

			entityManager.remove(event);
			entityManager.flush();

			check( event );
		});
	}

	private void check(SoftDeleteTimestampTest.Event event) {
		Assert.assertNotNull(event.getDate());
		Assert.assertNotNull(event.getCalendar());
		Assert.assertNotNull(event.getSqlDate());
		Assert.assertNotNull(event.getTime());
		Assert.assertNotNull(event.getTimestamp());
		Assert.assertNotNull(event.getInstant());
		Assert.assertNotNull(event.getLocalDate());
		Assert.assertNotNull(event.getLocalDateTime());
		Assert.assertNotNull(event.getLocalTime());
		Assert.assertNotNull(event.getMonthDay());
		Assert.assertNotNull(event.getOffsetDateTime());
		Assert.assertNotNull(event.getOffsetTime());
		Assert.assertNotNull(event.getYear());
		Assert.assertNotNull(event.getYearMonth());
		Assert.assertNotNull(event.getZonedDateTime());
	}
}
