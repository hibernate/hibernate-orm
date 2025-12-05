/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query.ids;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.orm.test.envers.entities.ids.EmbId;
import org.hibernate.orm.test.envers.entities.onetomany.ids.SetRefEdEmbIdEntity;
import org.hibernate.orm.test.envers.entities.onetomany.ids.SetRefIngEmbIdEntity;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Jpa(annotatedClasses = {
		SetRefEdEmbIdEntity.class,
		SetRefIngEmbIdEntity.class
})
@EnversTest
@SuppressWarnings("unchecked")
public class EmbIdOneToManyQuery {
	private EmbId id1;
	private EmbId id2;

	private EmbId id3;
	private EmbId id4;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		id1 = new EmbId( 0, 1 );
		id2 = new EmbId( 10, 11 );
		id3 = new EmbId( 20, 21 );
		id4 = new EmbId( 30, 31 );

		// Revision 1
		scope.inTransaction( entityManager -> {
			SetRefIngEmbIdEntity refIng1 = new SetRefIngEmbIdEntity( id1, "x", null );
			SetRefIngEmbIdEntity refIng2 = new SetRefIngEmbIdEntity( id2, "y", null );

			entityManager.persist( refIng1 );
			entityManager.persist( refIng2 );
		} );

		// Revision 2
		scope.inTransaction( entityManager -> {
			SetRefEdEmbIdEntity refEd3 = new SetRefEdEmbIdEntity( id3, "a" );
			SetRefEdEmbIdEntity refEd4 = new SetRefEdEmbIdEntity( id4, "a" );

			entityManager.persist( refEd3 );
			entityManager.persist( refEd4 );

			SetRefIngEmbIdEntity refIng1 = entityManager.find( SetRefIngEmbIdEntity.class, id1 );
			SetRefIngEmbIdEntity refIng2 = entityManager.find( SetRefIngEmbIdEntity.class, id2 );

			refIng1.setReference( refEd3 );
			refIng2.setReference( refEd4 );
		} );

