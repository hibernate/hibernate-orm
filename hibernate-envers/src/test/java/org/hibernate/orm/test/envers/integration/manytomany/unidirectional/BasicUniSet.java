/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.manytomany.unidirectional;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.entities.manytomany.unidirectional.SetUniEntity;
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
@Jpa(annotatedClasses = {SetUniEntity.class, StrTestEntity.class})
public class BasicUniSet {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			StrTestEntity ed1 = new StrTestEntity( "data_ed_1" );
			StrTestEntity ed2 = new StrTestEntity( "data_ed_2" );

			SetUniEntity ing1 = new SetUniEntity( 3, "data_ing_1" );
			SetUniEntity ing2 = new SetUniEntity( 4, "data_ing_2" );

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
			SetUniEntity ing1 = em.find( SetUniEntity.class, ing1_id );
			SetUniEntity ing2 = em.find( SetUniEntity.class, ing2_id );
			StrTestEntity ed1 = em.find( StrTestEntity.class, ed1_id );
			StrTestEntity ed2 = em.find( StrTestEntity.class, ed2_id );

			ing1.setReferences( new HashSet<StrTestEntity>() );
			ing1.getReferences().add( ed1 );

			ing2.setReferences( new HashSet<StrTestEntity>() );
			ing2.getReferences().add( ed1 );
			ing2.getReferences().add( ed2 );
		} );

		// Revision 3
		scope.inTransaction( em -> {
			SetUniEntity ing1 = em.find( SetUniEntity.class, ing1_id );
			StrTestEntity ed2 = em.find( StrTestEntity.class, ed2_id );

			ing1.getReferences().add( ed2 );
		} );

		// Revision 4
		scope.inTransaction( em -> {
			SetUniEntity ing1 = em.find( SetUniEntity.class, ing1_id );
			StrTestEntity ed1 = em.find( StrTestEntity.class, ed1_id );

			ing1.getReferences().remove( ed1 );
		} );

		// Revision 5
		scope.inTransaction( em -> {
			SetUniEntity ing1 = em.find( SetUniEntity.class, ing1_id );

			ing1.setReferences( null );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( StrTestEntity.class, ed1_id ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( StrTestEntity.class, ed2_id ) );

			assertEquals( Arrays.asList( 1, 2, 3, 4, 5 ), auditReader.getRevisions( SetUniEntity.class, ing1_id ) );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( SetUniEntity.class, ing2_id ) );
		} );
	}

	@Test
	public void testHistoryOfEdIng1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			StrTestEntity ed1 = em.find( StrTestEntity.class, ed1_id );
			StrTestEntity ed2 = em.find( StrTestEntity.class, ed2_id );

			SetUniEntity rev1 = auditReader.find( SetUniEntity.class, ing1_id, 1 );
			SetUniEntity rev2 = auditReader.find( SetUniEntity.class, ing1_id, 2 );
			SetUniEntity rev3 = auditReader.find( SetUniEntity.class, ing1_id, 3 );
			SetUniEntity rev4 = auditReader.find( SetUniEntity.class, ing1_id, 4 );
			SetUniEntity rev5 = auditReader.find( SetUniEntity.class, ing1_id, 5 );

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
			StrTestEntity ed1 = em.find( StrTestEntity.class, ed1_id );
			StrTestEntity ed2 = em.find( StrTestEntity.class, ed2_id );

			SetUniEntity rev1 = auditReader.find( SetUniEntity.class, ing2_id, 1 );
			SetUniEntity rev2 = auditReader.find( SetUniEntity.class, ing2_id, 2 );
			SetUniEntity rev3 = auditReader.find( SetUniEntity.class, ing2_id, 3 );
			SetUniEntity rev4 = auditReader.find( SetUniEntity.class, ing2_id, 4 );
			SetUniEntity rev5 = auditReader.find( SetUniEntity.class, ing2_id, 5 );

			assertEquals( Collections.EMPTY_SET, rev1.getReferences() );
			assertEquals( TestTools.makeSet( ed1, ed2 ), rev2.getReferences() );
			assertEquals( TestTools.makeSet( ed1, ed2 ), rev3.getReferences() );
			assertEquals( TestTools.makeSet( ed1, ed2 ), rev4.getReferences() );
			assertEquals( TestTools.makeSet( ed1, ed2 ), rev5.getReferences() );
		} );
	}
}
