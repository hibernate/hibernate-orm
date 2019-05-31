/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.inheritance.mixed;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.inheritance.mixed.AbstractActivity;
import org.hibernate.envers.test.support.domains.inheritance.mixed.AbstractCheckActivity;
import org.hibernate.envers.test.support.domains.inheritance.mixed.Activity;
import org.hibernate.envers.test.support.domains.inheritance.mixed.ActivityId;
import org.hibernate.envers.test.support.domains.inheritance.mixed.CheckInActivity;
import org.hibernate.envers.test.support.domains.inheritance.mixed.NormalActivity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Michal Skowronek (mskowr at o2 pl)
 */
@Disabled("NYI - Mixed Inheritance Support")
public class MixedInheritanceStrategiesEntityTest extends EnversEntityManagerFactoryBasedFunctionalTest {

	private ActivityId id2;
	private ActivityId id1;
	private ActivityId id3;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				AbstractActivity.class,
				AbstractCheckActivity.class,
				CheckInActivity.class,
				NormalActivity.class
		};
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					NormalActivity normalActivity = new NormalActivity();
					id1 = new ActivityId( 1, 2 );
					normalActivity.setId( id1 );
					normalActivity.setSequenceNumber( 1 );
					entityManager.persist( normalActivity );
				},

				// Revision 2
				entityManager -> {
					NormalActivity normalActivity = entityManager.find( NormalActivity.class, id1 );
					CheckInActivity checkInActivity = new CheckInActivity();
					id2 = new ActivityId( 2, 3 );
					checkInActivity.setId( id2 );
					checkInActivity.setSequenceNumber( 0 );
					checkInActivity.setDurationInMinutes( 30 );
					checkInActivity.setRelatedActivity( normalActivity );

					entityManager.persist( checkInActivity );
				},

				// Revision 3
				entityManager -> {
					NormalActivity normalActivity = new NormalActivity();
					id3 = new ActivityId( 3, 4 );
					normalActivity.setId( id3 );
					normalActivity.setSequenceNumber( 2 );
					entityManager.persist( normalActivity );
				},

				// Revision 4
				entityManager -> {
					NormalActivity normalActivity = entityManager.find( NormalActivity.class, id3 );
					CheckInActivity checkInActivity = entityManager.find( CheckInActivity.class, id2 );
					checkInActivity.setRelatedActivity( normalActivity );

					entityManager.merge( checkInActivity );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( NormalActivity.class, id1 ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( NormalActivity.class, id3 ), contains( 3 ) );
		assertThat( getAuditReader().getRevisions( CheckInActivity.class, id2 ), contains( 2, 4 ) );
	}

	@DynamicTest
	public void testCurrentStateOfCheckInActivity() {
		final CheckInActivity checkInActivity = inTransaction( em -> { return em.find( CheckInActivity.class, id2 ); } );
		final NormalActivity normalActivity = inTransaction( em -> { return em.find( NormalActivity.class, id3 ); } );

		assertThat( checkInActivity.getId(), equalTo( id2 ) );
		assertThat( checkInActivity.getSequenceNumber(), equalTo( 0 ) );
		assertThat( checkInActivity.getDurationInMinutes(), equalTo( 30 ) );

		final Activity relatedActivity = checkInActivity.getRelatedActivity();
		assertThat( relatedActivity.getId(), equalTo( normalActivity.getId() ) );
		assertThat( relatedActivity.getSequenceNumber(), equalTo( normalActivity.getSequenceNumber() ) );
	}

	@DynamicTest
	public void testCheckCurrentStateOfNormalActivities() throws Exception {
		final NormalActivity normalActivity1 = inTransaction( em -> { return em.find( NormalActivity.class, id1 ); } );
		final NormalActivity normalActivity2 = inTransaction( em -> { return em.find( NormalActivity.class, id3 ); } );

		assertThat( normalActivity1.getId(), equalTo( id1 ) );
		assertThat( normalActivity1.getSequenceNumber(), equalTo( 1 ) );
		assertThat( normalActivity2.getId(), equalTo( id3 ) );
		assertThat( normalActivity2.getSequenceNumber(), equalTo( 2 ) );
	}

	@DynamicTest
	public void doTestFirstRevisionOfCheckInActivity() throws Exception {
		CheckInActivity checkInActivity = getAuditReader().find( CheckInActivity.class, id2, 2 );
		NormalActivity normalActivity = getAuditReader().find( NormalActivity.class, id1, 2 );

		assertThat( checkInActivity.getId(), equalTo( id2 ) );
		assertThat( checkInActivity.getSequenceNumber(), equalTo( 0 ) );
		assertThat( checkInActivity.getDurationInMinutes(), equalTo( 30 ) );

		final Activity relatedActivity = checkInActivity.getRelatedActivity();
		assertThat( relatedActivity.getId(), equalTo( normalActivity.getId() ) );
		assertThat( relatedActivity.getSequenceNumber(), equalTo( normalActivity.getSequenceNumber() ) );
	}

	@DynamicTest
	public void doTestSecondRevisionOfCheckInActivity() throws Exception {
		CheckInActivity checkInActivity = getAuditReader().find( CheckInActivity.class, id2, 4 );
		NormalActivity normalActivity = getAuditReader().find( NormalActivity.class, id3, 4 );

		assertThat( checkInActivity.getId(), equalTo( id2 ) );
		assertThat( checkInActivity.getSequenceNumber(), equalTo( 0 ) );
		assertThat( checkInActivity.getDurationInMinutes(), equalTo( 30 ) );

		final Activity relatedActivity = checkInActivity.getRelatedActivity();
		assertThat( relatedActivity.getId(), equalTo( normalActivity.getId() ) );
		assertThat( relatedActivity.getSequenceNumber(), equalTo( normalActivity.getSequenceNumber() ) );
	}
}
