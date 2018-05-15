/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.query.Query;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
public class ScrollableTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {MyEntity.class};
	}

	@Override
	protected void prepareTest() throws Exception {
		try (Session session = openSession()) {
			session.getTransaction().begin();
			try {
				session.save( new MyEntity( 1L, "entity_1" ) );
				session.save( new MyEntity( 2L, "entity_2" ) );
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
	@TestForIssue(jiraKey = "HHH-10860")
	public void testScrollableResults() {
		final List params = new ArrayList();
		params.add( 1L );
		params.add( 2L );

		try (Session s = openSession()) {
			final Query query = s.createQuery( "from MyEntity e where e.id in (:ids)" )
					.setParameter( "ids", params )
					.setFetchSize( 10 );
			try (ScrollableResults scroll = query.scroll( ScrollMode.FORWARD_ONLY )) {
				int i = 0;
				while ( scroll.next() ) {
					if ( i == 0 ) {
						assertThat( ((MyEntity) scroll.get()[0]).getDescription(), is( "entity_1" ) );
					}
					else {
						assertThat( ((MyEntity) scroll.get()[0]).getDescription(), is( "entity_2" ) );
					}
					i++;
				}
			}
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10860")
	public void testScrollableResults2() {
		final List params = new ArrayList();
		params.add( 1L );
		params.add( 2L );

		try (Session s = openSession()) {
			final Query query = s.createQuery( "from MyEntity e where e.id in (:ids)" )
					.setParameter( "ids", params )
					.setFetchSize( 10 );
			try (ScrollableResults scroll = query.scroll( )) {
				int i = 0;
				while ( scroll.next() ) {
					if ( i == 0 ) {
						assertThat( ((MyEntity) scroll.get()[0]).getDescription(), is( "entity_1" ) );
					}
					else {
						assertThat( ((MyEntity) scroll.get()[0]).getDescription(), is( "entity_2" ) );
					}
					i++;
				}
			}
		}
	}

	@Entity(name = "MyEntity")
	@Table(name = "MY_ENTITY")
	public static class MyEntity {
		@Id
		private Long id;

		private String description;

		public MyEntity() {
		}

		public MyEntity(Long id, String description) {
			this.id = id;
			this.description = description;
		}

		public String getDescription() {
			return description;
		}
	}

}
