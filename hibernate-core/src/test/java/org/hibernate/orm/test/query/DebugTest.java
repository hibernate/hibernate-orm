package org.hibernate.orm.test.query;

import java.time.Duration;
import java.time.Instant;

import org.hibernate.exception.SQLGrammarException;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DomainModel(annotatedClasses = DebugTest.SimpleEntity.class)
@SessionFactory
@JiraKey("HHH-18201")
public class DebugTest {

	@Test
	@Disabled
	void literalVsTemporalPlusDuration(SessionFactoryScope scope) {
		scope.inSession( session -> {
			session.createQuery( "from SimpleEntity where instant > inst + 2 second", SimpleEntity.class )
					.getResultCount();
		} );
	}

	@Test
	@Disabled
	void literalPlusDurationVsTemporal(SessionFactoryScope scope) {
		scope.inSession( session -> {
			session.createQuery( "from SimpleEntity where instant + 2 second > inst", SimpleEntity.class )
					.getResultCount();
		} );
	}

	@Test
	@Disabled
	void temporalVsLiteralPlusDuration(SessionFactoryScope scope) {
		scope.inSession( session -> {
			session.createQuery( "from SimpleEntity where inst > instant + 2 second", SimpleEntity.class )
					.getResultCount();
		} );
	}

	@Test
	@Disabled
	void temporalPlusDurationVsLiteral(SessionFactoryScope scope) {
		scope.inSession( session -> {
			session.createQuery( "from SimpleEntity where inst + 2 second > instant", SimpleEntity.class )
					.getResultCount();
		} );
	}

	@Test
//	@Disabled
	void temporalParameterPlusDuration(SessionFactoryScope scope) {
		scope.inSession( session -> {
			session.createQuery( "from SimpleEntity where inst > :i + 1 second + 1 second", SimpleEntity.class )
					.setParameter( "i", Instant.now() )
					.getResultCount();
		} );
	}

	@Test
	@Disabled
	void temporalParameterPlusDurationNative(SessionFactoryScope scope) {
		scope.inSession( session -> {
			session.createNativeQuery(
					//"select count(*) from SimpleEntity se1_0 where se1_0.inst>{fn timestampadd(sql_tsi_second, bigint(1), :i)}"
					"select count(*) from SimpleEntity se1_0 where se1_0.inst>cast(? as timestamp) +interval '1 second'"
							, Long.class )
					.setParameter( "i", Instant.now() )
					.getResultCount();
		} );
	}

	@Test
	@Disabled
	void temporalEqualsTemporalParameter(SessionFactoryScope scope) {
		scope.inSession( session -> {
			session.createQuery( "from SimpleEntity where inst = :i", SimpleEntity.class )
					.setParameter( "i", Instant.now() )
					.getResultCount();
		} );
	}

	@Test
	@Disabled
	void temporalParameterWithoutAddedDuration(SessionFactoryScope scope) {
		scope.inSession( session -> {
			session.createQuery( "from SimpleEntity where inst > :i", SimpleEntity.class )
					.setParameter( "i", Instant.now() )
					.getResultCount();
		} );
	}

	@Test
	@Disabled
	void durationPlusDuration(SessionFactoryScope scope) {
		assertThrows( SQLGrammarException.class, () ->
				scope.inSession( session -> {
					session.createQuery( "from SimpleEntity where inst > :i + 1 second", SimpleEntity.class )
							.setParameter( "i", Duration.ofMinutes( 1 ) )
							.getResultCount();
				} ) );
	}

	@Entity(name = "SimpleEntity")
	public static class SimpleEntity {
		@Id
		Integer id;

		Instant inst;
	}
}
