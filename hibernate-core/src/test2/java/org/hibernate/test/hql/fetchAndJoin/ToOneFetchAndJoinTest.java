/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.hql.fetchAndJoin;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
public class ToOneFetchAndJoinTest extends BaseCoreFunctionalTestCase {

	@Before
	public void setupData() {
		Entity1 e1 = new Entity1();
		e1.setValue( "entity1" );
		Entity2 e2 = new Entity2();
		e2.setValue( "entity2" );
		Entity3 e3 = new Entity3();
		e3.setValue( "entity3" );

		e1.setEntity2( e2 );
		e2.setEntity3( e3 );

		Entity2 e2a = new Entity2();
		e2a.setValue( "entity2a" );

		Session s = openSession();
		s.getTransaction().begin();
		s.persist( e3 );
		s.persist( e2 );
		s.persist( e1 );
		s.persist( e2a );
		s.getTransaction().commit();
		s.close();
	}

	@After
	public void cleanupData() {
		Session s = openSession();
		s.getTransaction().begin();
		s.createQuery( "delete Entity1" ).executeUpdate();
		s.createQuery( "delete Entity2" ).executeUpdate();
		s.createQuery( "delete Entity3" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9637")
	public void testFetchJoinsWithImplicitJoinInRestriction() {

		Session s = openSession();
		s.getTransaction().begin();

		Entity1 e1Queryied =
				(Entity1) s.createQuery(
						"select e1 from Entity1 e1 inner join fetch e1.entity2 e2 inner join fetch e2.entity3 where e1.entity2.value = 'entity2'" )
						.uniqueResult();
		assertEquals( "entity1", e1Queryied.getValue() );
		assertTrue( Hibernate.isInitialized( e1Queryied.getEntity2() ) );
		assertTrue( Hibernate.isInitialized( e1Queryied.getEntity2().getEntity3() ) );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9637")
	public void testExplicitJoinBeforeFetchJoins() {

		Session s = openSession();
		s.getTransaction().begin();

		Entity1 e1Queryied =
				(Entity1) s.createQuery(
						"select e1 from Entity1 e1 inner join e1.entity2 e1Restrict inner join fetch e1.entity2 e2 inner join fetch e2.entity3 where e1Restrict.value = 'entity2'" )
						.uniqueResult();
		assertEquals( "entity1", e1Queryied.getValue() );
		assertTrue( Hibernate.isInitialized( e1Queryied.getEntity2() ) );
		assertTrue( Hibernate.isInitialized( e1Queryied.getEntity2().getEntity3() ) );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9637")
	public void testExplicitJoinBetweenFetchJoins() {

		Session s = openSession();
		s.getTransaction().begin();

		Entity1 e1Queryied =
				(Entity1) s.createQuery(
						"select e1 from Entity1 e1 inner join fetch e1.entity2 e2 inner join e1.entity2 e1Restrict inner join fetch e2.entity3 where e1Restrict.value = 'entity2'" )
						.uniqueResult();
		assertEquals( "entity1", e1Queryied.getValue() );
		assertTrue( Hibernate.isInitialized( e1Queryied.getEntity2() ) );
		assertTrue( Hibernate.isInitialized( e1Queryied.getEntity2().getEntity3() ) );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9637")
	public void testExplicitJoinAfterFetchJoins() {

		Session s = openSession();
		s.getTransaction().begin();

		Entity1 e1Queryied =
				(Entity1) s.createQuery(
						"select e1 from Entity1 e1 inner join fetch e1.entity2 e2 inner join fetch e2.entity3 inner join e1.entity2 e1Restrict where e1Restrict.value = 'entity2'" )
						.uniqueResult();
		assertEquals( "entity1", e1Queryied.getValue() );
		assertTrue( Hibernate.isInitialized( e1Queryied.getEntity2() ) );
		assertTrue( Hibernate.isInitialized( e1Queryied.getEntity2().getEntity3() ) );
		s.getTransaction().commit();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Entity1.class,
				Entity2.class,
				Entity3.class
		};
	}
}
