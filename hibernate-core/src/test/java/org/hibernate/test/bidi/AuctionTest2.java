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
package org.hibernate.test.bidi;

import java.math.BigDecimal;
import java.util.Date;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 */
@RequiresDialectFeature(
		value = DialectChecks.SupportsExistsInSelectCheck.class,
		comment = "dialect does not support exist predicates in the select clause"
)
public class AuctionTest2 extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "bidi/Auction2.hbm.xml" };
	}

	@Test
	public void testLazy() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Auction a = new Auction();
		a.setDescription( "an auction for something" );
		a.setEnd( new Date() );
		Bid b = new Bid();
		b.setAmount( new BigDecimal( 123.34 ).setScale( 19, BigDecimal.ROUND_DOWN ) );
		b.setSuccessful( true );
		b.setDatetime( new Date() );
		b.setItem( a );
		a.getBids().add( b );
		a.setSuccessfulBid( b );
		s.persist( b );
		t.commit();
		s.close();

		Long aid = a.getId();
		Long bid = b.getId();

		s = openSession();
		t = s.beginTransaction();
		b = ( Bid ) s.load( Bid.class, bid );
		assertFalse( Hibernate.isInitialized( b ) );
		a = ( Auction ) s.get( Auction.class, aid );
		assertFalse( Hibernate.isInitialized( a.getBids() ) );
		assertFalse( Hibernate.isInitialized( a.getSuccessfulBid() ) );
		assertSame( a.getBids().iterator().next(), b );
		assertSame( b, a.getSuccessfulBid() );
		assertTrue( Hibernate.isInitialized( b ) );
		assertTrue( b.isSuccessful() );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		b = ( Bid ) s.load( Bid.class, bid );
		assertFalse( Hibernate.isInitialized( b ) );
		a = ( Auction ) s.createQuery( "from Auction a left join fetch a.bids" ).uniqueResult();
		assertTrue( Hibernate.isInitialized( b ) );
		assertTrue( Hibernate.isInitialized( a.getBids() ) );
		assertSame( b, a.getSuccessfulBid() );
		assertSame( a.getBids().iterator().next(), b );
		assertTrue( b.isSuccessful() );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		b = ( Bid ) s.load( Bid.class, bid );
		a = ( Auction ) s.load( Auction.class, aid );
		assertFalse( Hibernate.isInitialized( b ) );
		assertFalse( Hibernate.isInitialized( a ) );
		s.createQuery( "from Auction a left join fetch a.successfulBid" ).list();
		assertTrue( Hibernate.isInitialized( b ) );
		assertTrue( Hibernate.isInitialized( a ) );
		assertSame( b, a.getSuccessfulBid() );
		assertFalse( Hibernate.isInitialized( a.getBids() ) );
		assertSame( a.getBids().iterator().next(), b );
		assertTrue( b.isSuccessful() );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		b = ( Bid ) s.load( Bid.class, bid );
		a = ( Auction ) s.load( Auction.class, aid );
		assertFalse( Hibernate.isInitialized( b ) );
		assertFalse( Hibernate.isInitialized( a ) );
		assertSame( s.get( Bid.class, bid ), b );
		assertTrue( Hibernate.isInitialized( b ) );
		assertSame( s.get( Auction.class, aid ), a );
		assertTrue( Hibernate.isInitialized( a ) );
		assertSame( b, a.getSuccessfulBid() );
		assertFalse( Hibernate.isInitialized( a.getBids() ) );
		assertSame( a.getBids().iterator().next(), b );
		assertTrue( b.isSuccessful() );
		t.commit();
		s.close();
	}

}
