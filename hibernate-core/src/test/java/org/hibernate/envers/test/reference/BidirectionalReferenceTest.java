/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.reference;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.reference.GreetingPO;
import org.hibernate.envers.test.support.domains.reference.GreetingSetPO;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BidirectionalReferenceTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Long set1_id;
	private Long set2_id;

	private Long g1_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { GreetingPO.class, GreetingSetPO.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					GreetingSetPO set1 = new GreetingSetPO();
					set1.setName( "a1" );

					GreetingSetPO set2 = new GreetingSetPO();
					set2.setName( "a2" );

					entityManager.persist( set1 );
					entityManager.persist( set2 );

					set1_id = set1.getId();
					set2_id = set2.getId();
				},

				// Revision 2
				entityManager -> {
					GreetingPO g1 = new GreetingPO();
					g1.setGreeting( "g1" );
					g1.setGreetingSet( entityManager.getReference( GreetingSetPO.class, set1_id ) );

					entityManager.persist( g1 );
					g1_id = g1.getId();
				},

				// Revision 3
				entityManager -> {
					GreetingPO g1 = entityManager.find( GreetingPO.class, g1_id );
					g1.setGreetingSet( entityManager.getReference( GreetingSetPO.class, set2_id ) );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( GreetingPO.class, g1_id ), contains( 2, 3 ) );

		assertThat( getAuditReader().getRevisions( GreetingSetPO.class, set1_id ), contains( 1, 2, 3 ) );
		assertThat(getAuditReader().getRevisions( GreetingSetPO.class, set2_id ), contains( 1, 3 ) );
	}

	@DynamicTest
	public void testHistoryOfG1() {
		GreetingPO rev1 = getAuditReader().find( GreetingPO.class, g1_id, 1 );
		GreetingPO rev2 = getAuditReader().find( GreetingPO.class, g1_id, 2 );
		GreetingPO rev3 = getAuditReader().find( GreetingPO.class, g1_id, 3 );

		assertThat( rev1, nullValue() );
		assertThat( rev2.getGreetingSet().getName(), equalTo( "a1" ) );
		assertThat( rev3.getGreetingSet().getName(), equalTo( "a2" ) );
	}

	@DynamicTest
	public void testHistoryOfSet1() {
		GreetingSetPO rev1 = getAuditReader().find( GreetingSetPO.class, set1_id, 1 );
		GreetingSetPO rev2 = getAuditReader().find( GreetingSetPO.class, set1_id, 2 );
		GreetingSetPO rev3 = getAuditReader().find( GreetingSetPO.class, set1_id, 3 );

		assertThat( rev1.getName(), equalTo( "a1" ) );
		assertThat( rev2.getName(), equalTo( "a1" ) );
		assertThat( rev3.getName(), equalTo( "a1" ) );

		GreetingPO g1 = new GreetingPO();
		g1.setId( g1_id );
		g1.setGreeting( "g1" );

		assertThat( rev1.getGreetings(), CollectionMatchers.isEmpty() );
		assertThat( rev2.getGreetings(), contains( g1 ) );
		assertThat( rev3.getGreetings(), CollectionMatchers.isEmpty() );
	}

	@DynamicTest
	public void testHistoryOfSet2() {
		GreetingSetPO rev1 = getAuditReader().find( GreetingSetPO.class, set2_id, 1 );
		GreetingSetPO rev2 = getAuditReader().find( GreetingSetPO.class, set2_id, 2 );
		GreetingSetPO rev3 = getAuditReader().find( GreetingSetPO.class, set2_id, 3 );

		assertThat( rev1.getName(), equalTo( "a2" ) );
		assertThat( rev2.getName(), equalTo( "a2" ) );
		assertThat( rev3.getName(), equalTo( "a2" ) );

		GreetingPO g1 = new GreetingPO();
		g1.setId( g1_id );
		g1.setGreeting( "g1" );

		assertThat( rev1.getGreetings(), CollectionMatchers.isEmpty() );
		assertThat( rev2.getGreetings(), CollectionMatchers.isEmpty() );
		assertThat( rev3.getGreetings(), contains( g1 ) );
	}
}