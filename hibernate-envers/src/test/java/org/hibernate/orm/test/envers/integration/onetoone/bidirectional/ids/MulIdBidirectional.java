/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetoone.bidirectional.ids;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.ids.MulId;
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
@Jpa(annotatedClasses = {BiMulIdRefEdEntity.class, BiMulIdRefIngEntity.class})
public class MulIdBidirectional {
	private MulId ed1_id;
	private MulId ed2_id;
	private MulId ing1_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		ed1_id = new MulId( 1, 2 );
		ed2_id = new MulId( 3, 4 );
		ing1_id = new MulId( 5, 6 );

		BiMulIdRefEdEntity ed1 = new BiMulIdRefEdEntity( ed1_id.getId1(), ed1_id.getId2(), "data_ed_1" );
		BiMulIdRefEdEntity ed2 = new BiMulIdRefEdEntity( ed2_id.getId1(), ed2_id.getId2(), "data_ed_2" );
		BiMulIdRefIngEntity ing1 = new BiMulIdRefIngEntity( ing1_id.getId1(), ing1_id.getId2(), "data_ing_1" );

		// Revision 1
		scope.inTransaction( em -> {
			ing1.setReference( ed1 );
			em.persist( ed1 );
			em.persist( ed2 );
			em.persist( ing1 );
		} );

		// Revision 2
		scope.inTransaction( em -> {
			BiMulIdRefIngEntity ing = em.find( BiMulIdRefIngEntity.class, ing1_id );
			BiMulIdRefEdEntity ed = em.find( BiMulIdRefEdEntity.class, ed2_id );
			ing.setReference( ed );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( BiMulIdRefEdEntity.class, ed1_id ) );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( BiMulIdRefEdEntity.class, ed2_id ) );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( BiMulIdRefIngEntity.class, ing1_id ) );
		} );
	}

	@Test
	public void testHistoryOfEdId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			var auditReader = AuditReaderFactory.get( em );
			BiMulIdRefIngEntity ing1 = em.find( BiMulIdRefIngEntity.class, ing1_id );

			BiMulIdRefEdEntity rev1 = auditReader.find( BiMulIdRefEdEntity.class, ed1_id, 1 );
			BiMulIdRefEdEntity rev2 = auditReader.find( BiMulIdRefEdEntity.class, ed1_id, 2 );

			assertEquals( ing1, rev1.getReferencing() );
			assertNull( rev2.getReferencing() );
		} );
	}

	@Test
	public void testHistoryOfEdId2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			var auditReader = AuditReaderFactory.get( em );
			BiMulIdRefIngEntity ing1 = em.find( BiMulIdRefIngEntity.class, ing1_id );

			BiMulIdRefEdEntity rev1 = auditReader.find( BiMulIdRefEdEntity.class, ed2_id, 1 );
			BiMulIdRefEdEntity rev2 = auditReader.find( BiMulIdRefEdEntity.class, ed2_id, 2 );

			assertNull( rev1.getReferencing() );
			assertEquals( ing1, rev2.getReferencing() );
		} );
	}

	@Test
	public void testHistoryOfIngId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			var auditReader = AuditReaderFactory.get( em );
			BiMulIdRefEdEntity ed1 = em.find( BiMulIdRefEdEntity.class, ed1_id );
			BiMulIdRefEdEntity ed2 = em.find( BiMulIdRefEdEntity.class, ed2_id );

			BiMulIdRefIngEntity rev1 = auditReader.find( BiMulIdRefIngEntity.class, ing1_id, 1 );
			BiMulIdRefIngEntity rev2 = auditReader.find( BiMulIdRefIngEntity.class, ing1_id, 2 );

			assertEquals( ed1, rev1.getReference() );
			assertEquals( ed2, rev2.getReference() );
		} );
	}
}
