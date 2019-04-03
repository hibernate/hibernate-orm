/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bidi;

import java.math.BigDecimal;
import java.util.Date;

import org.hibernate.Hibernate;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.dialect.PostgreSQLDialect;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gavin King
 */
@Disabled("Post insert identifier generator and Formula not yet implemented")
public class AuctionTest extends SessionFactoryBasedFunctionalTest {
	@Override
	public String[] getHbmMappingFiles() {
		return new String[] { "bidi/Auction.hbm.xml" };
	}

	@Test
	@SuppressWarnings({ "unchecked" })
	@SkipForDialect(value = { PostgreSQL81Dialect.class, PostgreSQLDialect.class }, comment = "doesn't like boolean=1")
	public void testLazy() {
		Auction auction = new Auction();
		Bid bid = new Bid();
		inTransaction(
				session -> {
					auction.setDescription( "an auction for something" );
					auction.setEnd( new Date() );
					bid.setAmount( new BigDecimal( 123.34 ).setScale( 19, BigDecimal.ROUND_DOWN ) );
					bid.setSuccessful( true );
					bid.setDatetime( new Date() );
					bid.setItem( auction );
					auction.getBids().add( bid );
					auction.setSuccessfulBid( bid );
					session.persist( bid );
				}
		);


		inTransaction(
				session -> {
					Bid b = session.load( Bid.class, bid.getId() );
					assertFalse( Hibernate.isInitialized( b ) );
					Auction a = session.get( Auction.class, auction.getId() );
					assertFalse( Hibernate.isInitialized( a.getBids() ) );
					assertTrue( Hibernate.isInitialized( a.getSuccessfulBid() ) );
					assertSame( a.getBids().iterator().next(), b );
					assertSame( b, a.getSuccessfulBid() );
					assertTrue( Hibernate.isInitialized( b ) );
					assertTrue( b.isSuccessful() );
				}
		);

		inTransaction(
				session -> {
					Bid b = session.load( Bid.class, bid.getId() );
					assertFalse( Hibernate.isInitialized( b ) );
					Auction a = (Auction) session.createQuery( "from Auction a left join fetch a.bids" ).uniqueResult();
					assertTrue( Hibernate.isInitialized( b ) );
					assertTrue( Hibernate.isInitialized( a.getBids() ) );
					assertSame( b, a.getSuccessfulBid() );
					assertSame( a.getBids().iterator().next(), b );
					assertTrue( b.isSuccessful() );
				}
		);

		inTransaction(
				session -> {
					Bid b = session.load( Bid.class, bid.getAmount() );
					Auction a = session.load( Auction.class, auction.getId() );
					assertFalse( Hibernate.isInitialized( b ) );
					assertFalse( Hibernate.isInitialized( a ) );
					session.createQuery( "from Auction a left join fetch a.successfulBid" ).list();
					assertTrue( Hibernate.isInitialized( b ) );
					assertTrue( Hibernate.isInitialized( a ) );
					assertSame( b, a.getSuccessfulBid() );
					assertFalse( Hibernate.isInitialized( a.getBids() ) );
					assertSame( a.getBids().iterator().next(), b );
					assertTrue( b.isSuccessful() );
				}
		);

		inTransaction(
				session -> {
					Bid b = session.load( Bid.class, bid.getId() );
					Auction a = session.load( Auction.class, auction.getId() );
					assertFalse( Hibernate.isInitialized( b ) );
					assertFalse( Hibernate.isInitialized( a ) );
					assertSame( session.get( Bid.class, bid.getId() ), b );
					assertTrue( Hibernate.isInitialized( b ) );
					assertSame( session.get( Auction.class, auction.getId() ), a );
					assertTrue( Hibernate.isInitialized( a ) );
					assertSame( b, a.getSuccessfulBid() );
					assertFalse( Hibernate.isInitialized( a.getBids() ) );
					assertSame( a.getBids().iterator().next(), b );
					assertTrue( b.isSuccessful() );
				}
		);
	}

}

