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
import org.hibernate.orm.test.envers.entities.ids.MulId;
import org.hibernate.orm.test.envers.entities.onetomany.ids.SetRefEdMulIdEntity;
import org.hibernate.orm.test.envers.entities.onetomany.ids.SetRefIngMulIdEntity;
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
@Jpa(annotatedClasses = {SetRefEdMulIdEntity.class, SetRefIngMulIdEntity.class})
public class BasicSetWithMulId {
	private MulId ed1_id;
	private MulId ed2_id;

	private MulId ing1_id;
	private MulId ing2_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		ed1_id = new MulId( 0, 1 );
		ed2_id = new MulId( 2, 3 );

		ing2_id = new MulId( 4, 5 );
		ing1_id = new MulId( 6, 7 );

		scope.inTransaction( em -> {
			SetRefEdMulIdEntity ed1 = new SetRefEdMulIdEntity( ed1_id.getId1(), ed1_id.getId2(), "data_ed_1" );
			SetRefEdMulIdEntity ed2 = new SetRefEdMulIdEntity( ed2_id.getId1(), ed2_id.getId2(), "data_ed_2" );

			SetRefIngMulIdEntity ing1 = new SetRefIngMulIdEntity( ing1_id.getId1(), ing1_id.getId2(), "data_ing_1", ed1 );
			SetRefIngMulIdEntity ing2 = new SetRefIngMulIdEntity( ing2_id.getId1(), ing2_id.getId2(), "data_ing_2", ed1 );

			em.persist( ed1 );
			em.persist( ed2 );

			em.persist( ing1 );
			em.persist( ing2 );
		} );

		scope.inTransaction( em -> {
			SetRefIngMulIdEntity ing1 = em.find( SetRefIngMulIdEntity.class, ing1_id );
			SetRefEdMulIdEntity ed2 = em.find( SetRefEdMulIdEntity.class, ed2_id );

			ing1.setReference( ed2 );
		} );

		scope.inTransaction( em -> {
			SetRefIngMulIdEntity ing2 = em.find( SetRefIngMulIdEntity.class, ing2_id );
			SetRefEdMulIdEntity ed2 = em.find( SetRefEdMulIdEntity.class, ed2_id );

			ing2.setReference( ed2 );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2, 3 ), auditReader.getRevisions( SetRefEdMulIdEntity.class, ed1_id ) );
			assertEquals( Arrays.asList( 1, 2, 3 ), auditReader.getRevisions( SetRefEdMulIdEntity.class, ed2_id ) );

			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( SetRefIngMulIdEntity.class, ing1_id ) );
			assertEquals( Arrays.asList( 1, 3 ), auditReader.getRevisions( SetRefIngMulIdEntity.class, ing2_id ) );
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
			SetRefIngMulIdEntity ing1 = em.find( SetRefIngMulIdEntity.class, ing1_id );
			SetRefIngMulIdEntity ing2 = em.find( SetRefIngMulIdEntity.class, ing2_id );

			SetRefEdMulIdEntity rev1 = auditReader.find( SetRefEdMulIdEntity.class, ed1_id, 1 );
			SetRefEdMulIdEntity rev2 = auditReader.find( SetRefEdMulIdEntity.class, ed1_id, 2 );
			SetRefEdMulIdEntity rev3 = auditReader.find( SetRefEdMulIdEntity.class, ed1_id, 3 );

			assertEquals( makeSet( ing1, ing2 ), rev1.getReffering() );
			assertEquals( makeSet( ing2 ), rev2.getReffering() );
			assertEquals( Collections.EMPTY_SET, rev3.getReffering() );
		} );
	}

	@Test
	public void testHistoryOfEdId2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			SetRefIngMulIdEntity ing1 = em.find( SetRefIngMulIdEntity.class, ing1_id );
			SetRefIngMulIdEntity ing2 = em.find( SetRefIngMulIdEntity.class, ing2_id );

			SetRefEdMulIdEntity rev1 = auditReader.find( SetRefEdMulIdEntity.class, ed2_id, 1 );
			SetRefEdMulIdEntity rev2 = auditReader.find( SetRefEdMulIdEntity.class, ed2_id, 2 );
			SetRefEdMulIdEntity rev3 = auditReader.find( SetRefEdMulIdEntity.class, ed2_id, 3 );

			assertEquals( Collections.EMPTY_SET, rev1.getReffering() );
			assertEquals( makeSet( ing1 ), rev2.getReffering() );
			assertEquals( makeSet( ing1, ing2 ), rev3.getReffering() );
		} );
	}

	@Test
	public void testHistoryOfEdIng1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			SetRefEdMulIdEntity ed1 = em.find( SetRefEdMulIdEntity.class, ed1_id );
			SetRefEdMulIdEntity ed2 = em.find( SetRefEdMulIdEntity.class, ed2_id );

			SetRefIngMulIdEntity rev1 = auditReader.find( SetRefIngMulIdEntity.class, ing1_id, 1 );
			SetRefIngMulIdEntity rev2 = auditReader.find( SetRefIngMulIdEntity.class, ing1_id, 2 );
			SetRefIngMulIdEntity rev3 = auditReader.find( SetRefIngMulIdEntity.class, ing1_id, 3 );

			assertEquals( ed1, rev1.getReference() );
			assertEquals( ed2, rev2.getReference() );
			assertEquals( ed2, rev3.getReference() );
		} );
	}

	@Test
	public void testHistoryOfEdIng2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			SetRefEdMulIdEntity ed1 = em.find( SetRefEdMulIdEntity.class, ed1_id );
			SetRefEdMulIdEntity ed2 = em.find( SetRefEdMulIdEntity.class, ed2_id );

			SetRefIngMulIdEntity rev1 = auditReader.find( SetRefIngMulIdEntity.class, ing2_id, 1 );
			SetRefIngMulIdEntity rev2 = auditReader.find( SetRefIngMulIdEntity.class, ing2_id, 2 );
			SetRefIngMulIdEntity rev3 = auditReader.find( SetRefIngMulIdEntity.class, ing2_id, 3 );

			assertEquals( ed1, rev1.getReference() );
			assertEquals( ed1, rev2.getReference() );
			assertEquals( ed2, rev3.getReference() );
		} );
	}
}
