/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.manytomany;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.manytomany.SetOwnedEntity;
import org.hibernate.orm.test.envers.entities.manytomany.SetOwningEntity;
import org.hibernate.orm.test.envers.tools.TestTools;
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
@Jpa(annotatedClasses = {SetOwningEntity.class, SetOwnedEntity.class})
public class BasicSet {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			SetOwnedEntity ed1 = new SetOwnedEntity( 1, "data_ed_1" );
			SetOwnedEntity ed2 = new SetOwnedEntity( 2, "data_ed_2" );

			SetOwningEntity ing1 = new SetOwningEntity( 3, "data_ing_1" );
			SetOwningEntity ing2 = new SetOwningEntity( 4, "data_ing_2" );

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
			SetOwningEntity ing1 = em.find( SetOwningEntity.class, ing1_id );
			SetOwningEntity ing2 = em.find( SetOwningEntity.class, ing2_id );
			SetOwnedEntity ed1 = em.find( SetOwnedEntity.class, ed1_id );
			SetOwnedEntity ed2 = em.find( SetOwnedEntity.class, ed2_id );

			ing1.setReferences( new HashSet<SetOwnedEntity>() );
			ing1.getReferences().add( ed1 );

			ing2.setReferences( new HashSet<SetOwnedEntity>() );
			ing2.getReferences().add( ed1 );
			ing2.getReferences().add( ed2 );
		} );

		// Revision 3
		scope.inTransaction( em -> {
			SetOwningEntity ing1 = em.find( SetOwningEntity.class, ing1_id );
			SetOwnedEntity ed2 = em.find( SetOwnedEntity.class, ed2_id );

			ing1.getReferences().add( ed2 );
		} );

		// Revision 4
		scope.inTransaction( em -> {
			SetOwningEntity ing1 = em.find( SetOwningEntity.class, ing1_id );
			SetOwnedEntity ed1 = em.find( SetOwnedEntity.class, ed1_id );

			ing1.getReferences().remove( ed1 );
		} );

		// Revision 5
		scope.inTransaction( em -> {
			SetOwningEntity ing1 = em.find( SetOwningEntity.class, ing1_id );

			ing1.setReferences( null );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2, 4 ), auditReader.getRevisions( SetOwnedEntity.class, ed1_id ) );
			assertEquals( Arrays.asList( 1, 2, 3, 5 ), auditReader.getRevisions( SetOwnedEntity.class, ed2_id ) );

			assertEquals( Arrays.asList( 1, 2, 3, 4, 5 ), auditReader.getRevisions( SetOwningEntity.class, ing1_id ) );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( SetOwningEntity.class, ing2_id ) );
		} );
	}

	@Test
	public void testHistoryOfEdId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			SetOwningEntity ing1 = em.find( SetOwningEntity.class, ing1_id );
			SetOwningEntity ing2 = em.find( SetOwningEntity.class, ing2_id );

			SetOwnedEntity rev1 = auditReader.find( SetOwnedEntity.class, ed1_id, 1 );
			SetOwnedEntity rev2 = auditReader.find( SetOwnedEntity.class, ed1_id, 2 );
			SetOwnedEntity rev3 = auditReader.find( SetOwnedEntity.class, ed1_id, 3 );
			SetOwnedEntity rev4 = auditReader.find( SetOwnedEntity.class, ed1_id, 4 );
			SetOwnedEntity rev5 = auditReader.find( SetOwnedEntity.class, ed1_id, 5 );

			assertEquals( Collections.EMPTY_SET, rev1.getReferencing() );
			assertEquals( TestTools.makeSet( ing1, ing2 ), rev2.getReferencing() );
			assertEquals( TestTools.makeSet( ing1, ing2 ), rev3.getReferencing() );
			assertEquals( TestTools.makeSet( ing2 ), rev4.getReferencing() );
			assertEquals( TestTools.makeSet( ing2 ), rev5.getReferencing() );
		} );
	}

	@Test
	public void testHistoryOfEdId2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			SetOwningEntity ing1 = em.find( SetOwningEntity.class, ing1_id );
			SetOwningEntity ing2 = em.find( SetOwningEntity.class, ing2_id );

			SetOwnedEntity rev1 = auditReader.find( SetOwnedEntity.class, ed2_id, 1 );
			SetOwnedEntity rev2 = auditReader.find( SetOwnedEntity.class, ed2_id, 2 );
			SetOwnedEntity rev3 = auditReader.find( SetOwnedEntity.class, ed2_id, 3 );
			SetOwnedEntity rev4 = auditReader.find( SetOwnedEntity.class, ed2_id, 4 );
			SetOwnedEntity rev5 = auditReader.find( SetOwnedEntity.class, ed2_id, 5 );

			assertEquals( Collections.EMPTY_SET, rev1.getReferencing() );
			assertEquals( TestTools.makeSet( ing2 ), rev2.getReferencing() );
			assertEquals( TestTools.makeSet( ing1, ing2 ), rev3.getReferencing() );
			assertEquals( TestTools.makeSet( ing1, ing2 ), rev4.getReferencing() );
			assertEquals( TestTools.makeSet( ing2 ), rev5.getReferencing() );
		} );
	}

	@Test
	public void testHistoryOfEdIng1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			SetOwnedEntity ed1 = em.find( SetOwnedEntity.class, ed1_id );
			SetOwnedEntity ed2 = em.find( SetOwnedEntity.class, ed2_id );

			SetOwningEntity rev1 = auditReader.find( SetOwningEntity.class, ing1_id, 1 );
			SetOwningEntity rev2 = auditReader.find( SetOwningEntity.class, ing1_id, 2 );
			SetOwningEntity rev3 = auditReader.find( SetOwningEntity.class, ing1_id, 3 );
			SetOwningEntity rev4 = auditReader.find( SetOwningEntity.class, ing1_id, 4 );
			SetOwningEntity rev5 = auditReader.find( SetOwningEntity.class, ing1_id, 5 );

			assertEquals( Collections.EMPTY_SET, rev1.getReferences() );
			assertEquals( TestTools.makeSet( ed1 ), rev2.getReferences() );
			assertEquals( TestTools.makeSet( ed1, ed2 ), rev3.getReferences() );
			assertEquals( TestTools.makeSet( ed2 ), rev4.getReferences() );
			assertEquals( Collections.EMPTY_SET, rev5.getReferences() );
		} );
	}

	@Test
	public void testHistoryOfEdIng2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			SetOwnedEntity ed1 = em.find( SetOwnedEntity.class, ed1_id );
			SetOwnedEntity ed2 = em.find( SetOwnedEntity.class, ed2_id );

			SetOwningEntity rev1 = auditReader.find( SetOwningEntity.class, ing2_id, 1 );
			SetOwningEntity rev2 = auditReader.find( SetOwningEntity.class, ing2_id, 2 );
			SetOwningEntity rev3 = auditReader.find( SetOwningEntity.class, ing2_id, 3 );
			SetOwningEntity rev4 = auditReader.find( SetOwningEntity.class, ing2_id, 4 );
			SetOwningEntity rev5 = auditReader.find( SetOwningEntity.class, ing2_id, 5 );

			assertEquals( Collections.EMPTY_SET, rev1.getReferences() );
			assertEquals( TestTools.makeSet( ed1, ed2 ), rev2.getReferences() );
			assertEquals( TestTools.makeSet( ed1, ed2 ), rev3.getReferences() );
			assertEquals( TestTools.makeSet( ed1, ed2 ), rev4.getReferences() );
			assertEquals( TestTools.makeSet( ed1, ed2 ), rev5.getReferences() );
		} );
	}
}
