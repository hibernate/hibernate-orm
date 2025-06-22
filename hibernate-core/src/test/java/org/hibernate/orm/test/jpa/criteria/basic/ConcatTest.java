/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.basic;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.io.Serializable;
import java.util.List;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.orm.junit.JiraKey;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-10843")
public class ConcatTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {TestEntity.class};
	}

	@Before
	public void setUp() {
		EntityManager entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();
		try {
			TestEntity testEntity = new TestEntity();
			testEntity.setName( "test_1" );
			entityManager.persist( testEntity );
			entityManager.getTransaction().commit();
		}
		catch (Exception e) {
			if ( entityManager.getTransaction().isActive() ) {
				entityManager.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			entityManager.close();
		}
	}

	@Test
	public void testSelectCaseWithConcat() throws Exception {
		EntityManager entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();
		try {
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
			entityManager.getTransaction().commit();
		}
		catch (Exception e) {
			if ( entityManager.getTransaction().isActive() ) {
				entityManager.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			entityManager.close();
		}
	}

	@Test
	public void testConcat() throws Exception {
		EntityManager entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();
		try {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery query = cb.createQuery();
			Root<TestEntity> testEntity = query.from( TestEntity.class );

			query.select( testEntity ).where( cb.equal( testEntity.get( "name" ), cb.concat( "test", cb.literal( "_1" ) ) ) );

			final List results = entityManager.createQuery( query ).getResultList();
			entityManager.getTransaction().commit();

			assertThat( results.size(), is( 1 ) );
		}
		catch (Exception e) {
			if ( entityManager.getTransaction().isActive() ) {
				entityManager.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			entityManager.close();
		}
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
