/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.filter.subclass.joined;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.orm.test.filter.subclass.SubClassTest;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTemporaryTable.class)
@DomainModel(
		annotatedClasses = {
				Animal.class, Mammal.class, Human.class, Club.class
		}
)
public class JoinedSubClassTest extends SubClassTest {

	@AfterEach
	protected void cleanupTest() throws Exception {
		super.cleanupTest();
	}

	@Override
	protected void persistTestData(SessionImplementor session) {
		Club club = new Club();
		club.setName( "Mensa applicants" );
		club.getMembers().add( createHuman( session, club, false, 90 ) );
		club.getMembers().add( createHuman( session, club, false, 100 ) );
		club.getMembers().add( createHuman( session, club, true, 110 ) );
		session.persist( club );
	}

	@Test
	public void testClub() {
		scope.inTransaction( session -> {
			Club club = session.createQuery( "from Club", Club.class ).uniqueResult();
			assertThat( club.getMembers() ).hasSize( 3 );
			session.clear();

			session.enableFilter( "pregnantMembers" );
			club = session.createQuery( "from Club", Club.class ).uniqueResult();
			assertThat( club.getMembers() ).hasSize( 1 );
			session.clear();

			session.enableFilter( "iqMin" ).setParameter( "min", 148 );
			club = session.createQuery( "from Club", Club.class ).uniqueResult();
			assertThat( club.getMembers() ).isEmpty();
		} );
	}

	private Human createHuman(SessionImplementor session, Club club, boolean pregnant, int iq) {
		Human human = new Human();
		human.setClub( club );
		human.setName( "Homo Sapiens" );
		human.setPregnant( pregnant );
		human.setIq( iq );
		session.persist( human );
		return human;
	}
}
