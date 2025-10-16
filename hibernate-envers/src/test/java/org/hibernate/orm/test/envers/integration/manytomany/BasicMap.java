/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.manytomany;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.manytomany.MapOwnedEntity;
import org.hibernate.orm.test.envers.entities.manytomany.MapOwningEntity;
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
@Jpa(annotatedClasses = {MapOwningEntity.class, MapOwnedEntity.class})
public class BasicMap {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1 (ing1: initialy empty, ing2: one mapping)
		scope.inTransaction( em -> {
			MapOwnedEntity ed1 = new MapOwnedEntity( 1, "data_ed_1" );
			MapOwnedEntity ed2 = new MapOwnedEntity( 2, "data_ed_2" );

			MapOwningEntity ing1 = new MapOwningEntity( 3, "data_ing_1" );
			MapOwningEntity ing2 = new MapOwningEntity( 4, "data_ing_2" );

			ing2.getReferences().put( "2", ed2 );

			em.persist( ed1 );
			em.persist( ed2 );
			em.persist( ing1 );
			em.persist( ing2 );

			ed1_id = ed1.getId();
			ed2_id = ed2.getId();
			ing1_id = ing1.getId();
			ing2_id = ing2.getId();
		} );

		// Revision 2 (ing1: adding two mappings, ing2: replacing an existing mapping)
		scope.inTransaction( em -> {
			MapOwningEntity ing1 = em.find( MapOwningEntity.class, ing1_id );
			MapOwningEntity ing2 = em.find( MapOwningEntity.class, ing2_id );
			MapOwnedEntity ed1 = em.find( MapOwnedEntity.class, ed1_id );

			ing1.getReferences().put( "1", ed1 );
			ing1.getReferences().put( "2", ed1 );

			ing2.getReferences().put( "2", ed1 );
		} );

		// No revision (ing1: adding an existing mapping, ing2: removing a non existing mapping)
		scope.inTransaction( em -> {
			MapOwningEntity ing1 = em.find( MapOwningEntity.class, ing1_id );
			MapOwningEntity ing2 = em.find( MapOwningEntity.class, ing2_id );
			MapOwnedEntity ed1 = em.find( MapOwnedEntity.class, ed1_id );

			ing1.getReferences().put( "1", ed1 );

			ing2.getReferences().remove( "3" );
		} );

		// Revision 3 (ing1: clearing, ing2: replacing with a new map)
		scope.inTransaction( em -> {
			MapOwningEntity ing1 = em.find( MapOwningEntity.class, ing1_id );
			MapOwningEntity ing2 = em.find( MapOwningEntity.class, ing2_id );
			MapOwnedEntity ed2 = em.find( MapOwnedEntity.class, ed2_id );

			ing1.getReferences().clear();
			ing2.setReferences( new HashMap<String, MapOwnedEntity>() );
			ing2.getReferences().put( "1", ed2 );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2, 3 ), auditReader.getRevisions( MapOwnedEntity.class, ed1_id ) );
			assertEquals( Arrays.asList( 1, 2, 3 ), auditReader.getRevisions( MapOwnedEntity.class, ed2_id ) );

			assertEquals( Arrays.asList( 1, 2, 3 ), auditReader.getRevisions( MapOwningEntity.class, ing1_id ) );
			assertEquals( Arrays.asList( 1, 2, 3 ), auditReader.getRevisions( MapOwningEntity.class, ing2_id ) );
		} );
	}

	@Test
	public void testHistoryOfEdId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			MapOwningEntity ing1 = em.find( MapOwningEntity.class, ing1_id );
			MapOwningEntity ing2 = em.find( MapOwningEntity.class, ing2_id );

			MapOwnedEntity rev1 = auditReader.find( MapOwnedEntity.class, ed1_id, 1 );
			MapOwnedEntity rev2 = auditReader.find( MapOwnedEntity.class, ed1_id, 2 );
			MapOwnedEntity rev3 = auditReader.find( MapOwnedEntity.class, ed1_id, 3 );

			assertEquals( Collections.EMPTY_SET, rev1.getReferencing() );
			assertEquals( TestTools.makeSet( ing1, ing2 ), rev2.getReferencing() );
			assertEquals( Collections.EMPTY_SET, rev3.getReferencing() );
		} );
	}

	@Test
	public void testHistoryOfEdId2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			MapOwningEntity ing2 = em.find( MapOwningEntity.class, ing2_id );

			MapOwnedEntity rev1 = auditReader.find( MapOwnedEntity.class, ed2_id, 1 );
			MapOwnedEntity rev2 = auditReader.find( MapOwnedEntity.class, ed2_id, 2 );
			MapOwnedEntity rev3 = auditReader.find( MapOwnedEntity.class, ed2_id, 3 );

			assertEquals( TestTools.makeSet( ing2 ), rev1.getReferencing() );
			assertEquals( Collections.EMPTY_SET, rev2.getReferencing() );
			assertEquals( TestTools.makeSet( ing2 ), rev3.getReferencing() );
		} );
	}

	@Test
	public void testHistoryOfEdIng1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			MapOwnedEntity ed1 = em.find( MapOwnedEntity.class, ed1_id );

			MapOwningEntity rev1 = auditReader.find( MapOwningEntity.class, ing1_id, 1 );
			MapOwningEntity rev2 = auditReader.find( MapOwningEntity.class, ing1_id, 2 );
			MapOwningEntity rev3 = auditReader.find( MapOwningEntity.class, ing1_id, 3 );

			assertEquals( Collections.EMPTY_MAP, rev1.getReferences() );
			assertEquals( TestTools.makeMap( "1", ed1, "2", ed1 ), rev2.getReferences() );
			assertEquals( Collections.EMPTY_MAP, rev3.getReferences() );
		} );
	}

	@Test
	public void testHistoryOfEdIng2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			MapOwnedEntity ed1 = em.find( MapOwnedEntity.class, ed1_id );
			MapOwnedEntity ed2 = em.find( MapOwnedEntity.class, ed2_id );

			MapOwningEntity rev1 = auditReader.find( MapOwningEntity.class, ing2_id, 1 );
			MapOwningEntity rev2 = auditReader.find( MapOwningEntity.class, ing2_id, 2 );
			MapOwningEntity rev3 = auditReader.find( MapOwningEntity.class, ing2_id, 3 );

			assertEquals( TestTools.makeMap( "2", ed2 ), rev1.getReferences() );
			assertEquals( TestTools.makeMap( "2", ed1 ), rev2.getReferences() );
			assertEquals( TestTools.makeMap( "1", ed2 ), rev3.getReferences() );
		} );
	}
}
