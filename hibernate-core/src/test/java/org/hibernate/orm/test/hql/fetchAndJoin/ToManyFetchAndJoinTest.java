/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql.fetchAndJoin;

import java.util.Iterator;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Gail Badner
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {
		Parent.class,
		Child.class,
		GrandChild.class
})
@SessionFactory
public class ToManyFetchAndJoinTest {

	@BeforeEach
	public void setupData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
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

			session.persist( p );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@JiraKey( value = "HHH-9637")
	public void testExplicitJoinBeforeFetchJoins(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {

			var hql = """
					select p
					from Parent p
						inner join p.children cRestrict
						inner join fetch p.children c
						inner join fetch c.grandChildren
					where cRestrict.value = 'c1'
					""";
			var parent = session.createQuery( hql, Parent.class ).uniqueResult();

			assertEquals( "p", parent.getValue() );
			assertTrue( Hibernate.isInitialized( parent.getChildren() ) );
			assertEquals( 2, parent.getChildren().size() );
			Iterator<Child> iterator = parent.getChildren().iterator();
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
		} );
	}

	@Test
	@JiraKey( value = "HHH-9637")
	public void testExplicitJoinBetweenFetchJoins(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			var hql = """
					select p
					from Parent p
						inner join fetch p.children c
						inner join p.children cRestrict
						inner join fetch c.grandChildren
					where cRestrict.value = 'c1'
					""";
			var parent = session.createQuery( hql, Parent.class ).uniqueResult();

			assertEquals( "p", parent.getValue() );
			assertTrue( Hibernate.isInitialized( parent.getChildren() ) );
			assertEquals( 2, parent.getChildren().size() );
			Iterator<Child> iterator = parent.getChildren().iterator();
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
		} );
	}

	@Test
	@JiraKey( value = "HHH-9637")
	public void testExplicitJoinAfterFetchJoins(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			var hql = """
					select p
					from Parent p
						inner join fetch p.children c
						inner join fetch c.grandChildren
						inner join p.children cRestrict
					where cRestrict.value = 'c1'
					""";
			Parent parent = session.createQuery( hql, Parent.class ).uniqueResult();

			assertEquals( "p", parent.getValue() );
			assertTrue( Hibernate.isInitialized( parent.getChildren() ) );
			assertEquals( 2, parent.getChildren().size() );
			Iterator<Child> iterator = parent.getChildren().iterator();
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
		} );
	}
}
