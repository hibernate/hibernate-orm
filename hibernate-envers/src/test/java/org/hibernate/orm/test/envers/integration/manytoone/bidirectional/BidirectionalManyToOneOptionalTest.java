/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.manytoone.bidirectional;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-8305")
@EnversTest
@Jpa(annotatedClasses = {
		BiRefingOptionalEntity.class,
		BiRefedOptionalEntity.class
})
public class BidirectionalManyToOneOptionalTest {
	private Integer refingWithNoRefedId;
	private Integer refingId;
	private Integer refedId;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			// store refing with null refed entity
			BiRefingOptionalEntity refingWithNoRefed = new BiRefingOptionalEntity();
			refingWithNoRefed.setReference( null );
			em.persist( refingWithNoRefed );

			// store refing with non-null refed entity
			BiRefingOptionalEntity refing = new BiRefingOptionalEntity();
			BiRefedOptionalEntity refed = new BiRefedOptionalEntity();
			refed.getReferences().add( refing );
			refing.setReference( refed );
			em.persist( refing );
			em.persist( refed );

			this.refingId = refing.getId();
			this.refedId = refed.getId();
			this.refingWithNoRefedId = refingWithNoRefed.getId();
		} );
	}

	@Test
	public void testRevisionCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( 1, auditReader.getRevisions( BiRefingOptionalEntity.class, refingId ).size() );
			assertEquals( 1, auditReader.getRevisions( BiRefingOptionalEntity.class, refingWithNoRefedId ).size() );
			assertEquals( 1, auditReader.getRevisions( BiRefedOptionalEntity.class, refedId ).size() );
		} );
	}

	@Test
	public void testRevisionHistoryNullReference(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			BiRefingOptionalEntity rev1 = auditReader.find( BiRefingOptionalEntity.class, refingWithNoRefedId, 1 );
			assertNull( rev1.getReference() );
		} );
	}

	@Test
	public void testRevisionHistoryWithNonNullReference(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertNotNull( auditReader.find( BiRefingOptionalEntity.class, refingId, 1 ).getReference() );
			assertEquals( 1, auditReader.find( BiRefedOptionalEntity.class, refedId, 1 ).getReferences().size() );
		} );
	}
}
