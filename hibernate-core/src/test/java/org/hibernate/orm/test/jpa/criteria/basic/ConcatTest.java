/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.basic;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.io.Serializable;
import java.util.List;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.hibernate.testing.orm.junit.JiraKey;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-10843")
@Jpa(annotatedClasses = {ConcatTest.TestEntity.class})
public class ConcatTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			TestEntity testEntity = new TestEntity();
			testEntity.setName( "test_1" );
			entityManager.persist( testEntity );
		} );
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testSelectCaseWithConcat(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Object[]> query = cb.createQuery( Object[].class );
			Root<TestEntity> testEntity = query.from( TestEntity.class );

			query.multiselect(
					cb.selectCase()
							.when( cb.isNotNull( testEntity.get( "id" ) ), cb.concat( "test", cb.literal( "_1" ) ) )
							.otherwise( cb.literal( "Empty" ) ),
					cb.trim( cb.concat( ".", cb.literal( "Test   " ) ) )
			);

			final List<Object[]> results = entityManager.createQuery( query ).getResultList();
			assertThat( results.size(), is( 1 ) );
			assertThat( results.get( 0 )[0], is( "test_1" ) );
			assertThat( results.get( 0 )[1], is( ".Test" ) );
		} );
	}

	@Test
	public void testConcat(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery query = cb.createQuery();
			Root<TestEntity> testEntity = query.from( TestEntity.class );

			query.select( testEntity ).where( cb.equal( testEntity.get( "name" ), cb.concat( "test", cb.literal( "_1" ) ) ) );

			final List<?> results = entityManager.createQuery( query ).getResultList();

			assertThat( results.size(), is( 1 ) );
		} );
	}

	@Entity(name = "TestEntity")
	@Table(name = "TEST_ENTITY")
	public static class TestEntity implements Serializable {
		@Id
		@GeneratedValue
		private Integer id;
		private String name;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
