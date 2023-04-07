package org.hibernate.orm.test.timezones;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.type.descriptor.DateTimeUtils;

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
		final Instant instant = Instant.now();
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
			if ( dialect instanceof SybaseDialect) {
				// Sybase with jTDS driver has 1/300th sec precision
				assertEquals( z.utcInstant.truncatedTo(ChronoUnit.SECONDS), instant.truncatedTo(ChronoUnit.SECONDS) );
				assertEquals( z.localInstant.truncatedTo(ChronoUnit.SECONDS), instant.truncatedTo(ChronoUnit.SECONDS) );
			}
			else {
				assertEquals(
						DateTimeUtils.roundToDefaultPrecision( z.utcInstant, dialect ),
						DateTimeUtils.roundToDefaultPrecision( instant, dialect )
				);
				assertEquals(
						DateTimeUtils.roundToDefaultPrecision( z.localInstant, dialect ),
						DateTimeUtils.roundToDefaultPrecision( instant, dialect )
				);
			}
		});
	}

	@Test void testWithSystemTimeZone(SessionFactoryScope scope) {
		TimeZone.setDefault( TimeZone.getTimeZone("CET") );
		final Instant instant = Instant.now();
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
			if ( dialect instanceof SybaseDialect) {
				// Sybase with jTDS driver has 1/300th sec precision
				assertEquals( z.utcInstant.truncatedTo(ChronoUnit.SECONDS), instant.truncatedTo(ChronoUnit.SECONDS) );
				assertEquals( z.localInstant.truncatedTo(ChronoUnit.SECONDS), instant.truncatedTo(ChronoUnit.SECONDS) );
			}
			else {
				assertEquals(
						DateTimeUtils.roundToDefaultPrecision( z.utcInstant, dialect ),
						DateTimeUtils.roundToDefaultPrecision( instant, dialect )
				);
				assertEquals(
						DateTimeUtils.roundToDefaultPrecision( z.localInstant, dialect ),
						DateTimeUtils.roundToDefaultPrecision( instant, dialect )
				);
			}
		});
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
