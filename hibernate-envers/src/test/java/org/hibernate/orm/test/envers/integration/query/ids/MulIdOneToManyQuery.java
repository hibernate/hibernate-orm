/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query.ids;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.orm.test.envers.entities.ids.MulId;
import org.hibernate.orm.test.envers.entities.onetomany.ids.SetRefEdMulIdEntity;
import org.hibernate.orm.test.envers.entities.onetomany.ids.SetRefIngMulIdEntity;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings("unchecked")
@Jpa(annotatedClasses = {
		SetRefEdMulIdEntity.class,
		SetRefIngMulIdEntity.class
})
@EnversTest
public class MulIdOneToManyQuery {
	private MulId id1;
	private MulId id2;

	private MulId id3;
	private MulId id4;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		id1 = new MulId( 0, 1 );
		id2 = new MulId( 10, 11 );
		id3 = new MulId( 20, 21 );
		id4 = new MulId( 30, 31 );

		// Revision 1
		scope.inTransaction( em -> {
			SetRefIngMulIdEntity refIng1 = new SetRefIngMulIdEntity( id1, "x", null );
			SetRefIngMulIdEntity refIng2 = new SetRefIngMulIdEntity( id2, "y", null );

			em.persist( refIng1 );
			em.persist( refIng2 );
		} );

		// Revision 2
		scope.inTransaction( em -> {
			SetRefEdMulIdEntity refEd3 = new SetRefEdMulIdEntity( id3, "a" );
			SetRefEdMulIdEntity refEd4 = new SetRefEdMulIdEntity( id4, "a" );

			em.persist( refEd3 );
			em.persist( refEd4 );

			SetRefIngMulIdEntity refIng1 = em.find( SetRefIngMulIdEntity.class, id1 );
			SetRefIngMulIdEntity refIng2 = em.find( SetRefIngMulIdEntity.class, id2 );

			refIng1.setReference( refEd3 );
			refIng2.setReference( refEd4 );
		} );

