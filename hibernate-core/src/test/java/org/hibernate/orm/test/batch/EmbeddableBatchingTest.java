package org.hibernate.orm.test.batch;

import org.hibernate.cfg.Environment;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

@Jpa(
		annotatedClasses = {
				EmbeddableBatchingTest.A.class,
				EmbeddableBatchingTest.C.class
		},
		integrationSettings = { @Setting(name = Environment.DEFAULT_BATCH_FETCH_SIZE, value = "2") }
)
@TestForIssue(jiraKey = "HHH-15644")
class EmbeddableBatchingTest {

	@Test
	void testSelect(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					final CriteriaQuery<A> c = cb.createQuery( A.class );
					c.from( A.class );
					entityManager.createQuery( c ).getResultList();
				}
		);
	}

	@Entity
	public static class A {

		@Id
		private Integer id;

		@Embedded
		private B b;

	}

	@Embeddable
	public static class B {

		@OneToOne
		private C c;

	}

	@Entity
	public static class C {

		@Id
		private Integer id;

	}

}
