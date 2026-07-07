/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations;

import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;
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
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Borys Piela
 * @author Yanming Zhou
 */
@Jpa( annotatedClasses = {UpdateTimestampTest.Event.class} )
public class UpdateTimestampTest {

	@Entity(name = "Event")
	static class Event {

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

		@Column(insertable = false)
		@UpdateTimestamp
		private LocalDateTime uninsertableLocalDateTime;

		@Column(insertable = false)
		@UpdateTimestamp(source = SourceType.DB)
		private LocalDateTime uninsertableLocalDateTimeWithDB;

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

		public LocalDateTime getUninsertableLocalDateTime() {
			return uninsertableLocalDateTime;
		}

		public LocalDateTime getUninsertableLocalDateTimeWithDB() {
			return uninsertableLocalDateTimeWithDB;
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

	@Test
	@JiraKey( value = "HHH-20663")
	public void uninsertableUpdateTimestamp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Event event = new Event();
			entityManager.persist( event );
			entityManager.flush();
			assertNull( event.getUninsertableLocalDateTime() );
		});
	}

	@Test
	@JiraKey( value = "HHH-20663")
	public void uninsertableUpdateTimestampWithDB(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Event event = new Event();
			entityManager.persist( event );
			entityManager.flush();
			assertNull( event.getUninsertableLocalDateTimeWithDB() );
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
