/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.query.Query;

import org.junit.Test;

import org.hibernate.testing.orm.junit.JiraKey;
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
				session.persist( new MyEntity( 1L, "entity_1" ) );
				session.persist( new MyEntity( 2L, "entity_2" ) );
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
				session.createMutationQuery( "delete from MyEntity" ).executeUpdate();
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
	@JiraKey(value = "HHH-10860")
	public void testScrollableResults() {
		final List<Long> params = new ArrayList<>();
		params.add( 1L );
		params.add( 2L );

		try (Session s = openSession()) {
			final Query<MyEntity> query = s.createQuery( "from MyEntity e where e.id in (:ids)", MyEntity.class )
					.setParameter( "ids", params )
					.setFetchSize( 10 );
			try (ScrollableResults<MyEntity> scroll = query.scroll( ScrollMode.FORWARD_ONLY )) {
				int i = 0;
				while ( scroll.next() ) {
					if ( i == 0 ) {
						assertThat( scroll.get().getDescription(), is( "entity_1" ) );
					}
					else {
						assertThat( scroll.get().getDescription(), is( "entity_2" ) );
					}
					i++;
				}
			}
		}
	}

	@Test
	@JiraKey(value = "HHH-10860")
	public void testScrollableResults2() {
		final List<Long> params = new ArrayList<>();
		params.add( 1L );
		params.add( 2L );

		try (Session s = openSession()) {
			final Query<MyEntity> query = s.createQuery( "from MyEntity e where e.id in (:ids)", MyEntity.class )
					.setParameter( "ids", params )
					.setFetchSize( 10 );
			try (ScrollableResults<MyEntity> scroll = query.scroll( )) {
				int i = 0;
				while ( scroll.next() ) {
					if ( i == 0 ) {
						assertThat( scroll.get().getDescription(), is( "entity_1" ) );
					}
					else {
						assertThat( scroll.get().getDescription(), is( "entity_2" ) );
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
