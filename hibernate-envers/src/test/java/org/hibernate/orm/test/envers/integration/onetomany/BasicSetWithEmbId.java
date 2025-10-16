/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.ids.EmbId;
import org.hibernate.orm.test.envers.entities.onetomany.ids.SetRefEdEmbIdEntity;
import org.hibernate.orm.test.envers.entities.onetomany.ids.SetRefIngEmbIdEntity;
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
@Jpa(annotatedClasses = {SetRefEdEmbIdEntity.class, SetRefIngEmbIdEntity.class})
public class BasicSetWithEmbId {
	private EmbId ed1_id;
	private EmbId ed2_id;

	private EmbId ing1_id;
	private EmbId ing2_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		ed1_id = new EmbId( 0, 1 );
		ed2_id = new EmbId( 2, 3 );

		ing2_id = new EmbId( 4, 5 );
		ing1_id = new EmbId( 6, 7 );

		scope.inTransaction( em -> {
			SetRefEdEmbIdEntity ed1 = new SetRefEdEmbIdEntity( ed1_id, "data_ed_1" );
			SetRefEdEmbIdEntity ed2 = new SetRefEdEmbIdEntity( ed2_id, "data_ed_2" );

			SetRefIngEmbIdEntity ing1 = new SetRefIngEmbIdEntity( ing1_id, "data_ing_1", ed1 );
			SetRefIngEmbIdEntity ing2 = new SetRefIngEmbIdEntity( ing2_id, "data_ing_2", ed1 );

			em.persist( ed1 );
			em.persist( ed2 );

			em.persist( ing1 );
			em.persist( ing2 );
		} );

		scope.inTransaction( em -> {
			SetRefIngEmbIdEntity ing1 = em.find( SetRefIngEmbIdEntity.class, ing1_id );
			SetRefEdEmbIdEntity ed2 = em.find( SetRefEdEmbIdEntity.class, ed2_id );

			ing1.setReference( ed2 );
		} );

		scope.inTransaction( em -> {
			SetRefIngEmbIdEntity ing2 = em.find( SetRefIngEmbIdEntity.class, ing2_id );
			SetRefEdEmbIdEntity ed2 = em.find( SetRefEdEmbIdEntity.class, ed2_id );

			ing2.setReference( ed2 );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2, 3 ), auditReader.getRevisions( SetRefEdEmbIdEntity.class, ed1_id ) );
			assertEquals( Arrays.asList( 1, 2, 3 ), auditReader.getRevisions( SetRefEdEmbIdEntity.class, ed2_id ) );

			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( SetRefIngEmbIdEntity.class, ing1_id ) );
			assertEquals( Arrays.asList( 1, 3 ), auditReader.getRevisions( SetRefIngEmbIdEntity.class, ing2_id ) );
		} );
	}

	private <T> Set<T> makeSet(T... objects) {
		Set<T> ret = new HashSet<T>();
		//noinspection ManualArrayToCollectionCopy
		for ( T obj : objects ) {
			ret.add( obj );
		}
		return ret;
	}

	@Test
	public void testHistoryOfEdId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			SetRefIngEmbIdEntity ing1 = em.find( SetRefIngEmbIdEntity.class, ing1_id );
			SetRefIngEmbIdEntity ing2 = em.find( SetRefIngEmbIdEntity.class, ing2_id );

			SetRefEdEmbIdEntity rev1 = auditReader.find( SetRefEdEmbIdEntity.class, ed1_id, 1 );
			SetRefEdEmbIdEntity rev2 = auditReader.find( SetRefEdEmbIdEntity.class, ed1_id, 2 );
			SetRefEdEmbIdEntity rev3 = auditReader.find( SetRefEdEmbIdEntity.class, ed1_id, 3 );

			assertEquals( makeSet( ing1, ing2 ), rev1.getReffering() );
			assertEquals( makeSet( ing2 ), rev2.getReffering() );
			assertEquals( Collections.EMPTY_SET, rev3.getReffering() );
		} );
	}

	@Test
	public void testHistoryOfEdId2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			SetRefIngEmbIdEntity ing1 = em.find( SetRefIngEmbIdEntity.class, ing1_id );
			SetRefIngEmbIdEntity ing2 = em.find( SetRefIngEmbIdEntity.class, ing2_id );

			SetRefEdEmbIdEntity rev1 = auditReader.find( SetRefEdEmbIdEntity.class, ed2_id, 1 );
			SetRefEdEmbIdEntity rev2 = auditReader.find( SetRefEdEmbIdEntity.class, ed2_id, 2 );
			SetRefEdEmbIdEntity rev3 = auditReader.find( SetRefEdEmbIdEntity.class, ed2_id, 3 );

			assertEquals( Collections.EMPTY_SET, rev1.getReffering() );
			assertEquals( makeSet( ing1 ), rev2.getReffering() );
			assertEquals( makeSet( ing1, ing2 ), rev3.getReffering() );
		} );
	}

	@Test
	public void testHistoryOfEdIng1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			SetRefEdEmbIdEntity ed1 = em.find( SetRefEdEmbIdEntity.class, ed1_id );
			SetRefEdEmbIdEntity ed2 = em.find( SetRefEdEmbIdEntity.class, ed2_id );

			SetRefIngEmbIdEntity rev1 = auditReader.find( SetRefIngEmbIdEntity.class, ing1_id, 1 );
			SetRefIngEmbIdEntity rev2 = auditReader.find( SetRefIngEmbIdEntity.class, ing1_id, 2 );
			SetRefIngEmbIdEntity rev3 = auditReader.find( SetRefIngEmbIdEntity.class, ing1_id, 3 );

			assertEquals( ed1, rev1.getReference() );
			assertEquals( ed2, rev2.getReference() );
			assertEquals( ed2, rev3.getReference() );
		} );
	}

	@Test
	public void testHistoryOfEdIng2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			SetRefEdEmbIdEntity ed1 = em.find( SetRefEdEmbIdEntity.class, ed1_id );
			SetRefEdEmbIdEntity ed2 = em.find( SetRefEdEmbIdEntity.class, ed2_id );

			SetRefIngEmbIdEntity rev1 = auditReader.find( SetRefIngEmbIdEntity.class, ing2_id, 1 );
			SetRefIngEmbIdEntity rev2 = auditReader.find( SetRefIngEmbIdEntity.class, ing2_id, 2 );
			SetRefIngEmbIdEntity rev3 = auditReader.find( SetRefIngEmbIdEntity.class, ing2_id, 3 );

			assertEquals( ed1, rev1.getReference() );
			assertEquals( ed1, rev2.getReference() );
			assertEquals( ed2, rev3.getReference() );
		} );
	}
}
