/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.timezones;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.type.descriptor.DateTimeUtils;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = TemporalRoundingTest.Zoned.class)
@SessionFactory
@ServiceRegistry(settings = @Setting(name = AvailableSettings.TIMEZONE_DEFAULT_STORAGE, value = "NORMALIZE"))
@SkipForDialect(dialectClass =  SybaseDialect.class, matchSubTypes = true)
public class TemporalRoundingTest {

	@Test void test(SessionFactoryScope scope) {
		final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		final ZonedDateTime nowZoned = ZonedDateTime.of( 2026, 5, 19, 17, 5, 33, 87779496, ZoneId.of("CET") );
		final OffsetDateTime nowOffset = OffsetDateTime.of( 2026, 5, 19, 17, 5, 33, 83231091, ZoneOffset.ofHours(3) );
		long id = scope.fromTransaction( s-> {
			Zoned z = new Zoned();
			z.zonedDateTime = nowZoned;
			z.offsetDateTime = nowOffset;
			s.persist(z);
			return z.id;
		});
		scope.inSession( s-> {
			Zoned z = s.find(Zoned.class, id);
			ZoneId systemZone = ZoneId.systemDefault();
			ZoneOffset systemOffset = systemZone.getRules().getOffset( Instant.now() );
			Instant expected = DateTimeUtils.adjustToPrecision( nowZoned.toInstant(), 6, dialect );
			Instant actual = DateTimeUtils.adjustToPrecision( z.zonedDateTime.toInstant(), 6, dialect );
			assertEquals(
					expected,
					actual
			);
			expected = DateTimeUtils.adjustToPrecision( nowOffset.toInstant(), 6, dialect );
			actual = DateTimeUtils.adjustToPrecision( z.offsetDateTime.toInstant(), 6, dialect );
			assertEquals(
					expected,
					actual
			);
			assertEquals( systemZone, z.zonedDateTime.getZone() );
			assertEquals( systemOffset, z.offsetDateTime.getOffset() );
		});
	}

	@Entity(name = "Zoned")
	public static class Zoned {
		@Id
		@GeneratedValue Long id;
		@Column(secondPrecision = 6)
		ZonedDateTime zonedDateTime;
		@Column(secondPrecision = 6)
		OffsetDateTime offsetDateTime;
	}
}
