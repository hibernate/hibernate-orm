/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Query;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test entities with a non-identifier property named 'id' with an EmbeddedId using
 * the constructor new syntax.
 *
 * @author Chris Cranford
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = SelectNewEmbeddedIdTest.Simple.class)
@SessionFactory
public class SelectNewEmbeddedIdTest {
	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	private void assertQueryRowCount(
			String queryString,
			@SuppressWarnings("SameParameterValue") int rowCount,
			SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.persist( new Simple( new SimpleId( 1, 1 ), 1 ) );
			session.flush();

			//noinspection deprecation
			Query query = session.createQuery( queryString );
			assertEquals( rowCount, query.getResultList().size() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-4712")
	public void testSelectNewListEntity(SessionFactoryScope factoryScope) {
		assertQueryRowCount( "select new list(e) FROM Simple e", 1, factoryScope );
	}

	@Test
	@JiraKey(value = "HHH-4712")
	public void testSelectNewListEmbeddedIdValue(SessionFactoryScope factoryScope) {
		assertQueryRowCount( "select new list(e.simpleId) FROM Simple e", 1, factoryScope );
	}

	@Test
	@JiraKey(value = "HHH-4712")
	public void testSelectNewMapEntity(SessionFactoryScope factoryScope) {
		assertQueryRowCount( "select new map(e.id, e) FROM Simple e", 1, factoryScope );
	}

	@Test
	@JiraKey(value = "HHH-4712")
	public void testSelectNewMapEmbeddedIdValue(SessionFactoryScope factoryScope) {
		assertQueryRowCount( "select new map(e.simpleId, e.simpleId) FROM Simple e", 1,  factoryScope );
	}

	@Test
	@JiraKey(value = "HHH-4712")
	public void testSelectNewObjectEntity(SessionFactoryScope factoryScope) {
		assertQueryRowCount( "select new " + Wrapper.class.getName() + "(e) FROM Simple e", 1, factoryScope );
	}

	@Test
	@JiraKey(value = "HHH-4712")
	public void testSelectNewObjectEmbeddedIdValue(SessionFactoryScope factoryScope) {
		assertQueryRowCount( "select new " + Wrapper.class.getName() + "(e.simpleId) FROM Simple e", 1, factoryScope );
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
