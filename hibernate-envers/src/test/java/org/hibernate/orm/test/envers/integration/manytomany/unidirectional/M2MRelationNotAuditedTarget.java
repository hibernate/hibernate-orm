/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.manytomany.unidirectional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.UnversionedStrTestEntity;
import org.hibernate.orm.test.envers.entities.manytomany.unidirectional.M2MTargetNotAuditedEntity;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.hibernate.orm.test.envers.tools.TestTools.checkCollection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A test for auditing a many-to-many relation where the target entity is not audited.
 *
 * @author Adam Warski
 */
@EnversTest
@Jpa(annotatedClasses = {M2MTargetNotAuditedEntity.class, UnversionedStrTestEntity.class})
public class M2MRelationNotAuditedTarget {
	private Integer tnae1_id;
	private Integer tnae2_id;

	private Integer uste1_id;
	private Integer uste2_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// No revision
		scope.inTransaction( em -> {
			UnversionedStrTestEntity uste1 = new UnversionedStrTestEntity( "str1" );
			UnversionedStrTestEntity uste2 = new UnversionedStrTestEntity( "str2" );

			em.persist( uste1 );
			em.persist( uste2 );

			uste1_id = uste1.getId();
			uste2_id = uste2.getId();
		} );

		// Revision 1
		scope.inTransaction( em -> {
			UnversionedStrTestEntity uste1 = em.find( UnversionedStrTestEntity.class, uste1_id );
			UnversionedStrTestEntity uste2 = em.find( UnversionedStrTestEntity.class, uste2_id );

			M2MTargetNotAuditedEntity tnae1 = new M2MTargetNotAuditedEntity(
					1,
					"tnae1",
					new ArrayList<UnversionedStrTestEntity>()
			);
			M2MTargetNotAuditedEntity tnae2 = new M2MTargetNotAuditedEntity(
					2,
					"tnae2",
					new ArrayList<UnversionedStrTestEntity>()
			);
			tnae2.getReferences().add( uste1 );
			tnae2.getReferences().add( uste2 );
			em.persist( tnae1 );
			em.persist( tnae2 );

			tnae1_id = tnae1.getId();
			tnae2_id = tnae2.getId();
		} );

		// Revision 2
		scope.inTransaction( em -> {
			M2MTargetNotAuditedEntity tnae1 = em.find( M2MTargetNotAuditedEntity.class, tnae1_id );
			M2MTargetNotAuditedEntity tnae2 = em.find( M2MTargetNotAuditedEntity.class, tnae2_id );
			UnversionedStrTestEntity uste1 = em.find( UnversionedStrTestEntity.class, uste1_id );

			tnae1.getReferences().add( uste1 );
			tnae2.getReferences().remove( uste1 );
		} );

		// Revision 3
		scope.inTransaction( em -> {
			M2MTargetNotAuditedEntity tnae1 = em.find( M2MTargetNotAuditedEntity.class, tnae1_id );
			M2MTargetNotAuditedEntity tnae2 = em.find( M2MTargetNotAuditedEntity.class, tnae2_id );
			UnversionedStrTestEntity uste1 = em.find( UnversionedStrTestEntity.class, uste1_id );
			UnversionedStrTestEntity uste2 = em.find( UnversionedStrTestEntity.class, uste2_id );

			//field not changed!!!
			tnae1.getReferences().add( uste1 );
			tnae2.getReferences().remove( uste2 );
		} );

		// Revision 4
		scope.inTransaction( em -> {
			M2MTargetNotAuditedEntity tnae1 = em.find( M2MTargetNotAuditedEntity.class, tnae1_id );
			M2MTargetNotAuditedEntity tnae2 = em.find( M2MTargetNotAuditedEntity.class, tnae2_id );
			UnversionedStrTestEntity uste1 = em.find( UnversionedStrTestEntity.class, uste1_id );
			UnversionedStrTestEntity uste2 = em.find( UnversionedStrTestEntity.class, uste2_id );

			tnae1.getReferences().add( uste2 );
			tnae2.getReferences().add( uste1 );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List<Number> revisions = auditReader.getRevisions( M2MTargetNotAuditedEntity.class, tnae1_id );
			assertEquals( Arrays.asList( 1, 2, 4 ), revisions );
			revisions = auditReader.getRevisions( M2MTargetNotAuditedEntity.class, tnae2_id );
			assertEquals( Arrays.asList( 1, 2, 3, 4 ), revisions );
		} );
	}

	@Test
	public void testHistoryOfTnae1_id(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			UnversionedStrTestEntity uste1 = em.find( UnversionedStrTestEntity.class, uste1_id );
			UnversionedStrTestEntity uste2 = em.find( UnversionedStrTestEntity.class, uste2_id );

			M2MTargetNotAuditedEntity rev1 = auditReader.find( M2MTargetNotAuditedEntity.class, tnae1_id, 1 );
			M2MTargetNotAuditedEntity rev2 = auditReader.find( M2MTargetNotAuditedEntity.class, tnae1_id, 2 );
			M2MTargetNotAuditedEntity rev3 = auditReader.find( M2MTargetNotAuditedEntity.class, tnae1_id, 3 );
			M2MTargetNotAuditedEntity rev4 = auditReader.find( M2MTargetNotAuditedEntity.class, tnae1_id, 4 );

			assertTrue( checkCollection( rev1.getReferences() ) );
			assertTrue( checkCollection( rev2.getReferences(), uste1 ) );
			assertTrue( checkCollection( rev3.getReferences(), uste1 ) );
			assertTrue( checkCollection( rev4.getReferences(), uste1, uste2 ) );
		} );
	}

	@Test
	public void testHistoryOfTnae2_id(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			UnversionedStrTestEntity uste1 = em.find( UnversionedStrTestEntity.class, uste1_id );
			UnversionedStrTestEntity uste2 = em.find( UnversionedStrTestEntity.class, uste2_id );

			M2MTargetNotAuditedEntity rev1 = auditReader.find( M2MTargetNotAuditedEntity.class, tnae2_id, 1 );
			M2MTargetNotAuditedEntity rev2 = auditReader.find( M2MTargetNotAuditedEntity.class, tnae2_id, 2 );
			M2MTargetNotAuditedEntity rev3 = auditReader.find( M2MTargetNotAuditedEntity.class, tnae2_id, 3 );
			M2MTargetNotAuditedEntity rev4 = auditReader.find( M2MTargetNotAuditedEntity.class, tnae2_id, 4 );

			assertTrue( checkCollection( rev1.getReferences(), uste1, uste2 ) );
			assertTrue( checkCollection( rev2.getReferences(), uste2 ) );
			assertTrue( checkCollection( rev3.getReferences() ) );
			assertTrue( checkCollection( rev4.getReferences(), uste1 ) );
		} );
	}
}
