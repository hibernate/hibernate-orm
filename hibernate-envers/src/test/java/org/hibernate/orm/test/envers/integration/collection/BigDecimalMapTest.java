/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.collection;

import java.math.BigDecimal;
import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.collection.BigDecimalMapEntity;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regression test for HHH-20926.
 *
 * MapCollectionMapper.isSame() was incorrectly using CollectionPersister#getKeyType()
 * instead of CollectionPersister#getIndexType() to compare map keys.
 *
 * For a persistent Map, getKeyType() represents the foreign-key type referencing
 * the collection owner (e.g., BigDecimal entity ID), while getIndexType() represents
 * the actual Java map-key type (e.g., String).
 *
 * This caused @ElementCollection Map<K,V> changes to be compared incorrectly by Envers.
 * Depending on the owner identifier type, this could result in:
 * - ClassCastException when the ID type performs type-specific comparison (e.g., BigDecimal)
 * - Incorrect collection diff with missing audit records when the ID type tolerates incompatible values
 *
 * The test uses:
 * - An audited entity with BigDecimal ID (non-String identifier type with type-specific equality)
 * - An @ElementCollection Map<String, String>
 * - Two initial entries {"A": "Reason A", "B": "Reason B"}
 * - A second transaction that removes one key ("A") and adds another ("C": "Reason C")
 * - Assertions verifying two revisions with correct historical map states
 *
 * @author Chikumar
 */
@EnversTest
@Jpa(annotatedClasses = {BigDecimalMapEntity.class})
public class BigDecimalMapTest {
	private BigDecimal entity_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		entity_id = new BigDecimal( "12345.67" );

		// Revision 1: Persist entity with two map entries
		scope.inTransaction( em -> {
			BigDecimalMapEntity entity = new BigDecimalMapEntity( entity_id );
			entity.getEntries().put( "A", "Reason A" );
			entity.getEntries().put( "B", "Reason B" );
			em.persist( entity );
		} );

		// Revision 2: Remove one entry and add a new one
		scope.inTransaction( em -> {
			BigDecimalMapEntity entity = em.find( BigDecimalMapEntity.class, entity_id );
			entity.getEntries().remove( "A" );
			entity.getEntries().put( "C", "Reason C" );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( BigDecimalMapEntity.class, entity_id ),
					"Entity should have exactly 2 revisions" );
		} );
	}

	@Test
	public void testRevision1Content(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			BigDecimalMapEntity rev1 = auditReader.find( BigDecimalMapEntity.class, entity_id, 1 );

			assertEquals( TestTools.makeMap( "A", "Reason A", "B", "Reason B" ), rev1.getEntries(),
					"Revision 1 should contain entries {A=Reason A, B=Reason B}" );
		} );
	}

	@Test
	public void testRevision2Content(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			BigDecimalMapEntity rev2 = auditReader.find( BigDecimalMapEntity.class, entity_id, 2 );

			assertEquals( TestTools.makeMap( "B", "Reason B", "C", "Reason C" ), rev2.getEntries(),
					"Revision 2 should contain entries {B=Reason B, C=Reason C}" );
		} );
	}

	@Test
	public void testMapKeyComparison(EntityManagerFactoryScope scope) {
		// This test specifically verifies that the fix prevents ClassCastException
		// and produces correct audit history when map keys are compared.
		// The bug would cause BigDecimal comparison to be used for String keys,
		// resulting in either ClassCastException or incorrect equality results.
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			// Get both revisions
			BigDecimalMapEntity rev1 = auditReader.find( BigDecimalMapEntity.class, entity_id, 1 );
			BigDecimalMapEntity rev2 = auditReader.find( BigDecimalMapEntity.class, entity_id, 2 );

			// Verify that key "A" was removed between revisions
			assertEquals( true, rev1.getEntries().containsKey( "A" ),
					"Revision 1 should contain key 'A'" );
			assertEquals( false, rev2.getEntries().containsKey( "A" ),
					"Revision 2 should not contain key 'A'" );

			// Verify that key "B" persisted across revisions
			assertEquals( true, rev1.getEntries().containsKey( "B" ),
					"Revision 1 should contain key 'B'" );
			assertEquals( true, rev2.getEntries().containsKey( "B" ),
					"Revision 2 should contain key 'B'" );

			// Verify that key "C" was added in revision 2
			assertEquals( false, rev1.getEntries().containsKey( "C" ),
					"Revision 1 should not contain key 'C'" );
			assertEquals( true, rev2.getEntries().containsKey( "C" ),
					"Revision 2 should contain key 'C'" );
		} );
	}
}
