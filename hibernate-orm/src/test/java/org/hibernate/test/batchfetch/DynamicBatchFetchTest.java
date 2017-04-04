/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