		// Revision 3
		scope.inTransaction( entityManager -> {
			SetRefEdEmbIdEntity refEd3 = entityManager.find( SetRefEdEmbIdEntity.class, id3 );
			SetRefIngEmbIdEntity refIng2 = entityManager.find( SetRefIngEmbIdEntity.class, id2 );
			refIng2.setReference( refEd3 );
		} );
	}

	@Test
	public void testEntitiesReferencedToId3(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			Set rev1_related = new HashSet(
					AuditReaderFactory.get( em ).createQuery()
							.forEntitiesAtRevision( SetRefIngEmbIdEntity.class, 1 )
							.add( AuditEntity.relatedId( "reference" ).eq( id3 ) )
							.getResultList()
			);

			Set rev1 = new HashSet(
					AuditReaderFactory.get( em ).createQuery()
							.forEntitiesAtRevision( SetRefIngEmbIdEntity.class, 1 )
							.add( AuditEntity.property( "reference" ).eq( new SetRefEdEmbIdEntity( id3, null ) ) )
							.getResultList()
			);

			Set rev2_related = new HashSet(
					AuditReaderFactory.get( em ).createQuery()
							.forEntitiesAtRevision( SetRefIngEmbIdEntity.class, 2 )
							.add( AuditEntity.relatedId( "reference" ).eq( id3 ) )
							.getResultList()
			);

			Set rev2 = new HashSet(
					AuditReaderFactory.get( em ).createQuery()
							.forEntitiesAtRevision( SetRefIngEmbIdEntity.class, 2 )
							.add( AuditEntity.property( "reference" ).eq( new SetRefEdEmbIdEntity( id3, null ) ) )
							.getResultList()
			);

			Set rev3_related = new HashSet(
					AuditReaderFactory.get( em ).createQuery()
							.forEntitiesAtRevision( SetRefIngEmbIdEntity.class, 3 )
							.add( AuditEntity.relatedId( "reference" ).eq( id3 ) )
							.getResultList()
			);

			Set rev3 = new HashSet(
					AuditReaderFactory.get( em ).createQuery()
							.forEntitiesAtRevision( SetRefIngEmbIdEntity.class, 3 )
							.add( AuditEntity.property( "reference" ).eq( new SetRefEdEmbIdEntity( id3, null ) ) )
							.getResultList()
			);

			assertEquals( rev1_related, rev1 );
			assertEquals( rev2_related, rev2 );
			assertEquals( rev3_related, rev3 );

			assertEquals( TestTools.makeSet(), rev1 );
			assertEquals( TestTools.makeSet( new SetRefIngEmbIdEntity( id1, "x", null ) ), rev2 );
			assertEquals(
					TestTools.makeSet(
							new SetRefIngEmbIdEntity( id1, "x", null ),
							new SetRefIngEmbIdEntity( id2, "y", null )
					),
					rev3
			);
		} );
	}

	@Test
	public void testEntitiesReferencedToId4(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			Set rev1_related = new HashSet(
					AuditReaderFactory.get( em ).createQuery()
							.forEntitiesAtRevision( SetRefIngEmbIdEntity.class, 1 )
							.add( AuditEntity.relatedId( "reference" ).eq( id4 ) )
							.getResultList()
			);

			Set rev2_related = new HashSet(
					AuditReaderFactory.get( em ).createQuery()
							.forEntitiesAtRevision( SetRefIngEmbIdEntity.class, 2 )
							.add( AuditEntity.relatedId( "reference" ).eq( id4 ) )
							.getResultList()
			);

			Set rev3_related = new HashSet(
					AuditReaderFactory.get( em ).createQuery()
							.forEntitiesAtRevision( SetRefIngEmbIdEntity.class, 3 )
							.add( AuditEntity.relatedId( "reference" ).eq( id4 ) )
							.getResultList()
			);

			assertEquals( TestTools.makeSet(), rev1_related );
			assertEquals( TestTools.makeSet( new SetRefIngEmbIdEntity( id2, "y", null ) ), rev2_related );
			assertEquals( TestTools.makeSet(), rev3_related );
		} );
	}

	@Test
	public void testEntitiesReferencedByIng1ToId3(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List rev1_related = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( SetRefIngEmbIdEntity.class, 1 )
					.add( AuditEntity.relatedId( "reference" ).eq( id3 ) )
					.add( AuditEntity.id().eq( id1 ) )
					.getResultList();

			Object rev2_related = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( SetRefIngEmbIdEntity.class, 2 )
					.add( AuditEntity.relatedId( "reference" ).eq( id3 ) )
					.add( AuditEntity.id().eq( id1 ) )
					.getSingleResult();

			Object rev3_related = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( SetRefIngEmbIdEntity.class, 3 )
					.add( AuditEntity.relatedId( "reference" ).eq( id3 ) )
					.add( AuditEntity.id().eq( id1 ) )
					.getSingleResult();

			assertEquals( 0, rev1_related.size() );
			assertEquals( new SetRefIngEmbIdEntity( id1, "x", null ), rev2_related );
			assertEquals( new SetRefIngEmbIdEntity( id1, "x", null ), rev3_related );
		} );
	}

	@Test
	public void testEntitiesReferencedByIng2ToId3(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List rev1_related = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( SetRefIngEmbIdEntity.class, 1 )
					.add( AuditEntity.relatedId( "reference" ).eq( id3 ) )
					.add( AuditEntity.id().eq( id2 ) )
					.getResultList();

			List rev2_related = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( SetRefIngEmbIdEntity.class, 2 )
					.add( AuditEntity.relatedId( "reference" ).eq( id3 ) )
					.add( AuditEntity.id().eq( id2 ) )
					.getResultList();

			Object rev3_related = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( SetRefIngEmbIdEntity.class, 3 )
					.add( AuditEntity.relatedId( "reference" ).eq( id3 ) )
					.add( AuditEntity.id().eq( id2 ) )
					.getSingleResult();

			assertEquals( 0, rev1_related.size() );
			assertEquals( 0, rev2_related.size() );
			assertEquals( new SetRefIngEmbIdEntity( id2, "y", null ), rev3_related );
		} );
	}
}
