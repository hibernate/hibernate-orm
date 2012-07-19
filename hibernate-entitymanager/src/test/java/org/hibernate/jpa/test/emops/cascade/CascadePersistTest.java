/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.test.emops.cascade;

import javax.persistence.EntityManager;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.stat.Statistics;

import static org.junit.Assert.assertEquals;

/**
 * @author Emmanuel Bernard
 */
public class CascadePersistTest extends BaseEntityManagerFunctionalTestCase {
	@Test
	public void testLazyCollectionsStayLazyOnPersist() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		//initialize
		A a = new A();
		a.setName( "name1" );
		em.persist( a );
		a = new A();
		a.setName( "name2" );
		em.persist( a );
		a = new A();
		a.setName( "name3" );
		em.persist( a );
		em.flush();
		a = em.find( A.class, 1 );
		for ( int i = 0; i < 3; i++ ) {
			B1 b1 = new B1();
			b1.setA( a );
			em.persist( b1 );
		}
		for ( int i = 0; i < 3; i++ ) {
			B2 b2 = new B2();
			b2.setA( a );
			em.persist( b2 );
		}
		for ( int i = 0; i < 3; i++ ) {
			B3 b3 = new B3();
			b3.setA( a );
			em.persist( b3 );
		}
		for ( int i = 0; i < 3; i++ ) {
			B4 b4 = new B4();
			b4.setA( a );
			em.persist( b4 );
		}
		em.flush();
		B1 b1 = em.find( B1.class, 1 );
		for ( int i = 0; i < 2; i++ ) {
			C1 c1 = new C1();
			c1.setB1( b1 );
			em.persist( c1 );
		}
		B2 b2 = em.find( B2.class, 1 );
		for ( int i = 0; i < 4; i++ ) {
			C2 c2 = new C2();
			c2.setB2( b2 );
			em.persist( c2 );
		}
		em.flush();
		em.clear();

		//test
		a = em.find( A.class, 1 );
		C2 c2 = new C2();
		for ( B2 anotherB2 : a.getB2List() ) {
			if ( anotherB2.getId() == 1 ) {
				anotherB2.getC2List().add( c2 );
				c2.setB2( anotherB2 );
			}
		}
		Statistics statistics = em.unwrap(Session.class).getSessionFactory().getStatistics();
		statistics.setStatisticsEnabled( true );
		statistics.clear();
		em.persist( c2 );
		long loaded = statistics.getEntityLoadCount();
		assertEquals( 0, loaded );
		em.flush();
		em.getTransaction().rollback();
		em.close();
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[]{
				A.class,
				B1.class,
				B2.class,
				B3.class,
				B4.class,
				C1.class,
				C2.class
		};
	}
}
