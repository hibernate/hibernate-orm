/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.onetomany.SetRefEdEntity;
import org.hibernate.orm.test.envers.entities.onetomany.SetRefIngEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {SetRefEdEntity.class, SetRefIngEntity.class})
public class BasicSetWithNullsDelete {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;
	private Integer ing2_id;
	private Integer ing3_id;
	private Integer ing4_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			SetRefEdEntity ed1 = new SetRefEdEntity( 1, "data_ed_1" );
			SetRefEdEntity ed2 = new SetRefEdEntity( 2, "data_ed_2" );

			SetRefIngEntity ing1 = new SetRefIngEntity( 3, "data_ing_1", ed1 );
			SetRefIngEntity ing2 = new SetRefIngEntity( 4, "data_ing_2", ed1 );
			SetRefIngEntity ing3 = new SetRefIngEntity( 5, "data_ing_3", ed1 );
			SetRefIngEntity ing4 = new SetRefIngEntity( 6, "data_ing_4", ed1 );

			// Revision 1
			em.getTransaction().begin();

			em.persist( ed1 );
			em.persist( ed2 );

			em.persist( ing1 );
			em.persist( ing2 );
			em.persist( ing3 );
			em.persist( ing4 );

			em.getTransaction().commit();

			// Revision 2
			em.getTransaction().begin();

			ing1 = em.find( SetRefIngEntity.class, ing1.getId() );

			ing1.setReference( null );

			em.getTransaction().commit();

			// Revision 3
			em.getTransaction().begin();

			ing2 = em.find( SetRefIngEntity.class, ing2.getId() );
			em.remove( ing2 );

			em.getTransaction().commit();

			// Revision 4
			em.getTransaction().begin();

			ing3 = em.find( SetRefIngEntity.class, ing3.getId() );
			ed2 = em.find( SetRefEdEntity.class, ed2.getId() );
			ing3.setReference( ed2 );

			em.getTransaction().commit();
			// Revision 5
			em.getTransaction().begin();

			ing4 = em.find( SetRefIngEntity.class, ing4.getId() );
			ed1 = em.find( SetRefEdEntity.class, ed1.getId() );
			em.remove( ed1 );
			ing4.setReference( null );

			em.getTransaction().commit();

			//

			ed1_id = ed1.getId();
			ed2_id = ed2.getId();

