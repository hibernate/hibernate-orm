/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import java.io.Serializable;

import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test entities with a non-identifier property named 'id' with an EmbeddedId using
 * the constructor new syntax.
 *
 * @author Chris Cranford
 */
public class SelectNewEmbeddedIdTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Simple.class };
	}

	private void assertQueryRowCount(String queryString, int rowCount) {
		Session session = openSession();
		try {
			// persist the data
			session.getTransaction().begin();
			session.persist( new Simple( new SimpleId( 1, 1 ), 1 ) );

			session.flush();
			session.clear();

			Query query = session.createQuery( queryString );
			assertEquals( rowCount, query.list().size() );
			session.getTransaction().rollback();
		}
		catch ( Exception e ) {
			if ( session.getTransaction().getStatus() == TransactionStatus.ACTIVE ) {
				session.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			session.close();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-4712")
	public void testSelectNewListEntity() {
		assertQueryRowCount( "select new list(e) FROM Simple e", 1 );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-4712")
	public void testSelectNewListEmbeddedIdValue() {
		assertQueryRowCount( "select new list(e.simpleId) FROM Simple e", 1 );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-4712")
	public void testSelectNewMapEntity() {
		assertQueryRowCount( "select new map(e.id, e) FROM Simple e", 1 );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-4712")
	public void testSelectNewMapEmbeddedIdValue() {
		assertQueryRowCount( "select new map(e.simpleId, e.simpleId) FROM Simple e", 1 );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-4712")
	public void testSelectNewObjectEntity() {
		assertQueryRowCount( "select new " + Wrapper.class.getName() + "(e) FROM Simple e", 1 );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-4712")
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
