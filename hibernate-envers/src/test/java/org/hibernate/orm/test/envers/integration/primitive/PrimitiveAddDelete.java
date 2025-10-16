/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.primitive;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.PrimitiveTestEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {PrimitiveTestEntity.class})
public class PrimitiveAddDelete {
	private Integer id1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			PrimitiveTestEntity pte = new PrimitiveTestEntity( 10, 11 );
			em.persist( pte );
			id1 = pte.getId();
		} );

		// Revision 2
		scope.inTransaction( em -> {
			PrimitiveTestEntity pte = em.find( PrimitiveTestEntity.class, id1 );
			pte.setNumVal1( 20 );
			pte.setNumVal2( 21 );
		} );

		// Revision 3
		scope.inTransaction( em -> {
			PrimitiveTestEntity pte = em.find( PrimitiveTestEntity.class, id1 );
			em.remove( pte );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2, 3 ), auditReader.getRevisions( PrimitiveTestEntity.class, id1 ) );
		} );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			PrimitiveTestEntity ver1 = new PrimitiveTestEntity( id1, 10, 0 );
			PrimitiveTestEntity ver2 = new PrimitiveTestEntity( id1, 20, 0 );

			assertEquals( ver1, auditReader.find( PrimitiveTestEntity.class, id1, 1 ) );
			assertEquals( ver2, auditReader.find( PrimitiveTestEntity.class, id1, 2 ) );
			assertNull( auditReader.find( PrimitiveTestEntity.class, id1, 3 ) );
		} );
	}

	@Test
	public void testQueryWithDeleted(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			// Selecting all entities, also the deleted ones
			var entities = auditReader.createQuery().forRevisionsOfEntity( PrimitiveTestEntity.class, true, true )
					.getResultList();

			assertEquals( 3, entities.size() );
			assertEquals( new PrimitiveTestEntity( id1, 10, 0 ), entities.get( 0 ) );
			assertEquals( new PrimitiveTestEntity( id1, 20, 0 ), entities.get( 1 ) );
			assertEquals( new PrimitiveTestEntity( id1, 0, 0 ), entities.get( 2 ) );
		} );
	}
}
