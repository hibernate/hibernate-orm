/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2006-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.batchfetch;

import java.util.List;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DynamicBatchFetchTest extends BaseCoreFunctionalTestCase {
	private static int currentId = 1;

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.setProperty( AvailableSettings.BATCH_FETCH_STYLE, "DYNAMIC" );
		super.configure( configuration );
		configuration.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { A.class, B.class };
	}

	@Test
	public void testDynamicBatchFetch() {
		Integer aId1 = createAAndB();
		Integer aId2 = createAAndB();
		Session s = openSession();
		s.getTransaction().begin();
		List resultList = s.createQuery("from A where id in (" + aId1 + "," + aId2 + ") order by id" ).list();
		A a1 = (A) resultList.get(0);
		A a2 = (A) resultList.get( 1 );
		assertEquals( aId1, a1.getId() );
		assertEquals( aId2, a2.getId() );
		assertFalse( Hibernate.isInitialized( a1.getB() ) );
		assertFalse( Hibernate.isInitialized( a2.getB() ) );
		assertEquals( "foo", a1.getB().getOtherProperty() );
		assertTrue( Hibernate.isInitialized( a1.getB() ) );
		// a2.getB() is still uninitialized
		assertFalse( Hibernate.isInitialized( a2.getB() ) );
		// the B entity has been loaded, but is has not been made the target of a2.getB() yet.
		assertTrue( ( (SessionImplementor) session ).getPersistenceContext().containsEntity(
						new EntityKey(
								( (SessionImplementor) session ).getContextEntityIdentifier( a2.getB() ),
								( (SessionImplementor) session ).getFactory().getEntityPersister( B.class.getName() )
						)
				)
		);
		// a2.getB() is still uninitialized; getting the ID for a2.getB() did not initialize it.
		assertFalse( Hibernate.isInitialized( a2.getB() ) );
		assertEquals( "foo", a2.getB().getOtherProperty() );
		// now it's initialized.
		assertTrue( Hibernate.isInitialized( a2.getB() ) );
		s.getTransaction().commit();
		s.close();

	}

	private int createAAndB() {
		Session s = openSession();
		s.getTransaction().begin();
		B b = new B();
		b.setIdPart1( currentId );
		b.setIdPart2( currentId);
		b.setOtherProperty("foo");
		s.save( b );

		A a = new A();
		a.setId( currentId );
		a.setB( b );

		s.save( a );

		s.getTransaction().commit();
		s.close();

		currentId++;

		return currentId - 1;
	}
}
