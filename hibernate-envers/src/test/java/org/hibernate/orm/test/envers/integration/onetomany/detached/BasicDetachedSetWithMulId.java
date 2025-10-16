/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany.detached;

import java.util.Arrays;
import java.util.HashSet;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.ids.MulId;
import org.hibernate.orm.test.envers.entities.ids.MulIdTestEntity;
import org.hibernate.orm.test.envers.entities.onetomany.detached.ids.SetRefCollEntityMulId;
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
@Jpa(annotatedClasses = {MulIdTestEntity.class, SetRefCollEntityMulId.class})
public class BasicDetachedSetWithMulId {
	private MulId str1_id;
	private MulId str2_id;

	private MulId coll1_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		str1_id = new MulId( 1, 2 );
		str2_id = new MulId( 3, 4 );
		coll1_id = new MulId( 5, 6 );

		// Revision 1
		scope.inTransaction( em -> {
			MulIdTestEntity str1 = new MulIdTestEntity( str1_id.getId1(), str1_id.getId2(), "str1" );
			MulIdTestEntity str2 = new MulIdTestEntity( str2_id.getId1(), str2_id.getId2(), "str2" );

			em.persist( str1 );
			em.persist( str2 );

			SetRefCollEntityMulId coll1 = new SetRefCollEntityMulId( coll1_id.getId1(), coll1_id.getId2(), "coll1" );
			coll1.setCollection( new HashSet<MulIdTestEntity>() );
			coll1.getCollection().add( str1 );
			em.persist( coll1 );
		} );

		// Revision 2
		scope.inTransaction( em -> {
			MulIdTestEntity str2 = em.find( MulIdTestEntity.class, str2_id );
			SetRefCollEntityMulId coll1 = em.find( SetRefCollEntityMulId.class, coll1_id );

			coll1.getCollection().add( str2 );
		} );

		// Revision 3
		scope.inTransaction( em -> {
			MulIdTestEntity str1 = em.find( MulIdTestEntity.class, str1_id );
			SetRefCollEntityMulId coll1 = em.find( SetRefCollEntityMulId.class, coll1_id );

			coll1.getCollection().remove( str1 );
		} );

		// Revision 4
		scope.inTransaction( em -> {
			SetRefCollEntityMulId coll1 = em.find( SetRefCollEntityMulId.class, coll1_id );

			coll1.getCollection().clear();
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2, 3, 4 ),
					auditReader.getRevisions( SetRefCollEntityMulId.class, coll1_id ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( MulIdTestEntity.class, str1_id ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( MulIdTestEntity.class, str2_id ) );
		} );
	}

	@Test
	public void testHistoryOfColl1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			MulIdTestEntity str1 = em.find( MulIdTestEntity.class, str1_id );
			MulIdTestEntity str2 = em.find( MulIdTestEntity.class, str2_id );

			final var auditReader = AuditReaderFactory.get( em );
			SetRefCollEntityMulId rev1 = auditReader.find( SetRefCollEntityMulId.class, coll1_id, 1 );
			SetRefCollEntityMulId rev2 = auditReader.find( SetRefCollEntityMulId.class, coll1_id, 2 );
			SetRefCollEntityMulId rev3 = auditReader.find( SetRefCollEntityMulId.class, coll1_id, 3 );
			SetRefCollEntityMulId rev4 = auditReader.find( SetRefCollEntityMulId.class, coll1_id, 4 );

			assertEquals( TestTools.makeSet( str1 ), rev1.getCollection() );
			assertEquals( TestTools.makeSet( str1, str2 ), rev2.getCollection() );
			assertEquals( TestTools.makeSet( str2 ), rev3.getCollection() );
			assertEquals( TestTools.makeSet(), rev4.getCollection() );

			assertEquals( "coll1", rev1.getData() );
			assertEquals( "coll1", rev2.getData() );
			assertEquals( "coll1", rev3.getData() );
			assertEquals( "coll1", rev4.getData() );
		} );
	}
}
