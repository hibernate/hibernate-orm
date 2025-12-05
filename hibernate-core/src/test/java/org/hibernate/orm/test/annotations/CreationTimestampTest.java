/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations;

import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

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

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Borys Piela
 */
@Jpa(annotatedClasses = {CreationTimestampTest.Event.class})
public class CreationTimestampTest {

	@Entity(name = "Event")
	static class Event {

		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "`date`")
		@CreationTimestamp
		private Date date;

		@Column(name = "`calendar`")
		@CreationTimestamp
		private Calendar calendar;

		@Column(name = "`sqlDate`")
		@CreationTimestamp
		private java.sql.Date sqlDate;

		@Column(name = "`time`")
		@CreationTimestamp
		private Time time;

		@Column(name = "`timestamp`")
		@CreationTimestamp
		private Timestamp timestamp;

		@Column(name = "`instant`")
		@CreationTimestamp
		private Instant instant;

		@Column(name = "`localDate`")
		@CreationTimestamp
		private LocalDate localDate;

		@Column(name = "`localDateTime`")
		@CreationTimestamp
		private LocalDateTime localDateTime;

		@Column(name = "`localTime`")
		@CreationTimestamp
		private LocalTime localTime;

		@Column(name = "`monthDay`")
		@CreationTimestamp
		private MonthDay monthDay;

		@Column(name = "`offsetDateTime`")
		@CreationTimestamp
		private OffsetDateTime offsetDateTime;

		@Column(name = "`offsetTime`")
		@CreationTimestamp
		private OffsetTime offsetTime;

		@Column(name = "`year`")
		@CreationTimestamp
		private Year year;

		@Column(name = "`yearMonth`")
		@CreationTimestamp
		private YearMonth yearMonth;

		@Column(name = "`zonedDateTime`")
		@CreationTimestamp
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
	public void generatesCurrentTimestamp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Event event = new Event();
			entityManager.persist(event);
			entityManager.flush();
			check( event );
		});
	}

	@Test
	@JiraKey( value = "HHH-16240")
	public void generatesCurrentTimestampInStatelessSession(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
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
		assertNotNull(event.getDate());
		assertNotNull(event.getCalendar());
		assertNotNull(event.getSqlDate());
		assertNotNull(event.getTime());
		assertNotNull(event.getTimestamp());
		assertNotNull(event.getInstant());
		assertNotNull(event.getLocalDate());
		assertNotNull(event.getLocalDateTime());
		assertNotNull(event.getLocalTime());
		assertNotNull(event.getMonthDay());
		assertNotNull(event.getOffsetDateTime());
		assertNotNull(event.getOffsetTime());
		assertNotNull(event.getYear());
		assertNotNull(event.getYearMonth());
		assertNotNull(event.getZonedDateTime());
	}
}
