/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.bytecode.enhancement;

import org.hibernate.Session;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * @author Steve Ebersole
 */
public class MostBasicEnhancementTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { MyEntity.class };
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
		MyEntity myEntity = (MyEntity) s.get( MyEntity.class, 1L );
		MyEntity myEntity2 = (MyEntity) s.get( MyEntity.class, 2L );

		assertNotNull( myEntity.$$_hibernate_getEntityInstance() );
		assertSame( myEntity, myEntity.$$_hibernate_getEntityInstance() );
		assertNotNull( myEntity.$$_hibernate_getEntityEntry() );
		assertNull( myEntity.$$_hibernate_getPreviousManagedEntity() );
		assertNotNull( myEntity.$$_hibernate_getNextManagedEntity() );

		assertNotNull( myEntity2.$$_hibernate_getEntityInstance() );
		assertSame( myEntity2, myEntity2.$$_hibernate_getEntityInstance() );
		assertNotNull( myEntity2.$$_hibernate_getEntityEntry() );
		assertNotNull( myEntity2.$$_hibernate_getPreviousManagedEntity() );
		assertNull( myEntity2.$$_hibernate_getNextManagedEntity() );

		s.createQuery( "delete MyEntity" ).executeUpdate();
		s.getTransaction().commit();
		s.close();

		assertNull( myEntity.$$_hibernate_getEntityEntry() );
	}


}
