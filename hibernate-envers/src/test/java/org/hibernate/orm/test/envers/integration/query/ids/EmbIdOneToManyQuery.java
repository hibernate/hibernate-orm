/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query.ids;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jakarta.persistence.EntityManager;

import org.hibernate.envers.query.AuditEntity;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.ids.EmbId;
import org.hibernate.orm.test.envers.entities.onetomany.ids.SetRefEdEmbIdEntity;
import org.hibernate.orm.test.envers.entities.onetomany.ids.SetRefIngEmbIdEntity;
import org.hibernate.orm.test.envers.tools.TestTools;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings("unchecked")
public class EmbIdOneToManyQuery extends BaseEnversJPAFunctionalTestCase {
	private EmbId id1;
	private EmbId id2;

	private EmbId id3;
	private EmbId id4;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {SetRefEdEmbIdEntity.class, SetRefIngEmbIdEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		id1 = new EmbId( 0, 1 );
		id2 = new EmbId( 10, 11 );
		id3 = new EmbId( 20, 21 );
		id4 = new EmbId( 30, 31 );

		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		SetRefIngEmbIdEntity refIng1 = new SetRefIngEmbIdEntity( id1, "x", null );
		SetRefIngEmbIdEntity refIng2 = new SetRefIngEmbIdEntity( id2, "y", null );

		em.persist( refIng1 );
		em.persist( refIng2 );

		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();

		SetRefEdEmbIdEntity refEd3 = new SetRefEdEmbIdEntity( id3, "a" );
		SetRefEdEmbIdEntity refEd4 = new SetRefEdEmbIdEntity( id4, "a" );

		em.persist( refEd3 );
		em.persist( refEd4 );

		refIng1 = em.find( SetRefIngEmbIdEntity.class, id1 );
		refIng2 = em.find( SetRefIngEmbIdEntity.class, id2 );

		refIng1.setReference( refEd3 );
		refIng2.setReference( refEd4 );

		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();

		refEd3 = em.find( SetRefEdEmbIdEntity.class, id3 );
		refIng2 = em.find( SetRefIngEmbIdEntity.class, id2 );
		refIng2.setReference( refEd3 );

		em.getTransaction().commit();
	}

	@Test
	public void testEntitiesReferencedToId3() {
		Set rev1_related = new HashSet(
				getAuditReader().createQuery()
						.forEntitiesAtRevision( SetRefIngEmbIdEntity.class, 1 )
						.add( AuditEntity.relatedId( "reference" ).eq( id3 ) )
						.getResultList()
		);

		Set rev1 = new HashSet(
				getAuditReader().createQuery()
						.forEntitiesAtRevision( SetRefIngEmbIdEntity.class, 1 )
						.add( AuditEntity.property( "reference" ).eq( new SetRefEdEmbIdEntity( id3, null ) ) )
						.getResultList()
		);

		Set rev2_related = new HashSet(
				getAuditReader().createQuery()
						.forEntitiesAtRevision( SetRefIngEmbIdEntity.class, 2 )
						.add( AuditEntity.relatedId( "reference" ).eq( id3 ) )
						.getResultList()
		);

		Set rev2 = new HashSet(
				getAuditReader().createQuery()
						.forEntitiesAtRevision( SetRefIngEmbIdEntity.class, 2 )
						.add( AuditEntity.property( "reference" ).eq( new SetRefEdEmbIdEntity( id3, null ) ) )
						.getResultList()
		);

		Set rev3_related = new HashSet(
				getAuditReader().createQuery()
						.forEntitiesAtRevision( SetRefIngEmbIdEntity.class, 3 )
						.add( AuditEntity.relatedId( "reference" ).eq( id3 ) )
						.getResultList()
		);

		Set rev3 = new HashSet(
				getAuditReader().createQuery()
						.forEntitiesAtRevision( SetRefIngEmbIdEntity.class, 3 )
						.add( AuditEntity.property( "reference" ).eq( new SetRefEdEmbIdEntity( id3, null ) ) )
						.getResultList()
		);

		assert rev1.equals( rev1_related );
		assert rev2.equals( rev2_related );
		assert rev3.equals( rev3_related );

		assert rev1.equals( TestTools.makeSet() );
		assert rev2.equals( TestTools.makeSet( new SetRefIngEmbIdEntity( id1, "x", null ) ) );
		assert rev3.equals(
				TestTools.makeSet(
						new SetRefIngEmbIdEntity( id1, "x", null ),
						new SetRefIngEmbIdEntity( id2, "y", null )
				)
		);
	}

