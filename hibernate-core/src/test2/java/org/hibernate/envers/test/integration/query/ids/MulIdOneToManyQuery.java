/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.query.ids;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.EntityManager;

import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.ids.MulId;
import org.hibernate.envers.test.entities.onetomany.ids.SetRefEdMulIdEntity;
import org.hibernate.envers.test.entities.onetomany.ids.SetRefIngMulIdEntity;
import org.hibernate.envers.test.tools.TestTools;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings({"unchecked"})
public class MulIdOneToManyQuery extends BaseEnversJPAFunctionalTestCase {
	private MulId id1;
	private MulId id2;

	private MulId id3;
	private MulId id4;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {SetRefEdMulIdEntity.class, SetRefIngMulIdEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		id1 = new MulId( 0, 1 );
		id2 = new MulId( 10, 11 );
		id3 = new MulId( 20, 21 );
		id4 = new MulId( 30, 31 );

		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		SetRefIngMulIdEntity refIng1 = new SetRefIngMulIdEntity( id1, "x", null );
		SetRefIngMulIdEntity refIng2 = new SetRefIngMulIdEntity( id2, "y", null );

		em.persist( refIng1 );
		em.persist( refIng2 );

		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();

		SetRefEdMulIdEntity refEd3 = new SetRefEdMulIdEntity( id3, "a" );
		SetRefEdMulIdEntity refEd4 = new SetRefEdMulIdEntity( id4, "a" );

		em.persist( refEd3 );
		em.persist( refEd4 );

		refIng1 = em.find( SetRefIngMulIdEntity.class, id1 );
		refIng2 = em.find( SetRefIngMulIdEntity.class, id2 );

		refIng1.setReference( refEd3 );
		refIng2.setReference( refEd4 );

		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();

		refEd3 = em.find( SetRefEdMulIdEntity.class, id3 );
		refIng2 = em.find( SetRefIngMulIdEntity.class, id2 );
		refIng2.setReference( refEd3 );

		em.getTransaction().commit();
	}

	@Test
	public void testEntitiesReferencedToId3() {
		Set rev1_related = new HashSet(
				getAuditReader().createQuery()
						.forEntitiesAtRevision( SetRefIngMulIdEntity.class, 1 )
						.add( AuditEntity.relatedId( "reference" ).eq( id3 ) )
						.getResultList()
		);

		Set rev1 = new HashSet(
				getAuditReader().createQuery()
						.forEntitiesAtRevision( SetRefIngMulIdEntity.class, 1 )
						.add( AuditEntity.property( "reference" ).eq( new SetRefEdMulIdEntity( id3, null ) ) )
						.getResultList()
		);

		Set rev2_related = new HashSet(
				getAuditReader().createQuery()
						.forEntitiesAtRevision( SetRefIngMulIdEntity.class, 2 )
						.add( AuditEntity.relatedId( "reference" ).eq( id3 ) )
						.getResultList()
		);

		Set rev2 = new HashSet(
				getAuditReader().createQuery()
						.forEntitiesAtRevision( SetRefIngMulIdEntity.class, 2 )
						.add( AuditEntity.property( "reference" ).eq( new SetRefEdMulIdEntity( id3, null ) ) )
						.getResultList()
		);

		Set rev3_related = new HashSet(
				getAuditReader().createQuery()
						.forEntitiesAtRevision( SetRefIngMulIdEntity.class, 3 )
						.add( AuditEntity.relatedId( "reference" ).eq( id3 ) )
						.getResultList()
		);

		Set rev3 = new HashSet(
				getAuditReader().createQuery()
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
	}

	@Test
	public void testEntitiesReferencedToId4() {
		Set rev1_related = new HashSet(
				getAuditReader().createQuery()
						.forEntitiesAtRevision( SetRefIngMulIdEntity.class, 1 )
						.add( AuditEntity.relatedId( "reference" ).eq( id4 ) )
						.getResultList()
		);

		Set rev2_related = new HashSet(
				getAuditReader().createQuery()
						.forEntitiesAtRevision( SetRefIngMulIdEntity.class, 2 )
						.add( AuditEntity.relatedId( "reference" ).eq( id4 ) )
						.getResultList()
		);

		Set rev3_related = new HashSet(
				getAuditReader().createQuery()
						.forEntitiesAtRevision( SetRefIngMulIdEntity.class, 3 )
						.add( AuditEntity.relatedId( "reference" ).eq( id4 ) )
						.getResultList()
		);
		assertEquals( rev1_related, TestTools.makeSet() );
		assertEquals( rev2_related, TestTools.makeSet( new SetRefIngMulIdEntity( id2, "y", null ) ) );
		assertEquals( rev3_related, TestTools.makeSet() );
	}

	@Test
	public void testEntitiesReferencedByIng1ToId3() {
		List rev1_related = getAuditReader().createQuery()
				.forEntitiesAtRevision( SetRefIngMulIdEntity.class, 1 )
				.add( AuditEntity.relatedId( "reference" ).eq( id3 ) )
				.add( AuditEntity.id().eq( id1 ) )
				.getResultList();

		Object rev2_related = getAuditReader().createQuery()
				.forEntitiesAtRevision( SetRefIngMulIdEntity.class, 2 )
				.add( AuditEntity.relatedId( "reference" ).eq( id3 ) )
				.add( AuditEntity.id().eq( id1 ) )
				.getSingleResult();

		Object rev3_related = getAuditReader().createQuery()
				.forEntitiesAtRevision( SetRefIngMulIdEntity.class, 3 )
				.add( AuditEntity.relatedId( "reference" ).eq( id3 ) )
				.add( AuditEntity.id().eq( id1 ) )
				.getSingleResult();
		assertEquals( 0, rev1_related.size() );
		assertEquals( rev2_related, new SetRefIngMulIdEntity( id1, "x", null ) );
		assertEquals( rev3_related, new SetRefIngMulIdEntity( id1, "x", null ) );
	}

	@Test
	public void testEntitiesReferencedByIng2ToId3() {
		List rev1_related = getAuditReader().createQuery()
				.forEntitiesAtRevision( SetRefIngMulIdEntity.class, 1 )
				.add( AuditEntity.relatedId( "reference" ).eq( id3 ) )
				.add( AuditEntity.id().eq( id2 ) )
				.getResultList();

		List rev2_related = getAuditReader().createQuery()
				.forEntitiesAtRevision( SetRefIngMulIdEntity.class, 2 )
				.add( AuditEntity.relatedId( "reference" ).eq( id3 ) )
				.add( AuditEntity.id().eq( id2 ) )
				.getResultList();

		Object rev3_related = getAuditReader().createQuery()
				.forEntitiesAtRevision( SetRefIngMulIdEntity.class, 3 )
				.add( AuditEntity.relatedId( "reference" ).eq( id3 ) )
				.add( AuditEntity.id().eq( id2 ) )
				.getSingleResult();
		assertEquals( 0, rev1_related.size() );
		assertEquals( 0, rev2_related.size() );
		assertEquals( new SetRefIngMulIdEntity( id2, "y", null ), rev3_related );
	}
}