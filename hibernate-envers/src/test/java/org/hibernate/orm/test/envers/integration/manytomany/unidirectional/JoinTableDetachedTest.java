/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.manytomany.unidirectional;

import java.util.Arrays;
import java.util.HashSet;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.entities.manytomany.unidirectional.JoinTableEntity;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-8087")
@EnversTest
@Jpa(annotatedClasses = {JoinTableEntity.class, StrTestEntity.class})
public class JoinTableDetachedTest {
	private Long collectionEntityId = null;
	private Integer element1Id = null;
	private Integer element2Id = null;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1 - addition
		scope.inTransaction( em -> {
			JoinTableEntity collectionEntity = new JoinTableEntity( "some data" );
			StrTestEntity element1 = new StrTestEntity( "str1" );
			StrTestEntity element2 = new StrTestEntity( "str2" );
			collectionEntity.getReferences().add( element1 );
			collectionEntity.getReferences().add( element2 );
			em.persist( element1 );
			em.persist( element2 );
			em.persist( collectionEntity );

			collectionEntityId = collectionEntity.getId();
			element1Id = element1.getId();
			element2Id = element2.getId();
		} );

		// Revision 2 - simple modification
		scope.inTransaction( em -> {
			JoinTableEntity collectionEntity = em.find( JoinTableEntity.class, collectionEntityId );
			collectionEntity.setData( "some other data" );
			em.merge( collectionEntity );
		} );

		// Revision 3 - remove detached object from collection
		final StrTestEntity element1 = new StrTestEntity( "str1", element1Id );
		scope.inTransaction( em -> {
			JoinTableEntity collectionEntity = em.find( JoinTableEntity.class, collectionEntityId );
			collectionEntity.getReferences().remove( element1 );
			em.merge( collectionEntity );
		} );

		// Revision 4 - replace the collection
		scope.inTransaction( em -> {
			JoinTableEntity collectionEntity = em.find( JoinTableEntity.class, collectionEntityId );
			collectionEntity.setReferences( new HashSet<StrTestEntity>() );
			em.merge( collectionEntity );
		} );

		// Revision 5 - add to collection
		scope.inTransaction( em -> {
			JoinTableEntity collectionEntity = em.find( JoinTableEntity.class, collectionEntityId );
			collectionEntity.getReferences().add( element1 );
			em.merge( collectionEntity );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2, 3, 4, 5 ),
					auditReader.getRevisions( JoinTableEntity.class, collectionEntityId ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( StrTestEntity.class, element1Id ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( StrTestEntity.class, element2Id ) );
		} );
	}

	@Test
	public void testHistoryOfCollectionEntity(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			// Revision 1
			JoinTableEntity collectionEntity = new JoinTableEntity( collectionEntityId, "some data" );
			StrTestEntity element1 = new StrTestEntity( "str1", element1Id );
			StrTestEntity element2 = new StrTestEntity( "str2", element2Id );
			collectionEntity.getReferences().add( element1 );
			collectionEntity.getReferences().add( element2 );
			JoinTableEntity ver1 = auditReader.find( JoinTableEntity.class, collectionEntityId, 1 );
			assertEquals( collectionEntity, ver1 );
			assertEquals( collectionEntity.getReferences(), ver1.getReferences() );

			// Revision 2
			collectionEntity.setData( "some other data" );
			JoinTableEntity ver2 = auditReader.find( JoinTableEntity.class, collectionEntityId, 2 );
			assertEquals( collectionEntity, ver2 );
			assertEquals( collectionEntity.getReferences(), ver2.getReferences() );

			// Revision 3
			collectionEntity.getReferences().remove( element1 );
			JoinTableEntity ver3 = auditReader.find( JoinTableEntity.class, collectionEntityId, 3 );
			assertEquals( collectionEntity, ver3 );
			assertEquals( collectionEntity.getReferences(), ver3.getReferences() );

			// Revision 4
			collectionEntity.setReferences( new HashSet<StrTestEntity>() );
			JoinTableEntity ver4 = auditReader.find( JoinTableEntity.class, collectionEntityId, 4 );
			assertEquals( collectionEntity, ver4 );
			assertEquals( collectionEntity.getReferences(), ver4.getReferences() );

			// Revision 5
			collectionEntity.getReferences().add( element1 );
			JoinTableEntity ver5 = auditReader.find( JoinTableEntity.class, collectionEntityId, 5 );
			assertEquals( collectionEntity, ver5 );
			assertEquals( collectionEntity.getReferences(), ver5.getReferences() );
		} );
	}
}
