/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.indexcoll.eager;

import java.util.Date;
import java.util.Iterator;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.test.annotations.indexcoll.Gas;
import org.hibernate.test.annotations.indexcoll.GasKey;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test index collections
 *
 * @author Emmanuel Bernard
 */
public class EagerIndexedCollectionTest extends BaseNonConfigCoreFunctionalTestCase {
	@Test
	public void testJPA2DefaultMapColumns() throws Exception {
		isDefaultKeyColumnPresent( Atmosphere.class.getName(), "gasesDef", "_KEY" );
		isDefaultKeyColumnPresent( Atmosphere.class.getName(), "gasesPerKeyDef", "_KEY" );
		isDefaultKeyColumnPresent( Atmosphere.class.getName(), "gasesDefLeg", "_KEY" );
	}

	private void isDefaultKeyColumnPresent(String collectionOwner, String propertyName, String suffix) {
		assertTrue( "Could not find " + propertyName + suffix,
				isDefaultColumnPresent(collectionOwner, propertyName, suffix) );
	}

	private boolean isDefaultColumnPresent(String collectionOwner, String propertyName, String suffix) {
		final Collection collection = metadata().getCollectionBinding( collectionOwner + "." + propertyName );
		final Iterator columnIterator = collection.getCollectionTable().getColumnIterator();
		boolean hasDefault = false;
		while ( columnIterator.hasNext() ) {
			Column column = (Column) columnIterator.next();
			if ( (propertyName + suffix).equals( column.getName() ) ) hasDefault = true;
		}
		return hasDefault;
	}

	@Test
	public void testRealMap() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Atmosphere atm = new Atmosphere();
		Atmosphere atm2 = new Atmosphere();
		GasKey key = new GasKey();
		key.setName( "O2" );
		Gas o2 = new Gas();
		o2.name = "oxygen";
		atm.gases.put( "100%", o2 );
		atm.gasesPerKey.put( key, o2 );
		atm2.gases.put( "100%", o2 );
		atm2.gasesPerKey.put( key, o2 );
		s.persist( key );
		s.persist( atm );
		s.persist( atm2 );

		s.flush();
		s.clear();

		atm = (Atmosphere) s.get( Atmosphere.class, atm.id );
		key = (GasKey) s.get( GasKey.class, key.getName() );
		assertEquals( 1, atm.gases.size() );
		assertEquals( o2.name, atm.gases.get( "100%" ).name );
		assertEquals( o2.name, atm.gasesPerKey.get( key ).name );
		tx.rollback();
		s.close();
	}

	@Test
	public void testTemporalKeyMap() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Atmosphere atm = new Atmosphere();
		atm.colorPerDate.put( new Date(1234567000), "red" );
		s.persist( atm );

		s.flush();
		s.clear();

		atm = (Atmosphere) s.get( Atmosphere.class, atm.id );
		assertEquals( 1, atm.colorPerDate.size() );
		final Date date = atm.colorPerDate.keySet().iterator().next();
		final long diff = new Date( 1234567000 ).getTime() - date.getTime();
		assertTrue( "24h diff max", diff >= 0 && diff < 24*60*60*1000 );
		tx.rollback();
		s.close();
	}

	@Test
	public void testEnumKeyType() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Atmosphere atm = new Atmosphere();
		atm.colorPerLevel.put( Atmosphere.Level.HIGH, "red" );
		s.persist( atm );

		s.flush();
		s.clear();

		atm = (Atmosphere) s.get( Atmosphere.class, atm.id );
		assertEquals( 1, atm.colorPerLevel.size() );
		assertEquals( "red", atm.colorPerLevel.get( Atmosphere.Level.HIGH) );
		tx.rollback();
		s.close();
	}

	@Test
	public void testEntityKeyElementTarget() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Atmosphere atm = new Atmosphere();
		Gas o2 = new Gas();
		o2.name = "oxygen";
		atm.composition.put( o2, 94.3 );
		s.persist( o2 );
		s.persist( atm );

		s.flush();
		s.clear();

		atm = (Atmosphere) s.get( Atmosphere.class, atm.id );
		assertTrue( Hibernate.isInitialized( atm.composition ) );
		assertEquals( 1, atm.composition.size() );
		assertEquals( o2.name, atm.composition.keySet().iterator().next().name );
		tx.rollback();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Atmosphere.class,
				Gas.class,
				GasKey.class
		};
	}
}
