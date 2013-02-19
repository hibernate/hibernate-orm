/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.manytomany.joinedsubclass;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Gail Badner (extracted from ManyToManyTest authored by Emmanuel Bernard)
 */
@FailureExpectedWithNewMetamodel
public class ManyToManyJoinedSubclassTest extends BaseCoreFunctionalTestCase {

	@Test
	public void testJoinedSubclassManyToMany() throws Exception {
		Session s = openSession();
		Zone a = new Zone();
		InspectorPrefixes ip = new InspectorPrefixes( "dgi" );
		Transaction tx = s.beginTransaction();
		s.save( a );
		s.save( ip );
		ip.getAreas().add( a );
		tx.commit();
		s.close();
		s = openSession();
		tx = s.beginTransaction();
		ip = (InspectorPrefixes) s.get( InspectorPrefixes.class, ip.getId() );
		assertNotNull( ip );
		assertEquals( 1, ip.getAreas().size() );
		assertEquals( a.getId(), ip.getAreas().get( 0 ).getId() );
		s.delete( ip );
		s.delete( ip.getAreas().get( 0 ) );
		tx.commit();
		s.close();
	}

	@Test
	public void testJoinedSubclassManyToManyWithNonPkReference() throws Exception {
		Session s = openSession();
		Zone a = new Zone();
		InspectorPrefixes ip = new InspectorPrefixes( "dgi" );
		ip.setName( "Inspector" );
		Transaction tx = s.beginTransaction();
		s.save( a );
		s.save( ip );
		ip.getDesertedAreas().add( a );
		tx.commit();
		s.close();
		s = openSession();
		tx = s.beginTransaction();
		ip = (InspectorPrefixes) s.get( InspectorPrefixes.class, ip.getId() );
		assertNotNull( ip );
		assertEquals( 1, ip.getDesertedAreas().size() );
		assertEquals( a.getId(), ip.getDesertedAreas().get( 0 ).getId() );
		s.delete( ip );
		s.delete( ip.getDesertedAreas().get( 0 ) );
		tx.commit();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Zone.class,
				Inspector.class,
				InspectorPrefixes.class,
		};
	}
}
