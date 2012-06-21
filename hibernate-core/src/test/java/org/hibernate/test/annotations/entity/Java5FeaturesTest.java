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
package org.hibernate.test.annotations.entity;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Emmanuel Bernard
 */
public class Java5FeaturesTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testInterface() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Race r = new Race();
		r.setId( new Integer( 1 ) );
		r.setLength( new Long( 3 ) );
		s.persist( r );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		r = (Race) s.get( Race.class, r.getId() );
		assertEquals( new Long( 3 ), r.getLength() );
		tx.commit();
		s.close();

	}

	@Test
	public void testEnums() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		CommunityBid communityBid = new CommunityBid();
		communityBid.setId( new Integer( 2 ) );
		communityBid.setCommunityNote( Starred.OK );
		Bid bid = new Bid();
		bid.setId( new Integer( 1 ) );
		bid.setDescription( "My best one" );
		bid.setNote( Starred.OK );
		bid.setEditorsNote( Starred.GOOD );
		s.persist( bid );
		s.persist( communityBid );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		//bid = (Bid) s.get( Bid.class, bid.getId() );
		bid = (Bid)s.createQuery( "select b from Bid b where b.note = " +
				Starred.class.getName() + ".OK and b.editorsNote = " +
				Starred.class.getName() + ".GOOD and b.id = :id")
				.setParameter( "id", bid.getId() ).uniqueResult();
		//testing constant value
		assertEquals( Starred.OK, bid.getNote() );
		assertEquals( Starred.GOOD, bid.getEditorsNote() );
		bid = (Bid)s.createQuery( "select b from Bid b where b.note = :note" +
				 " and b.editorsNote = :editorNote " +
				" and b.id = :id")
				.setParameter( "id", bid.getId() )
				.setParameter( "note", Starred.OK )
				.setParameter( "editorNote", Starred.GOOD )
				.uniqueResult();
		//testing constant value
		assertEquals( Starred.OK, bid.getNote() );
		assertEquals( Starred.GOOD, bid.getEditorsNote() );
		bid.setNote( null );
		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		bid = (Bid) s.get( Bid.class, bid.getId() );
		communityBid = (CommunityBid) s.get( CommunityBid.class, communityBid.getId() );
		assertNull( bid.getNote() );
		assertEquals( Starred.OK, communityBid.getCommunityNote() );
		s.delete( bid );
		s.clear();
		communityBid = (CommunityBid) s.createSQLQuery( "select {b.*} from Bid b where b.id = ?" )
				.addEntity( "b", CommunityBid.class )
				.setInteger( 0, communityBid.getId() ).uniqueResult();
		assertEquals( Starred.OK, communityBid.getCommunityNote() );
		s.delete( communityBid );
		tx.commit();
		s.close();
	}

	public void testAutoboxing() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Bid bid = new Bid();
		bid.setId( new Integer( 2 ) );
		bid.setDescription( "My best one" );
		bid.setNote( Starred.OK );
		bid.setEditorsNote( Starred.GOOD );
		bid.setApproved( null );
		s.persist( bid );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		bid = (Bid) s.get( Bid.class, bid.getId() );
		assertEquals( null, bid.getApproved() );
		s.delete( bid );
		tx.commit();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Race.class,
				Bid.class,
				CommunityBid.class
		};
	}
}
