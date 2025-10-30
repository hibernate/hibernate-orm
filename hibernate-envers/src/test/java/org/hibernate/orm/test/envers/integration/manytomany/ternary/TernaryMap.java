/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.manytomany.ternary;

import java.util.Arrays;

import org.hibernate.dialect.HSQLDialect;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.IntTestPrivSeqEntity;
import org.hibernate.orm.test.envers.entities.StrTestPrivSeqEntity;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SkipForDialect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {TernaryMapEntity.class, StrTestPrivSeqEntity.class, IntTestPrivSeqEntity.class})
public class TernaryMap {
	private Integer str1_id;
	private Integer str2_id;

	private Integer int1_id;
	private Integer int2_id;

	private Integer map1_id;
	private Integer map2_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		StrTestPrivSeqEntity str1 = new StrTestPrivSeqEntity( "a" );
		StrTestPrivSeqEntity str2 = new StrTestPrivSeqEntity( "b" );

		IntTestPrivSeqEntity int1 = new IntTestPrivSeqEntity( 1 );
		IntTestPrivSeqEntity int2 = new IntTestPrivSeqEntity( 2 );

		TernaryMapEntity map1 = new TernaryMapEntity();
		TernaryMapEntity map2 = new TernaryMapEntity();

		// Revision 1 (map1: initialy one mapping int1 -> str1, map2: empty)
		scope.inTransaction( em -> {
			em.persist( str1 );
			em.persist( str2 );
			em.persist( int1 );
			em.persist( int2 );

			map1.getMap().put( int1, str1 );

			em.persist( map1 );
			em.persist( map2 );
		} );

		// Revision 2 (map1: replacing the mapping, map2: adding two mappings)
		scope.inTransaction( em -> {
			TernaryMapEntity map1Ref = em.find( TernaryMapEntity.class, map1.getId() );
			TernaryMapEntity map2Ref = em.find( TernaryMapEntity.class, map2.getId() );

			StrTestPrivSeqEntity str1Ref = em.find( StrTestPrivSeqEntity.class, str1.getId() );
			StrTestPrivSeqEntity str2Ref = em.find( StrTestPrivSeqEntity.class, str2.getId() );

			IntTestPrivSeqEntity int1Ref = em.find( IntTestPrivSeqEntity.class, int1.getId() );
			IntTestPrivSeqEntity int2Ref = em.find( IntTestPrivSeqEntity.class, int2.getId() );

			map1Ref.getMap().put( int1Ref, str2Ref );

			map2Ref.getMap().put( int1Ref, str1Ref );
			map2Ref.getMap().put( int2Ref, str1Ref );
		} );

		// Revision 3 (map1: removing a non-existing mapping, adding an existing mapping - no changes, map2: removing a mapping)
		scope.inTransaction( em -> {
			TernaryMapEntity map1Ref = em.find( TernaryMapEntity.class, map1.getId() );
			TernaryMapEntity map2Ref = em.find( TernaryMapEntity.class, map2.getId() );

			StrTestPrivSeqEntity str2Ref = em.find( StrTestPrivSeqEntity.class, str2.getId() );

			IntTestPrivSeqEntity int1Ref = em.find( IntTestPrivSeqEntity.class, int1.getId() );
			IntTestPrivSeqEntity int2Ref = em.find( IntTestPrivSeqEntity.class, int2.getId() );

			map1Ref.getMap().remove( int2Ref );
			map1Ref.getMap().put( int1Ref, str2Ref );

			map2Ref.getMap().remove( int1Ref );
		} );

