/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.query;

import java.util.List;

import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.IntTestEntity;
import org.hibernate.envers.test.support.domains.ids.UnusualIdNamingEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings({"unchecked"})
public class AggregateQueryTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id1;
	private Integer id2;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { IntTestEntity.class, UnusualIdNamingEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					final IntTestEntity ite1 = new IntTestEntity( 2 );
					final IntTestEntity ite2 = new IntTestEntity( 10 );
					entityManager.persist( ite1 );
					entityManager.persist( ite2 );
					id1 = ite1.getId();
					id2 = ite2.getId();
				},

				// Revision 2
				entityManager -> {
					final IntTestEntity ite3 = new IntTestEntity( 8 );
					final UnusualIdNamingEntity uine1 = new UnusualIdNamingEntity( "id1", "data1" );
					entityManager.persist( uine1 );
					entityManager.persist( ite3 );

					final IntTestEntity ite1 = entityManager.find( IntTestEntity.class, id1 );
					ite1.setNumber( 0 );
				},

				// Revision 3
				entityManager -> {
					final IntTestEntity ite2 = entityManager.find( IntTestEntity.class, id2 );
					ite2.setNumber( 52 );
				}
		);
	}

	@DynamicTest
	@Disabled("BasesqmToSqlAstConverter#visitAvgFunction creates AvgFunction with a null resultType which leads to NPE")
	public void testEntitiesAvgMaxQuery() {
		Object[] ver1 = (Object[]) getAuditReader().createQuery()
				.forEntitiesAtRevision( IntTestEntity.class, 1 )
				.addProjection( AuditEntity.property( "number" ).max() )
				.addProjection( AuditEntity.property( "number" ).function( "avg" ) )
				.getSingleResult();

		Object[] ver2 = (Object[]) getAuditReader().createQuery()
				.forEntitiesAtRevision( IntTestEntity.class, 2 )
				.addProjection( AuditEntity.property( "number" ).max() )
				.addProjection( AuditEntity.property( "number" ).function( "avg" ) )
				.getSingleResult();

		Object[] ver3 = (Object[]) getAuditReader().createQuery()
				.forEntitiesAtRevision( IntTestEntity.class, 3 )
				.addProjection( AuditEntity.property( "number" ).max() )
				.addProjection( AuditEntity.property( "number" ).function( "avg" ) )
				.getSingleResult();

		assertThat( ver1[0], equalTo( 10 ) );
		assertThat( ver1[1], equalTo( 6.0 ) );

		assertThat( ver2[0], equalTo( 10 ) );
		assertThat( ver2[1], equalTo( 6.0 ) );

		assertThat( ver3[0], equalTo( 52 ) );
		assertThat( ver3[1], equalTo( 20.0 ) );
	}

	@DynamicTest
	@TestForIssue(jiraKey = "HHH-8036")
	public void testEntityIdProjection() {
		Integer maxId = (Integer) getAuditReader().createQuery()
				.forRevisionsOfEntity( IntTestEntity.class, true, true )
				.addProjection( AuditEntity.id().max() )
				.add( AuditEntity.revisionNumber().gt( 2 ) )
				.getSingleResult();
		assertThat( maxId, equalTo( 2 ) );
	}

	@DynamicTest
	@TestForIssue(jiraKey = "HHH-8036")
	public void testEntityIdRestriction() {
		List<IntTestEntity> list = getAuditReader().createQuery()
				.forRevisionsOfEntity( IntTestEntity.class, true, true )
				.add( AuditEntity.id().between( 2, 3 ) )
				.getResultList();
		assertThat( list, contains( new IntTestEntity( 2, 10 ), new IntTestEntity( 3, 8 ), new IntTestEntity( 2, 52 ) ) );
	}

	@DynamicTest
	@TestForIssue(jiraKey = "HHH-8036")
	public void testEntityIdOrdering() {
		List<IntTestEntity> list = getAuditReader().createQuery()
				.forRevisionsOfEntity( IntTestEntity.class, true, true )
				.add( AuditEntity.revisionNumber().lt( 2 ) )
				.addOrder( AuditEntity.id().desc() )
				.getResultList();
		assertThat( list, contains( new IntTestEntity( 2, 10 ), new IntTestEntity( 1, 2 ) ) );
	}

	@DynamicTest
	@TestForIssue(jiraKey = "HHH-8036")
	@Disabled("ClassCastException - BasicValuedNavigableReference cannot be cast to SQL AST Expression")
	public void testUnusualIdFieldName() {
		UnusualIdNamingEntity entity = (UnusualIdNamingEntity) getAuditReader().createQuery()
				.forRevisionsOfEntity( UnusualIdNamingEntity.class, true, true )
				.add( AuditEntity.id().like( "id1" ) )
				.getSingleResult();
		assertThat( entity, equalTo( new UnusualIdNamingEntity( "id1", "data1" ) ) );
	}

	@DynamicTest(expected = UnsupportedOperationException.class)
	@TestForIssue(jiraKey = "HHH-8036")
	public void testEntityIdModifiedFlagNotSupportedHasChanged() {
		List results = getAuditReader().createQuery()
				.forRevisionsOfEntity( IntTestEntity.class, true, true )
				.add( AuditEntity.id().hasChanged() )
				.getResultList();
	}

	@DynamicTest(expected = UnsupportedOperationException.class)
	@TestForIssue(jiraKey = "HHH-8036")
	public void testEntityIdModifiedFlagNotSupportedHasNotChanged() {
		List results = getAuditReader().createQuery()
				.forRevisionsOfEntity( IntTestEntity.class, true, true )
				.add( AuditEntity.id().hasNotChanged() )
				.getResultList();
	}
}