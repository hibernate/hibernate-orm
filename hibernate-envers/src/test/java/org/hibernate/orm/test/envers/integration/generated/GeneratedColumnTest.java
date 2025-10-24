/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.generated;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.AssertionsKt.assertNull;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-10841")
@EnversTest
@Jpa(annotatedClasses = {SimpleEntity.class})
public class GeneratedColumnTest {
	private Integer entityId;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			// Revision 1
			SimpleEntity se = new SimpleEntity();
			se.setData( "data" );
			entityManager.getTransaction().begin();
			entityManager.persist( se );
			entityManager.getTransaction().commit();
			entityManager.clear();
			entityId = se.getId();

			// Revision 2
			entityManager.getTransaction().begin();
			se = entityManager.find( SimpleEntity.class, se.getId() );
			se.setData( "data2" );
			entityManager.merge( se );
			entityManager.getTransaction().commit();

			// Revision 3
			entityManager.getTransaction().begin();
			se = entityManager.find( SimpleEntity.class, se.getId() );
			entityManager.remove( se );
			entityManager.getTransaction().commit();
		} );
	}

	@Test
	public void getRevisionCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( 3, auditReader.getRevisions( SimpleEntity.class, entityId ).size() );
		} );
	}

	@Test
	public void testRevisionHistory(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			final var auditReader = AuditReaderFactory.get( entityManager );

			// revision - insertion
			final SimpleEntity rev1 = auditReader.find( SimpleEntity.class, entityId, 1 );
			assertEquals( "data", rev1.getData() );
			assertEquals( 1, rev1.getCaseNumberInsert() );

			// revision - update
			final SimpleEntity rev2 = auditReader.find( SimpleEntity.class, entityId, 2 );
			assertEquals( "data2", rev2.getData() );
			assertEquals( 1, rev2.getCaseNumberInsert() );

			// revision - deletion
			final SimpleEntity rev3 = auditReader.find( SimpleEntity.class, entityId, 3 );
			assertNull( rev3 );
		} );
	}
}