		// Revision 4 (map1: adding a mapping, map2: adding a mapping)
		scope.inTransaction( em -> {
			TernaryMapEntity map1Ref = em.find( TernaryMapEntity.class, map1.getId() );
			TernaryMapEntity map2Ref = em.find( TernaryMapEntity.class, map2.getId() );

			StrTestPrivSeqEntity str2Ref = em.find( StrTestPrivSeqEntity.class, str2.getId() );

			IntTestPrivSeqEntity int1Ref = em.find( IntTestPrivSeqEntity.class, int1.getId() );
			IntTestPrivSeqEntity int2Ref = em.find( IntTestPrivSeqEntity.class, int2.getId() );

			map1Ref.getMap().put( int2Ref, str2Ref );

			map2Ref.getMap().put( int1Ref, str2Ref );
		} );

		map1_id = map1.getId();
		map2_id = map2.getId();

		str1_id = str1.getId();
		str2_id = str2.getId();

		int1_id = int1.getId();
		int2_id = int2.getId();
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2, 4 ), auditReader.getRevisions( TernaryMapEntity.class, map1_id ) );
			assertEquals( Arrays.asList( 1, 2, 3, 4 ), auditReader.getRevisions( TernaryMapEntity.class, map2_id ) );

			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( StrTestPrivSeqEntity.class, str1_id ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( StrTestPrivSeqEntity.class, str2_id ) );

			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( IntTestPrivSeqEntity.class, int1_id ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( IntTestPrivSeqEntity.class, int2_id ) );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = HSQLDialect.class, reason = "No idea why this fails. Looks like a HSQLDB bug")
	public void testHistoryOfMap1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			StrTestPrivSeqEntity str1 = em.find( StrTestPrivSeqEntity.class, str1_id );
			StrTestPrivSeqEntity str2 = em.find( StrTestPrivSeqEntity.class, str2_id );

			IntTestPrivSeqEntity int1 = em.find( IntTestPrivSeqEntity.class, int1_id );
			IntTestPrivSeqEntity int2 = em.find( IntTestPrivSeqEntity.class, int2_id );

			TernaryMapEntity rev1 = auditReader.find( TernaryMapEntity.class, map1_id, 1 );
			TernaryMapEntity rev2 = auditReader.find( TernaryMapEntity.class, map1_id, 2 );
			TernaryMapEntity rev3 = auditReader.find( TernaryMapEntity.class, map1_id, 3 );
			TernaryMapEntity rev4 = auditReader.find( TernaryMapEntity.class, map1_id, 4 );

			assertEquals( TestTools.makeMap( int1, str1 ), rev1.getMap() );
			assertEquals( TestTools.makeMap( int1, str2 ), rev2.getMap() );
			assertEquals( TestTools.makeMap( int1, str2 ), rev3.getMap() );
			assertEquals( TestTools.makeMap( int1, str2, int2, str2 ), rev4.getMap() );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = HSQLDialect.class, reason = "No idea why this fails. Looks like a HSQLDB bug")
	public void testHistoryOfMap2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			StrTestPrivSeqEntity str1 = em.find( StrTestPrivSeqEntity.class, str1_id );
			StrTestPrivSeqEntity str2 = em.find( StrTestPrivSeqEntity.class, str2_id );

			IntTestPrivSeqEntity int1 = em.find( IntTestPrivSeqEntity.class, int1_id );
			IntTestPrivSeqEntity int2 = em.find( IntTestPrivSeqEntity.class, int2_id );

			TernaryMapEntity rev1 = auditReader.find( TernaryMapEntity.class, map2_id, 1 );
			TernaryMapEntity rev2 = auditReader.find( TernaryMapEntity.class, map2_id, 2 );
			TernaryMapEntity rev3 = auditReader.find( TernaryMapEntity.class, map2_id, 3 );
			TernaryMapEntity rev4 = auditReader.find( TernaryMapEntity.class, map2_id, 4 );

			assert rev1.getMap().equals( TestTools.makeMap() );
			assert rev2.getMap().equals( TestTools.makeMap( int1, str1, int2, str1 ) );
			assert rev3.getMap().equals( TestTools.makeMap( int2, str1 ) );
			assert rev4.getMap().equals( TestTools.makeMap( int1, str2, int2, str1 ) );
		} );
	}
}
