/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.query;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.envers.RevisionType;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.criteria.AuditDisjunction;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrIntTestEntity;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@SuppressWarnings({"unchecked"})
public class MaximalizePropertyQueryTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	Integer id1;
	Integer id2;
	Integer id3;
	Integer id4;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { StrIntTestEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					final StrIntTestEntity site1 = new StrIntTestEntity( "a", 10 );
					final StrIntTestEntity site2 = new StrIntTestEntity( "b", 15 );
					final StrIntTestEntity site3 = new StrIntTestEntity( "c", 42 );
					final StrIntTestEntity site4 = new StrIntTestEntity( "d", 52 );

					entityManager.persist( site1 );
					entityManager.persist( site2 );
					entityManager.persist( site3 );
					entityManager.persist( site4 );

					id1 = site1.getId();
					id2 = site2.getId();
					id3 = site3.getId();
					id4 = site4.getId();
				},

				// Revision 2
				entityManager -> {
					final StrIntTestEntity site1 = entityManager.find( StrIntTestEntity.class, id1 );
					final StrIntTestEntity site2 = entityManager.find( StrIntTestEntity.class, id2 );
					site1.setStr1( "d" );
					site2.setNumber( 20 );
				},

				// Revision 3
				entityManager -> {
					final StrIntTestEntity site1 = entityManager.find( StrIntTestEntity.class, id1 );
					final StrIntTestEntity site2 = entityManager.find( StrIntTestEntity.class, id2 );

					site1.setNumber( 30 );
					site2.setStr1( "z" );
				},

				// Revision 4
				entityManager -> {
					final StrIntTestEntity site1 = entityManager.find( StrIntTestEntity.class, id1 );
					final StrIntTestEntity site2 = entityManager.find( StrIntTestEntity.class, id2 );

					site1.setNumber( 5 );
					site2.setStr1( "a" );
				},

				// Revision 5
				entityManager -> {
					final StrIntTestEntity site4 = entityManager.find( StrIntTestEntity.class, id4 );
					entityManager.remove( site4 );
				}
		);
	}

	@DynamicTest
	public void testMaximizeWithIdEq() {
		List<Number> revs_id1 = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.addProjection( AuditEntity.revisionNumber() )
				.add( AuditEntity.property( "number" ).maximize().add( AuditEntity.id().eq( id2 ) ) )
				.addOrder( AuditEntity.revisionNumber().asc() )
				.getResultList();
		assertThat( revs_id1, contains( 2, 3, 4 ) );
	}

	@DynamicTest
	public void testMinimizeWithPropertyEq() {
		List<Number> result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.addProjection( AuditEntity.revisionNumber() )
				.add( AuditEntity.property( "number" ).minimize().add( AuditEntity.property( "str1" ).eq( "a" ) ) )
				.getResultList();
		assertThat( result, contains( 1 ) );
	}

	@DynamicTest
	public void testMaximizeRevision() {
		List<Number> result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.addProjection( AuditEntity.revisionNumber() )
				.add( AuditEntity.revisionNumber().maximize().add( AuditEntity.property( "number" ).eq( 10 ) ) )
				.getResultList();
		assertThat( result, contains( 2 ) );
	}

	@DynamicTest
	@TestForIssue(jiraKey = "HHH-7800")
	public void testMaximizeInDisjunction() {
		final AuditDisjunction disjunction = AuditEntity.disjunction()
				.add( AuditEntity.revisionNumber().maximize().add( AuditEntity.id().eq( id1 ) ) )
				.add( AuditEntity.revisionNumber().maximize().add( AuditEntity.id().eq( id3 ) ) );

		List<StrIntTestEntity> result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, true, true )
				.add( disjunction )
				.getResultList();

		Set<Integer> idsSeen = new HashSet<>();
		for ( Integer id : result.stream().map( StrIntTestEntity::getId ).collect( Collectors.toList() ) ) {
			assertThat( id, isOneOf( id1, id3 ) );
			assertThat( "Multiple revisions returned with ID " + id + "; expected 1.", idsSeen.add( id ), is( true ) );
		}
	}

	@DynamicTest
	@TestForIssue(jiraKey = "HHH-7827")
	public void testAllLatestRevisionsOfEntityType() {
		List<Object[]> result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.add( AuditEntity.revisionNumber().maximize().computeAggregationInInstanceContext() )
				.addOrder( AuditEntity.property( "id" ).asc() )
				.getResultList();

		assertThat( result, CollectionMatchers.hasSize( 4 ) );

		assertRevisionData( result.get( 0 ), 4, RevisionType.MOD, new StrIntTestEntity( "d", 5, id1 ) );
		assertRevisionData( result.get( 1 ), 4, RevisionType.MOD, new StrIntTestEntity( "a", 20, id2 ) );
		assertRevisionData( result.get( 2 ), 1, RevisionType.ADD, new StrIntTestEntity( "c", 42, id3 ) );
		assertRevisionData( result.get( 3 ), 5, RevisionType.DEL, new StrIntTestEntity( null, null, id4 ) );
	}

	private void assertRevisionData(Object[] result, int revision, RevisionType type, StrIntTestEntity entity) {
		assertThat( result[0], equalTo( entity ) );
		assertThat( result[1], instanceOf( SequenceIdRevisionEntity.class ) );
		assertThat( ( (SequenceIdRevisionEntity) result[1] ).getId(), equalTo( revision ) );
		assertThat( result[2], equalTo( type ) );
	}
}