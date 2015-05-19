/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.naturalid.nullable;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Steve Ebersole
 */
public class NullableNaturalIdTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { A.class, B.class, C.class, D.class };
	}

	@Test
	public void testNaturalIdNullValueOnPersist() {
		Session session = openSession();
		session.beginTransaction();
		C c = new C();
		session.persist( c );
		c.name = "someName";
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		session.delete( c );
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testUniqueAssociation() {
		Session session = openSession();
		session.beginTransaction();
		A a = new A();
		B b = new B();
		b.naturalid = 100;
		session.persist( a );
		session.persist( b ); //b.assA is declared NaturalId, his value is null this moment
		b.assA = a;
		a.assB.add( b );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		// this is OK
		assertNotNull( session.byNaturalId( B.class ).using( "naturalid", 100 ).using( "assA", a ).load() );
		// this fails, cause EntityType.compare(Object x, Object y) always returns 0 !
		assertNull( session.byNaturalId( B.class ).using( "naturalid", 100 ).using( "assA", null ).load() );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		session.delete( b );
		session.delete( a );
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testNaturalIdQuerySupportingNullValues() {
		Session session = openSession();
		session.beginTransaction();
		D d1 = new D();
		d1.name = "Titi";
		d1.associatedC = null;
		D d2 = new D();
		d2.name = null;
		C c = new C();
		d2.associatedC = c;
		session.persist( d1 );
		session.persist( d2 );
		session.persist( c );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		assertNotNull( session.byNaturalId( D.class ).using( "name", null ).using( "associatedC", c ).load() );
		assertNotNull( session.byNaturalId( D.class ).using( "name", "Titi" ).using( "associatedC", null ).load() );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		session.delete( c );
		session.delete( d1 );
		session.delete( d2 );
		session.getTransaction().commit();
		session.close();
	}
}
