/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.manytomany.unidirectional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.entities.manytomany.unidirectional.ListUniEntity;
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
@Jpa(annotatedClasses = {ListUniEntity.class, StrTestEntity.class})
public class BasicUniList {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		StrTestEntity ed1 = new StrTestEntity( "data_ed_1" );
		StrTestEntity ed2 = new StrTestEntity( "data_ed_2" );

		ListUniEntity ing1 = new ListUniEntity( 3, "data_ing_1" );
		ListUniEntity ing2 = new ListUniEntity( 4, "data_ing_2" );

		// Revision 1
		scope.inTransaction( em -> {
			em.persist( ed1 );
			em.persist( ed2 );
			em.persist( ing1 );
			em.persist( ing2 );
		} );

		// Revision 2
		scope.inTransaction( em -> {
			ListUniEntity ing1Ref = em.find( ListUniEntity.class, ing1.getId() );
			ListUniEntity ing2Ref = em.find( ListUniEntity.class, ing2.getId() );
			StrTestEntity ed1Ref = em.find( StrTestEntity.class, ed1.getId() );
			StrTestEntity ed2Ref = em.find( StrTestEntity.class, ed2.getId() );

			ing1Ref.setReferences( new ArrayList<StrTestEntity>() );
			ing1Ref.getReferences().add( ed1Ref );

			ing2Ref.setReferences( new ArrayList<StrTestEntity>() );
			ing2Ref.getReferences().add( ed1Ref );
			ing2Ref.getReferences().add( ed2Ref );
		} );

		// Revision 3
		scope.inTransaction( em -> {
			ListUniEntity ing1Ref = em.find( ListUniEntity.class, ing1.getId() );
			StrTestEntity ed2Ref = em.find( StrTestEntity.class, ed2.getId() );

			ing1Ref.getReferences().add( ed2Ref );
		} );

		// Revision 4
		scope.inTransaction( em -> {
			ListUniEntity ing1Ref = em.find( ListUniEntity.class, ing1.getId() );
			StrTestEntity ed1Ref = em.find( StrTestEntity.class, ed1.getId() );

			ing1Ref.getReferences().remove( ed1Ref );
		} );

		// Revision 5
		scope.inTransaction( em -> {
			ListUniEntity ing1Ref = em.find( ListUniEntity.class, ing1.getId() );

			ing1Ref.setReferences( null );
		} );

		ed1_id = ed1.getId();
		ed2_id = ed2.getId();

		ing1_id = ing1.getId();
		ing2_id = ing2.getId();
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( StrTestEntity.class, ed1_id ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( StrTestEntity.class, ed2_id ) );

			assertEquals( Arrays.asList( 1, 2, 3, 4, 5 ), auditReader.getRevisions( ListUniEntity.class, ing1_id ) );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( ListUniEntity.class, ing2_id ) );
		} );
	}

	@Test
	public void testHistoryOfEdIng1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			StrTestEntity ed1 = em.find( StrTestEntity.class, ed1_id );
			StrTestEntity ed2 = em.find( StrTestEntity.class, ed2_id );

			ListUniEntity rev1 = auditReader.find( ListUniEntity.class, ing1_id, 1 );
			ListUniEntity rev2 = auditReader.find( ListUniEntity.class, ing1_id, 2 );
			ListUniEntity rev3 = auditReader.find( ListUniEntity.class, ing1_id, 3 );
			ListUniEntity rev4 = auditReader.find( ListUniEntity.class, ing1_id, 4 );
			ListUniEntity rev5 = auditReader.find( ListUniEntity.class, ing1_id, 5 );

			assert rev1.getReferences().equals( Collections.EMPTY_LIST );
			assert TestTools.checkCollection( rev2.getReferences(), ed1 );
			assert TestTools.checkCollection( rev3.getReferences(), ed1, ed2 );
			assert TestTools.checkCollection( rev4.getReferences(), ed2 );
			assert rev5.getReferences().equals( Collections.EMPTY_LIST );
		} );
	}

	@Test
	public void testHistoryOfEdIng2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			StrTestEntity ed1 = em.find( StrTestEntity.class, ed1_id );
			StrTestEntity ed2 = em.find( StrTestEntity.class, ed2_id );

			ListUniEntity rev1 = auditReader.find( ListUniEntity.class, ing2_id, 1 );
			ListUniEntity rev2 = auditReader.find( ListUniEntity.class, ing2_id, 2 );
			ListUniEntity rev3 = auditReader.find( ListUniEntity.class, ing2_id, 3 );
			ListUniEntity rev4 = auditReader.find( ListUniEntity.class, ing2_id, 4 );
			ListUniEntity rev5 = auditReader.find( ListUniEntity.class, ing2_id, 5 );

			assert rev1.getReferences().equals( Collections.EMPTY_LIST );
			assert TestTools.checkCollection( rev2.getReferences(), ed1, ed2 );
			assert TestTools.checkCollection( rev3.getReferences(), ed1, ed2 );
			assert TestTools.checkCollection( rev4.getReferences(), ed1, ed2 );
			assert TestTools.checkCollection( rev5.getReferences(), ed1, ed2 );
		} );
	}
}
