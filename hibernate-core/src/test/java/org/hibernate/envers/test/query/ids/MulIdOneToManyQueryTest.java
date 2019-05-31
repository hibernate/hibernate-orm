/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.query.ids;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.ids.MulId;
import org.hibernate.envers.test.support.domains.onetomany.ids.SetRefEdMulIdEntity;
import org.hibernate.envers.test.support.domains.onetomany.ids.SetRefIngMulIdEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings({"unchecked"})
@Disabled("ClassCastException - EntityJavaDescriptorImpl->EmbeddableJavaDescriptor in EmbeddedJavaDescriptorImpl#resolveJtd")
public class MulIdOneToManyQueryTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private MulId id1;
	private MulId id2;

	private MulId id3;
	private MulId id4;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { SetRefEdMulIdEntity.class, SetRefIngMulIdEntity.class};
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		id1 = new MulId( 0, 1 );
		id2 = new MulId( 10, 11 );
		id3 = new MulId( 20, 21 );
		id4 = new MulId( 30, 31 );

		inTransactions(
				// Revision 1
				entityManager -> {
					SetRefIngMulIdEntity refIng1 = new SetRefIngMulIdEntity( id1, "x", null );
					SetRefIngMulIdEntity refIng2 = new SetRefIngMulIdEntity( id2, "y", null );

					entityManager.persist( refIng1 );
					entityManager.persist( refIng2 );
				},

				// Revision 2
				entityManager -> {
					SetRefEdMulIdEntity refEd3 = new SetRefEdMulIdEntity( id3, "a" );
					SetRefEdMulIdEntity refEd4 = new SetRefEdMulIdEntity( id4, "a" );

					entityManager.persist( refEd3 );
					entityManager.persist( refEd4 );

					SetRefIngMulIdEntity refIng1 = entityManager.find( SetRefIngMulIdEntity.class, id1 );
					SetRefIngMulIdEntity refIng2 = entityManager.find( SetRefIngMulIdEntity.class, id2 );

					refIng1.setReference( refEd3 );
					refIng2.setReference( refEd4 );
				},

				// Revision 3
				entityManager -> {
					SetRefEdMulIdEntity refEd3 = entityManager.find( SetRefEdMulIdEntity.class, id3 );
					SetRefIngMulIdEntity refIng2 = entityManager.find( SetRefIngMulIdEntity.class, id2 );
					refIng2.setReference( refEd3 );
				}
		);
	}

	@DynamicTest
	@SuppressWarnings("unchecked")
	public void testEntitiesReferencedToId3() {
		Set rev1_related = new HashSet(
				getAuditReader().createQuery()
						.forEntitiesAtRevision( SetRefIngMulIdEntity.class, 1 )
						.add( AuditEntity.relatedId( "reference" ).eq( id3 ) )
						.getResultList()
		);

		Set<SetRefIngMulIdEntity> rev1 = new HashSet(
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

		Set<SetRefIngMulIdEntity> rev2 = new HashSet(
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

		Set<SetRefIngMulIdEntity> rev3 = new HashSet(
				getAuditReader().createQuery()
						.forEntitiesAtRevision( SetRefIngMulIdEntity.class, 3 )
						.add( AuditEntity.property( "reference" ).eq( new SetRefEdMulIdEntity( id3, null ) ) )
						.getResultList()
		);

		final SetRefIngMulIdEntity x = new SetRefIngMulIdEntity( id1, "x", null );
		final SetRefIngMulIdEntity y = new SetRefIngMulIdEntity( id2, "y", null );

		assertThat( rev1_related, equalTo( rev1 ) );
		assertThat( rev2_related, equalTo( rev2 ) );
		assertThat( rev3_related, equalTo( rev3 ) );
		assertThat( rev1, CollectionMatchers.isEmpty() );
		assertThat( rev2, containsInAnyOrder( x ) );
		assertThat( rev3, containsInAnyOrder( x, y ) );
	}

	@DynamicTest
	@SuppressWarnings("unchecked")
	public void testEntitiesReferencedToId4() {
		Set<SetRefIngMulIdEntity> rev1_related = new HashSet(
				getAuditReader().createQuery()
						.forEntitiesAtRevision( SetRefIngMulIdEntity.class, 1 )
						.add( AuditEntity.relatedId( "reference" ).eq( id4 ) )
						.getResultList()
		);

		Set<SetRefIngMulIdEntity> rev2_related = new HashSet(
				getAuditReader().createQuery()
						.forEntitiesAtRevision( SetRefIngMulIdEntity.class, 2 )
						.add( AuditEntity.relatedId( "reference" ).eq( id4 ) )
						.getResultList()
		);

		Set<SetRefIngMulIdEntity> rev3_related = new HashSet(
				getAuditReader().createQuery()
						.forEntitiesAtRevision( SetRefIngMulIdEntity.class, 3 )
						.add( AuditEntity.relatedId( "reference" ).eq( id4 ) )
						.getResultList()
		);

		final SetRefIngMulIdEntity y = new SetRefIngMulIdEntity( id2, "y", null );

		assertThat( rev1_related, CollectionMatchers.isEmpty() );
		assertThat( rev2_related, containsInAnyOrder( y ) );
		assertThat( rev3_related, CollectionMatchers.isEmpty() );
	}

	@DynamicTest
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

		final SetRefIngMulIdEntity x = new SetRefIngMulIdEntity( id1, "x", null );

		assertThat( rev1_related, CollectionMatchers.isEmpty() );
		assertThat( rev2_related, equalTo( x ) );
		assertThat( rev3_related, equalTo( x ) );
	}

	@DynamicTest
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

		final SetRefIngMulIdEntity y = new SetRefIngMulIdEntity( id2, "y", null );

		assertThat( rev1_related, CollectionMatchers.isEmpty() );
		assertThat( rev2_related, CollectionMatchers.isEmpty() );
		assertThat( rev3_related, equalTo( y ) );
	}
}