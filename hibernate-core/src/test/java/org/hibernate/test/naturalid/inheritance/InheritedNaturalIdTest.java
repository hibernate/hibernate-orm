/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.naturalid.inheritance;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * @author Steve Ebersole
 */
public class InheritedNaturalIdTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Principal.class, User.class };
	}

	@Test
	public void testIt() {
		Session s = openSession();
		s.beginTransaction();
		s.save(  new User( "steve" ) );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.bySimpleNaturalId( Principal.class ).load( "steve" );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.bySimpleNaturalId( User.class ).load( "steve" );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.delete( s.bySimpleNaturalId( User.class ).load( "steve" ) );
		s.getTransaction().commit();
		s.close();
	}


	@Test
	public void testSubclassModifieablNaturalId() {
		Session s = openSession();
		s.beginTransaction();
		s.save( new User( "steve" ) );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		Principal p = (Principal) s.bySimpleNaturalId( Principal.class ).load( "steve" );
		assertNotNull( p );
		User u = (User) s.bySimpleNaturalId( User.class ).load( "steve" );
		assertNotNull( u );
		assertSame( p, u );

		// change the natural id
		u.setUid( "sebersole" );
		s.flush();

		// make sure we can no longer access the info based on the old natural id value
		assertNull( s.bySimpleNaturalId( Principal.class ).load( "steve" ) );
		assertNull( s.bySimpleNaturalId( User.class ).load( "steve" ) );

		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.delete( u );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testSubclassDeleteNaturalId() {
		Session s = openSession();
		s.beginTransaction();
		s.save( new User( "steve" ) );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		Principal p = (Principal) s.bySimpleNaturalId( Principal.class ).load( "steve" );
		assertNotNull( p );

		s.delete( p );
		s.flush();

//		assertNull( s.bySimpleNaturalId( Principal.class ).load( "steve" ) );
		assertNull( s.bySimpleNaturalId( User.class ).load( "steve" ) );

		s.getTransaction().commit();
		s.close();
	}
}
