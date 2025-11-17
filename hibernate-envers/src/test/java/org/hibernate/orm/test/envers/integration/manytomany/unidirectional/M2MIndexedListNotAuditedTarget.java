/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.manytomany.unidirectional;

import java.util.Arrays;
import java.util.List;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.UnversionedStrTestEntity;
import org.hibernate.orm.test.envers.entities.manytomany.unidirectional.M2MIndexedListTargetNotAuditedEntity;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.hibernate.orm.test.envers.tools.TestTools.checkCollection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A test for auditing a many-to-many indexed list where the target entity is not audited.
 *
 * @author Vladimir Klyushnikov
 * @author Adam Warski
 */
@EnversTest
@Jpa(annotatedClasses = {UnversionedStrTestEntity.class, M2MIndexedListTargetNotAuditedEntity.class})
public class M2MIndexedListNotAuditedTarget {
	private Integer itnae1_id;
	private Integer itnae2_id;

	private UnversionedStrTestEntity uste1;
	private UnversionedStrTestEntity uste2;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// No revision
		scope.inTransaction( em -> {
			uste1 = new UnversionedStrTestEntity( "str1" );
			uste2 = new UnversionedStrTestEntity( "str2" );

			em.persist( uste1 );
			em.persist( uste2 );
		} );

		// Revision 1
		scope.inTransaction( em -> {
			uste1 = em.find( UnversionedStrTestEntity.class, uste1.getId() );
			uste2 = em.find( UnversionedStrTestEntity.class, uste2.getId() );

			M2MIndexedListTargetNotAuditedEntity itnae1 = new M2MIndexedListTargetNotAuditedEntity( 1, "tnae1" );

			itnae1.getReferences().add( uste1 );
			itnae1.getReferences().add( uste2 );

			em.persist( itnae1 );

			itnae1_id = itnae1.getId();
		} );

		// Revision 2
		scope.inTransaction( em -> {
			M2MIndexedListTargetNotAuditedEntity itnae2 = new M2MIndexedListTargetNotAuditedEntity( 2, "tnae2" );

			itnae2.getReferences().add( uste2 );

			em.persist( itnae2 );

			itnae2_id = itnae2.getId();
		} );

		// Revision 3
		scope.inTransaction( em -> {
			M2MIndexedListTargetNotAuditedEntity itnae1 = em.find(
					M2MIndexedListTargetNotAuditedEntity.class,
					itnae1_id
			);

			itnae1.getReferences().set( 0, uste2 );
			itnae1.getReferences().set( 1, uste1 );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List<Number> revisions = auditReader.getRevisions( M2MIndexedListTargetNotAuditedEntity.class, itnae1_id );
			assertEquals( revisions, Arrays.asList( 1, 3 ) );

			revisions = auditReader.getRevisions( M2MIndexedListTargetNotAuditedEntity.class, itnae2_id );
			assertEquals( revisions, Arrays.asList( 2 ) );
		} );
	}

	@Test
	public void testHistory1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			M2MIndexedListTargetNotAuditedEntity rev1 = auditReader.find(
					M2MIndexedListTargetNotAuditedEntity.class,
					itnae1_id,
					1
			);
			M2MIndexedListTargetNotAuditedEntity rev2 = auditReader.find(
					M2MIndexedListTargetNotAuditedEntity.class,
					itnae1_id,
					2
			);
			M2MIndexedListTargetNotAuditedEntity rev3 = auditReader.find(
					M2MIndexedListTargetNotAuditedEntity.class,
					itnae1_id,
					3
			);

			assertTrue( checkCollection( rev1.getReferences(), uste1, uste2 ) );
			assertTrue( checkCollection( rev2.getReferences(), uste1, uste2 ) );
			assertTrue( checkCollection( rev3.getReferences(), uste2, uste1 ) );
		} );
	}

	@Test
	public void testHistory2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			M2MIndexedListTargetNotAuditedEntity rev1 = auditReader.find(
					M2MIndexedListTargetNotAuditedEntity.class,
					itnae2_id,
					1
			);
			M2MIndexedListTargetNotAuditedEntity rev2 = auditReader.find(
					M2MIndexedListTargetNotAuditedEntity.class,
					itnae2_id,
					2
			);
			M2MIndexedListTargetNotAuditedEntity rev3 = auditReader.find(
					M2MIndexedListTargetNotAuditedEntity.class,
					itnae2_id,
					3
			);

			assertNull( rev1 );
			assertTrue( checkCollection( rev2.getReferences(), uste2 ) );
			assertTrue( checkCollection( rev3.getReferences(), uste2 ) );
		} );
	}
}
