/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.basic;

import java.net.URL;
import java.net.URLClassLoader;

import org.hibernate.Session;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * @author Steve Ebersole
 */
public class BasicInSessionTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {MyEntity.class};
	}

	@Test
	public void testIt() {
		Session s = openSession();
		s.beginTransaction();
		s.save( new MyEntity( 1L ) );
		s.save( new MyEntity( 2L ) );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		MyEntity myEntity1 = s.get( MyEntity.class, 1L );
		MyEntity myEntity2 = s.get( MyEntity.class, 2L );

		assertNotNull( myEntity1.$$_hibernate_getEntityInstance() );
		assertSame( myEntity1, myEntity1.$$_hibernate_getEntityInstance() );
		assertNotNull( myEntity1.$$_hibernate_getEntityEntry() );
		assertNull( myEntity1.$$_hibernate_getPreviousManagedEntity() );
		assertNotNull( myEntity1.$$_hibernate_getNextManagedEntity() );

		assertNotNull( myEntity2.$$_hibernate_getEntityInstance() );
		assertSame( myEntity2, myEntity2.$$_hibernate_getEntityInstance() );
		assertNotNull( myEntity2.$$_hibernate_getEntityEntry() );
		assertNotNull( myEntity2.$$_hibernate_getPreviousManagedEntity() );
		assertNull( myEntity2.$$_hibernate_getNextManagedEntity() );

		s.createQuery( "delete MyEntity" ).executeUpdate();
		s.getTransaction().commit();
		s.close();

		assertNull( myEntity1.$$_hibernate_getEntityEntry() );
	}

	@Test
	public void enhacementTest() {
		try {
			EnhancerTestUtils.enhanceAndDecompile( SimpleEntity.class, new URLClassLoader( new URL[0] ) );
		}
		catch (Exception e) {
			e.printStackTrace();
			Assert.fail( "Unexpected exception in EnhancerTestUtils.enhanceAndDecompile(): " + e.getMessage() );
		}
	}

}
