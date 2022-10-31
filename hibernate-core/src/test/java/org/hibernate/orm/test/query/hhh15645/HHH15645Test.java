package org.hibernate.orm.test.query.hhh15645;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestForIssue(jiraKey = "HHH-15645")
@Jpa(
		annotatedClasses = { HHH15645Test.TestParent.class, HHH15645Test.TestChild.class}
)
public class HHH15645Test {

	@Test
	public void testInnerJoin(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final TestParent p = new TestParent();
			p.setId(1);
			entityManager.persist(p);

			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaQuery<TestChild> c = cb.createQuery(TestChild.class);
			final Root<TestChild> r = c.from(TestChild.class);
			final Join<TestChild, TestParent> g = r.join( "parent" );
			c.where(cb.and(
					cb.equal(r.get("parent"), p),
					cb.isFalse(g.get("deleted"))));

			entityManager.createQuery(c).getResultList();
		} );
	}

	@Entity
	public static class TestChild {

		@Id
		private Integer id;

		@ManyToOne(optional = false, fetch = FetchType.LAZY)
		private TestParent parent;

	}

	@Entity
	public static class TestParent {

		@Id
		private Integer id;

		private boolean deleted;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}
	
}
