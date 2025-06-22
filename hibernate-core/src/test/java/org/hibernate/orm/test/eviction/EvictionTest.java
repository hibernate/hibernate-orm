/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.eviction;

import org.hibernate.Session;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
public class EvictionTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { IsolatedEvictableEntity.class };
	}

	@Test
	@JiraKey( value = "HHH-7912" )
	public void testNormalUsage() {
		Session session = openSession();
		session.beginTransaction();
		session.persist( new IsolatedEvictableEntity( 1 ) );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		IsolatedEvictableEntity entity = (IsolatedEvictableEntity) session.get( IsolatedEvictableEntity.class, 1 );
		assertTrue( session.contains( entity ) );
		session.evict( entity );
		assertFalse( session.contains( entity ) );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		session.remove( entity );
		session.getTransaction().commit();
		session.close();
	}

	@Test
	@JiraKey( value = "HHH-7912" )
	public void testEvictingNull() {
		Session session = openSession();
		session.beginTransaction();
		try {
			session.evict( null );
			fail( "Expecting evict(null) to throw IAE" );
		}
		catch (IllegalArgumentException expected) {
		}
		session.getTransaction().commit();
		session.close();
	}

	@Test
	@JiraKey( value = "HHH-7912" )
	public void testEvictingTransientEntity() {
		Session session = openSession();
		session.beginTransaction();
		session.evict( new IsolatedEvictableEntity( 1 ) );
		session.getTransaction().commit();
		session.close();
	}

	@Test
	@JiraKey( value = "HHH-7912" )
	public void testEvictingDetachedEntity() {
		Session session = openSession();
		session.beginTransaction();
		session.persist( new IsolatedEvictableEntity( 1 ) );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		IsolatedEvictableEntity entity = (IsolatedEvictableEntity) session.get( IsolatedEvictableEntity.class, 1 );
		assertTrue( session.contains( entity ) );
		// detach the entity
		session.evict( entity );
		assertFalse( session.contains( entity ) );
		// evict it again the entity
		session.evict( entity );
		assertFalse( session.contains( entity ) );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		session.remove( entity );
		session.getTransaction().commit();
		session.close();
	}

	@Test
	@JiraKey( value = "HHH-7912" )
	public void testEvictingNonEntity() {
		Session session = openSession();
		session.beginTransaction();
		try {
			session.evict( new EvictionTest() );
			fail( "Expecting evict(non-entity) to throw IAE" );
		}
		catch (IllegalArgumentException expected) {
		}
		session.getTransaction().commit();
		session.close();
	}

}
