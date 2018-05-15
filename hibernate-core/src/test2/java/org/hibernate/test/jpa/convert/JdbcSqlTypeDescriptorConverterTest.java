/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.jpa.convert;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import javax.persistence.AttributeConverter;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
public class JdbcSqlTypeDescriptorConverterTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { JavaTimeBean.class };
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12586")
	public void testJava8TimeObjectsUsingJdbcSqlTypeDescriptors() {
		// Because some databases do not support millisecond values in timestamps, we clear it here.
		// This will serve sufficient for our test to verify that the retrieved values match persisted.
		LocalDateTime now = LocalDateTime.now().withNano( 0 );

		// persist the record.
		Integer rowId = doInJPA( this::entityManagerFactory, entityManager -> {
			JavaTimeBean javaTime = new JavaTimeBean();
			javaTime.setLocalDate( now.toLocalDate() );
			javaTime.setLocalTime( now.toLocalTime() );
			javaTime.setLocalDateTime( now );
			entityManager.persist( javaTime );
			return javaTime.getId();
		} );

		// retrieve the record and verify values.
		doInJPA( this::entityManagerFactory, entityManager -> {
			final JavaTimeBean javaTime = entityManager.find( JavaTimeBean.class, rowId );
			assertEquals( now.toLocalDate(), javaTime.getLocalDate() );
			assertEquals( now.toLocalTime(), javaTime.getLocalTime() );
			assertEquals( now, javaTime.getLocalDateTime() );
		} );
	}

	@Entity(name = "JavaTimeBean")
	public static class JavaTimeBean {
		@Id
		@GeneratedValue
		private Integer id;

		@Convert(converter = LocalDateToDateConverter.class)
		@Column(name = "LDATE")
		private LocalDate localDate;

		@Convert(converter = LocalTimeToTimeConverter.class)
		@Column(name = "LTIME" )
		private LocalTime localTime;

		@Convert(converter = LocalDateTimeToTimestampConverter.class)
		@Column(name = "LDATETIME")
		private LocalDateTime localDateTime;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public LocalDate getLocalDate() {
			return localDate;
		}

		public void setLocalDate(LocalDate localDate) {
			this.localDate = localDate;
		}

		public LocalTime getLocalTime() {
			return localTime;
		}

		public void setLocalTime(LocalTime localTime) {
			this.localTime = localTime;
		}

		public LocalDateTime getLocalDateTime() {
			return localDateTime;
		}

		public void setLocalDateTime(LocalDateTime localDateTime) {
			this.localDateTime = localDateTime;
		}
	}

	public static class LocalDateToDateConverter implements AttributeConverter<LocalDate, Date> {
		@Override
		public Date convertToDatabaseColumn(LocalDate localDate) {
			return localDate == null ? null : Date.valueOf( localDate );
		}

		@Override
		public LocalDate convertToEntityAttribute(Date date) {
			return date == null ? null : date.toLocalDate();
		}
	}

	public static class LocalDateTimeToTimestampConverter implements AttributeConverter<LocalDateTime, Timestamp> {
		@Override
		public Timestamp convertToDatabaseColumn(LocalDateTime localDateTime) {
			return localDateTime == null ? null : Timestamp.valueOf( localDateTime );
		}

		@Override
		public LocalDateTime convertToEntityAttribute(Timestamp timestamp) {
			return timestamp == null ? null : timestamp.toLocalDateTime();
		}
	}

	public static class LocalTimeToTimeConverter implements AttributeConverter<LocalTime, Time> {
		@Override
		public Time convertToDatabaseColumn(LocalTime localTime) {
			return localTime == null ? null : Time.valueOf( localTime );
		}

		@Override
		public LocalTime convertToEntityAttribute(Time time) {
			return time == null ? null : time.toLocalTime();
		}
	}
}
