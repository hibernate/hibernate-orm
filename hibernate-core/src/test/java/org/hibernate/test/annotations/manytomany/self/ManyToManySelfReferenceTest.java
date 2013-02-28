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
package org.hibernate.test.annotations.manytomany.self;

import java.util.HashSet;
import java.util.Set;

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
public class ManyToManySelfReferenceTest extends BaseCoreFunctionalTestCase {

	@Test
	public void testSelf() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Friend f = new Friend();
		Friend sndF = new Friend();
		f.setName( "Starsky" );
		sndF.setName( "Hutch" );
		Set frnds = new HashSet();
		frnds.add( sndF );
		f.setFriends( frnds );
		//Starsky is a friend of Hutch but hutch is not
		s.persist( f );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		f = (Friend) s.load( Friend.class, f.getId() );
		assertNotNull( f );
		assertNotNull( f.getFriends() );
		assertEquals( 1, f.getFriends().size() );
		Friend fromDb2ndFrnd = f.getFriends().iterator().next();
		assertEquals( fromDb2ndFrnd.getId(), sndF.getId() );
		assertEquals( 0, fromDb2ndFrnd.getFriends().size() );
		tx.commit();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Friend.class,
		};
	}
}
