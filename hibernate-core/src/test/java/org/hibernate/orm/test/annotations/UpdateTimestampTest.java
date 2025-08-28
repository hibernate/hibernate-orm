/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations;

import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;

import org.hibernate.testing.orm.junit.JiraKey;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Borys Piela
 */
public class UpdateTimestampTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{
				Event.class
		};
	}

	@Entity(name = "Event")
	private static class Event {

		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "`date`")
		@UpdateTimestamp
		private Date date;

		@Column(name = "`calendar`")
		@UpdateTimestamp
		private Calendar calendar;

		@Column(name = "`sqlDate`")
		@UpdateTimestamp
		private java.sql.Date sqlDate;

		@Column(name = "`time`")
		@UpdateTimestamp
		private Time time;

		@Column(name = "`timestamp`")
		@UpdateTimestamp
		private Timestamp timestamp;

		@Column(name = "`instant`")
		@UpdateTimestamp
		private Instant instant;

		@Column(name = "`localDate`")
		@UpdateTimestamp
		private LocalDate localDate;

		@Column(name = "`localDateTime`")
		@UpdateTimestamp
		private LocalDateTime localDateTime;

		@Column(name = "`localTime`")
		@UpdateTimestamp
		private LocalTime localTime;

		@Column(name = "`monthDay`")
		@UpdateTimestamp
		private MonthDay monthDay;

		@Column(name = "`offsetDateTime`")
		@UpdateTimestamp
		private OffsetDateTime offsetDateTime;

		@Column(name = "`offsetTime`")
		@UpdateTimestamp
		private OffsetTime offsetTime;

		@Column(name = "`year`")
		@UpdateTimestamp
		private Year year;

		@Column(name = "`yearMonth`")
		@UpdateTimestamp
		private YearMonth yearMonth;

		@Column(name = "`zonedDateTime`")
		@UpdateTimestamp
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
			check( event );
		});
	}

	@Test
	@JiraKey( value = "HHH-16240")
	public void generatesCurrentTimestampInStatelessSession() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap( Session.class);
			try (StatelessSession statelessSession = session.getSessionFactory().openStatelessSession()) {
				Event event = new Event();
				statelessSession.getTransaction().begin();
				statelessSession.insert(event);
				statelessSession.getTransaction().commit();
				check( event );
			}
		});
	}

	private void check(Event event) {
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
