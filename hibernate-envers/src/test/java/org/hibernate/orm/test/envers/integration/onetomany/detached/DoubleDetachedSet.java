/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany.detached;

import java.util.Arrays;
import java.util.HashSet;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.entities.onetomany.detached.DoubleSetRefCollEntity;
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
@Jpa(annotatedClasses = {StrTestEntity.class, DoubleSetRefCollEntity.class})
public class DoubleDetachedSet {
	private Integer str1_id;
	private Integer str2_id;

	private Integer coll1_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		StrTestEntity str1 = new StrTestEntity( "str1" );
		StrTestEntity str2 = new StrTestEntity( "str2" );

		DoubleSetRefCollEntity coll1 = new DoubleSetRefCollEntity( 3, "coll1" );

		// Revision 1
		scope.inTransaction( em -> {
			em.persist( str1 );
			em.persist( str2 );

			coll1.setCollection( new HashSet<StrTestEntity>() );
			coll1.getCollection().add( str1 );
			em.persist( coll1 );

			coll1.setCollection2( new HashSet<StrTestEntity>() );
			coll1.getCollection2().add( str2 );
			em.persist( coll1 );
		} );

		// Revision 2
		scope.inTransaction( em -> {
			StrTestEntity str2Ref = em.find( StrTestEntity.class, str2.getId() );
			DoubleSetRefCollEntity coll1Ref = em.find( DoubleSetRefCollEntity.class, coll1.getId() );

			coll1Ref.getCollection().add( str2Ref );
		} );

		// Revision 3
		scope.inTransaction( em -> {
			StrTestEntity str1Ref = em.find( StrTestEntity.class, str1.getId() );
			DoubleSetRefCollEntity coll1Ref = em.find( DoubleSetRefCollEntity.class, coll1.getId() );

			coll1Ref.getCollection().remove( str1Ref );
			coll1Ref.getCollection2().add( str1Ref );
		} );

		str1_id = str1.getId();
		str2_id = str2.getId();
		coll1_id = coll1.getId();
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals(
					Arrays.asList( 1, 2, 3 ),
					auditReader.getRevisions( DoubleSetRefCollEntity.class, coll1_id )
			);
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( StrTestEntity.class, str1_id ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( StrTestEntity.class, str2_id ) );
		} );
	}

	@Test
	public void testHistoryOfColl1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			StrTestEntity str1 = em.find( StrTestEntity.class, str1_id );
			StrTestEntity str2 = em.find( StrTestEntity.class, str2_id );

			DoubleSetRefCollEntity rev1 = auditReader.find( DoubleSetRefCollEntity.class, coll1_id, 1 );
			DoubleSetRefCollEntity rev2 = auditReader.find( DoubleSetRefCollEntity.class, coll1_id, 2 );
			DoubleSetRefCollEntity rev3 = auditReader.find( DoubleSetRefCollEntity.class, coll1_id, 3 );

			assertEquals( rev1.getCollection(), TestTools.makeSet( str1 ) );
			assertEquals( rev2.getCollection(), TestTools.makeSet( str1, str2 ) );
			assertEquals( rev3.getCollection(), TestTools.makeSet( str2 ) );

			assertEquals( rev1.getCollection2(), TestTools.makeSet( str2 ) );
			assertEquals( rev2.getCollection2(), TestTools.makeSet( str2 ) );
			assertEquals( rev3.getCollection2(), TestTools.makeSet( str1, str2 ) );
		} );
	}
}
