/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import java.io.Serializable;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test entities with a non-identifier property named 'id' with an EmbeddedId using
 * the constructor new syntax.
 *
 * @author Chris Cranford
 */
public class SelectNewEmbeddedIdTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Simple.class };
	}

	private void assertQueryRowCount(String queryString, int rowCount) {
		EntityManager entityManager = getOrCreateEntityManager();
		try {
			// persist the data
			entityManager.getTransaction().begin();
			entityManager.persist( new Simple( new SimpleId( 1, 1 ), 1 ) );
			entityManager.getTransaction().commit();

			Query query = entityManager.createQuery( queryString );
			assertEquals( rowCount, query.getResultList().size() );
		}
		catch ( Exception e ) {
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
	@JiraKey(value = "HHH-4712")
	public void testSelectNewListEntity() {
		assertQueryRowCount( "select new list(e) FROM Simple e", 1 );
	}

	@Test
	@JiraKey(value = "HHH-4712")
	public void testSelectNewListEmbeddedIdValue() {
		assertQueryRowCount( "select new list(e.simpleId) FROM Simple e", 1 );
	}

	@Test
	@JiraKey(value = "HHH-4712")
	public void testSelectNewMapEntity() {
		assertQueryRowCount( "select new map(e.id, e) FROM Simple e", 1 );
	}

	@Test
	@JiraKey(value = "HHH-4712")
	public void testSelectNewMapEmbeddedIdValue() {
		assertQueryRowCount( "select new map(e.simpleId, e.simpleId) FROM Simple e", 1 );
	}

	@Test
	@JiraKey(value = "HHH-4712")
	public void testSelectNewObjectEntity() {
		assertQueryRowCount( "select new " + Wrapper.class.getName() + "(e) FROM Simple e", 1 );
	}

	@Test
	@JiraKey(value = "HHH-4712")
	public void testSelectNewObjectEmbeddedIdValue() {
		assertQueryRowCount( "select new " + Wrapper.class.getName() + "(e.simpleId) FROM Simple e", 1 );
	}

	@Entity(name = "Simple")
	public static class Simple {
		@EmbeddedId
		private SimpleId simpleId;
		private int id;

		public Simple() {

		}

		public Simple(SimpleId simpleId, int id) {
			this.simpleId = simpleId;
			this.id = id;
		}
	}

	@Embeddable
	public static class SimpleId implements Serializable {
		private int realId;
		private int otherId;

		public SimpleId() {
		}

		public SimpleId(int realId, int otherId) {
			this.realId = realId;
			this.otherId = otherId;
		}
	}

	public static class Wrapper {
		private Simple simple;

		public Wrapper() {

		}

		public Wrapper(Simple simple) {
			this.simple = simple;
		}

		public Wrapper(SimpleId simpleId) {

		}
	}
}
