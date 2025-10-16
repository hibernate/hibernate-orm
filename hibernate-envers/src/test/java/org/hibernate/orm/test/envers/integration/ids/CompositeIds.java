/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.ids;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.ids.CustomEnum;
import org.hibernate.orm.test.envers.entities.ids.EmbId;
import org.hibernate.orm.test.envers.entities.ids.EmbIdTestEntity;
import org.hibernate.orm.test.envers.entities.ids.EmbIdWithCustomType;
import org.hibernate.orm.test.envers.entities.ids.EmbIdWithCustomTypeTestEntity;
import org.hibernate.orm.test.envers.entities.ids.MulId;
import org.hibernate.orm.test.envers.entities.ids.MulIdTestEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {EmbIdTestEntity.class, MulIdTestEntity.class, EmbIdWithCustomTypeTestEntity.class})
public class CompositeIds {
	private EmbId id1;
	private EmbId id2;
	private MulId id3;
	private MulId id4;
	private EmbIdWithCustomType id5;
	private EmbIdWithCustomType id6;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		id1 = new EmbId( 1, 2 );
		id2 = new EmbId( 10, 20 );
		id3 = new MulId( 100, 101 );
		id4 = new MulId( 102, 103 );
		id5 = new EmbIdWithCustomType( 25, CustomEnum.NO );
		id6 = new EmbIdWithCustomType( 27, CustomEnum.YES );

		// Revision 1
		scope.inTransaction( em -> {
			em.persist( new EmbIdTestEntity( id1, "x" ) );
			em.persist( new MulIdTestEntity( id3.getId1(), id3.getId2(), "a" ) );
			em.persist( new EmbIdWithCustomTypeTestEntity( id5, "c" ) );
		} );

		// Revision 2
		scope.inTransaction( em -> {
			em.persist( new EmbIdTestEntity( id2, "y" ) );
			em.persist( new MulIdTestEntity( id4.getId1(), id4.getId2(), "b" ) );
			em.persist( new EmbIdWithCustomTypeTestEntity( id6, "d" ) );
		} );

		// Revision 3
		scope.inTransaction( em -> {
			EmbIdTestEntity ete1 = em.find( EmbIdTestEntity.class, id1 );
			EmbIdTestEntity ete2 = em.find( EmbIdTestEntity.class, id2 );
			MulIdTestEntity mte3 = em.find( MulIdTestEntity.class, id3 );
			MulIdTestEntity mte4 = em.find( MulIdTestEntity.class, id4 );
			EmbIdWithCustomTypeTestEntity cte5 = em.find( EmbIdWithCustomTypeTestEntity.class, id5 );
			EmbIdWithCustomTypeTestEntity cte6 = em.find( EmbIdWithCustomTypeTestEntity.class, id6 );

			ete1.setStr1( "x2" );
			ete2.setStr1( "y2" );
			mte3.setStr1( "a2" );
			mte4.setStr1( "b2" );
			cte5.setStr1( "c2" );
			cte6.setStr1( "d2" );
		} );

		// Revision 4
		scope.inTransaction( em -> {
			EmbIdTestEntity ete1 = em.find( EmbIdTestEntity.class, id1 );
			EmbIdTestEntity ete2 = em.find( EmbIdTestEntity.class, id2 );
			MulIdTestEntity mte3 = em.find( MulIdTestEntity.class, id3 );
			EmbIdWithCustomTypeTestEntity cte5 = em.find( EmbIdWithCustomTypeTestEntity.class, id5 );
			EmbIdWithCustomTypeTestEntity cte6 = em.find( EmbIdWithCustomTypeTestEntity.class, id6 );

			em.remove( ete1 );
			em.remove( mte3 );
			em.remove( cte6 );

			ete2.setStr1( "y3" );
			cte5.setStr1( "c3" );
		} );

