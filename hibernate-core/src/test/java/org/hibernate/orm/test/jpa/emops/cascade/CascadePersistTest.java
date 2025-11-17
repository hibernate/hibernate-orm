/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.emops.cascade;

import org.hibernate.Session;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Emmanuel Bernard
 */
@Jpa(annotatedClasses = {
		A.class,
		B1.class,
		B2.class,
		B3.class,
		B4.class,
		C1.class,
		C2.class
})
public class CascadePersistTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testLazyCollectionsStayLazyOnPersist(EntityManagerFactoryScope scope) throws Exception {
		scope.inEntityManager(
				entityManager -> {
					try {
						entityManager.getTransaction().begin();
						//initialize
						A a = new A();
						a.setName( "name1" );
						entityManager.persist( a );
						a = new A();
						a.setName( "name2" );
						entityManager.persist( a );
						a = new A();
						a.setName( "name3" );
						entityManager.persist( a );
						entityManager.flush();
						a = entityManager.find( A.class, 1 );
						for ( int i = 0; i < 3; i++ ) {
							B1 b1 = new B1();
							b1.setA( a );
							entityManager.persist( b1 );
						}
						for ( int i = 0; i < 3; i++ ) {
							B2 b2 = new B2();
							b2.setA( a );
							entityManager.persist( b2 );
						}
						for ( int i = 0; i < 3; i++ ) {
							B3 b3 = new B3();
							b3.setA( a );
							entityManager.persist( b3 );
						}
						for ( int i = 0; i < 3; i++ ) {
							B4 b4 = new B4();
							b4.setA( a );
							entityManager.persist( b4 );
						}
						entityManager.flush();
						B1 b1 = entityManager.find( B1.class, 1 );
						for ( int i = 0; i < 2; i++ ) {
							C1 c1 = new C1();
							c1.setB1( b1 );
							entityManager.persist( c1 );
						}
						B2 b2 = entityManager.find( B2.class, 1 );
						for ( int i = 0; i < 4; i++ ) {
							C2 c2 = new C2();
							c2.setB2( b2 );
							entityManager.persist( c2 );
						}
						entityManager.flush();
						entityManager.clear();

						//test
						a = entityManager.find( A.class, 1 );
						C2 c2 = new C2();
						for ( B2 anotherB2 : a.getB2List() ) {
							if ( anotherB2.getId() == 1 ) {
								anotherB2.getC2List().add( c2 );
								c2.setB2( anotherB2 );
							}
						}
						Statistics statistics = entityManager.unwrap(Session.class).getSessionFactory().getStatistics();
						statistics.setStatisticsEnabled( true );
						statistics.clear();
						entityManager.persist( c2 );
						long loaded = statistics.getEntityLoadCount();
						assertEquals( 0, loaded );
						entityManager.flush();
						entityManager.getTransaction().rollback();
					}
					catch (Exception e) {
						if ( entityManager.getTransaction().isActive() ) {
							entityManager.getTransaction().rollback();
						}
						throw e;
					}
				}
		);
	}
}
