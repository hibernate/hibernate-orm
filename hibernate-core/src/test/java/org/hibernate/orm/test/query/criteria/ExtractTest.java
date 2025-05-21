/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria;

import jakarta.persistence.criteria.LocalDateField;
import jakarta.persistence.criteria.LocalDateTimeField;
import jakarta.persistence.criteria.LocalTimeField;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@Jpa()
class ExtractTest {
	@Test void testLocalDate(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			var builder = entityManager.getCriteriaBuilder();
			var query = builder.createQuery( Object[].class );
			query.select( builder.array(
					builder.extract( LocalDateField.YEAR, builder.localDate() ),
					builder.extract( LocalDateField.MONTH, builder.localDate() ),
					builder.extract( LocalDateField.DAY, builder.localDate() )
			) );
			var result = entityManager.createQuery( query ).getSingleResult();
			LocalDate now = LocalDate.now();
			assertEquals( now.getYear(), result[0] );
			assertEquals( now.getMonth().getValue(), result[1] );
			assertEquals( now.getDayOfMonth(), result[2] );
		} );
	}

	@Test void testLocalTime(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			var builder = entityManager.getCriteriaBuilder();
			var query = builder.createQuery( Object[].class );
			final LocalTime localTime = LocalTime.of( 3, 30, 11 );
			query.select( builder.array(
					builder.extract( LocalTimeField.HOUR, builder.literal( localTime ) ),
					builder.extract( LocalTimeField.MINUTE, builder.literal( localTime ) ),
					builder.extract( LocalTimeField.SECOND, builder.literal( localTime ) )
			) );
			var result = entityManager.createQuery( query ).getSingleResult();
			assertEquals( localTime.getHour(), result[0] );
			assertEquals( localTime.getMinute(), result[1] );
			assertEquals( localTime.getSecond(), ((Double) result[2]).intValue() );
		} );
	}

	@Test void testLocalDateTime(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			var builder = entityManager.getCriteriaBuilder();
			var query = builder.createQuery( Object[].class );
			final LocalDateTime localDateTime =
					LocalDateTime.of(2020, 10, 10, 3, 30, 11 );
			query.select( builder.array(
					builder.extract( LocalDateTimeField.YEAR, builder.literal( localDateTime ) ),
					builder.extract( LocalDateTimeField.MONTH, builder.literal( localDateTime ) ),
					builder.extract( LocalDateTimeField.DAY, builder.literal( localDateTime ) ),
					builder.extract( LocalDateTimeField.HOUR, builder.literal( localDateTime ) ),
					builder.extract( LocalDateTimeField.MINUTE, builder.literal( localDateTime ) ),
					builder.extract( LocalDateTimeField.SECOND, builder.literal( localDateTime ) ),
					builder.extract( LocalDateTimeField.QUARTER, builder.literal( localDateTime ) ),
					builder.extract( LocalDateTimeField.DATE, builder.literal( localDateTime ) ),
					builder.extract( LocalDateTimeField.TIME, builder.literal( localDateTime ) )
			) );
			var result = entityManager.createQuery( query ).getSingleResult();
			assertEquals( localDateTime.getYear(), result[0] );
			assertEquals( localDateTime.getMonth().getValue(), result[1] );
			assertEquals( localDateTime.getDayOfMonth(), result[2] );
			assertEquals( localDateTime.getHour(), result[3] );
			assertEquals( localDateTime.getMinute(), result[4] );
			assertEquals( localDateTime.getSecond(), ((Double) result[5]).intValue() );
			assertEquals( 4, result[6] );
			assertInstanceOf( LocalDate.class, result[7] );
			LocalDate date = (LocalDate) result[7];
			assertEquals( localDateTime.getYear(), date.getYear() );
			assertEquals( localDateTime.getMonth(), date.getMonth() );
			assertEquals( localDateTime.getDayOfMonth(), date.getDayOfMonth() );
			assertInstanceOf( LocalTime.class, result[8] );
			LocalTime time = (LocalTime) result[8];
			assertEquals( localDateTime.getHour(), time.getHour() );
			assertEquals( localDateTime.getMinute(), time.getMinute() );
			assertEquals( localDateTime.getSecond(), time.getSecond() );
		} );
	}
}
