/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.naturalid.mutable.cached;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.Serializable;

import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * Tests of mutable natural ids stored in second level cache
 * 
 * @author Guenther Demetz
 * @author Steve Ebersole
 */
public abstract class CachedMutableNaturalIdTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {A.class, Another.class, AllCached.class, B.class};
	}

	@Override
	public void configure(Configuration cfg) {
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}

	@Test
	public void testNaturalIdChangedWhileAttached() {
		Session session = openSession();
		session.beginTransaction();
		Another it = new Another( "it" );
		session.save( it );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		it = session.bySimpleNaturalId( Another.class ).load( "it" );
		assertNotNull( it );
		// change it's name
		it.setName( "it2" );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		it = session.bySimpleNaturalId( Another.class ).load( "it" );
		assertNull( it );
		it = session.bySimpleNaturalId( Another.class ).load( "it2" );
		assertNotNull( it );
		session.delete( it );
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testNaturalIdChangedWhileDetached() {
		Session session = openSession();
		session.beginTransaction();
		Another it = new Another( "it" );
		session.save( it );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		it = session.bySimpleNaturalId( Another.class ).load( "it" );
		assertNotNull( it );
		session.getTransaction().commit();
		session.close();

		it.setName( "it2" );

		session = openSession();
		session.beginTransaction();
		session.update( it );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		it = session.bySimpleNaturalId( Another.class ).load( "it" );
		assertNull( it );
		it = session.bySimpleNaturalId( Another.class ).load( "it2" );
		assertNotNull( it );
		session.delete( it );
		session.getTransaction().commit();
		session.close();
	}
	
	
	
	@Test
	public void testNaturalIdRecachingWhenNeeded() {
		Session session = openSession();
		session.getSessionFactory().getStatistics().clear();
		session.beginTransaction();
		Another it = new Another( "it");
		session.save( it );
		Serializable id = it.getId();
		session.getTransaction().commit();
		session.close();
		
		session = openSession();
		for (int i=0; i < 10; i++) {
			session.beginTransaction();
			it = session.byId(Another.class).load(id);
			if (i == 9) {
				it.setName("name" + i);
			}
			it.setSurname("surname" + i); // changing something but not the natural-id's
			session.getTransaction().commit();
		}
		
		session = openSession();
		session.beginTransaction();
		it = session.bySimpleNaturalId(Another.class).load("it");
		assertNull(it);
		assertEquals( 0, session.getSessionFactory().getStatistics().getNaturalIdCacheHitCount() );
		it = session.byId(Another.class).load(id);
		session.delete(it);
		session.getTransaction().commit();
		session.close();
		
		// finally there should be only 2 NaturalIdCache puts : 1. insertion, 2. when updating natural-id from 'it' to 'name9'
		assertEquals( 2, session.getSessionFactory().getStatistics().getNaturalIdCachePutCount() );
	}
	
	@Test
	@TestForIssue( jiraKey = "HHH-7245" )
	public void testNaturalIdChangeAfterResolveEntityFrom2LCache() {
			Session session = openSession();
			session.beginTransaction();
			AllCached it = new AllCached( "it" );
			
			session.save( it );
			Serializable id = it.getId();
			session.getTransaction().commit();
			session.close();

			session = openSession();
			session.beginTransaction();
			it = session.byId( AllCached.class ).load( id );

			it.setName( "it2" );
			it = (AllCached) session.bySimpleNaturalId( AllCached.class ).load( "it" );
			assertNull( it );
			it = (AllCached) session.bySimpleNaturalId( AllCached.class ).load( "it2" );
			assertNotNull( it );
			session.delete( it );
			session.getTransaction().commit();
			session.close();
	}
	
	@Test
	public void testReattachementUnmodifiedInstance() {
		Session session = openSession();
		session.beginTransaction();
		A a = new A();
		B b = new B();
		b.naturalid = 100;
		session.persist( a );
		session.persist( b ); 
		b.assA = a;
		a.assB.add( b );
		session.getTransaction().commit();
		session.close();


		session = openSession();
		session.beginTransaction();
		session.buildLockRequest(LockOptions.NONE).lock( b ); // HHH-7513 failure during reattachment
		session.delete( b.assA );
		session.delete( b );
		session.flush();

		// true if the re-attachment worked
		assertEquals( session.createQuery( "FROM A" ).list().size(), 0 );
		assertEquals( session.createQuery( "FROM B" ).list().size(), 0 );

		session.getTransaction().commit();
		session.close();
	}

}

