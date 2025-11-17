/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.onetomany.ListRefEdEntity;
import org.hibernate.orm.test.envers.entities.onetomany.ListRefIngEntity;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {ListRefEdEntity.class, ListRefIngEntity.class})
public class BasicList {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			// Revision 1
			ListRefEdEntity ed1 = new ListRefEdEntity( 1, "data_ed_1" );
			ListRefEdEntity ed2 = new ListRefEdEntity( 2, "data_ed_2" );

			ListRefIngEntity ing1 = new ListRefIngEntity( 3, "data_ing_1", ed1 );
			ListRefIngEntity ing2 = new ListRefIngEntity( 4, "data_ing_2", ed1 );

			em.persist( ed1 );
			em.persist( ed2 );

			em.persist( ing1 );
			em.persist( ing2 );

			ed1_id = ed1.getId();
			ed2_id = ed2.getId();

			ing1_id = ing1.getId();
			ing2_id = ing2.getId();
		} );

		scope.inTransaction( em -> {
			// Revision 2
			ListRefIngEntity ing1 = em.find( ListRefIngEntity.class, ing1_id );
			ListRefEdEntity ed2 = em.find( ListRefEdEntity.class, ed2_id );

			ing1.setReference( ed2 );
		} );

		scope.inTransaction( em -> {
			// Revision 3
			ListRefIngEntity ing2 = em.find( ListRefIngEntity.class, ing2_id );
			ListRefEdEntity ed2 = em.find( ListRefEdEntity.class, ed2_id );

			ing2.setReference( ed2 );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2, 3 ), auditReader.getRevisions( ListRefEdEntity.class, ed1_id ) );
			assertEquals( Arrays.asList( 1, 2, 3 ), auditReader.getRevisions( ListRefEdEntity.class, ed2_id ) );

			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( ListRefIngEntity.class, ing1_id ) );
			assertEquals( Arrays.asList( 1, 3 ), auditReader.getRevisions( ListRefIngEntity.class, ing2_id ) );
		} );
	}

	@Test
	public void testHistoryOfEdId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			ListRefIngEntity ing1 = em.find( ListRefIngEntity.class, ing1_id );
			ListRefIngEntity ing2 = em.find( ListRefIngEntity.class, ing2_id );

			ListRefEdEntity rev1 = auditReader.find( ListRefEdEntity.class, ed1_id, 1 );
			ListRefEdEntity rev2 = auditReader.find( ListRefEdEntity.class, ed1_id, 2 );
			ListRefEdEntity rev3 = auditReader.find( ListRefEdEntity.class, ed1_id, 3 );

			assertTrue( TestTools.checkCollection( rev1.getReffering(), ing1, ing2 ) );
			assertTrue( TestTools.checkCollection( rev2.getReffering(), ing2 ) );
			assertTrue( TestTools.checkCollection( rev3.getReffering() ) );
		} );
	}

	@Test
	public void testHistoryOfEdId2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			ListRefIngEntity ing1 = em.find( ListRefIngEntity.class, ing1_id );
			ListRefIngEntity ing2 = em.find( ListRefIngEntity.class, ing2_id );

			ListRefEdEntity rev1 = auditReader.find( ListRefEdEntity.class, ed2_id, 1 );
			ListRefEdEntity rev2 = auditReader.find( ListRefEdEntity.class, ed2_id, 2 );
			ListRefEdEntity rev3 = auditReader.find( ListRefEdEntity.class, ed2_id, 3 );

			assertTrue( TestTools.checkCollection( rev1.getReffering() ) );
			assertTrue( TestTools.checkCollection( rev2.getReffering(), ing1 ) );
			assertTrue( TestTools.checkCollection( rev3.getReffering(), ing1, ing2 ) );
		} );
	}

	@Test
	public void testHistoryOfEdIng1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			ListRefEdEntity ed1 = em.find( ListRefEdEntity.class, ed1_id );
			ListRefEdEntity ed2 = em.find( ListRefEdEntity.class, ed2_id );

			ListRefIngEntity rev1 = auditReader.find( ListRefIngEntity.class, ing1_id, 1 );
			ListRefIngEntity rev2 = auditReader.find( ListRefIngEntity.class, ing1_id, 2 );
			ListRefIngEntity rev3 = auditReader.find( ListRefIngEntity.class, ing1_id, 3 );

			assertEquals( ed1, rev1.getReference() );
			assertEquals( ed2, rev2.getReference() );
			assertEquals( ed2, rev3.getReference() );
		} );
	}

	@Test
	public void testHistoryOfEdIng2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			ListRefEdEntity ed1 = em.find( ListRefEdEntity.class, ed1_id );
			ListRefEdEntity ed2 = em.find( ListRefEdEntity.class, ed2_id );

			ListRefIngEntity rev1 = auditReader.find( ListRefIngEntity.class, ing2_id, 1 );
			ListRefIngEntity rev2 = auditReader.find( ListRefIngEntity.class, ing2_id, 2 );
			ListRefIngEntity rev3 = auditReader.find( ListRefIngEntity.class, ing2_id, 3 );

			assertEquals( ed1, rev1.getReference() );
			assertEquals( ed1, rev2.getReference() );
			assertEquals( ed2, rev3.getReference() );
		} );
	}
}
