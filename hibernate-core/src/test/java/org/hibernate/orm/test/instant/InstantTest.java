/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = InstantTest.Instants.class)
@SessionFactory
public class InstantTest {
	@Test
	public void testStorage(SessionFactoryScope scope) {
		final Instant now = Instant.now();
		final ZoneOffset zone = ZoneOffset.ofHours(1);

		scope.inTransaction( (session) -> {
			final Instants instants = new Instants();
			instants.instantInUtc = now;
			instants.instantInLocalTimeZone = now;
			instants.instantWithTimeZone = now;
			instants.localDateTime = LocalDateTime.ofInstant( now, zone );
			instants.offsetDateTime = OffsetDateTime.ofInstant( now, zone );
			instants.localDateTimeUtc = LocalDateTime.ofInstant( now, ZoneOffset.UTC );
			instants.offsetDateTimeUtc = OffsetDateTime.ofInstant( now, ZoneOffset.UTC );
			session.persist( instants );
		} );

		scope.inTransaction( (session) -> {
			final Instants instants = session.find( Instants.class, 0 );
			assertEqualInstants( now, instants.instantInUtc );
			assertEqualInstants( now, instants.instantInLocalTimeZone );
			assertEqualInstants( now, instants.instantWithTimeZone );
			assertEqualInstants( now, instants.offsetDateTime.toInstant() );
			assertEqualInstants( now, instants.localDateTime.toInstant( zone ) );
			assertEqualInstants( now, instants.offsetDateTimeUtc.toInstant() );
			assertEqualInstants( now, instants.localDateTimeUtc.toInstant( ZoneOffset.UTC ) );
		} );
	}

	@Test
	void testQueryRestriction(SessionFactoryScope scope) {
		final Instant instant = Instant.from( DateTimeFormatter.ISO_INSTANT.parse( "2025-02-27T01:01:01.123Z" ) );

		scope.inTransaction( (session) -> {
			session.persist( new Instants( 1, instant ) );
		} );

		// parameter
		scope.inTransaction( (session) -> {
			final String queryText = "select id from Instants where instantInUtc = :p";
			final List<Long> matches = session.createSelectionQuery( queryText, Long.class )
					.setParameter( "p", instant )
					.list();
			assertThat( matches ).hasSize( 1 );
		} );

		// literal
		scope.inTransaction( (session) -> {
			final String queryText = "select id from Instants where instantInUtc = zoned datetime 2025-02-27 01:01:01.123Z";
			final List<Long> matches = session.createSelectionQuery( queryText, Long.class )
					.list();
			assertThat( matches ).hasSize( 1 );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	void assertEqualInstants(Instant x, Instant y) {
		assertEquals( x.truncatedTo( ChronoUnit.SECONDS ), y.truncatedTo( ChronoUnit.SECONDS ) );
	}

	@Entity(name="Instants")
	static class Instants {
		@Id
		long id;
		Instant instantInUtc;
		@JdbcTypeCode(TIMESTAMP) Instant instantInLocalTimeZone;
		@JdbcTypeCode(TIMESTAMP_WITH_TIMEZONE) Instant instantWithTimeZone;
		LocalDateTime localDateTime;
		OffsetDateTime offsetDateTime;
		LocalDateTime localDateTimeUtc;
		OffsetDateTime offsetDateTimeUtc;

		public Instants() {
		}

		public Instants(long id, Instant instantInUtc) {
			this.id = id;
			this.instantInUtc = instantInUtc;
		}
	}
}
