/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.entity;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = {
				Race.class,
				Bid.class,
				CommunityBid.class
		}
)
@SessionFactory
public class Java5FeaturesTest {

	@AfterEach
	public void afterEach(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testInterface(SessionFactoryScope scope) {
		Race r = new Race();
		scope.inTransaction(
				session -> {
					r.setId( 1 );
					r.setLength( 3L );
					session.persist( r );
				}
		);

		scope.inTransaction(
				session -> {
					Race race = session.find( Race.class, r.getId() );
					assertThat( race.getLength() ).isEqualTo( 3 );
				}
		);
	}

	@Test
	public void testEnums(SessionFactoryScope scope) {
		Bid b = new Bid();
		CommunityBid cb = new CommunityBid();
		scope.inTransaction(
				session -> {
					cb.setId(  2  );
					cb.setCommunityNote( Starred.OK );
					b.setId(  1 );
					b.setDescription( "My best one" );
					b.setNote( Starred.OK );
					b.setEditorsNote( Starred.GOOD );
					session.persist( b );
					session.persist( cb );
				}
		);

		scope.inTransaction(
				session -> {
					//bid = (Bid) s.get( Bid.class, bid.getId() );
					Bid bid = session.createQuery( "select b from Bid b where b.note = " +
												Starred.class.getName() + ".OK and b.editorsNote = " +
												Starred.class.getName() + ".GOOD and b.id = :id", Bid.class )
							.setParameter( "id", b.getId() ).uniqueResult();
					//testing constant value
					assertThat( bid.getNote() ).isEqualTo( Starred.OK );
					assertThat( bid.getEditorsNote() ).isEqualTo( Starred.GOOD );
					bid = session.createQuery( "select b from Bid b where b.note = :note" +
											" and b.editorsNote = :editorNote " +
											" and b.id = :id", Bid.class )
							.setParameter( "id", bid.getId() )
							.setParameter( "note", Starred.OK )
							.setParameter( "editorNote", Starred.GOOD )
							.uniqueResult();
					//testing constant value
					assertThat( bid.getNote() ).isEqualTo( Starred.OK );
					assertThat( bid.getEditorsNote() ).isEqualTo( Starred.GOOD );
					bid.setNote( null );
				}
		);

		scope.inTransaction(
				session -> {
					Bid bid = session.find( Bid.class, b.getId() );
					CommunityBid communityBid = session.find( CommunityBid.class, cb.getId() );
					assertThat( bid.getNote() ).isNull();
					assertThat( communityBid.getCommunityNote() ).isEqualTo( Starred.OK );
					session.remove( bid );
					session.clear();
					communityBid = session.createNativeQuery( "select {b.*} from Bid b where b.id = ?",
									CommunityBid.class )
							.addEntity( "b", CommunityBid.class )
							.setParameter( 1, communityBid.getId() ).uniqueResult();
					assertThat( communityBid.getCommunityNote() ).isEqualTo( Starred.OK );
					session.remove( communityBid );
				}
		);
	}

	@Test
	public void testAutoboxing(SessionFactoryScope scope) {
		Bid b = new Bid();
		scope.inTransaction(
				session -> {
					b.setId( 2 );
					b.setDescription( "My best one" );
					b.setNote( Starred.OK );
					b.setEditorsNote( Starred.GOOD );
					b.setApproved( null );
					session.persist( b );
				}
		);

		scope.inTransaction(
				session -> {
					Bid bid = session.find( Bid.class, b.getId() );
					assertThat( bid.getApproved() ).isNull();
					session.remove( bid );
				}
		);
	}
}
