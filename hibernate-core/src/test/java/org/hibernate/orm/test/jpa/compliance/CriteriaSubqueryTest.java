/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance;

import java.util.List;

import org.hibernate.dialect.MySQLDialect;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.EntityType;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

@Jpa(
		annotatedClasses = { CriteriaSubqueryTest.TestEntity.class }
)
public class CriteriaSubqueryTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					TestEntity testEntity = new TestEntity( 1, "1", 10 );
					TestEntity testEntity2 = new TestEntity( 2, "2", 17 );
					TestEntity testEntity3 = new TestEntity( 3, "3", 22 );
					TestEntity testEntity4 = new TestEntity( 4, "4", 38 );
					entityManager.persist( testEntity );
					entityManager.persist( testEntity2 );
					entityManager.persist( testEntity3 );
					entityManager.persist( testEntity4 );
				}
		);
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void existsInSubqueryTest(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final Integer expectedId = 2;
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

					final CriteriaQuery<TestEntity> criteriaQuery = criteriaBuilder.createQuery( TestEntity.class );
					final Root<TestEntity> from = criteriaQuery.from( TestEntity.class );

					final EntityType<TestEntity> testEntityType = from.getModel();

					final Subquery<TestEntity> subquery = criteriaQuery.subquery( TestEntity.class );
					final Root<TestEntity> subqueryFrom = subquery.from( TestEntity.class );

					assertThat( subqueryFrom.getModel().getName(), is( TestEntity.class.getSimpleName() ) );

					subquery.where( criteriaBuilder.equal(
							from.get( testEntityType.getSingularAttribute( "id", Integer.class ) ),
							expectedId
					) ).select( subqueryFrom );

					criteriaQuery.where( criteriaBuilder.exists( subquery ) );

					criteriaQuery.select( from );

					final List<TestEntity> testEntities = entityManager.createQuery( criteriaQuery ).getResultList();

					assertThat( testEntities.size(), is( 1 ) );
					assertThat( testEntities.get( 0 ).getId(), is( expectedId ) );
				}
		);
	}

	@Test
	public void subqueryCiteriaSelectTest(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final Integer expectedId = 2;
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

					final CriteriaQuery<TestEntity> criteriaQuery = criteriaBuilder.createQuery( TestEntity.class );
					final Root<TestEntity> from = criteriaQuery.from( TestEntity.class );

					final EntityType<TestEntity> testEntityType = from.getModel();

					final Subquery<TestEntity> subquery = criteriaQuery.subquery( TestEntity.class );
					final Root<TestEntity> subqueryFrom = subquery.from( testEntityType );

					assertThat( subqueryFrom.getModel().getName(), is( testEntityType.getName() ) );

					subquery.where( criteriaBuilder.equal(
							from.get( testEntityType.getSingularAttribute( "id", Integer.class ) ),
							expectedId
					) ).select( subqueryFrom );

					criteriaQuery.where( criteriaBuilder.exists( subquery ) );

					criteriaQuery.select( from );

					final List<TestEntity> testEntities = entityManager.createQuery( criteriaQuery ).getResultList();
					assertThat( testEntities.size(), is( 1 ) );
					assertThat( testEntities.get( 0 ).getId(), is( expectedId ) );
				}
		);
	}

	@Test
	@SkipForDialect( dialectClass = MySQLDialect.class, matchSubTypes = true, reason = "does not support specifying in the subquery from clause the same table used in the delete/update ")
	public void subqueryCriteriaDeleteTest(EntityManagerFactoryScope scope) {

		scope.inTransaction(
				entityManager -> {
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

					final CriteriaDelete<TestEntity> criteriaDelete = criteriaBuilder.createCriteriaDelete( TestEntity.class );
					final Root<TestEntity> from = criteriaDelete.from( TestEntity.class );

					final EntityType<TestEntity> testEntityType = from.getModel();

					final Subquery<TestEntity> subquery = criteriaDelete.subquery( TestEntity.class );
					final Root<TestEntity> subqueryFrom = subquery.from( TestEntity.class );

					subquery.where( criteriaBuilder.equal(
									from.get( testEntityType.getSingularAttribute( "id", Integer.class ) ), 2 ) )
							.select( subqueryFrom );

					criteriaDelete.where( criteriaBuilder.exists( subquery ) );

					final int entityDeleted = entityManager.createQuery( criteriaDelete ).executeUpdate();
					assertThat( entityDeleted, is( 1 ) );
				}
		);

		scope.inTransaction(
				entityManager -> {
					final TestEntity testEntity = entityManager.find( TestEntity.class, 2 );
					assertNull( testEntity );
				}
		);

	}

	@Test
	@SkipForDialect( dialectClass = MySQLDialect.class, matchSubTypes = true, reason = "does not support specifying in the subquery from clause the same table used in the delete/update ")
	public void subqueryCriteriaUpdateTest(EntityManagerFactoryScope scope) {

		scope.inTransaction(
				entityManager -> {
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

					final CriteriaUpdate<TestEntity> criteriaUpdate = criteriaBuilder.createCriteriaUpdate( TestEntity.class );
					final Root<TestEntity> from = criteriaUpdate.from( TestEntity.class );
					final EntityType<TestEntity> testEntityType = from.getModel();

					criteriaUpdate.set(
							from.<Integer>get( "age" ),
							criteriaBuilder.sum(
									from.get( testEntityType.getSingularAttribute( "age", Integer.class ) ),
									13
							)
					);


					final Subquery<TestEntity> subquery = criteriaUpdate.subquery( TestEntity.class );
					final Root<TestEntity> subqueryFrom = subquery.from( TestEntity.class );

					subquery.where( criteriaBuilder.equal(
									from.get( testEntityType.getSingularAttribute( "id", Integer.class ) ), 2 ) )
							.select( subqueryFrom );

					criteriaUpdate.where( criteriaBuilder.exists( subquery ) );

					int entityUpdated = entityManager.createQuery( criteriaUpdate ).executeUpdate();
					assertThat( entityUpdated, is( 1 ) );
				}
		);

		scope.inTransaction(
				entityManager -> {
					TestEntity testEntity = entityManager.find( TestEntity.class, 2 );
					assertThat( testEntity.getAge(), is( 30 ) );
				}
		);
	}


	@Entity(name = "TestEntity")
	public static class TestEntity {

		@Id
		private Integer id;

		private String name;

		private Integer age;

		public TestEntity() {
		}

		public TestEntity(Integer id, String name, Integer age) {
			this.id = id;
			this.name = name;
			this.age = age;
		}

		public Integer getId() {
			return id;
		}

		public Integer getAge() {
			return age;
		}
	}
}
