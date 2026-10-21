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
 * Additional test for HHH-20926 covering value changes for existing keys.
 *
 * This test verifies that when a map value is changed for an existing key,
 * the audit history correctly reflects the change. This ensures that
 * MapCollectionMapper.isSame() properly compares both keys AND values.
 *
 * @author Chikumar
 */
@EnversTest
@Jpa(annotatedClasses = {BigDecimalMapEntity.class})
public class BigDecimalMapValueChangeTest {
	private BigDecimal entity_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		entity_id = new BigDecimal( "99999.99" );

		// Revision 1: Persist entity with initial map entries
		scope.inTransaction( em -> {
			BigDecimalMapEntity entity = new BigDecimalMapEntity( entity_id );
			entity.getEntries().put( "key1", "value1" );
			entity.getEntries().put( "key2", "value2" );
			em.persist( entity );
		} );

		// Revision 2: Change value for existing key
		scope.inTransaction( em -> {
			BigDecimalMapEntity entity = em.find( BigDecimalMapEntity.class, entity_id );
			entity.getEntries().put( "key1", "updated_value1" );
		} );

		// Revision 3: Add new key and change another value
		scope.inTransaction( em -> {
			BigDecimalMapEntity entity = em.find( BigDecimalMapEntity.class, entity_id );
			entity.getEntries().put( "key3", "value3" );
			entity.getEntries().put( "key2", "updated_value2" );
		} );

		// No revision: Set the same value again (no change)
		scope.inTransaction( em -> {
			BigDecimalMapEntity entity = em.find( BigDecimalMapEntity.class, entity_id );
			entity.getEntries().put( "key2", "updated_value2" );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2, 3 ), auditReader.getRevisions( BigDecimalMapEntity.class, entity_id ),
					"Entity should have exactly 3 revisions (no revision for setting same value)" );
		} );
	}

	@Test
	public void testRevision1Content(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			BigDecimalMapEntity rev1 = auditReader.find( BigDecimalMapEntity.class, entity_id, 1 );

			assertEquals( TestTools.makeMap( "key1", "value1", "key2", "value2" ), rev1.getEntries(),
					"Revision 1 should contain original values" );
		} );
	}

	@Test
	public void testRevision2Content(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			BigDecimalMapEntity rev2 = auditReader.find( BigDecimalMapEntity.class, entity_id, 2 );

			assertEquals( TestTools.makeMap( "key1", "updated_value1", "key2", "value2" ), rev2.getEntries(),
					"Revision 2 should reflect updated value for key1" );
		} );
	}

	@Test
	public void testRevision3Content(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			BigDecimalMapEntity rev3 = auditReader.find( BigDecimalMapEntity.class, entity_id, 3 );

			assertEquals( TestTools.makeMap( "key1", "updated_value1", "key2", "updated_value2", "key3", "value3" ),
					rev3.getEntries(),
					"Revision 3 should reflect new key3 and updated value for key2" );
		} );
	}
}
