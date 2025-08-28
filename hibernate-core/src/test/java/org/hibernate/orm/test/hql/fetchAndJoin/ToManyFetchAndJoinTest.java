/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql.fetchAndJoin;

import java.util.Iterator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Gail Badner
 */
public class ToManyFetchAndJoinTest extends BaseCoreFunctionalTestCase {

	@Before
	public void setupData() {
		Parent p = new Parent( "p" );
		Child c1 = new Child( "c1" );
		GrandChild gc11 = new GrandChild( "gc11" );
		GrandChild gc12 = new GrandChild( "gc12" );
		p.getChildren().add( c1 );
		c1.getGrandChildren().add( gc11 );
		c1.getGrandChildren().add( gc12 );

		Child c2 = new Child( "c2" );
		GrandChild gc21 = new GrandChild( "gc21" );
		GrandChild gc22 = new GrandChild( "gc22" );
		GrandChild gc23 = new GrandChild( "gc23" );
		p.getChildren().add( c2 );
		c2.getGrandChildren().add( gc21 );
		c2.getGrandChildren().add( gc22 );
		c2.getGrandChildren().add( gc23 );

		Session s = openSession();
		s.getTransaction().begin();
		s.persist( p );
		s.getTransaction().commit();
		s.close();
	}

	@After
	public void cleanupData() {
		Session s = openSession();
		s.getTransaction().begin();
		s.createQuery( "delete GrandChild" ).executeUpdate();
		s.createQuery( "delete Child" ).executeUpdate();
		s.createQuery( "delete Parent" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@JiraKey( value = "HHH-9637")
	public void testExplicitJoinBeforeFetchJoins() {

		Session s = openSession();
		s.getTransaction().begin();

		Parent p =
				(Parent) s.createQuery(
						"select p from Parent p inner join p.children cRestrict inner join fetch p.children c inner join fetch c.grandChildren where cRestrict.value = 'c1'" )
						.uniqueResult();

		assertEquals( "p", p.getValue() );
		assertTrue( Hibernate.isInitialized( p.getChildren() ) );
		assertEquals( 2, p.getChildren().size() );
		Iterator<Child> iterator = p.getChildren().iterator();
		Child cA = iterator.next();
		assertTrue( Hibernate.isInitialized( cA.getGrandChildren() ) );
		if ( cA.getValue().equals( "c1" ) ) {
			assertEquals( 2, cA.getGrandChildren().size() );
			Child cB = iterator.next();
			assertTrue( Hibernate.isInitialized( cB.getGrandChildren() ) );
			assertEquals( 3, cB.getGrandChildren().size() );
		}
		else if ( cA.getValue().equals( "c2" ) ) {
			assertEquals( 3, cA.getGrandChildren().size() );
			Child cB = iterator.next();
			assertTrue( Hibernate.isInitialized( cB.getGrandChildren() ) );
			assertEquals( 2, cB.getGrandChildren().size() );
		}
		else {
			fail( "unexpected value" );
		}

		s.getTransaction().commit();
		s.close();
	}

	@Test
	@JiraKey( value = "HHH-9637")
	public void testExplicitJoinBetweenFetchJoins() {

		Session s = openSession();
		s.getTransaction().begin();

		Parent p =
				(Parent) s.createQuery(
						"select p from Parent p inner join fetch p.children c inner join p.children cRestrict inner join fetch c.grandChildren where cRestrict.value = 'c1'" )
						.uniqueResult();

		assertEquals( "p", p.getValue() );
		assertTrue( Hibernate.isInitialized( p.getChildren() ) );
		assertEquals( 2, p.getChildren().size() );
		Iterator<Child> iterator = p.getChildren().iterator();
		Child cA = iterator.next();
		assertTrue( Hibernate.isInitialized( cA.getGrandChildren() ) );
		if ( cA.getValue().equals( "c1" ) ) {
			assertEquals( 2, cA.getGrandChildren().size() );
			Child cB = iterator.next();
			assertTrue( Hibernate.isInitialized( cB.getGrandChildren() ) );
			assertEquals( 3, cB.getGrandChildren().size() );
		}
		else if ( cA.getValue().equals( "c2" ) ) {
			assertEquals( 3, cA.getGrandChildren().size() );
			Child cB = iterator.next();
			assertTrue( Hibernate.isInitialized( cB.getGrandChildren() ) );
			assertEquals( 2, cB.getGrandChildren().size() );
		}
		else {
			fail( "unexpected value" );
		}

		s.getTransaction().commit();
		s.close();
	}

	@Test
	@JiraKey( value = "HHH-9637")
	public void testExplicitJoinAfterFetchJoins() {

		Session s = openSession();
		s.getTransaction().begin();

		Parent p =
				(Parent) s.createQuery(
						"select p from Parent p inner join fetch p.children c inner join fetch c.grandChildren inner join p.children cRestrict where cRestrict.value = 'c1'" )
						.uniqueResult();

		assertEquals( "p", p.getValue() );
		assertTrue( Hibernate.isInitialized( p.getChildren() ) );
		assertEquals( 2, p.getChildren().size() );
		Iterator<Child> iterator = p.getChildren().iterator();
		Child cA = iterator.next();
		assertTrue( Hibernate.isInitialized( cA.getGrandChildren() ) );
		if ( cA.getValue().equals( "c1" ) ) {
			assertEquals( 2, cA.getGrandChildren().size() );
			Child cB = iterator.next();
			assertTrue( Hibernate.isInitialized( cB.getGrandChildren() ) );
			assertEquals( 3, cB.getGrandChildren().size() );
		}
		else if ( cA.getValue().equals( "c2" ) ) {
			assertEquals( 3, cA.getGrandChildren().size() );
			Child cB = iterator.next();
			assertTrue( Hibernate.isInitialized( cB.getGrandChildren() ) );
			assertEquals( 2, cB.getGrandChildren().size() );
		}
		else {
			fail( "unexpected value" );
		}

		s.getTransaction().commit();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Parent.class,
				Child.class,
				GrandChild.class
		};
	}
}
