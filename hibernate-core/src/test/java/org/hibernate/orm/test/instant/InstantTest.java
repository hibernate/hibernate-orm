/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(annotatedClasses = InstantTest.Instants.class)
public class InstantTest {
	@Test
	public void test(EntityManagerFactoryScope scope) {
		Instant now = Instant.now();
		ZoneOffset zone = ZoneOffset.ofHours(1);
		scope.getEntityManagerFactory()
				.runInTransaction( entityManager -> {
					Instants instants = new Instants();
					instants.instantInUtc = now;
					instants.instantInLocalTimeZone = now;
					instants.instantWithTimeZone = now;
					instants.localDateTime = LocalDateTime.ofInstant( now, zone );
					instants.offsetDateTime = OffsetDateTime.ofInstant( now, zone );
					instants.localDateTimeUtc = LocalDateTime.ofInstant( now, ZoneOffset.UTC );
					instants.offsetDateTimeUtc = OffsetDateTime.ofInstant( now, ZoneOffset.UTC );
					entityManager.persist( instants );
				} );
		scope.getEntityManagerFactory()
				.runInTransaction( entityManager -> {
					Instants instants = entityManager.find( Instants.class, 0 );
					assertEqualInstants( now, instants.instantInUtc );
					assertEqualInstants( now, instants.instantInLocalTimeZone );
					assertEqualInstants( now, instants.instantWithTimeZone );
					assertEqualInstants( now, instants.offsetDateTime.toInstant() );
					assertEqualInstants( now, instants.localDateTime.toInstant( zone ) );
					assertEqualInstants( now, instants.offsetDateTimeUtc.toInstant() );
					assertEqualInstants( now, instants.localDateTimeUtc.toInstant( ZoneOffset.UTC ) );
				} );
	}
	void assertEqualInstants(Instant x, Instant y) {
		assertEquals( x.truncatedTo( ChronoUnit.SECONDS ), y.truncatedTo( ChronoUnit.SECONDS ) );
	}
	@Entity(name="Instants")
	static class Instants {
		@Id long id;
		Instant instantInUtc;
		@JdbcTypeCode(TIMESTAMP) Instant instantInLocalTimeZone;
		@JdbcTypeCode(TIMESTAMP_WITH_TIMEZONE) Instant instantWithTimeZone;
		LocalDateTime localDateTime;
		OffsetDateTime offsetDateTime;
		LocalDateTime localDateTimeUtc;
		OffsetDateTime offsetDateTimeUtc;
	}
}
