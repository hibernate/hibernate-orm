
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
package org.hibernate.test.annotations.cascade;

import java.util.ArrayList;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Check some of the individual cascade styles
 *
 * @todo do something for refresh
 *
 * @author Emmanuel Bernard
 */
public class CascadeTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testPersist() {
		Session s;
		Transaction tx;
		s = openSession();
		Tooth tooth = new Tooth();
		Tooth leftTooth = new Tooth();
		tooth.leftNeighbour = leftTooth;
		s.persist( tooth );
		tx = s.beginTransaction();
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		leftTooth = (Tooth) s.get( Tooth.class, leftTooth.id );
		assertNotNull( leftTooth );
		tx.commit();
		s.close();
	}

	@Test
	public void testMerge() {
		Session s;
		Transaction tx;
		s = openSession();
		Tooth tooth = new Tooth();
		Tooth rightTooth = new Tooth();
		tooth.type = "canine";
		tooth.rightNeighbour = rightTooth;
		rightTooth.type = "incisive";
		s.persist( rightTooth );
		s.persist( tooth );
		tx = s.beginTransaction();
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		tooth = (Tooth) s.get( Tooth.class, tooth.id );
		assertEquals( "incisive", tooth.rightNeighbour.type );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		tooth.rightNeighbour.type = "premolars";
		s.merge( tooth );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		rightTooth = (Tooth) s.get( Tooth.class, rightTooth.id );
		assertEquals( "premolars", rightTooth.type );
		tx.commit();
		s.close();
	}

	@Test
	public void testRemove() {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Tooth tooth = new Tooth();
		Mouth mouth = new Mouth();
		s.persist( mouth );
		s.persist( tooth );
		tooth.mouth = mouth;
		mouth.teeth = new ArrayList<Tooth>();
		mouth.teeth.add( tooth );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		tooth = (Tooth) s.get( Tooth.class, tooth.id );
		assertNotNull( tooth );
		s.delete( tooth.mouth );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		tooth = (Tooth) s.get( Tooth.class, tooth.id );
		assertNull( tooth );
		tx.commit();
		s.close();
	}

	@Test
	public void testDetach() {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Tooth tooth = new Tooth();
		Mouth mouth = new Mouth();
		s.persist( mouth );
		s.persist( tooth );
		tooth.mouth = mouth;
		mouth.teeth = new ArrayList<Tooth>();
		mouth.teeth.add( tooth );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		mouth = (Mouth) s.get( Mouth.class, mouth.id );
		assertNotNull( mouth );
		assertEquals( 1, mouth.teeth.size() );
		tooth = mouth.teeth.iterator().next();
		s.evict( mouth );
		assertFalse( s.contains( tooth ) );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		s.delete( s.get( Mouth.class, mouth.id ) );
		
		tx.commit();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Mouth.class,
				Tooth.class
		};
	}
}