			ing1_id = ing1.getId();
			ing2_id = ing2.getId();
			ing3_id = ing3.getId();
			ing4_id = ing4.getId();
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2, 3, 4, 5 ), auditReader.getRevisions( SetRefEdEntity.class, ed1_id ) );
			assertEquals( Arrays.asList( 1, 4 ), auditReader.getRevisions( SetRefEdEntity.class, ed2_id ) );

			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( SetRefIngEntity.class, ing1_id ) );
			assertEquals( Arrays.asList( 1, 3 ), auditReader.getRevisions( SetRefIngEntity.class, ing2_id ) );
			assertEquals( Arrays.asList( 1, 4 ), auditReader.getRevisions( SetRefIngEntity.class, ing3_id ) );
			assertEquals( Arrays.asList( 1, 5 ), auditReader.getRevisions( SetRefIngEntity.class, ing4_id ) );
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
			SetRefIngEntity ing1 = em.find( SetRefIngEntity.class, ing1_id );
			SetRefIngEntity ing2 = new SetRefIngEntity( 4, "data_ing_2", new SetRefEdEntity( 1, "data_ed_1" ) );
			SetRefIngEntity ing3 = em.find( SetRefIngEntity.class, ing3_id );
			SetRefIngEntity ing4 = em.find( SetRefIngEntity.class, ing4_id );

			SetRefEdEntity rev1 = auditReader.find( SetRefEdEntity.class, ed1_id, 1 );
			SetRefEdEntity rev2 = auditReader.find( SetRefEdEntity.class, ed1_id, 2 );
			SetRefEdEntity rev3 = auditReader.find( SetRefEdEntity.class, ed1_id, 3 );
			SetRefEdEntity rev4 = auditReader.find( SetRefEdEntity.class, ed1_id, 4 );
			SetRefEdEntity rev5 = auditReader.find( SetRefEdEntity.class, ed1_id, 5 );

			assertEquals( makeSet( ing1, ing2, ing3, ing4 ), rev1.getReffering() );
			assertEquals( makeSet( ing2, ing3, ing4 ), rev2.getReffering() );
			assertEquals( makeSet( ing3, ing4 ), rev3.getReffering() );
			assertEquals( makeSet( ing4 ), rev4.getReffering() );
			assertNull( rev5 );
		} );
	}

	@Test
	public void testHistoryOfEdId2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			SetRefIngEntity ing3 = em.find( SetRefIngEntity.class, ing3_id );

			SetRefEdEntity rev1 = auditReader.find( SetRefEdEntity.class, ed2_id, 1 );
			SetRefEdEntity rev2 = auditReader.find( SetRefEdEntity.class, ed2_id, 2 );
			SetRefEdEntity rev3 = auditReader.find( SetRefEdEntity.class, ed2_id, 3 );
			SetRefEdEntity rev4 = auditReader.find( SetRefEdEntity.class, ed2_id, 4 );
			SetRefEdEntity rev5 = auditReader.find( SetRefEdEntity.class, ed2_id, 5 );

			assertEquals( Collections.EMPTY_SET, rev1.getReffering() );
			assertEquals( Collections.EMPTY_SET, rev2.getReffering() );
			assertEquals( Collections.EMPTY_SET, rev3.getReffering() );
			assertEquals( makeSet( ing3 ), rev4.getReffering() );
			assertEquals( makeSet( ing3 ), rev5.getReffering() );
		} );
	}

	@Test
	public void testHistoryOfEdIng1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			SetRefEdEntity ed1 = new SetRefEdEntity( 1, "data_ed_1" );

			SetRefIngEntity rev1 = auditReader.find( SetRefIngEntity.class, ing1_id, 1 );
			SetRefIngEntity rev2 = auditReader.find( SetRefIngEntity.class, ing1_id, 2 );
			SetRefIngEntity rev3 = auditReader.find( SetRefIngEntity.class, ing1_id, 3 );
			SetRefIngEntity rev4 = auditReader.find( SetRefIngEntity.class, ing1_id, 4 );
			SetRefIngEntity rev5 = auditReader.find( SetRefIngEntity.class, ing1_id, 5 );

			assertEquals( ed1, rev1.getReference() );
			assertNull( rev2.getReference() );
			assertNull( rev3.getReference() );
			assertNull( rev4.getReference() );
			assertNull( rev5.getReference() );
		} );
	}

	@Test
	public void testHistoryOfEdIng2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			SetRefEdEntity ed1 = new SetRefEdEntity( 1, "data_ed_1" );

			SetRefIngEntity rev1 = auditReader.find( SetRefIngEntity.class, ing2_id, 1 );
			SetRefIngEntity rev2 = auditReader.find( SetRefIngEntity.class, ing2_id, 2 );
			SetRefIngEntity rev3 = auditReader.find( SetRefIngEntity.class, ing2_id, 3 );
			SetRefIngEntity rev4 = auditReader.find( SetRefIngEntity.class, ing2_id, 4 );
			SetRefIngEntity rev5 = auditReader.find( SetRefIngEntity.class, ing2_id, 5 );

			assertEquals( ed1, rev1.getReference() );
			assertEquals( ed1, rev2.getReference() );
			assertNull( rev3 );
			assertNull( rev4 );
			assertNull( rev5 );
		} );
	}

	@Test
	public void testHistoryOfEdIng3(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			SetRefEdEntity ed1 = new SetRefEdEntity( 1, "data_ed_1" );
			SetRefEdEntity ed2 = new SetRefEdEntity( 2, "data_ed_2" );

			SetRefIngEntity rev1 = auditReader.find( SetRefIngEntity.class, ing3_id, 1 );
			SetRefIngEntity rev2 = auditReader.find( SetRefIngEntity.class, ing3_id, 2 );
			SetRefIngEntity rev3 = auditReader.find( SetRefIngEntity.class, ing3_id, 3 );
			SetRefIngEntity rev4 = auditReader.find( SetRefIngEntity.class, ing3_id, 4 );
			SetRefIngEntity rev5 = auditReader.find( SetRefIngEntity.class, ing3_id, 5 );

			assertEquals( ed1, rev1.getReference() );
			assertEquals( ed1, rev2.getReference() );
			assertEquals( ed1, rev3.getReference() );
			assertEquals( ed2, rev4.getReference() );
			assertEquals( ed2, rev5.getReference() );
		} );
	}

	@Test
	public void testHistoryOfEdIng4(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			SetRefEdEntity ed1 = new SetRefEdEntity( 1, "data_ed_1" );

			SetRefIngEntity rev1 = auditReader.find( SetRefIngEntity.class, ing4_id, 1 );
			SetRefIngEntity rev2 = auditReader.find( SetRefIngEntity.class, ing4_id, 2 );
			SetRefIngEntity rev3 = auditReader.find( SetRefIngEntity.class, ing4_id, 3 );
			SetRefIngEntity rev4 = auditReader.find( SetRefIngEntity.class, ing4_id, 4 );
			SetRefIngEntity rev5 = auditReader.find( SetRefIngEntity.class, ing4_id, 5 );

			assertEquals( ed1, rev1.getReference() );
			assertEquals( ed1, rev2.getReference() );
			assertEquals( ed1, rev3.getReference() );
			assertEquals( ed1, rev4.getReference() );
			assertNull( rev5.getReference() );
		} );
	}
}
