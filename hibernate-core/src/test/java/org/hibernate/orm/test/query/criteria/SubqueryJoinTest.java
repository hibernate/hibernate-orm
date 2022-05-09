package org.hibernate.orm.test.query.criteria;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.validation.constraints.NotNull;

@Jpa(
		annotatedClasses = {
				SubqueryJoinTest.TestContext.class,
				SubqueryJoinTest.TestUser.class,
		}
)
public class SubqueryJoinTest {

	@Test
	@TestForIssue(jiraKey = "HHH-15260")
	public void subqueryJoinTest(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					final CriteriaBuilder cb = entityManager.getCriteriaBuilder();

					final CriteriaQuery<TestContext> c = cb.createQuery( TestContext.class );
					final Root<TestContext> from = c.from( TestContext.class );
					from.join( "user" );

					final Subquery<TestUser.TestMarker> subQuery = c.subquery( TestUser.TestMarker.class );
					final Root<TestUser> sRoot = subQuery.from( TestUser.class );
					final Join<TestUser, TestUser.TestMarker> join = sRoot.join( "markers" );
					subQuery.where( cb.equal( sRoot.get( "id" ), from.get( "user" ).get( "id" ) ) );
					subQuery.select( join );
					c.where( cb.exists( subQuery ).not() );

					entityManager.createQuery( c ).getResultList();
				}
		);
	}

	@Entity
	public static class TestContext {

		@Id
		private Integer id;
		
		@NotNull
		@OneToOne(optional = false)
		private TestUser user;
		
	}

	@Entity
	public static class TestUser {

		@Id
		private Integer id;
		
		@ElementCollection
		@Enumerated(EnumType.STRING)
		private List<TestMarker> markers = new ArrayList<>();

		public enum TestMarker {
			TEST
		}
	}

}
