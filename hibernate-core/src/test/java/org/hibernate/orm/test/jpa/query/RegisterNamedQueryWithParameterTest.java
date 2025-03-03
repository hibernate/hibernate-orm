/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Query;
import jakarta.persistence.Table;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Jpa(
		annotatedClasses = RegisterNamedQueryWithParameterTest.TestEntity.class
)
@JiraKey(value = "HHH-15653")
public class RegisterNamedQueryWithParameterTest {

	private static final String QUERY_NAME = "ENTITY_BY_NAME";
	private static final String QUERY = "select t.id from TEST_ENTITY t where t.anInteger = :value";

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Query query = entityManager.createNativeQuery( QUERY );
					scope.getEntityManagerFactory().addNamedQuery( "ENTITY_BY_NAME", query );

					TestEntity entity = new TestEntity( 1L, "And", 1 );
					TestEntity entity2 = new TestEntity( 2L, "Fab", 2 );
					entityManager.persist( entity );
					entityManager.persist( entity2 );
				}
		);
	}

	@AfterAll
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> entityManager.createQuery( "delete from TestEntity" ).executeUpdate()
		);
	}

	@Test
	public void testExecuteNativeQuery(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Query query = entityManager.createNamedQuery( QUERY_NAME );
					query.setParameter( "value", 1 );
					List results = query.getResultList();
					assertThat( results.size() ).isEqualTo( 1 );
				}
		);
	}

	@Entity(name = "TestEntity")
	@Table(name = "TEST_ENTITY")
	public static class TestEntity {
		@Id
		Long id;

		String name;

		Integer anInteger;

		public TestEntity() {
		}

		public TestEntity(Long id, String name, Integer anInteger) {
			this.id = id;
			this.name = name;
			this.anInteger = anInteger;
		}
	}
}
