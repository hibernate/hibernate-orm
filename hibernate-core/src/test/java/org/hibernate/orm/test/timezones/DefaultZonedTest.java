/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.timezones;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.TimeZoneSupport;
import org.hibernate.type.descriptor.DateTimeUtils;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = DefaultZonedTest.Zoned.class)
@SessionFactory
public class DefaultZonedTest {

	@Test void test(SessionFactoryScope scope) {
		final ZonedDateTime nowZoned;
		final OffsetDateTime nowOffset;
		final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		if ( dialect instanceof SybaseDialect ) {
			// Sybase has 1/300th sec precision
			nowZoned = ZonedDateTime.now().withZoneSameInstant( ZoneId.of("CET") )
					.with( ChronoField.NANO_OF_SECOND, 0L );
			nowOffset = OffsetDateTime.now().withOffsetSameInstant( ZoneOffset.ofHours(3) )
					.with( ChronoField.NANO_OF_SECOND, 0L );
		}
		else if ( dialect.getDefaultTimestampPrecision() == 6 ) {
			nowZoned = ZonedDateTime.now().withZoneSameInstant( ZoneId.of("CET") ).truncatedTo( ChronoUnit.MICROS );
			nowOffset = OffsetDateTime.now().withOffsetSameInstant( ZoneOffset.ofHours(3) ).truncatedTo( ChronoUnit.MICROS );
		}
		else {
			nowZoned = ZonedDateTime.now().withZoneSameInstant( ZoneId.of("CET") );
			nowOffset = OffsetDateTime.now().withOffsetSameInstant( ZoneOffset.ofHours(3) );
		}
		long id = scope.fromTransaction( s-> {
			Zoned z = new Zoned();
			z.zonedDateTime = nowZoned;
			z.offsetDateTime = nowOffset;
			s.persist(z);
			return z.id;
		});
		scope.inSession( s -> {
			Zoned z = s.find(Zoned.class, id);
			Instant expected = DateTimeUtils.adjustToDefaultPrecision( nowZoned.toInstant(), dialect );
			Instant actual = DateTimeUtils.adjustToDefaultPrecision( z.zonedDateTime.toInstant(), dialect );
			assertEquals(
					expected,
					actual
			);
			expected = DateTimeUtils.adjustToDefaultPrecision( nowOffset.toInstant(), dialect );
			actual = DateTimeUtils.adjustToDefaultPrecision( z.offsetDateTime.toInstant(), dialect );
			assertEquals(
					expected,
					actual
			);
			if ( dialect.getTimeZoneSupport() == TimeZoneSupport.NATIVE ) {
				assertEquals( nowZoned.toOffsetDateTime().getOffset(), z.zonedDateTime.toOffsetDateTime().getOffset() );
				assertEquals( nowOffset.getOffset(), z.offsetDateTime.getOffset() );
			}
			else {
				assertEquals( ZoneId.of("Z"), z.zonedDateTime.getZone() );
				assertEquals( ZoneOffset.ofHours(0), z.offsetDateTime.getOffset() );
			}
		});
	}

	@Entity
	public static class Zoned {
		@Id
		@GeneratedValue Long id;
		ZonedDateTime zonedDateTime;
		OffsetDateTime offsetDateTime;
	}
}
