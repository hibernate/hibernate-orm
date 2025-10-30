/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.manytomany;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.IntNoAutoIdTestEntity;
import org.hibernate.orm.test.envers.entities.manytomany.WhereJoinTableEntity;
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
@Jpa(annotatedClasses = {WhereJoinTableEntity.class, IntNoAutoIdTestEntity.class})
public class BasicWhereJoinTable {
	private Integer ite1_1_id;
	private Integer ite1_2_id;
	private Integer ite2_1_id;
	private Integer ite2_2_id;

	private Integer wjte1_id;
	private Integer wjte2_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			IntNoAutoIdTestEntity ite1_1 = new IntNoAutoIdTestEntity( 1, 10 );
			IntNoAutoIdTestEntity ite1_2 = new IntNoAutoIdTestEntity( 1, 11 );
			IntNoAutoIdTestEntity ite2_1 = new IntNoAutoIdTestEntity( 2, 20 );
			IntNoAutoIdTestEntity ite2_2 = new IntNoAutoIdTestEntity( 2, 21 );

			WhereJoinTableEntity wjte1 = new WhereJoinTableEntity();
			wjte1.setData( "wjte1" );

			WhereJoinTableEntity wjte2 = new WhereJoinTableEntity();
			wjte2.setData( "wjte2" );

			em.persist( ite1_1 );
			em.persist( ite1_2 );
			em.persist( ite2_1 );
			em.persist( ite2_2 );
			em.persist( wjte1 );
			em.persist( wjte2 );

			ite1_1_id = ite1_1.getId();
			ite1_2_id = ite1_2.getId();
			ite2_1_id = ite2_1.getId();
			ite2_2_id = ite2_2.getId();
			wjte1_id = wjte1.getId();
			wjte2_id = wjte2.getId();
		} );

		// Revision 2 (wjte1: 1_1, 2_1)
		scope.inTransaction( em -> {
			WhereJoinTableEntity wjte1 = em.find( WhereJoinTableEntity.class, wjte1_id );
			IntNoAutoIdTestEntity ite1_1 = em.find( IntNoAutoIdTestEntity.class, ite1_1_id );
			IntNoAutoIdTestEntity ite2_1 = em.find( IntNoAutoIdTestEntity.class, ite2_1_id );

			wjte1.getReferences1().add( ite1_1 );
			wjte1.getReferences2().add( ite2_1 );
		} );

		// Revision 3 (wjte1: 1_1, 2_1; wjte2: 1_1, 1_2)
		scope.inTransaction( em -> {
			WhereJoinTableEntity wjte2 = em.find( WhereJoinTableEntity.class, wjte2_id );
			IntNoAutoIdTestEntity ite1_1 = em.find( IntNoAutoIdTestEntity.class, ite1_1_id );
			IntNoAutoIdTestEntity ite1_2 = em.find( IntNoAutoIdTestEntity.class, ite1_2_id );

			wjte2.getReferences1().add( ite1_1 );
			wjte2.getReferences1().add( ite1_2 );
		} );

		// Revision 4 (wjte1: 2_1; wjte2: 1_1, 1_2, 2_2)
		scope.inTransaction( em -> {
			WhereJoinTableEntity wjte1 = em.find( WhereJoinTableEntity.class, wjte1_id );
			WhereJoinTableEntity wjte2 = em.find( WhereJoinTableEntity.class, wjte2_id );
			IntNoAutoIdTestEntity ite1_1 = em.find( IntNoAutoIdTestEntity.class, ite1_1_id );
			IntNoAutoIdTestEntity ite2_2 = em.find( IntNoAutoIdTestEntity.class, ite2_2_id );

			wjte1.getReferences1().remove( ite1_1 );
			wjte2.getReferences2().add( ite2_2 );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2, 4 ), auditReader.getRevisions( WhereJoinTableEntity.class, wjte1_id ) );
			assertEquals( Arrays.asList( 1, 3, 4 ), auditReader.getRevisions( WhereJoinTableEntity.class, wjte2_id ) );

			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( IntNoAutoIdTestEntity.class, ite1_1_id ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( IntNoAutoIdTestEntity.class, ite1_2_id ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( IntNoAutoIdTestEntity.class, ite2_1_id ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( IntNoAutoIdTestEntity.class, ite2_2_id ) );
		} );
	}

	@Test
	public void testHistoryOfWjte1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			IntNoAutoIdTestEntity ite1_1 = em.find( IntNoAutoIdTestEntity.class, ite1_1_id );
			IntNoAutoIdTestEntity ite2_1 = em.find( IntNoAutoIdTestEntity.class, ite2_1_id );

			WhereJoinTableEntity rev1 = auditReader.find( WhereJoinTableEntity.class, wjte1_id, 1 );
			WhereJoinTableEntity rev2 = auditReader.find( WhereJoinTableEntity.class, wjte1_id, 2 );
			WhereJoinTableEntity rev3 = auditReader.find( WhereJoinTableEntity.class, wjte1_id, 3 );
			WhereJoinTableEntity rev4 = auditReader.find( WhereJoinTableEntity.class, wjte1_id, 4 );

			// Checking 1st list
			assertTrue( TestTools.checkCollection( rev1.getReferences1() ) );
			assertTrue( TestTools.checkCollection( rev2.getReferences1(), ite1_1 ) );
			assertTrue( TestTools.checkCollection( rev3.getReferences1(), ite1_1 ) );
			assertTrue( TestTools.checkCollection( rev4.getReferences1() ) );

			// Checking 2nd list
			assertTrue( TestTools.checkCollection( rev1.getReferences2() ) );
			assertTrue( TestTools.checkCollection( rev2.getReferences2(), ite2_1 ) );
			assertTrue( TestTools.checkCollection( rev3.getReferences2(), ite2_1 ) );
			assertTrue( TestTools.checkCollection( rev4.getReferences2(), ite2_1 ) );
		} );
	}

	@Test
	public void testHistoryOfWjte2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			IntNoAutoIdTestEntity ite1_1 = em.find( IntNoAutoIdTestEntity.class, ite1_1_id );
			IntNoAutoIdTestEntity ite1_2 = em.find( IntNoAutoIdTestEntity.class, ite1_2_id );
			IntNoAutoIdTestEntity ite2_2 = em.find( IntNoAutoIdTestEntity.class, ite2_2_id );

			WhereJoinTableEntity rev1 = auditReader.find( WhereJoinTableEntity.class, wjte2_id, 1 );
			WhereJoinTableEntity rev2 = auditReader.find( WhereJoinTableEntity.class, wjte2_id, 2 );
			WhereJoinTableEntity rev3 = auditReader.find( WhereJoinTableEntity.class, wjte2_id, 3 );
			WhereJoinTableEntity rev4 = auditReader.find( WhereJoinTableEntity.class, wjte2_id, 4 );

			// Checking 1st list
			assertTrue( TestTools.checkCollection( rev1.getReferences1() ) );
			assertTrue( TestTools.checkCollection( rev2.getReferences1() ) );
			assertTrue( TestTools.checkCollection( rev3.getReferences1(), ite1_1, ite1_2 ) );
			assertTrue( TestTools.checkCollection( rev4.getReferences1(), ite1_1, ite1_2 ) );

			// Checking 2nd list
			assertTrue( TestTools.checkCollection( rev1.getReferences2() ) );
			assertTrue( TestTools.checkCollection( rev2.getReferences2() ) );
			assertTrue( TestTools.checkCollection( rev3.getReferences2() ) );
			assertTrue( TestTools.checkCollection( rev4.getReferences2(), ite2_2 ) );
		} );
	}
}
