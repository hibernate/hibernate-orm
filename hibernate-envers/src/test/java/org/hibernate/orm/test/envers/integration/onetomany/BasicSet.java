/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany;

import java.util.Arrays;
import java.util.Collections;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.onetomany.SetRefEdEntity;
import org.hibernate.orm.test.envers.entities.onetomany.SetRefIngEntity;
import org.hibernate.orm.test.envers.tools.TestTools;
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
@Jpa(annotatedClasses = {SetRefEdEntity.class, SetRefIngEntity.class})
public class BasicSet {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			SetRefEdEntity ed1 = new SetRefEdEntity( 1, "data_ed_1" );
			SetRefEdEntity ed2 = new SetRefEdEntity( 2, "data_ed_2" );

			em.persist( ed1 );
			em.persist( ed2 );

			ed1_id = ed1.getId();
			ed2_id = ed2.getId();
		} );

		scope.inTransaction( em -> {
			SetRefEdEntity ed1 = em.find( SetRefEdEntity.class, ed1_id );

			SetRefIngEntity ing1 = new SetRefIngEntity( 3, "data_ing_1" );
			SetRefIngEntity ing2 = new SetRefIngEntity( 4, "data_ing_2" );

			ing1.setReference( ed1 );
			ing2.setReference( ed1 );

			em.persist( ing1 );
			em.persist( ing2 );

			ing1_id = ing1.getId();
			ing2_id = ing2.getId();
		} );

		scope.inTransaction( em -> {
			SetRefIngEntity ing1 = em.find( SetRefIngEntity.class, ing1_id );
			SetRefEdEntity ed2 = em.find( SetRefEdEntity.class, ed2_id );

			ing1.setReference( ed2 );
		} );

		scope.inTransaction( em -> {
			SetRefIngEntity ing2 = em.find( SetRefIngEntity.class, ing2_id );
			SetRefEdEntity ed2 = em.find( SetRefEdEntity.class, ed2_id );

			ing2.setReference( ed2 );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2, 3, 4 ), auditReader.getRevisions( SetRefEdEntity.class, ed1_id ) );
			assertEquals( Arrays.asList( 1, 3, 4 ), auditReader.getRevisions( SetRefEdEntity.class, ed2_id ) );

			assertEquals( Arrays.asList( 2, 3 ), auditReader.getRevisions( SetRefIngEntity.class, ing1_id ) );
			assertEquals( Arrays.asList( 2, 4 ), auditReader.getRevisions( SetRefIngEntity.class, ing2_id ) );
		} );
	}

	@Test
	public void testHistoryOfEdId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			SetRefIngEntity ing1 = em.find( SetRefIngEntity.class, ing1_id );
			SetRefIngEntity ing2 = em.find( SetRefIngEntity.class, ing2_id );

			SetRefEdEntity rev1 = auditReader.find( SetRefEdEntity.class, ed1_id, 1 );
			SetRefEdEntity rev2 = auditReader.find( SetRefEdEntity.class, ed1_id, 2 );
			SetRefEdEntity rev3 = auditReader.find( SetRefEdEntity.class, ed1_id, 3 );
			SetRefEdEntity rev4 = auditReader.find( SetRefEdEntity.class, ed1_id, 4 );

			assertEquals( Collections.EMPTY_SET, rev1.getReffering() );
			assertEquals( TestTools.makeSet( ing1, ing2 ), rev2.getReffering() );
			assertEquals( TestTools.makeSet( ing2 ), rev3.getReffering() );
			assertEquals( Collections.EMPTY_SET, rev4.getReffering() );
		} );
	}

	@Test
	public void testHistoryOfEdId2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			SetRefIngEntity ing1 = em.find( SetRefIngEntity.class, ing1_id );
			SetRefIngEntity ing2 = em.find( SetRefIngEntity.class, ing2_id );

			SetRefEdEntity rev1 = auditReader.find( SetRefEdEntity.class, ed2_id, 1 );
			SetRefEdEntity rev2 = auditReader.find( SetRefEdEntity.class, ed2_id, 2 );
			SetRefEdEntity rev3 = auditReader.find( SetRefEdEntity.class, ed2_id, 3 );
			SetRefEdEntity rev4 = auditReader.find( SetRefEdEntity.class, ed2_id, 4 );

			assertEquals( Collections.EMPTY_SET, rev1.getReffering() );
			assertEquals( Collections.EMPTY_SET, rev2.getReffering() );
			assertEquals( TestTools.makeSet( ing1 ), rev3.getReffering() );
			assertEquals( TestTools.makeSet( ing1, ing2 ), rev4.getReffering() );
		} );
	}

	@Test
	public void testHistoryOfEdIng1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			SetRefEdEntity ed1 = em.find( SetRefEdEntity.class, ed1_id );
			SetRefEdEntity ed2 = em.find( SetRefEdEntity.class, ed2_id );

			SetRefIngEntity rev1 = auditReader.find( SetRefIngEntity.class, ing1_id, 1 );
			SetRefIngEntity rev2 = auditReader.find( SetRefIngEntity.class, ing1_id, 2 );
			SetRefIngEntity rev3 = auditReader.find( SetRefIngEntity.class, ing1_id, 3 );
			SetRefIngEntity rev4 = auditReader.find( SetRefIngEntity.class, ing1_id, 4 );

			assertNull( rev1 );
			assertEquals( ed1, rev2.getReference() );
			assertEquals( ed2, rev3.getReference() );
			assertEquals( ed2, rev4.getReference() );
		} );
	}

	@Test
	public void testHistoryOfEdIng2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			SetRefEdEntity ed1 = em.find( SetRefEdEntity.class, ed1_id );
			SetRefEdEntity ed2 = em.find( SetRefEdEntity.class, ed2_id );

			SetRefIngEntity rev1 = auditReader.find( SetRefIngEntity.class, ing2_id, 1 );
			SetRefIngEntity rev2 = auditReader.find( SetRefIngEntity.class, ing2_id, 2 );
			SetRefIngEntity rev3 = auditReader.find( SetRefIngEntity.class, ing2_id, 3 );
			SetRefIngEntity rev4 = auditReader.find( SetRefIngEntity.class, ing2_id, 4 );

			assertNull( rev1 );
			assertEquals( ed1, rev2.getReference() );
			assertEquals( ed1, rev3.getReference() );
			assertEquals( ed2, rev4.getReference() );
		} );
	}
}
