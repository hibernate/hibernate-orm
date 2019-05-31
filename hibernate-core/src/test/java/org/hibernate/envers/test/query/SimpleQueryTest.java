/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.query;

import java.util.List;

import org.hibernate.envers.RevisionType;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.criteria.MatchMode;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrIntTestEntity;
import org.hibernate.envers.test.support.domains.ids.EmbId;
import org.hibernate.envers.test.support.domains.ids.EmbIdTestEntity;
import org.hibernate.envers.test.support.domains.ids.MulId;
import org.hibernate.envers.test.support.domains.ids.MulIdTestEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@SuppressWarnings({"unchecked"})
@Disabled("ClassCastException - EntityJavaDescriptorImpl->EmbeddableJavaDescriptor in EmbeddedJavaDescriptorImpl#resolveJtd")
public class SimpleQueryTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id1;
	private Integer id2;
	private Integer id3;
	private MulId mulId1;
	private EmbId embId1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { StrIntTestEntity.class, MulIdTestEntity.class, EmbIdTestEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					StrIntTestEntity site1 = new StrIntTestEntity( "a", 10 );
					StrIntTestEntity site2 = new StrIntTestEntity( "a", 10 );
					StrIntTestEntity site3 = new StrIntTestEntity( "b", 5 );

					entityManager.persist( site1 );
					entityManager.persist( site2 );
					entityManager.persist( site3 );

					id1 = site1.getId();
					id2 = site2.getId();
					id3 = site3.getId();
				},

				// Revision 2
				entityManager -> {
					mulId1 = new MulId( 1, 2 );
					entityManager.persist( new MulIdTestEntity( mulId1.getId1(), mulId1.getId2(), "data" ) );

					embId1 = new EmbId( 3, 4 );
					entityManager.persist( new EmbIdTestEntity( embId1, "something" ) );

					StrIntTestEntity site1 = entityManager.find( StrIntTestEntity.class, id1 );
					StrIntTestEntity site2 = entityManager.find( StrIntTestEntity.class, id2 );

					site1.setStr1( "aBc" );
					site2.setNumber( 20 );
				},

				// Revision 3
				entityManager -> {
					StrIntTestEntity site3 = entityManager.find( StrIntTestEntity.class, id3 );
					site3.setStr1( "a" );
				},

				// Revision 4
				entityManager -> {
					StrIntTestEntity site1 = entityManager.find( StrIntTestEntity.class, id1 );
					entityManager.remove( site1 );
				}
		);
	}

	@DynamicTest
	public void testEntitiesIdQuery() {
		StrIntTestEntity ver2 = (StrIntTestEntity) getAuditReader().createQuery()
				.forEntitiesAtRevision( StrIntTestEntity.class, 2 )
				.add( AuditEntity.id().eq( id2 ) )
				.getSingleResult();

		assertThat( ver2, equalTo( new StrIntTestEntity( "a", 20, id2 ) ) );
	}

	@DynamicTest
	@SuppressWarnings("unchecked")
	public void testEntitiesPropertyEqualsQuery() {
		List<StrIntTestEntity> ver1 = getAuditReader().createQuery()
				.forEntitiesAtRevision( StrIntTestEntity.class, 1 )
				.add( AuditEntity.property( "str1" ).eq( "a" ) )
				.getResultList();

		List<StrIntTestEntity> ver2 = getAuditReader().createQuery()
				.forEntitiesAtRevision( StrIntTestEntity.class, 2 )
				.add( AuditEntity.property( "str1" ).eq( "a" ) )
				.getResultList();

		List<StrIntTestEntity> ver3 = getAuditReader().createQuery()
				.forEntitiesAtRevision( StrIntTestEntity.class, 3 )
				.add( AuditEntity.property( "str1" ).eq( "a" ) )
				.getResultList();

		final StrIntTestEntity aId1 = new StrIntTestEntity( "a", 10, id1 );
		final StrIntTestEntity aId2_1 = new StrIntTestEntity( "a", 10, id2 );
		final StrIntTestEntity aId2_2 = new StrIntTestEntity( "a", 20, id2 );
		final StrIntTestEntity aId3 = new StrIntTestEntity( "a", 5, id3 );

		assertThat( ver1, containsInAnyOrder( aId1, aId2_1 ) );
		assertThat( ver2, containsInAnyOrder( aId2_2 ) );
		assertThat( ver3, containsInAnyOrder( aId2_2, aId3 ) );
	}

	@DynamicTest
	@SuppressWarnings("unchecked")
	public void testEntitiesPropertyLeQuery() {
		List<StrIntTestEntity> ver1 = getAuditReader().createQuery()
				.forEntitiesAtRevision( StrIntTestEntity.class, 1 )
				.add( AuditEntity.property( "number" ).le( 10 ) )
				.getResultList();

		List<StrIntTestEntity> ver2 = getAuditReader().createQuery()
				.forEntitiesAtRevision( StrIntTestEntity.class, 2 )
				.add( AuditEntity.property( "number" ).le( 10 ) )
				.getResultList();

		List<StrIntTestEntity> ver3 = getAuditReader().createQuery()
				.forEntitiesAtRevision( StrIntTestEntity.class, 3 )
				.add( AuditEntity.property( "number" ).le( 10 ) )
				.getResultList();

		final StrIntTestEntity aId1 = new StrIntTestEntity( "a", 10, id1 );
		final StrIntTestEntity aId2 = new StrIntTestEntity( "a", 10, id2 );
		final StrIntTestEntity aId3 = new StrIntTestEntity( "a", 5, id3 );
		final StrIntTestEntity bId3 = new StrIntTestEntity( "b", 5, id3 );
		final StrIntTestEntity abcId1 = new StrIntTestEntity( "aBc", 10, id1 );

		assertThat( ver1, containsInAnyOrder( aId1, aId2, bId3 ) );
		assertThat( ver2, containsInAnyOrder( abcId1, bId3 ) );
		assertThat( ver3, containsInAnyOrder( abcId1, aId3 ) );
	}

	@DynamicTest
	@SuppressWarnings("unchecked")
	public void testRevisionsPropertyEqQuery() {
		List<Number> revs_id1 = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.addProjection( AuditEntity.revisionNumber() )
				.add( AuditEntity.property( "str1" ).le( "a" ) )
				.add( AuditEntity.id().eq( id1 ) )
				.getResultList();

		List<Number> revs_id2 = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.addProjection( AuditEntity.revisionNumber() )
				.add( AuditEntity.property( "str1" ).le( "a" ) )
				.add( AuditEntity.id().eq( id2 ) )
				.addOrder( AuditEntity.revisionNumber().asc() )
				.getResultList();

		List<Number> revs_id3 = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.addProjection( AuditEntity.revisionNumber() )
				.add( AuditEntity.property( "str1" ).le( "a" ) )
				.add( AuditEntity.id().eq( id3 ) )
				.getResultList();

		assertThat( revs_id1, contains( 1 ) );
		assertThat( revs_id2, contains( 1, 2 ) );
		assertThat( revs_id3, contains( 3 ) );
	}

	@DynamicTest
	public void testSelectEntitiesQuery() {
		List<StrIntTestEntity> result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, true, false )
				.add( AuditEntity.id().eq( id1 ) )
				.getResultList();

		assertThat( result, contains( new StrIntTestEntity( "a", 10, id1 ), new StrIntTestEntity( "aBc", 10, id1 ) ) );
	}

	@DynamicTest
	public void testSelectEntitiesAndRevisionsQuery() {
		List result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.add( AuditEntity.id().eq( id1 ) )
				.getResultList();

		assertThat( result, CollectionMatchers.hasSize( 3 ) );

		assertThat( ( (Object[]) result.get( 0 ) )[0], equalTo( new StrIntTestEntity( "a", 10, id1 ) ) );
		assertThat( ( (Object[]) result.get( 1 ) )[0], equalTo( new StrIntTestEntity( "aBc", 10, id1 ) ) );
		assertThat( ( (Object[]) result.get( 2 ) )[0], equalTo( new StrIntTestEntity( null, null, id1 ) ) );

		assertThat( ( (SequenceIdRevisionEntity) ( (Object[]) result.get( 0 ) )[1] ).getId(), equalTo( 1 ) );
		assertThat( ( (SequenceIdRevisionEntity) ( (Object[]) result.get( 1 ) )[1] ).getId(), equalTo( 2 ) );
		assertThat( ( (SequenceIdRevisionEntity) ( (Object[]) result.get( 2 ) )[1] ).getId(), equalTo( 4 ) );

		assertThat( ( (Object[]) result.get( 0 ) )[2], equalTo( RevisionType.ADD ) );
		assertThat( ( (Object[]) result.get( 1 ) )[2], equalTo( RevisionType.MOD ) );
		assertThat( ( (Object[]) result.get( 2 ) )[2], equalTo( RevisionType.DEL ) );
	}

	@DynamicTest
	@SuppressWarnings("unchecked")
	public void testSelectRevisionTypeQuery() {
		List<RevisionType> result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.addProjection( AuditEntity.revisionType() )
				.add( AuditEntity.id().eq( id1 ) )
				.addOrder( AuditEntity.revisionNumber().asc() )
				.getResultList();

		assertThat( result, contains( RevisionType.ADD, RevisionType.MOD, RevisionType.DEL ) );
	}

	@DynamicTest
	public void testEmptyRevisionOfEntityQuery() {
		List result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.getResultList();

		assertThat( result, CollectionMatchers.hasSize( 7 ) );
	}

	@DynamicTest
	public void testEmptyConjunctionRevisionOfEntityQuery() {
		List result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.add( AuditEntity.conjunction() )
				.getResultList();

		assertThat( result, CollectionMatchers.hasSize( 7 ) );
	}

	@DynamicTest
	public void testEmptyDisjunctionRevisionOfEntityQuery() {
		List result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.add( AuditEntity.disjunction() )
				.getResultList();

		assertThat( result, CollectionMatchers.isEmpty() );
	}

	@DynamicTest
	@SuppressWarnings("unchecked")
	public void testEntitiesAddedAtRevision() {
		StrIntTestEntity site1 = new StrIntTestEntity( "a", 10, id1 );
		StrIntTestEntity site2 = new StrIntTestEntity( "a", 10, id2 );
		StrIntTestEntity site3 = new StrIntTestEntity( "b", 5, id3 );

		List<StrIntTestEntity> result = getAuditReader().createQuery()
				.forEntitiesModifiedAtRevision( StrIntTestEntity.class, StrIntTestEntity.class.getName(), 1 )
				.getResultList();
		RevisionType revisionType = (RevisionType) getAuditReader().createQuery()
				.forEntitiesModifiedAtRevision( StrIntTestEntity.class, 1 )
				.addProjection( AuditEntity.revisionType() ).add( AuditEntity.id().eq( id1 ) )
				.getSingleResult();

		assertThat( result, containsInAnyOrder( site1, site2, site3 ) );
		assertThat( revisionType, equalTo( RevisionType.ADD ) );
	}

	@DynamicTest
	@SuppressWarnings("unchecked")
	public void testEntitiesChangedAtRevision() {
		StrIntTestEntity site1 = new StrIntTestEntity( "aBc", 10, id1 );
		StrIntTestEntity site2 = new StrIntTestEntity( "a", 20, id2 );

		List<StrIntTestEntity> result = getAuditReader().createQuery()
				.forEntitiesModifiedAtRevision( StrIntTestEntity.class, 2 )
				.getResultList();
		RevisionType revisionType = (RevisionType) getAuditReader().createQuery()
				.forEntitiesModifiedAtRevision( StrIntTestEntity.class, 2 )
				.addProjection( AuditEntity.revisionType() ).add( AuditEntity.id().eq( id1 ) )
				.getSingleResult();

		assertThat( result, containsInAnyOrder( site1, site2 ) );
		assertThat( revisionType, equalTo( RevisionType.MOD ) );
	}

	@DynamicTest
	@SuppressWarnings("unchecked")
	public void testEntitiesRemovedAtRevision() {
		StrIntTestEntity site1 = new StrIntTestEntity( null, null, id1 );

		List<StrIntTestEntity> result = getAuditReader().createQuery()
				.forEntitiesModifiedAtRevision( StrIntTestEntity.class, 4 )
				.getResultList();
		RevisionType revisionType = (RevisionType) getAuditReader().createQuery()
				.forEntitiesModifiedAtRevision( StrIntTestEntity.class, 4 )
				.addProjection( AuditEntity.revisionType() ).add( AuditEntity.id().eq( id1 ) )
				.getSingleResult();

		assertThat( result, containsInAnyOrder( site1 ) );
		assertThat( revisionType, equalTo( RevisionType.DEL ) );
	}

	@DynamicTest
	public void testEntityNotModifiedAtRevision() {
		List result = getAuditReader().createQuery().forEntitiesModifiedAtRevision( StrIntTestEntity.class, 3 )
				.add( AuditEntity.id().eq( id1 ) ).getResultList();
		assertThat( result, CollectionMatchers.isEmpty() );
	}

	@DynamicTest
	public void testNoEntitiesModifiedAtRevision() {
		List result = getAuditReader().createQuery()
				.forEntitiesModifiedAtRevision( StrIntTestEntity.class, 5 )
				.getResultList();
		assertThat( result, CollectionMatchers.isEmpty() );
	}

	@DynamicTest
	@TestForIssue(jiraKey = "HHH-7800")
	public void testBetweenInsideDisjunction() {
		List result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, true, true )
				.add(
						AuditEntity.disjunction()
								.add( AuditEntity.property( "number" ).between( 0, 5 ) )
								.add( AuditEntity.property( "number" ).between( 20, 100 ) )
				)
				.getResultList();

		for ( Object o : result ) {
			StrIntTestEntity entity = (StrIntTestEntity) o;
			int number = entity.getNumber();
			assertThat( (number >= 0 && number <= 5) || (number >= 20 && number <= 100), is( true ) );
		}
	}
	
	@DynamicTest
	@TestForIssue(jiraKey = "HHH-8495")
	public void testIlike() {
		StrIntTestEntity site1 = new StrIntTestEntity( "aBc", 10, id1 );
		
		StrIntTestEntity result = (StrIntTestEntity) getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, true, true )
				.add( AuditEntity.property( "str1" ).ilike( "abc" ) )
				.getSingleResult();
		
		assertThat( result, equalTo( site1 ) );
	}
	
	@DynamicTest
	@TestForIssue(jiraKey = "HHH-8495")
	public void testIlikeWithMatchMode() {
		StrIntTestEntity site1 = new StrIntTestEntity( "aBc", 10, id1 );
		
		StrIntTestEntity result = (StrIntTestEntity) getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, true, true )
				.add( AuditEntity.property( "str1" ).ilike( "BC", MatchMode.ANYWHERE ) )
				.getSingleResult();

		assertThat( result, equalTo( site1 ) );
	}

	@DynamicTest
	@TestForIssue(jiraKey = "HHH-8567")
	public void testIdPropertyRestriction() {
		StrIntTestEntity ver2 = (StrIntTestEntity) getAuditReader().createQuery()
				.forEntitiesAtRevision( StrIntTestEntity.class, 2 )
				.add( AuditEntity.property( "id" ).eq( id2 ) )
				.getSingleResult();

		assertThat( ver2, equalTo( new StrIntTestEntity( "a", 20, id2 ) ) );
	}

	@DynamicTest
	@TestForIssue(jiraKey = "HHH-8567")
	public void testMultipleIdPropertyRestriction() {
		MulIdTestEntity ver2 = (MulIdTestEntity) getAuditReader().createQuery()
				.forEntitiesAtRevision( MulIdTestEntity.class, 2 )
				.add( AuditEntity.property( "id1" ).eq( mulId1.getId1() ) )
				.add( AuditEntity.property( "id2" ).eq( mulId1.getId2() ) )
				.getSingleResult();

		assertThat( ver2, equalTo( new MulIdTestEntity( mulId1.getId1(), mulId1.getId2(), "data" ) ) );
	}

	@DynamicTest
	@TestForIssue(jiraKey = "HHH-8567")
	public void testEmbeddedIdPropertyRestriction() {
		EmbIdTestEntity ver2 = (EmbIdTestEntity) getAuditReader().createQuery()
				.forEntitiesAtRevision( EmbIdTestEntity.class, 2 )
				.add( AuditEntity.property( "id.x" ).eq( embId1.getX() ) )
				.add( AuditEntity.property( "id.y" ).eq( embId1.getY() ) )
				.getSingleResult();

		assertThat( ver2, equalTo( new EmbIdTestEntity( embId1, "something" ) ) );
	}
}
