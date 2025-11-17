/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetoone.bidirectional.ids;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.ids.EmbId;
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
@Jpa(annotatedClasses = {BiEmbIdRefEdEntity.class, BiEmbIdRefIngEntity.class})
public class EmbIdBidirectional {
	private EmbId ed1_id;
	private EmbId ed2_id;
	private EmbId ing1_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		ed1_id = new EmbId( 1, 2 );
		ed2_id = new EmbId( 3, 4 );
		ing1_id = new EmbId( 5, 6 );

		BiEmbIdRefEdEntity ed1 = new BiEmbIdRefEdEntity( ed1_id, "data_ed_1" );
		BiEmbIdRefEdEntity ed2 = new BiEmbIdRefEdEntity( ed2_id, "data_ed_2" );
		BiEmbIdRefIngEntity ing1 = new BiEmbIdRefIngEntity( ing1_id, "data_ing_1" );

		// Revision 1
		scope.inTransaction( em -> {
			ing1.setReference( ed1 );
			em.persist( ed1 );
			em.persist( ed2 );
			em.persist( ing1 );
		} );

		// Revision 2
		scope.inTransaction( em -> {
			BiEmbIdRefIngEntity ing = em.find( BiEmbIdRefIngEntity.class, ing1_id );
			BiEmbIdRefEdEntity ed = em.find( BiEmbIdRefEdEntity.class, ed2_id );
			ing.setReference( ed );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( BiEmbIdRefEdEntity.class, ed1_id ) );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( BiEmbIdRefEdEntity.class, ed2_id ) );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( BiEmbIdRefIngEntity.class, ing1_id ) );
		} );
	}

	@Test
	public void testHistoryOfEdId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			var auditReader = AuditReaderFactory.get( em );
			BiEmbIdRefIngEntity ing1 = em.find( BiEmbIdRefIngEntity.class, ing1_id );

			BiEmbIdRefEdEntity rev1 = auditReader.find( BiEmbIdRefEdEntity.class, ed1_id, 1 );
			BiEmbIdRefEdEntity rev2 = auditReader.find( BiEmbIdRefEdEntity.class, ed1_id, 2 );

			assertEquals( ing1, rev1.getReferencing() );
			assertNull( rev2.getReferencing() );
		} );
	}

	@Test
	public void testHistoryOfEdId2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			var auditReader = AuditReaderFactory.get( em );
			BiEmbIdRefIngEntity ing1 = em.find( BiEmbIdRefIngEntity.class, ing1_id );

			BiEmbIdRefEdEntity rev1 = auditReader.find( BiEmbIdRefEdEntity.class, ed2_id, 1 );
			BiEmbIdRefEdEntity rev2 = auditReader.find( BiEmbIdRefEdEntity.class, ed2_id, 2 );

			assertNull( rev1.getReferencing() );
			assertEquals( ing1, rev2.getReferencing() );
		} );
	}

	@Test
	public void testHistoryOfIngId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			var auditReader = AuditReaderFactory.get( em );
			BiEmbIdRefEdEntity ed1 = em.find( BiEmbIdRefEdEntity.class, ed1_id );
			BiEmbIdRefEdEntity ed2 = em.find( BiEmbIdRefEdEntity.class, ed2_id );

			BiEmbIdRefIngEntity rev1 = auditReader.find( BiEmbIdRefIngEntity.class, ing1_id, 1 );
			BiEmbIdRefIngEntity rev2 = auditReader.find( BiEmbIdRefIngEntity.class, ing1_id, 2 );

			assertEquals( ed1, rev1.getReference() );
			assertEquals( ed2, rev2.getReference() );
		} );
	}
}
