/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.collection.dereferenced;

import java.util.HashSet;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.collection.internal.AbstractPersistentCollection;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.testing.TestForIssue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
public class VersionedNoCascadeDereferencedCollectionTest extends AbstractDereferencedCollectionTest {

	@Test
	@TestForIssue( jiraKey = "HHH-9777" )
	public void testMergeNullCollection() {
		Session s = openSession();
		s.getTransaction().begin();
		VersionedNoCascadeOne one = new VersionedNoCascadeOne();
		assertNull( one.getManies() );
		s.save( one );
		assertNull( one.getManies() );
		EntityEntry eeOne = getEntityEntry( s, one  );
		assertNull( eeOne.getLoadedValue( "manies" ) );
		s.flush();
		assertNull( one.getManies() );
		assertNull( eeOne.getLoadedValue( "manies" ) );
		s.getTransaction().commit();
		s.close();

		final String role =VersionedNoCascadeOne.class.getName() + ".manies";

		s = openSession();
		s.getTransaction().begin();
		one = (VersionedNoCascadeOne) s.merge( one );

		// after merging, one.getManies() should still be null;
		// the EntityEntry loaded state should contain a PersistentCollection though.

		assertNull( one.getManies() );
		eeOne = getEntityEntry( s, one  );
		AbstractPersistentCollection maniesEEOneStateOrig = (AbstractPersistentCollection) eeOne.getLoadedValue( "manies" );
		assertNotNull( maniesEEOneStateOrig );

		// Ensure maniesEEOneStateOrig has role, key, and session properly defined (even though one.manies == null)
		assertEquals( role, maniesEEOneStateOrig.getRole() );
		assertEquals( one.getId(), maniesEEOneStateOrig.getKey() );
		assertSame( s, maniesEEOneStateOrig.getSession() );

		// Ensure there is a CollectionEntry for maniesEEOneStateOrig and that the role, persister, and key are set properly.
		CollectionEntry ceManiesOrig = getCollectionEntry( s, maniesEEOneStateOrig );
		assertNotNull( ceManiesOrig );
		assertEquals( role, ceManiesOrig.getRole() );
		assertSame( sessionFactory().getCollectionPersister( role ), ceManiesOrig.getLoadedPersister() );
		assertEquals( one.getId(), ceManiesOrig.getKey() );

		s.flush();

		// Ensure the same EntityEntry is being used.
		assertSame( eeOne, getEntityEntry( s, one ) );

		// Ensure one.getManies() is still null.
		assertNull( one.getManies() );

		// Ensure CollectionEntry for maniesEEOneStateOrig is no longer in the PersistenceContext.
		assertNull( getCollectionEntry( s, maniesEEOneStateOrig ) );

		// Ensure the original CollectionEntry has role, persister, and key set to null.
		assertNull( ceManiesOrig.getRole() );
		assertNull( ceManiesOrig.getLoadedPersister() );
		assertNull( ceManiesOrig.getKey() );

		// Ensure the PersistentCollection (that was previously returned by eeOne.getLoadedState())
		// has key and role set to null.
		assertNull( maniesEEOneStateOrig.getKey() );
		assertNull( maniesEEOneStateOrig.getRole() );

		// Ensure eeOne.getLoadedState() returns null for collection after flush.
		assertNull( eeOne.getLoadedValue( "manies" ) );

		// Ensure the session in maniesEEOneStateOrig has been unset.
		assertNull( maniesEEOneStateOrig.getSession() );

		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9777" )
	public void testGetAndNullifyCollection() {
		Session s = openSession();
		s.getTransaction().begin();
		VersionedNoCascadeOne one = new VersionedNoCascadeOne();
		assertNull( one.getManies() );
		s.save( one );
		assertNull( one.getManies() );
		EntityEntry eeOne = getEntityEntry( s, one  );
		assertNull( eeOne.getLoadedValue( "manies" ) );
		s.flush();
		assertNull( one.getManies() );
		assertNull( eeOne.getLoadedValue( "manies" ) );
		s.getTransaction().commit();
		s.close();

		final String role =VersionedNoCascadeOne.class.getName() + ".manies";

		s = openSession();
		s.getTransaction().begin();
		one = (VersionedNoCascadeOne) s.get(VersionedNoCascadeOne.class, one.getId() );

		// When returned by Session.get(), one.getManies() will return a PersistentCollection;
		// the EntityEntry loaded state should contain the same PersistentCollection.

		eeOne = getEntityEntry( s, one  );
		assertNotNull( one.getManies() );
		AbstractPersistentCollection maniesEEOneStateOrig = (AbstractPersistentCollection) eeOne.getLoadedValue( "manies" );
		assertSame( one.getManies(), maniesEEOneStateOrig );

		// Ensure maniesEEOneStateOrig has role, key, and session properly defined (even though one.manies == null)
		assertEquals( role, maniesEEOneStateOrig.getRole() );
		assertEquals( one.getId(), maniesEEOneStateOrig.getKey() );
		assertSame( s, maniesEEOneStateOrig.getSession() );

		// Ensure there is a CollectionEntry for maniesEEOneStateOrig and that the role, persister, and key are set properly.
		CollectionEntry ceManies = getCollectionEntry( s, maniesEEOneStateOrig );
		assertNotNull( ceManies );
		assertEquals( role, ceManies.getRole() );
		assertSame( sessionFactory().getCollectionPersister( role ), ceManies.getLoadedPersister() );
		assertEquals( one.getId(), ceManies.getKey() );

		// nullify collection
		one.setManies( null );

		s.flush();

		// Ensure the same EntityEntry is being used.
		assertSame( eeOne, getEntityEntry( s, one ) );

		// Ensure one.getManies() is still null.
		assertNull( one.getManies() );

		// Ensure CollectionEntry for maniesEEOneStateOrig is no longer in the PersistenceContext.
		assertNull( getCollectionEntry( s, maniesEEOneStateOrig ) );

		// Ensure the original CollectionEntry has role, persister, and key set to null.
		assertNull( ceManies.getRole() );
		assertNull( ceManies.getLoadedPersister() );
		assertNull( ceManies.getKey() );

		// Ensure the PersistentCollection (that was previously returned by eeOne.getLoadedState())
		// has key and role set to null.
		assertNull( maniesEEOneStateOrig.getKey() );
		assertNull( maniesEEOneStateOrig.getRole() );

		// Ensure eeOne.getLoadedState() returns null for collection after flush.
		assertNull( eeOne.getLoadedValue( "manies" ) );

		// Ensure the session in maniesEEOneStateOrig has been unset.
		assertNull( maniesEEOneStateOrig.getSession() );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9777" )
	public void testGetAndReplaceCollection() {
		Session s = openSession();
		s.getTransaction().begin();
		VersionedNoCascadeOne one = new VersionedNoCascadeOne();
		assertNull( one.getManies() );
		s.save( one );
		assertNull( one.getManies() );
		EntityEntry eeOne = getEntityEntry( s, one  );
		assertNull( eeOne.getLoadedValue( "manies" ) );
		s.flush();
		assertNull( one.getManies() );
		assertNull( eeOne.getLoadedValue( "manies" ) );
		s.getTransaction().commit();
		s.close();

		final String role =VersionedNoCascadeOne.class.getName() + ".manies";

		s = openSession();
		s.getTransaction().begin();
		one = (VersionedNoCascadeOne) s.get(VersionedNoCascadeOne.class, one.getId() );

		// When returned by Session.get(), one.getManies() will return a PersistentCollection;
		// the EntityEntry loaded state should contain the same PersistentCollection.

		eeOne = getEntityEntry( s, one );
		assertNotNull( one.getManies() );
		AbstractPersistentCollection maniesEEOneStateOrig = (AbstractPersistentCollection) eeOne.getLoadedValue( "manies" );
		assertSame( one.getManies(), maniesEEOneStateOrig );

		// Ensure maniesEEOneStateOrig has role, key, and session properly defined (even though one.manies == null)
		assertEquals( role, maniesEEOneStateOrig.getRole() );
		assertEquals( one.getId(), maniesEEOneStateOrig.getKey() );
		assertSame( s, maniesEEOneStateOrig.getSession() );

		// Ensure there is a CollectionEntry for maniesEEOneStateOrig and that the role, persister, and key are set properly.
		CollectionEntry ceManiesOrig = getCollectionEntry( s, maniesEEOneStateOrig );
		assertNotNull( ceManiesOrig );
		assertEquals( role, ceManiesOrig.getRole() );
		assertSame( sessionFactory().getCollectionPersister( role ), ceManiesOrig.getLoadedPersister() );
		assertEquals( one.getId(), ceManiesOrig.getKey() );

		// replace collection
		one.setManies( new HashSet<Many>() );

		s.flush();

		// Ensure the same EntityEntry is being used.
		assertSame( eeOne, getEntityEntry( s, one ) );

		// Ensure CollectionEntry for maniesEEOneStateOrig is no longer in the PersistenceContext.
		assertNull( getCollectionEntry( s, maniesEEOneStateOrig ) );

		// Ensure the original CollectionEntry has role, persister, and key set to null.
		assertNull( ceManiesOrig.getRole() );
		assertNull( ceManiesOrig.getLoadedPersister() );
		assertNull( ceManiesOrig.getKey() );

		// Ensure the PersistentCollection (that was previously returned by eeOne.getLoadedState())
		// has key and role set to null.
		assertNull( maniesEEOneStateOrig.getKey() );
		assertNull( maniesEEOneStateOrig.getRole() );

		// one.getManies() should be "wrapped" by a PersistentCollection now; role, key, and session should be set properly.
		assertTrue( PersistentCollection.class.isInstance( one.getManies() ) );
		assertEquals( role, ( (PersistentCollection) one.getManies() ).getRole() );
		assertEquals( one.getId(), ( (PersistentCollection) one.getManies() ).getKey() );
		assertSame( s, ( (AbstractPersistentCollection) one.getManies() ).getSession() );

		// Ensure eeOne.getLoadedState() contains the new collection.
		assertSame( one.getManies(), eeOne.getLoadedValue( "manies" ) );

		// Ensure there is a new CollectionEntry for the new collection and that role, persister, and key are set properly.
		CollectionEntry ceManiesAfterReplace = getCollectionEntry( s, (PersistentCollection) one.getManies() );
		assertNotNull( ceManiesAfterReplace );
		assertEquals( role, ceManiesAfterReplace.getRole() );
		assertSame( sessionFactory().getCollectionPersister( role ), ceManiesAfterReplace.getLoadedPersister() );
		assertEquals( one.getId(), ceManiesAfterReplace.getKey() );

		// Ensure the session in maniesEEOneStateOrig has been unset.
		assertNull( maniesEEOneStateOrig.getSession() );

		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testSaveOrUpdateNullCollection() {
		Session s = openSession();
		s.getTransaction().begin();
		VersionedNoCascadeOne one = new VersionedNoCascadeOne();
		assertNull( one.getManies() );
		s.save( one );
		assertNull( one.getManies() );
		EntityEntry eeOne = getEntityEntry( s, one  );
		assertNull( eeOne.getLoadedValue( "manies" ) );
		s.flush();
		assertNull( one.getManies() );
		assertNull( eeOne.getLoadedValue( "manies" ) );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		s.saveOrUpdate( one );

		// Ensure one.getManies() is still null.
		assertNull( one.getManies() );

		// Ensure the EntityEntry loaded state contains null for the manies collection.
		eeOne = getEntityEntry( s, one  );
		assertNull( eeOne.getLoadedValue( "manies" ) );

		s.flush();

		// Ensure one.getManies() is still null.
		assertNull( one.getManies() );

		// Ensure the same EntityEntry is being used.
		assertSame( eeOne, getEntityEntry( s, one ) );

		// Ensure the EntityEntry loaded state still contains null for the manies collection.
		assertNull( eeOne.getLoadedValue( "manies" ) );

		s.getTransaction().commit();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				VersionedNoCascadeOne.class,
				Many.class
		};
	}
}
