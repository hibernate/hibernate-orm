/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import org.hibernate.query.sqm.spi.SqmStatementAccess;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

@Jpa(
		annotatedClasses = {
				ToHqlStringTest.TestEntity.class,
				ToHqlStringTest.TestEntitySub.class
		}
)
@JiraKey( value = "HHH-15389")
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
					query.unwrap( SqmStatementAccess.class ).getSqmStatement().toHqlString();
				}
		);
	}

	@Test
	public void testHqlCountDistinctToHqlString(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Query query = entityManager.createQuery( "select count (distinct t) from TestEntity t" );
					query.unwrap( SqmStatementAccess.class ).getSqmStatement().toHqlString();
				}
		);
	}

	@Test
	public void testDynamicInstantiationToHqlString(EntityManagerFactoryScope scope){
		scope.inTransaction(
				entityManager -> {
					Query query = entityManager.createQuery( "select new org.hibernate.orm.test.query.ToHqlStringTest$TestDto("
					+ " t.id, t.name ) from TestEntity t" );
					query.unwrap( SqmStatementAccess.class ).getSqmStatement().toHqlString();
				}
		);
	}

	@Test
	@JiraKey( "HHH-16676" )
	public void testCriteriaWithTreatToHqlString(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder builder = entityManager.getCriteriaBuilder();
					CriteriaQuery<Object> criteriaQuery = builder.createQuery( Object.class );

					Root<TestEntity> root = criteriaQuery.from( TestEntity.class );
					Join<TestEntitySub, Object> entity = builder.treat( root, TestEntitySub.class ).join( "entity" );
					criteriaQuery = criteriaQuery.select( entity );

					TypedQuery<Object> query = entityManager.createQuery( criteriaQuery );
					String hqlString = query.unwrap( SqmStatementAccess.class ).getSqmStatement().toHqlString();
					final int fromIndex = hqlString.indexOf( " from " );
					final String alias = hqlString.substring( "select ".length(), fromIndex );
					assertThat( hqlString.substring( fromIndex ), containsString( alias ) );
				}
		);
	}

	@Test
	@JiraKey("HHH-16526")
	public void testCriteriaWithFunctionTakingOneArgument(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder builder = entityManager.getCriteriaBuilder();
					CriteriaQuery<Object> criteriaQuery = builder.createQuery( Object.class );

					criteriaQuery.from( TestEntity.class );
					criteriaQuery.where(
							builder.equal( builder.lower( builder.literal( "Foo" ) ), builder.literal( "foo" ) )
					);

					TypedQuery<Object> query = entityManager.createQuery( criteriaQuery );
					String hqlString = query.unwrap( SqmStatementAccess.class ).getSqmStatement().toHqlString();
					assertThat( hqlString, containsString( "where lower('Foo') = 'foo'" ) );
				}
		);
	}

	@Test
	@JiraKey("HHH-16526")
	public void testCriteriaWithFunctionTakingTwoArguments(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder builder = entityManager.getCriteriaBuilder();
					CriteriaQuery<Object> criteriaQuery = builder.createQuery( Object.class );

					criteriaQuery.from( TestEntity.class );
					criteriaQuery.where( builder.isTrue(
							builder.function( "myFunction", Boolean.class, builder.literal( 0 ), builder.literal( 10 ) )
					) );

					TypedQuery<Object> query = entityManager.createQuery( criteriaQuery );
					String hqlString = query.unwrap( SqmStatementAccess.class ).getSqmStatement().toHqlString();
					assertThat( hqlString, containsString( "where myFunction(0, 10)" ) );
				}
		);
	}

	@Test
	@JiraKey("HHH-19075")
	public void testTrimWithThreeArgumentsToHqlString(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Query query = entityManager.createQuery(
							"select trim(trailing '_' from t.name) from TestEntity t"
					);
					String hqlString = query.unwrap( SqmStatementAccess.class ).getSqmStatement().toHqlString();
					assertThat( hqlString, containsString( "trim(TRAILING '_' from t.name)" ) );
				}
		);
	}

	public static class TestDto {
		@Id
		public Integer id;

		public String name;

		public TestDto(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		public Integer id;

		public String name;
	}

	@Entity(name = "TestEntitySub")
	public static class TestEntitySub extends TestEntity {
		@ManyToOne(fetch = FetchType.LAZY)
		TestEntity entity;
	}
}
