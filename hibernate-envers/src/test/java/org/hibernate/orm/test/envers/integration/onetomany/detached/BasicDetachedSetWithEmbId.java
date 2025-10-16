/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany.detached;

import java.util.Arrays;
import java.util.HashSet;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.ids.EmbId;
import org.hibernate.orm.test.envers.entities.ids.EmbIdTestEntity;
import org.hibernate.orm.test.envers.entities.onetomany.detached.ids.SetRefCollEntityEmbId;
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
@Jpa(annotatedClasses = {EmbIdTestEntity.class, SetRefCollEntityEmbId.class})
public class BasicDetachedSetWithEmbId {
	private EmbId str1_id;
	private EmbId str2_id;

	private EmbId coll1_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		str1_id = new EmbId( 1, 2 );
		str2_id = new EmbId( 3, 4 );
		coll1_id = new EmbId( 5, 6 );

		// Revision 1
		scope.inTransaction( em -> {
			EmbIdTestEntity str1 = new EmbIdTestEntity( str1_id, "str1" );
			EmbIdTestEntity str2 = new EmbIdTestEntity( str2_id, "str2" );

			em.persist( str1 );
			em.persist( str2 );

			SetRefCollEntityEmbId coll1 = new SetRefCollEntityEmbId( coll1_id, "coll1" );
			coll1.setCollection( new HashSet<EmbIdTestEntity>() );
			coll1.getCollection().add( str1 );
			em.persist( coll1 );
		} );

		// Revision 2
		scope.inTransaction( em -> {
			EmbIdTestEntity str2 = em.find( EmbIdTestEntity.class, str2_id );
			SetRefCollEntityEmbId coll1 = em.find( SetRefCollEntityEmbId.class, coll1_id );

			coll1.getCollection().add( str2 );
		} );

		// Revision 3
		scope.inTransaction( em -> {
			EmbIdTestEntity str1 = em.find( EmbIdTestEntity.class, str1_id );
			SetRefCollEntityEmbId coll1 = em.find( SetRefCollEntityEmbId.class, coll1_id );

			coll1.getCollection().remove( str1 );
		} );

		// Revision 4
		scope.inTransaction( em -> {
			SetRefCollEntityEmbId coll1 = em.find( SetRefCollEntityEmbId.class, coll1_id );

			coll1.getCollection().clear();
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2, 3, 4 ),
					auditReader.getRevisions( SetRefCollEntityEmbId.class, coll1_id ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( EmbIdTestEntity.class, str1_id ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( EmbIdTestEntity.class, str2_id ) );
		} );
	}

	@Test
	public void testHistoryOfColl1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			EmbIdTestEntity str1 = em.find( EmbIdTestEntity.class, str1_id );
			EmbIdTestEntity str2 = em.find( EmbIdTestEntity.class, str2_id );

			final var auditReader = AuditReaderFactory.get( em );
			SetRefCollEntityEmbId rev1 = auditReader.find( SetRefCollEntityEmbId.class, coll1_id, 1 );
			SetRefCollEntityEmbId rev2 = auditReader.find( SetRefCollEntityEmbId.class, coll1_id, 2 );
			SetRefCollEntityEmbId rev3 = auditReader.find( SetRefCollEntityEmbId.class, coll1_id, 3 );
			SetRefCollEntityEmbId rev4 = auditReader.find( SetRefCollEntityEmbId.class, coll1_id, 4 );

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