	@Test
	public void testEntitiesReferencedToId4() {
		Set rev1_related = new HashSet(
				getAuditReader().createQuery()
						.forEntitiesAtRevision( SetRefIngEmbIdEntity.class, 1 )
						.add( AuditEntity.relatedId( "reference" ).eq( id4 ) )
						.getResultList()
		);

		Set rev2_related = new HashSet(
				getAuditReader().createQuery()
						.forEntitiesAtRevision( SetRefIngEmbIdEntity.class, 2 )
						.add( AuditEntity.relatedId( "reference" ).eq( id4 ) )
						.getResultList()
		);

		Set rev3_related = new HashSet(
				getAuditReader().createQuery()
						.forEntitiesAtRevision( SetRefIngEmbIdEntity.class, 3 )
						.add( AuditEntity.relatedId( "reference" ).eq( id4 ) )
						.getResultList()
		);

		assert rev1_related.equals( TestTools.makeSet() );
		assert rev2_related.equals( TestTools.makeSet( new SetRefIngEmbIdEntity( id2, "y", null ) ) );
		assert rev3_related.equals( TestTools.makeSet() );
	}

	@Test
	public void testEntitiesReferencedByIng1ToId3() {
		List rev1_related = getAuditReader().createQuery()
				.forEntitiesAtRevision( SetRefIngEmbIdEntity.class, 1 )
				.add( AuditEntity.relatedId( "reference" ).eq( id3 ) )
				.add( AuditEntity.id().eq( id1 ) )
				.getResultList();

		Object rev2_related = getAuditReader().createQuery()
				.forEntitiesAtRevision( SetRefIngEmbIdEntity.class, 2 )
				.add( AuditEntity.relatedId( "reference" ).eq( id3 ) )
				.add( AuditEntity.id().eq( id1 ) )
				.getSingleResult();

		Object rev3_related = getAuditReader().createQuery()
				.forEntitiesAtRevision( SetRefIngEmbIdEntity.class, 3 )
				.add( AuditEntity.relatedId( "reference" ).eq( id3 ) )
				.add( AuditEntity.id().eq( id1 ) )
				.getSingleResult();

		assert rev1_related.size() == 0;
		assert rev2_related.equals( new SetRefIngEmbIdEntity( id1, "x", null ) );
		assert rev3_related.equals( new SetRefIngEmbIdEntity( id1, "x", null ) );
	}

	@Test
	public void testEntitiesReferencedByIng2ToId3() {
		List rev1_related = getAuditReader().createQuery()
				.forEntitiesAtRevision( SetRefIngEmbIdEntity.class, 1 )
				.add( AuditEntity.relatedId( "reference" ).eq( id3 ) )
				.add( AuditEntity.id().eq( id2 ) )
				.getResultList();

		List rev2_related = getAuditReader().createQuery()
				.forEntitiesAtRevision( SetRefIngEmbIdEntity.class, 2 )
				.add( AuditEntity.relatedId( "reference" ).eq( id3 ) )
				.add( AuditEntity.id().eq( id2 ) )
				.getResultList();

		Object rev3_related = getAuditReader().createQuery()
				.forEntitiesAtRevision( SetRefIngEmbIdEntity.class, 3 )
				.add( AuditEntity.relatedId( "reference" ).eq( id3 ) )
				.add( AuditEntity.id().eq( id2 ) )
				.getSingleResult();

		assert rev1_related.size() == 0;
		assert rev2_related.size() == 0;
		assert rev3_related.equals( new SetRefIngEmbIdEntity( id2, "y", null ) );
	}
}