		// Revision 5
		scope.inTransaction( em -> {
			EmbIdTestEntity ete2 = em.find( EmbIdTestEntity.class, id2 );
			em.remove( ete2 );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 3, 4 ), auditReader.getRevisions( EmbIdTestEntity.class, id1 ) );
			assertEquals( Arrays.asList( 2, 3, 4, 5 ), auditReader.getRevisions( EmbIdTestEntity.class, id2 ) );
			assertEquals( Arrays.asList( 1, 3, 4 ), auditReader.getRevisions( MulIdTestEntity.class, id3 ) );
			assertEquals( Arrays.asList( 2, 3 ), auditReader.getRevisions( MulIdTestEntity.class, id4 ) );
			assertEquals( Arrays.asList( 1, 3, 4 ), auditReader.getRevisions( EmbIdWithCustomTypeTestEntity.class, id5 ) );
			assertEquals( Arrays.asList( 2, 3, 4 ), auditReader.getRevisions( EmbIdWithCustomTypeTestEntity.class, id6 ) );
		} );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			EmbIdTestEntity ver1 = new EmbIdTestEntity( id1, "x" );
			EmbIdTestEntity ver2 = new EmbIdTestEntity( id1, "x2" );

			assertEquals( ver1, auditReader.find( EmbIdTestEntity.class, id1, 1 ) );
			assertEquals( ver1, auditReader.find( EmbIdTestEntity.class, id1, 2 ) );
			assertEquals( ver2, auditReader.find( EmbIdTestEntity.class, id1, 3 ) );
			assertNull( auditReader.find( EmbIdTestEntity.class, id1, 4 ) );
			assertNull( auditReader.find( EmbIdTestEntity.class, id1, 5 ) );
		} );
	}

	@Test
	public void testHistoryOfId2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			EmbIdTestEntity ver1 = new EmbIdTestEntity( id2, "y" );
			EmbIdTestEntity ver2 = new EmbIdTestEntity( id2, "y2" );
			EmbIdTestEntity ver3 = new EmbIdTestEntity( id2, "y3" );

			assertNull( auditReader.find( EmbIdTestEntity.class, id2, 1 ) );
			assertEquals( ver1, auditReader.find( EmbIdTestEntity.class, id2, 2 ) );
			assertEquals( ver2, auditReader.find( EmbIdTestEntity.class, id2, 3 ) );
			assertEquals( ver3, auditReader.find( EmbIdTestEntity.class, id2, 4 ) );
			assertNull( auditReader.find( EmbIdTestEntity.class, id2, 5 ) );
		} );
	}

	@Test
	public void testHistoryOfId3(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			MulIdTestEntity ver1 = new MulIdTestEntity( id3.getId1(), id3.getId2(), "a" );
			MulIdTestEntity ver2 = new MulIdTestEntity( id3.getId1(), id3.getId2(), "a2" );

			assertEquals( ver1, auditReader.find( MulIdTestEntity.class, id3, 1 ) );
			assertEquals( ver1, auditReader.find( MulIdTestEntity.class, id3, 2 ) );
			assertEquals( ver2, auditReader.find( MulIdTestEntity.class, id3, 3 ) );
			assertNull( auditReader.find( MulIdTestEntity.class, id3, 4 ) );
			assertNull( auditReader.find( MulIdTestEntity.class, id3, 5 ) );
		} );
	}

	@Test
	public void testHistoryOfId4(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			MulIdTestEntity ver1 = new MulIdTestEntity( id4.getId1(), id4.getId2(), "b" );
			MulIdTestEntity ver2 = new MulIdTestEntity( id4.getId1(), id4.getId2(), "b2" );

			assertNull( auditReader.find( MulIdTestEntity.class, id4, 1 ) );
			assertEquals( ver1, auditReader.find( MulIdTestEntity.class, id4, 2 ) );
			assertEquals( ver2, auditReader.find( MulIdTestEntity.class, id4, 3 ) );
			assertEquals( ver2, auditReader.find( MulIdTestEntity.class, id4, 4 ) );
			assertEquals( ver2, auditReader.find( MulIdTestEntity.class, id4, 5 ) );
		} );
	}

	@Test
	public void testHistoryOfId5(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			EmbIdWithCustomTypeTestEntity ver1 = new EmbIdWithCustomTypeTestEntity( id5, "c" );
			EmbIdWithCustomTypeTestEntity ver2 = new EmbIdWithCustomTypeTestEntity( id5, "c2" );
			EmbIdWithCustomTypeTestEntity ver3 = new EmbIdWithCustomTypeTestEntity( id5, "c3" );

			assertEquals( ver1, auditReader.find( EmbIdWithCustomTypeTestEntity.class, id5, 1 ) );
			assertEquals( ver1, auditReader.find( EmbIdWithCustomTypeTestEntity.class, id5, 2 ) );
			assertEquals( ver2, auditReader.find( EmbIdWithCustomTypeTestEntity.class, id5, 3 ) );
			assertEquals( ver3, auditReader.find( EmbIdWithCustomTypeTestEntity.class, id5, 4 ) );
			assertEquals( ver3, auditReader.find( EmbIdWithCustomTypeTestEntity.class, id5, 5 ) );
		} );
	}

	@Test
	public void testHistoryOfId6(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			EmbIdWithCustomTypeTestEntity ver1 = new EmbIdWithCustomTypeTestEntity( id6, "d" );
			EmbIdWithCustomTypeTestEntity ver2 = new EmbIdWithCustomTypeTestEntity( id6, "d2" );

			assertNull( auditReader.find( EmbIdWithCustomTypeTestEntity.class, id6, 1 ) );
			assertEquals( ver1, auditReader.find( EmbIdWithCustomTypeTestEntity.class, id6, 2 ) );
			assertEquals( ver2, auditReader.find( EmbIdWithCustomTypeTestEntity.class, id6, 3 ) );
			assertNull( auditReader.find( EmbIdWithCustomTypeTestEntity.class, id6, 4 ) );
			assertNull( auditReader.find( EmbIdWithCustomTypeTestEntity.class, id6, 5 ) );
		} );
	}
}
