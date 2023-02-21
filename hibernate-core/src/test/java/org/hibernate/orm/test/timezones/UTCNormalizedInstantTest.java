package org.hibernate.orm.test.timezones;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.TimeZone;

import static java.sql.Types.TIMESTAMP;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = UTCNormalizedInstantTest.Zoned.class)
@SessionFactory
public class UTCNormalizedInstantTest {

	@SkipForDialect(dialectClass = SybaseDialect.class, matchSubTypes = true)
	@Test void test(SessionFactoryScope scope) {
		Instant instant = Instant.now();
		long id = scope.fromTransaction( s-> {
			Zoned z = new Zoned();
			z.utcInstant = instant;
			z.localInstant = instant;
			s.persist(z);
			return z.id;
		});
		scope.inSession( s-> {
			Zoned z = s.find(Zoned.class, id);
			assertEquals( instant, z.utcInstant );
			assertEquals( instant, z.localInstant );
		});
	}

	@SkipForDialect(dialectClass = SybaseDialect.class, matchSubTypes = true)
	@Test void testWithSystemTimeZone(SessionFactoryScope scope) {
		TimeZone.setDefault( TimeZone.getTimeZone("CET") );
		Instant instant = Instant.now();
		long id = scope.fromTransaction( s-> {
			Zoned z = new Zoned();
			z.utcInstant = instant;
			z.localInstant = instant;
			s.persist(z);
			return z.id;
		});
		scope.inSession( s-> {
			Zoned z = s.find(Zoned.class, id);
			assertEquals( instant, z.utcInstant );
			assertEquals( instant, z.localInstant );
		});
	}

	@Test void testSybase(SessionFactoryScope scope) {
		Instant instant = Instant.now().truncatedTo(SECONDS);
		long id = scope.fromTransaction( s-> {
			Zoned z = new Zoned();
			z.utcInstant = instant;
			z.localInstant = instant;
			s.persist(z);
			return z.id;
		});
		scope.inSession( s-> {
			Zoned z = s.find(Zoned.class, id);
			assertEquals( instant, z.utcInstant );
			assertEquals( instant, z.localInstant );
		});
	}

	@Test void testWithSystemTimeZoneSybase(SessionFactoryScope scope) {
		TimeZone.setDefault( TimeZone.getTimeZone("CET") );
		Instant instant = Instant.now().truncatedTo(SECONDS);
		long id = scope.fromTransaction( s-> {
			Zoned z = new Zoned();
			z.utcInstant = instant;
			z.localInstant = instant;
			s.persist(z);
			return z.id;
		});
		scope.inSession( s-> {
			Zoned z = s.find(Zoned.class, id);
			assertEquals( instant, z.utcInstant );
			assertEquals( instant, z.localInstant );
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
