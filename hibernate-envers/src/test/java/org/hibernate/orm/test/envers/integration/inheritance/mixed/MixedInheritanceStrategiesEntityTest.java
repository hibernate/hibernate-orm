/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.inheritance.mixed;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.integration.inheritance.mixed.entities.AbstractActivity;
import org.hibernate.orm.test.envers.integration.inheritance.mixed.entities.AbstractCheckActivity;
import org.hibernate.orm.test.envers.integration.inheritance.mixed.entities.Activity;
import org.hibernate.orm.test.envers.integration.inheritance.mixed.entities.ActivityId;
import org.hibernate.orm.test.envers.integration.inheritance.mixed.entities.CheckInActivity;
import org.hibernate.orm.test.envers.integration.inheritance.mixed.entities.NormalActivity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Michal Skowronek (mskowr at o2 pl)
 */
@EnversTest
@Jpa(annotatedClasses = {
		AbstractActivity.class,
		AbstractCheckActivity.class,
		CheckInActivity.class,
		NormalActivity.class
})
public class MixedInheritanceStrategiesEntityTest {

	private ActivityId id2;
	private ActivityId id1;
	private ActivityId id3;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		NormalActivity normalActivity = new NormalActivity();
		id1 = new ActivityId( 1, 2 );
		normalActivity.setId( id1 );
		normalActivity.setSequenceNumber( 1 );

		// Revision 1
		scope.inTransaction( em -> {
			em.persist( normalActivity );
		} );

		// Revision 2
		scope.inTransaction( em -> {
			NormalActivity na = em.find( NormalActivity.class, id1 );
			CheckInActivity checkInActivity = new CheckInActivity();
			id2 = new ActivityId( 2, 3 );
			checkInActivity.setId( id2 );
			checkInActivity.setSequenceNumber( 0 );
			checkInActivity.setDurationInMinutes( 30 );
			checkInActivity.setRelatedActivity( na );
			em.persist( checkInActivity );
		} );

		// Revision 3
		NormalActivity normalActivity3 = new NormalActivity();
		id3 = new ActivityId( 3, 4 );
		normalActivity3.setId( id3 );
		normalActivity3.setSequenceNumber( 2 );

		scope.inTransaction( em -> {
			em.persist( normalActivity3 );
		} );

		// Revision 4
		scope.inTransaction( em -> {
			NormalActivity na = em.find( NormalActivity.class, id3 );
			CheckInActivity checkInActivity = em.find( CheckInActivity.class, id2 );
			checkInActivity.setRelatedActivity( na );
			em.merge( checkInActivity );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( NormalActivity.class, id1 ) );
			assertEquals( Arrays.asList( 3 ), auditReader.getRevisions( NormalActivity.class, id3 ) );
			assertEquals( Arrays.asList( 2, 4 ), auditReader.getRevisions( CheckInActivity.class, id2 ) );
		} );
	}

	@Test
	public void testCurrentStateOfCheckInActivity(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final CheckInActivity checkInActivity = em.find( CheckInActivity.class, id2 );
			final NormalActivity normalActivity = em.find( NormalActivity.class, id3 );

			assertEquals( id2, checkInActivity.getId() );
			assertEquals( 0, checkInActivity.getSequenceNumber().intValue() );
			assertEquals( 30, checkInActivity.getDurationInMinutes().intValue() );
			final Activity relatedActivity = checkInActivity.getRelatedActivity();
			assertEquals( normalActivity.getId(), relatedActivity.getId() );
			assertEquals( normalActivity.getSequenceNumber(), relatedActivity.getSequenceNumber() );
		} );
	}

	@Test
	public void testCheckCurrentStateOfNormalActivities(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final NormalActivity normalActivity1 = em.find( NormalActivity.class, id1 );
			final NormalActivity normalActivity2 = em.find( NormalActivity.class, id3 );

			assertEquals( id1, normalActivity1.getId() );
			assertEquals( 1, normalActivity1.getSequenceNumber().intValue() );
			assertEquals( id3, normalActivity2.getId() );
			assertEquals( 2, normalActivity2.getSequenceNumber().intValue() );
		} );
	}

	@Test
	public void doTestFirstRevisionOfCheckInActivity(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			var auditReader = AuditReaderFactory.get( em );
			CheckInActivity checkInActivity = auditReader.find( CheckInActivity.class, id2, 2 );
			NormalActivity normalActivity = auditReader.find( NormalActivity.class, id1, 2 );

			assertEquals( id2, checkInActivity.getId() );
			assertEquals( 0, checkInActivity.getSequenceNumber().intValue() );
			assertEquals( 30, checkInActivity.getDurationInMinutes().intValue() );
			Activity relatedActivity = checkInActivity.getRelatedActivity();
			assertEquals( normalActivity.getId(), relatedActivity.getId() );
			assertEquals( normalActivity.getSequenceNumber(), relatedActivity.getSequenceNumber() );
		} );
	}

	@Test
	public void doTestSecondRevisionOfCheckInActivity(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			var auditReader = AuditReaderFactory.get( em );
			CheckInActivity checkInActivity = auditReader.find( CheckInActivity.class, id2, 4 );
			NormalActivity normalActivity = auditReader.find( NormalActivity.class, id3, 4 );

			assertEquals( id2, checkInActivity.getId() );
			assertEquals( 0, checkInActivity.getSequenceNumber().intValue() );
			assertEquals( 30, checkInActivity.getDurationInMinutes().intValue() );
			Activity relatedActivity = checkInActivity.getRelatedActivity();
			assertEquals( normalActivity.getId(), relatedActivity.getId() );
			assertEquals( normalActivity.getSequenceNumber(), relatedActivity.getSequenceNumber() );
		} );
	}
}
