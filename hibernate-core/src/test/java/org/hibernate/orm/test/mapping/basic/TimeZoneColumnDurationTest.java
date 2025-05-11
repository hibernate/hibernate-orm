/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(annotatedClasses = TimeZoneColumnDurationTest.EntityWithDateTimes.class)
class TimeZoneColumnDurationTest {
	@Test void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityWithDateTimes entityWithDateTimes = new EntityWithDateTimes();
			entityWithDateTimes.dateTime1 = OffsetDateTime.of(2000,2, 1, 3, 15, 0, 0, ZoneOffset.UTC );
			entityWithDateTimes.dateTime2 = OffsetDateTime.of(2000,2, 1, 3, 45, 0, 0, ZoneOffset.ofHours( 5 ) );
			session.persist( entityWithDateTimes );

			assertEquals( Duration.ofHours( 4 ).plus( Duration.ofMinutes( 30 ) ),
					session.createQuery( "select dateTime1 - dateTime2 from EntityWithDateTimes", Duration.class ).getSingleResult() );
			assertEquals(270, (long) session.createQuery( "select (dateTime1 - dateTime2) by minute from EntityWithDateTimes", long.class ).getSingleResult() );
			assertEquals( OffsetDateTime.of(2000,2, 1, 4, 15, 0, 0, ZoneOffset.UTC ),
					session.createQuery( "select dateTime1 + 1 hour from EntityWithDateTimes", OffsetDateTime.class ).getSingleResult() );
			assertEquals( OffsetDateTime.of(2000,2, 1, 4, 0, 0, 0, ZoneOffset.ofHours( 5 ) ),
					session.createQuery( "select dateTime2 + 15 minute from EntityWithDateTimes", OffsetDateTime.class ).getSingleResult() );
			assertEquals( entityWithDateTimes.dateTime1, session.createQuery( "select dateTime1 from EntityWithDateTimes" ).getSingleResult() );
			assertEquals( entityWithDateTimes.dateTime2, session.createQuery( "select dateTime2 from EntityWithDateTimes" ).getSingleResult() );
		} );
	}

	@Entity(name = "EntityWithDateTimes")
	static class EntityWithDateTimes {
		@Id long id;

		@TimeZoneStorage(TimeZoneStorageType.COLUMN)
		@TimeZoneColumn(name = "offset1")
		@Column(name = "datetime1")
		OffsetDateTime dateTime1;

		@TimeZoneStorage(TimeZoneStorageType.COLUMN)
		@TimeZoneColumn(name = "offset2")
		@Column(name = "datetime2")
		OffsetDateTime dateTime2;
	}
}
