/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.propertyref.cachedcollections;

import org.junit.Test;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Set of tests originally developed to verify and fix HHH-5853
 *
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-5853" )
public class CachedPropertyRefCollectionTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[]{"propertyref/cachedcollections/Mappings.hbm.xml"};
	}

	@Test
	public void testRetrievalOfCachedCollectionWithPropertyRefKey() {
		// create the test data...
		Session session = openSession();
		session.beginTransaction();
		ManagedObject mo = new ManagedObject( "test", "test" );
		mo.getMembers().add( "members" );
		session.save( mo );
		session.getTransaction().commit();
		session.close();

		// First attempt to load it via PK lookup
		session = openSession();
		session.beginTransaction();
		ManagedObject obj = (ManagedObject) session.get( ManagedObject.class, 1L );
		assertNotNull( obj );
		assertTrue( Hibernate.isInitialized( obj ) );
		obj.getMembers().size();
		assertTrue( Hibernate.isInitialized( obj.getMembers() ) );
		session.getTransaction().commit();
		session.close();

		// Now try to access it via natural key
		session = openSession();
		session.beginTransaction();
		Criteria criteria = session.createCriteria( ManagedObject.class )
				.add( Restrictions.naturalId().set( "name", "test" ) )
				.setCacheable( true )
				.setFetchMode( "members", FetchMode.JOIN );
		obj = (ManagedObject) criteria.uniqueResult();
		assertNotNull( obj );
		assertTrue( Hibernate.isInitialized( obj ) );
		obj.getMembers().size();
		assertTrue( Hibernate.isInitialized( obj.getMembers() ) );
		session.getTransaction().commit();
		session.close();

		// Clean up
		session = openSession();
		session.beginTransaction();
		session.delete( obj );
		session.getTransaction().commit();
		session.close();
	}
}

