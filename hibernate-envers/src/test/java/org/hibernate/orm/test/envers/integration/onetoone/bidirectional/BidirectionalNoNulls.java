/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetoone.bidirectional;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {BiRefEdEntity.class, BiRefIngEntity.class})
public class BidirectionalNoNulls {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		BiRefEdEntity ed1 = new BiRefEdEntity( 1, "data_ed_1" );
		BiRefEdEntity ed2 = new BiRefEdEntity( 2, "data_ed_2" );

		BiRefIngEntity ing1 = new BiRefIngEntity( 3, "data_ing_1" );
		BiRefIngEntity ing2 = new BiRefIngEntity( 4, "data_ing_2" );

		// Revision 1
		scope.inTransaction( em -> {
			ing1.setReference( ed1 );
			ing2.setReference( ed2 );

			em.persist( ed1 );
			em.persist( ed2 );

			em.persist( ing1 );
			em.persist( ing2 );
		} );

		// Revision 2
		scope.inTransaction( em -> {
			BiRefIngEntity ing1Ref = em.find( BiRefIngEntity.class, ing1.getId() );
			BiRefIngEntity ing2Ref = em.find( BiRefIngEntity.class, ing2.getId() );

			BiRefEdEntity ed1Ref = em.find( BiRefEdEntity.class, ed1.getId() );
			BiRefEdEntity ed2Ref = em.find( BiRefEdEntity.class, ed2.getId() );

			ing1Ref.setReference( ed2Ref );
			ing2Ref.setReference( ed1Ref );
		} );

		//

		ed1_id = ed1.getId();
		ed2_id = ed2.getId();

		ing1_id = ing1.getId();
		ing2_id = ing2.getId();
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( BiRefEdEntity.class, ed1_id ) );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( BiRefEdEntity.class, ed2_id ) );

			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( BiRefIngEntity.class, ing1_id ) );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( BiRefIngEntity.class, ing2_id ) );
		} );
	}

	@Test
	public void testHistoryOfEdId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			BiRefIngEntity ing1 = em.find( BiRefIngEntity.class, ing1_id );
			BiRefIngEntity ing2 = em.find( BiRefIngEntity.class, ing2_id );

			BiRefEdEntity rev1 = auditReader.find( BiRefEdEntity.class, ed1_id, 1 );
			BiRefEdEntity rev2 = auditReader.find( BiRefEdEntity.class, ed1_id, 2 );

			assertEquals( ing1, rev1.getReferencing() );
			assertEquals( ing2, rev2.getReferencing() );
		} );
	}

	@Test
	public void testHistoryOfEdId2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			BiRefIngEntity ing1 = em.find( BiRefIngEntity.class, ing1_id );
			BiRefIngEntity ing2 = em.find( BiRefIngEntity.class, ing2_id );

			BiRefEdEntity rev1 = auditReader.find( BiRefEdEntity.class, ed2_id, 1 );
			BiRefEdEntity rev2 = auditReader.find( BiRefEdEntity.class, ed2_id, 2 );

			assertEquals( ing2, rev1.getReferencing() );
			assertEquals( ing1, rev2.getReferencing() );
		} );
	}
}
