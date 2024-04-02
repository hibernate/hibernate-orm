package org.hibernate.orm.test.timezones;

import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.TimeZone;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.type.descriptor.DateTimeUtils;

import org.hibernate.testing.jdbc.SharedDriverManagerConnectionProviderImpl;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static java.sql.Types.TIMESTAMP;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = UTCNormalizedInstantTest.Zoned.class)
@SessionFactory
public class UTCNormalizedInstantTest {

	@Test void test(SessionFactoryScope scope) {
		final Instant instant;
		if ( scope.getSessionFactory().getJdbcServices().getDialect() instanceof SybaseDialect ) {
			// Sybase has 1/300th sec precision
			instant = Instant.now().with( ChronoField.NANO_OF_SECOND, 0L );
		}
		else {
			instant = Instant.now();
		}
		long id = scope.fromTransaction( s-> {
			final Zoned z = new Zoned();
			z.utcInstant = instant;
			z.localInstant = instant;
			s.persist(z);
			return z.id;
		});
		scope.inSession( s-> {
			final Zoned z = s.find(Zoned.class, id);
			final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
			assertEquals(
					DateTimeUtils.adjustToDefaultPrecision( z.utcInstant, dialect ),
					DateTimeUtils.adjustToDefaultPrecision( instant, dialect )
			);
			assertEquals(
					DateTimeUtils.adjustToDefaultPrecision( z.localInstant, dialect ),
					DateTimeUtils.adjustToDefaultPrecision( instant, dialect )
			);
		});
	}

	@Test void testWithSystemTimeZone(SessionFactoryScope scope) {
		final TimeZone timeZoneBefore = TimeZone.getDefault();
		TimeZone.setDefault( TimeZone.getTimeZone( "CET" ) );
		SharedDriverManagerConnectionProviderImpl.getInstance().onDefaultTimeZoneChange();
		try {
			final Instant instant;
			if ( scope.getSessionFactory().getJdbcServices().getDialect() instanceof SybaseDialect ) {
				// Sybase has 1/300th sec precision
				instant = Instant.now().with( ChronoField.NANO_OF_SECOND, 0L );
			}
			else {
				instant = Instant.now();
			}
			long id = scope.fromTransaction( s-> {
				final Zoned z = new Zoned();
				z.utcInstant = instant;
				z.localInstant = instant;
				s.persist(z);
				return z.id;
			});
			scope.inSession( s-> {
				final Zoned z = s.find(Zoned.class, id);
				final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
				Instant expected = DateTimeUtils.adjustToDefaultPrecision( z.utcInstant, dialect );
			Instant actual = DateTimeUtils.adjustToDefaultPrecision( instant, dialect );
				assertEquals(
						expected,
					actual
			);
			expected = DateTimeUtils.adjustToDefaultPrecision( z.localInstant, dialect );
			assertEquals(
					expected,
					actual
				);
			});
		}
		finally {
			TimeZone.setDefault( timeZoneBefore );
			SharedDriverManagerConnectionProviderImpl.getInstance().onDefaultTimeZoneChange();
		}
	}

	@Entity(name = "Zoned")
	public static class Zoned {
		@Id
		@GeneratedValue Long id;
		Instant utcInstant;
		@JdbcTypeCode(TIMESTAMP)
		Instant localInstant;
	}
}
