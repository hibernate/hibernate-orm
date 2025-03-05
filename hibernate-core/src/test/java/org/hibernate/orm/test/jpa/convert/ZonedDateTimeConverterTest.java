/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.convert;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(
		annotatedClasses = ZonedDateTimeConverterTest.JavaTimeBean.class
)
public class ZonedDateTimeConverterTest {

	@Test
	@JiraKey(value = "HHH-15605")
	public void testConvertedTemporalJavaType(EntityManagerFactoryScope scope) {
		// Because some databases do not support millisecond values in timestamps, we clear it here.
		// This will serve sufficient for our test to verify that the retrieved values match persisted.
		ZonedDateTime today = ZonedDateTime.now( ZonedDateTimeConverter.BERLIN ).withNano( 0 );

		// persist the record.
		JavaTimeBean testEntity = scope.fromTransaction( entityManager -> {
			JavaTimeBean javaTime = new JavaTimeBean();
			javaTime.setUpdated( today.minusDays( 14L ) );
			entityManager.persist( javaTime );
			return javaTime;
		} );

		// retrieve the record and verify values.
		scope.inTransaction( entityManager -> {
			JavaTimeBean demo = entityManager.createQuery(
							"select d1_0 from JavaTimeBean d1_0 where d1_0.updated<?1", JavaTimeBean.class )
					.setParameter( 1, today )
					.getSingleResult();

			assertEquals( testEntity.getUpdated(), demo.getUpdated() );
		} );
	}

	@Entity(name = "JavaTimeBean")
	public static class JavaTimeBean {
		@Id
		@GeneratedValue
		private Integer id;

		@Convert(converter = ZonedDateTimeConverter.class)
		private ZonedDateTime updated;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public ZonedDateTime getUpdated() {
			return updated;
		}

		public void setUpdated(ZonedDateTime updated) {
			this.updated = updated;
		}
	}

	public static class ZonedDateTimeConverter implements AttributeConverter<ZonedDateTime, String> {

		public static final ZoneId BERLIN = ZoneId.of( "Europe/Berlin");

		@Override
		public String convertToDatabaseColumn(ZonedDateTime localDateTime) {

			if (localDateTime == null) {

				return null;
			}

			String bla = localDateTime.withZoneSameInstant(ZonedDateTimeConverter.BERLIN)
					.format( DateTimeFormatter.ISO_OFFSET_DATE_TIME);
			return bla;
		}

		@Override
		public ZonedDateTime convertToEntityAttribute(String databaseString) {

			if (databaseString == null) {

				return null;
			}

			return ZonedDateTime.parse(databaseString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
					.withZoneSameInstant(ZonedDateTimeConverter.BERLIN);
		}
	}
}