		// Revision 3
		scope.inTransaction( em -> {
			SetRefEdMulIdEntity refEd3 = em.find( SetRefEdMulIdEntity.class, id3 );
			SetRefIngMulIdEntity refIng2 = em.find( SetRefIngMulIdEntity.class, id2 );
			refIng2.setReference( refEd3 );
		} );
	}

	@Test
	public void testEntitiesReferencedToId3(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			Set rev1_related = new HashSet(
					AuditReaderFactory.get( em ).createQuery()
							.forEntitiesAtRevision( SetRefIngMulIdEntity.class, 1 )
							.add( AuditEntity.relatedId( "reference" ).eq( id3 ) )
							.getResultList()
			);

			Set rev1 = new HashSet(
					AuditReaderFactory.get( em ).createQuery()
							.forEntitiesAtRevision( SetRefIngMulIdEntity.class, 1 )
							.add( AuditEntity.property( "reference" ).eq( new SetRefEdMulIdEntity( id3, null ) ) )
							.getResultList()
			);

			Set rev2_related = new HashSet(
					AuditReaderFactory.get( em ).createQuery()
							.forEntitiesAtRevision( SetRefIngMulIdEntity.class, 2 )
							.add( AuditEntity.relatedId( "reference" ).eq( id3 ) )
							.getResultList()
			);

			Set rev2 = new HashSet(
					AuditReaderFactory.get( em ).createQuery()
							.forEntitiesAtRevision( SetRefIngMulIdEntity.class, 2 )
							.add( AuditEntity.property( "reference" ).eq( new SetRefEdMulIdEntity( id3, null ) ) )
							.getResultList()
			);

			Set rev3_related = new HashSet(
					AuditReaderFactory.get( em ).createQuery()
							.forEntitiesAtRevision( SetRefIngMulIdEntity.class, 3 )
							.add( AuditEntity.relatedId( "reference" ).eq( id3 ) )
							.getResultList()
			);

			Set rev3 = new HashSet(
					AuditReaderFactory.get( em ).createQuery()
							.forEntitiesAtRevision( SetRefIngMulIdEntity.class, 3 )
							.add( AuditEntity.property( "reference" ).eq( new SetRefEdMulIdEntity( id3, null ) ) )
							.getResultList()
			);
			assertEquals( rev1, rev1_related );
			assertEquals( rev2, rev2_related );
			assertEquals( rev3, rev3_related );
			assertEquals( rev1, TestTools.makeSet() );
			assertEquals( rev2, TestTools.makeSet( new SetRefIngMulIdEntity( id1, "x", null ) ) );
			assertEquals(
					rev3, TestTools.makeSet(
					new SetRefIngMulIdEntity( id1, "x", null ),
					new SetRefIngMulIdEntity( id2, "y", null )
			)
			);
		} );
	}

	@Test
	public void testEntitiesReferencedToId4(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			Set rev1_related = new HashSet(
					AuditReaderFactory.get( em ).createQuery()
							.forEntitiesAtRevision( SetRefIngMulIdEntity.class, 1 )
							.add( AuditEntity.relatedId( "reference" ).eq( id4 ) )
							.getResultList()
			);

			Set rev2_related = new HashSet(
					AuditReaderFactory.get( em ).createQuery()
							.forEntitiesAtRevision( SetRefIngMulIdEntity.class, 2 )
							.add( AuditEntity.relatedId( "reference" ).eq( id4 ) )
							.getResultList()
			);

			Set rev3_related = new HashSet(
					AuditReaderFactory.get( em ).createQuery()
							.forEntitiesAtRevision( SetRefIngMulIdEntity.class, 3 )
							.add( AuditEntity.relatedId( "reference" ).eq( id4 ) )
							.getResultList()
			);
			assertEquals( rev1_related, TestTools.makeSet() );
			assertEquals( rev2_related, TestTools.makeSet( new SetRefIngMulIdEntity( id2, "y", null ) ) );
			assertEquals( rev3_related, TestTools.makeSet() );
		} );
	}

	@Test
	public void testEntitiesReferencedByIng1ToId3(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List rev1_related = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( SetRefIngMulIdEntity.class, 1 )
					.add( AuditEntity.relatedId( "reference" ).eq( id3 ) )
					.add( AuditEntity.id().eq( id1 ) )
					.getResultList();

			Object rev2_related = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( SetRefIngMulIdEntity.class, 2 )
					.add( AuditEntity.relatedId( "reference" ).eq( id3 ) )
					.add( AuditEntity.id().eq( id1 ) )
					.getSingleResult();

			Object rev3_related = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( SetRefIngMulIdEntity.class, 3 )
					.add( AuditEntity.relatedId( "reference" ).eq( id3 ) )
					.add( AuditEntity.id().eq( id1 ) )
					.getSingleResult();
			assertEquals( 0, rev1_related.size() );
			assertEquals( rev2_related, new SetRefIngMulIdEntity( id1, "x", null ) );
			assertEquals( rev3_related, new SetRefIngMulIdEntity( id1, "x", null ) );
		} );
	}

	@Test
	public void testEntitiesReferencedByIng2ToId3(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List rev1_related = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( SetRefIngMulIdEntity.class, 1 )
					.add( AuditEntity.relatedId( "reference" ).eq( id3 ) )
					.add( AuditEntity.id().eq( id2 ) )
					.getResultList();

			List rev2_related = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( SetRefIngMulIdEntity.class, 2 )
					.add( AuditEntity.relatedId( "reference" ).eq( id3 ) )
					.add( AuditEntity.id().eq( id2 ) )
					.getResultList();

			Object rev3_related = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( SetRefIngMulIdEntity.class, 3 )
					.add( AuditEntity.relatedId( "reference" ).eq( id3 ) )
					.add( AuditEntity.id().eq( id2 ) )
					.getSingleResult();
			assertEquals( 0, rev1_related.size() );
			assertEquals( 0, rev2_related.size() );
			assertEquals( new SetRefIngMulIdEntity( id2, "y", null ), rev3_related );
		} );
	}
}
