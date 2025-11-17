/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetoone.unidirectional;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
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
@Jpa(annotatedClasses = {UniRefEdEntity.class, UniRefIngEntity.class})
public class UnidirectionalWithNulls {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			UniRefEdEntity ed1 = new UniRefEdEntity( 1, "data_ed_1" );
			UniRefEdEntity ed2 = new UniRefEdEntity( 2, "data_ed_2" );

			UniRefIngEntity ing1 = new UniRefIngEntity( 3, "data_ing_1", ed1 );
			UniRefIngEntity ing2 = new UniRefIngEntity( 4, "data_ing_2", null );

			em.persist( ed1 );
			em.persist( ed2 );

			em.persist( ing1 );
			em.persist( ing2 );

			ed1_id = ed1.getId();
			ed2_id = ed2.getId();

			ing1_id = ing1.getId();
			ing2_id = ing2.getId();
		} );

		// Revision 2
		scope.inTransaction( em -> {
			UniRefIngEntity ing1 = em.find( UniRefIngEntity.class, ing1_id );
			ing1.setReference( null );
		} );

		// Revision 3
		scope.inTransaction( em -> {
			UniRefIngEntity ing2 = em.find( UniRefIngEntity.class, ing2_id );
			UniRefEdEntity ed2 = em.find( UniRefEdEntity.class, ed2_id );

			ing2.setReference( ed2 );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( UniRefEdEntity.class, ed1_id ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( UniRefEdEntity.class, ed2_id ) );

			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( UniRefIngEntity.class, ing1_id ) );
			assertEquals( Arrays.asList( 1, 3 ), auditReader.getRevisions( UniRefIngEntity.class, ing2_id ) );
		} );
	}

	@Test
	public void testHistoryOfIngId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			UniRefEdEntity ed1 = em.find( UniRefEdEntity.class, ed1_id );

			UniRefIngEntity rev1 = auditReader.find( UniRefIngEntity.class, ing1_id, 1 );
			UniRefIngEntity rev2 = auditReader.find( UniRefIngEntity.class, ing1_id, 2 );
			UniRefIngEntity rev3 = auditReader.find( UniRefIngEntity.class, ing1_id, 3 );

			assertEquals( ed1, rev1.getReference() );
			assertNull( rev2.getReference() );
			assertNull( rev3.getReference() );
		} );
	}

	@Test
	public void testHistoryOfIngId2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			UniRefEdEntity ed2 = em.find( UniRefEdEntity.class, ed2_id );

			UniRefIngEntity rev1 = auditReader.find( UniRefIngEntity.class, ing2_id, 1 );
			UniRefIngEntity rev2 = auditReader.find( UniRefIngEntity.class, ing2_id, 2 );
			UniRefIngEntity rev3 = auditReader.find( UniRefIngEntity.class, ing2_id, 3 );

			assertNull( rev1.getReference() );
			assertNull( rev2.getReference() );
			assertEquals( ed2, rev3.getReference() );
		} );
	}
}
