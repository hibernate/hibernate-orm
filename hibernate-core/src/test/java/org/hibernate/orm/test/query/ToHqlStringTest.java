package org.hibernate.orm.test.query;

import org.hibernate.query.spi.SqmQuery;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;

@Jpa(
		annotatedClasses = ToHqlStringTest.TestEntity.class
)
@TestForIssue( jiraKey = "HHH-15389")
public class ToHqlStringTest {

	@Test
	public void testCriteriaCountDistinctToHqlString(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder builder = entityManager.getCriteriaBuilder();
					CriteriaQuery<Long> criteriaQuery = builder.createQuery( Long.class );

					Root<TestEntity> root = criteriaQuery.from( TestEntity.class );
					Expression<Long> countDistinct = builder.countDistinct( root );
					criteriaQuery = criteriaQuery.select( countDistinct );

					TypedQuery<Long> query = entityManager.createQuery( criteriaQuery );
					( (SqmQuery) query ).getSqmStatement().toHqlString();
				}
		);
	}

	@Test
	public void testHqlCountDistinctToHqlString(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Query query = entityManager.createQuery( "select count (distinct t) from TestEntity t" );
					( (SqmQuery) query ).getSqmStatement().toHqlString();
				}
		);
	}

	@Test
	public void testDynamicInstantiationToHqlString(EntityManagerFactoryScope scope){
		scope.inTransaction(
				entityManager -> {
					Query query = entityManager.createQuery( "select new org.hibernate.orm.test.query.ToHqlStringTest$TestDto("
					+ " t.id, t.name ) from TestEntity t" );
					( (SqmQuery) query ).getSqmStatement().toHqlString();
				}
		);
	}

	public static class TestDto {
		@Id
		public Integer id;

		public String name;
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		public Integer id;

		public String name;
	}
}
