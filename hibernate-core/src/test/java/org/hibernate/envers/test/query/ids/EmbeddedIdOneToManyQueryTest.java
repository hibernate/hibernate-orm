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
import org.hibernate.envers.test.support.domains.ids.EmbId;
import org.hibernate.envers.test.support.domains.onetomany.ids.SetRefEdEmbIdEntity;
import org.hibernate.envers.test.support.domains.onetomany.ids.SetRefIngEmbIdEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings({"unchecked"})
@Disabled("NPE - lamba expression inside EntityIdentifierCompositeAggregatedImpl")
public class EmbeddedIdOneToManyQueryTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private EmbId id1;
	private EmbId id2;

	private EmbId id3;
	private EmbId id4;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { SetRefEdEmbIdEntity.class, SetRefIngEmbIdEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		// Initialize identifier values
		id1 = new EmbId( 0, 1 );
		id2 = new EmbId( 10, 11 );
		id3 = new EmbId( 20, 21 );
		id4 = new EmbId( 30, 31 );

		inTransactions(
				// Revision 1
				entityManager -> {
					final SetRefIngEmbIdEntity refIng1 = new SetRefIngEmbIdEntity( id1, "x", null );
					final SetRefIngEmbIdEntity refIng2 = new SetRefIngEmbIdEntity( id2, "y", null );

					entityManager.persist( refIng1 );
					entityManager.persist( refIng2 );
				},

				// Revision 2
				entityManager -> {
					final SetRefEdEmbIdEntity refEd3 = new SetRefEdEmbIdEntity( id3, "a" );
					final SetRefEdEmbIdEntity refEd4 = new SetRefEdEmbIdEntity( id4, "a" );

					entityManager.persist( refEd3 );
					entityManager.persist( refEd4 );

					final SetRefIngEmbIdEntity refIng1 = entityManager.find( SetRefIngEmbIdEntity.class, id1 );
					final SetRefIngEmbIdEntity refIng2 = entityManager.find( SetRefIngEmbIdEntity.class, id2 );

					refIng1.setReference( refEd3 );
					refIng2.setReference( refEd4 );
				},

				// Revision 3
				entityManager -> {
					final SetRefEdEmbIdEntity refEd3 = entityManager.find( SetRefEdEmbIdEntity.class, id3 );
					final SetRefIngEmbIdEntity refIng2 = entityManager.find( SetRefIngEmbIdEntity.class, id2 );
					refIng2.setReference( refEd3 );
				}
		);
	}

	@DynamicTest
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

		assertThat( rev1, equalTo( rev1_related ) );
		assertThat( rev2, equalTo( rev2_related ) );
		assertThat( rev3, equalTo( rev3_related ) );

		final SetRefIngEmbIdEntity xId1 = new SetRefIngEmbIdEntity( id1, "x", null );
		final SetRefIngEmbIdEntity yId2 = new SetRefIngEmbIdEntity( id2, "y", null );

		assertThat( rev1, CollectionMatchers.isEmpty() );
		assertThat( (Iterable<? extends SetRefIngEmbIdEntity>) rev2, contains( xId1 ) );
		assertThat( (Iterable<? extends SetRefIngEmbIdEntity>) rev3, containsInAnyOrder( xId1, yId2 ) );
	}

	@DynamicTest
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

		assertThat( rev1_related, CollectionMatchers.isEmpty() );
		assertThat( (Iterable<? extends SetRefIngEmbIdEntity>) rev2_related, contains( new SetRefIngEmbIdEntity( id2, "y", null ) ) );
		assertThat( rev3_related, CollectionMatchers.isEmpty() );
	}

	@DynamicTest
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

		assertThat( rev1_related, CollectionMatchers.isEmpty() );
		assertThat( rev2_related, equalTo( new SetRefIngEmbIdEntity( id1, "x", null ) ) );
		assertThat( rev3_related, equalTo( new SetRefIngEmbIdEntity( id1, "x", null ) ) );
	}

	@DynamicTest
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

		assertThat( rev1_related, CollectionMatchers.isEmpty() );
		assertThat( rev2_related, CollectionMatchers.isEmpty() );
		assertThat( rev3_related, equalTo( new SetRefIngEmbIdEntity( id2, "y", null ) ) );
	}
}
