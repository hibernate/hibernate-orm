/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bidi;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Jan Schatteman
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(xmlMappings = "org/hibernate/orm/test/bidi/Auction3.xml")
@SessionFactory
@JiraKey( value = "HHH-987" )
@JiraKey( value = "HHH-992" )
public class AuctionWithAbstractBidClassTest {
	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testAbstractSuperClassMapping(SessionFactoryScope scope) {
		SpecialAuction auction = new SpecialAuction();
		auction.setEnd( new Date() );
		auction.setDescription( "an auction for something" );

		SpecialBid ssbid = new SpecialBid();
		ssbid.setAmount( new BigDecimal( "123.34" ).setScale( 19, RoundingMode.DOWN ) );
		ssbid.setSuccessful( true );
		ssbid.setSpecial( false );
		ssbid.setDatetime( new Date() );
		ssbid.setItem( auction );
		auction.getBids().add( ssbid );
		auction.setSuccessfulBid( ssbid );

		SpecialBid sbid = new SpecialBid();
		sbid.setAmount( new BigDecimal( "321.43" ).setScale( 19, RoundingMode.DOWN ) );
		sbid.setSuccessful( false );
		sbid.setSpecial( true );
		sbid.setDatetime( new Date() );
		sbid.setItem( auction );
		auction.getBids().add( sbid );

		scope.inTransaction(
				session ->
						session.persist( auction )
		);

		Long auctionId = auction.getId();
		Long ssbidId = ssbid.getId();

		scope.inTransaction(
				session -> {
					SpecialAuction auc = session.find( SpecialAuction.class, auctionId );
					SpecialBid successfulBid = (SpecialBid) auc.getSuccessfulBid();
					assertTrue( successfulBid.isSuccessful() );
					assertEquals( successfulBid.getId(), ssbidId );
				}
		);
	}
}
