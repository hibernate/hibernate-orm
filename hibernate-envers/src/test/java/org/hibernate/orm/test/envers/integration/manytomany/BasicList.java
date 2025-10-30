/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.manytomany;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.manytomany.ListOwnedEntity;
import org.hibernate.orm.test.envers.entities.manytomany.ListOwningEntity;
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
@Jpa(annotatedClasses = {ListOwningEntity.class, ListOwnedEntity.class})
public class BasicList {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			ListOwnedEntity ed1 = new ListOwnedEntity( 1, "data_ed_1" );
			ListOwnedEntity ed2 = new ListOwnedEntity( 2, "data_ed_2" );

			ListOwningEntity ing1 = new ListOwningEntity( 3, "data_ing_1" );
			ListOwningEntity ing2 = new ListOwningEntity( 4, "data_ing_2" );

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
			ListOwningEntity ing1 = em.find( ListOwningEntity.class, ing1_id );
			ListOwningEntity ing2 = em.find( ListOwningEntity.class, ing2_id );
			ListOwnedEntity ed1 = em.find( ListOwnedEntity.class, ed1_id );
			ListOwnedEntity ed2 = em.find( ListOwnedEntity.class, ed2_id );

			ing1.setReferences( new ArrayList<ListOwnedEntity>() );
			ing1.getReferences().add( ed1 );

			ing2.setReferences( new ArrayList<ListOwnedEntity>() );
			ing2.getReferences().add( ed1 );
			ing2.getReferences().add( ed2 );
		} );

		// Revision 3
		scope.inTransaction( em -> {
			ListOwningEntity ing1 = em.find( ListOwningEntity.class, ing1_id );
			ListOwnedEntity ed2 = em.find( ListOwnedEntity.class, ed2_id );

			ing1.getReferences().add( ed2 );
		} );

		// Revision 4
		scope.inTransaction( em -> {
			ListOwningEntity ing1 = em.find( ListOwningEntity.class, ing1_id );
			ListOwnedEntity ed1 = em.find( ListOwnedEntity.class, ed1_id );

			ing1.getReferences().remove( ed1 );
		} );

		// Revision 5
		scope.inTransaction( em -> {
			ListOwningEntity ing1 = em.find( ListOwningEntity.class, ing1_id );

			ing1.setReferences( null );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2, 4 ), auditReader.getRevisions( ListOwnedEntity.class, ed1_id ) );
			assertEquals( Arrays.asList( 1, 2, 3, 5 ), auditReader.getRevisions( ListOwnedEntity.class, ed2_id ) );

			assertEquals( Arrays.asList( 1, 2, 3, 4, 5 ),
					auditReader.getRevisions( ListOwningEntity.class, ing1_id ) );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( ListOwningEntity.class, ing2_id ) );
		} );
	}

	@Test
	public void testHistoryOfEdId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			ListOwningEntity ing1 = em.find( ListOwningEntity.class, ing1_id );
			ListOwningEntity ing2 = em.find( ListOwningEntity.class, ing2_id );

			ListOwnedEntity rev1 = auditReader.find( ListOwnedEntity.class, ed1_id, 1 );
			ListOwnedEntity rev2 = auditReader.find( ListOwnedEntity.class, ed1_id, 2 );
			ListOwnedEntity rev3 = auditReader.find( ListOwnedEntity.class, ed1_id, 3 );
			ListOwnedEntity rev4 = auditReader.find( ListOwnedEntity.class, ed1_id, 4 );
			ListOwnedEntity rev5 = auditReader.find( ListOwnedEntity.class, ed1_id, 5 );

			assertEquals( Collections.EMPTY_LIST, rev1.getReferencing() );
			assert TestTools.checkCollection( rev2.getReferencing(), ing1, ing2 );
			assert TestTools.checkCollection( rev3.getReferencing(), ing1, ing2 );
			assert TestTools.checkCollection( rev4.getReferencing(), ing2 );
			assert TestTools.checkCollection( rev5.getReferencing(), ing2 );
		} );
	}

	@Test
	public void testHistoryOfEdId2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			ListOwningEntity ing1 = em.find( ListOwningEntity.class, ing1_id );
			ListOwningEntity ing2 = em.find( ListOwningEntity.class, ing2_id );

			ListOwnedEntity rev1 = auditReader.find( ListOwnedEntity.class, ed2_id, 1 );
			ListOwnedEntity rev2 = auditReader.find( ListOwnedEntity.class, ed2_id, 2 );
			ListOwnedEntity rev3 = auditReader.find( ListOwnedEntity.class, ed2_id, 3 );
			ListOwnedEntity rev4 = auditReader.find( ListOwnedEntity.class, ed2_id, 4 );
			ListOwnedEntity rev5 = auditReader.find( ListOwnedEntity.class, ed2_id, 5 );

			assertEquals( Collections.EMPTY_LIST, rev1.getReferencing() );
			assert TestTools.checkCollection( rev2.getReferencing(), ing2 );
			assert TestTools.checkCollection( rev3.getReferencing(), ing1, ing2 );
			assert TestTools.checkCollection( rev4.getReferencing(), ing1, ing2 );
			assert TestTools.checkCollection( rev5.getReferencing(), ing2 );
		} );
	}

	@Test
	public void testHistoryOfEdIng1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			ListOwnedEntity ed1 = em.find( ListOwnedEntity.class, ed1_id );
			ListOwnedEntity ed2 = em.find( ListOwnedEntity.class, ed2_id );

			ListOwningEntity rev1 = auditReader.find( ListOwningEntity.class, ing1_id, 1 );
			ListOwningEntity rev2 = auditReader.find( ListOwningEntity.class, ing1_id, 2 );
			ListOwningEntity rev3 = auditReader.find( ListOwningEntity.class, ing1_id, 3 );
			ListOwningEntity rev4 = auditReader.find( ListOwningEntity.class, ing1_id, 4 );
			ListOwningEntity rev5 = auditReader.find( ListOwningEntity.class, ing1_id, 5 );

			assertEquals( Collections.EMPTY_LIST, rev1.getReferences() );
			assert TestTools.checkCollection( rev2.getReferences(), ed1 );
			assert TestTools.checkCollection( rev3.getReferences(), ed1, ed2 );
			assert TestTools.checkCollection( rev4.getReferences(), ed2 );
			assertEquals( Collections.EMPTY_LIST, rev5.getReferences() );
		} );
	}

	@Test
	public void testHistoryOfEdIng2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			ListOwnedEntity ed1 = em.find( ListOwnedEntity.class, ed1_id );
			ListOwnedEntity ed2 = em.find( ListOwnedEntity.class, ed2_id );

			ListOwningEntity rev1 = auditReader.find( ListOwningEntity.class, ing2_id, 1 );
			ListOwningEntity rev2 = auditReader.find( ListOwningEntity.class, ing2_id, 2 );
			ListOwningEntity rev3 = auditReader.find( ListOwningEntity.class, ing2_id, 3 );
			ListOwningEntity rev4 = auditReader.find( ListOwningEntity.class, ing2_id, 4 );
			ListOwningEntity rev5 = auditReader.find( ListOwningEntity.class, ing2_id, 5 );

			assertEquals( Collections.EMPTY_LIST, rev1.getReferences() );
			assert TestTools.checkCollection( rev2.getReferences(), ed1, ed2 );
			assert TestTools.checkCollection( rev3.getReferences(), ed1, ed2 );
			assert TestTools.checkCollection( rev4.getReferences(), ed1, ed2 );
			assert TestTools.checkCollection( rev5.getReferences(), ed1, ed2 );
		} );
	}
}
