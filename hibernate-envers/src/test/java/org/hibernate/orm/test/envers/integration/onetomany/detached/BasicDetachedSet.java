/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany.detached;

import java.util.Arrays;
import java.util.HashSet;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.entities.onetomany.detached.SetRefCollEntity;
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
@Jpa(annotatedClasses = {StrTestEntity.class, SetRefCollEntity.class})
public class BasicDetachedSet {
	private Integer str1_id;
	private Integer str2_id;

	private Integer coll1_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			StrTestEntity str1 = new StrTestEntity( "str1" );
			StrTestEntity str2 = new StrTestEntity( "str2" );

			em.persist( str1 );
			em.persist( str2 );

			SetRefCollEntity coll1 = new SetRefCollEntity( 3, "coll1" );
			coll1.setCollection( new HashSet<StrTestEntity>() );
			coll1.getCollection().add( str1 );
			em.persist( coll1 );

			str1_id = str1.getId();
			str2_id = str2.getId();
			coll1_id = coll1.getId();
		} );

		// Revision 2
		scope.inTransaction( em -> {
			StrTestEntity str2 = em.find( StrTestEntity.class, str2_id );
			SetRefCollEntity coll1 = em.find( SetRefCollEntity.class, coll1_id );

			coll1.getCollection().add( str2 );
		} );

		// Revision 3
		scope.inTransaction( em -> {
			StrTestEntity str1 = em.find( StrTestEntity.class, str1_id );
			SetRefCollEntity coll1 = em.find( SetRefCollEntity.class, coll1_id );

			coll1.getCollection().remove( str1 );
		} );

		// Revision 4
		scope.inTransaction( em -> {
			SetRefCollEntity coll1 = em.find( SetRefCollEntity.class, coll1_id );

			coll1.getCollection().clear();
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2, 3, 4 ), auditReader.getRevisions( SetRefCollEntity.class, coll1_id ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( StrTestEntity.class, str1_id ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( StrTestEntity.class, str2_id ) );
		} );
	}

	@Test
	public void testHistoryOfColl1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			StrTestEntity str1 = em.find( StrTestEntity.class, str1_id );
			StrTestEntity str2 = em.find( StrTestEntity.class, str2_id );

			final var auditReader = AuditReaderFactory.get( em );
			SetRefCollEntity rev1 = auditReader.find( SetRefCollEntity.class, coll1_id, 1 );
			SetRefCollEntity rev2 = auditReader.find( SetRefCollEntity.class, coll1_id, 2 );
			SetRefCollEntity rev3 = auditReader.find( SetRefCollEntity.class, coll1_id, 3 );
			SetRefCollEntity rev4 = auditReader.find( SetRefCollEntity.class, coll1_id, 4 );

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
