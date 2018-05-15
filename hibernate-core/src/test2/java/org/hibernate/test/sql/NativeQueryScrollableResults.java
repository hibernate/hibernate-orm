/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.sql;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.query.Query;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-11033")
public class NativeQueryScrollableResults extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {MyEntity.class};
	}

	@Override
	protected void prepareTest() throws Exception {
		try (Session session = openSession()) {
			session.getTransaction().begin();
			try {
				session.save( new MyEntity( 1L, "entity_1", new BigInteger( "3" ) ) );
				session.save( new MyEntity( 2L, "entity_2", new BigInteger( "6" ) ) );
				session.getTransaction().commit();
			}
			catch (Exception e) {
				if ( session.getTransaction().isActive() ) {
					session.getTransaction().rollback();
				}
				throw e;
			}
		}
	}

	@Override
	protected void cleanupTest() throws Exception {
		try (Session session = openSession()) {
			session.getTransaction().begin();
			try {
				session.createQuery( "delete from MyEntity" ).executeUpdate();
				session.getTransaction().commit();
			}
			catch (Exception e) {
				if ( session.getTransaction().isActive() ) {
					session.getTransaction().rollback();
				}
				throw e;
			}
		}
	}

	@Test
	public void testSetParameters() {
		final List params = new ArrayList();
		params.add( new BigInteger( "2" ) );
		params.add( new BigInteger( "3" ) );

		try (Session s = openSession()) {
			final Query query = s.createNativeQuery( "select e.big from MY_ENTITY e where e.big in (:bigValues)" )
					.setParameter( "bigValues", params );
			try (ScrollableResults scroll = query.scroll()) {
				while ( scroll.next() ) {
					assertThat( scroll.get()[0], not( nullValue()) );
				}
			}
		}
	}

	@Entity(name = "MyEntity")
	@Table(name = "MY_ENTITY")
	public static class MyEntity {
		@Id
		private Long id;

		private BigInteger big;

		private String description;

		public MyEntity() {
		}

		public MyEntity(Long id, String description, BigInteger big) {
			this.id = id;
			this.description = description;
			this.big = big;
		}

		public String getDescription() {
			return description;
		}
	}
}
