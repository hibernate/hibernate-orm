/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.query;

import java.util.List;
import java.util.Map;

import org.hibernate.envers.RevisionType;
import org.hibernate.envers.configuration.EnversSettings;
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
import static org.hamcrest.Matchers.equalTo;

/**
 * A test which checks if the data of a deleted entity is stored when the setting is on.
 *
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings({"unchecked"})
public class StoreDeletedDataTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id1;
	private Integer id2;
	private Integer id3;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { StrIntTestEntity.class };
	}

	@Override
	protected void addSettings(Map<String, Object> settings) {
		super.addSettings( settings );
		settings.put( EnversSettings.STORE_DATA_AT_DELETE, "true" );
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					final StrIntTestEntity site1 = new StrIntTestEntity( "a", 10 );
					entityManager.persist( site1 );
					id1 = site1.getId();
				},

				// Revision 2
				entityManager -> {
					final StrIntTestEntity site1 = entityManager.find( StrIntTestEntity.class, id1 );
					entityManager.remove( site1 );
				},

				// Revision 3
				entityManager -> {
					final StrIntTestEntity site2 = new StrIntTestEntity( "b", 20 );
					entityManager.persist( site2 );
					id2 = site2.getId();

					final StrIntTestEntity site3 = new StrIntTestEntity( "c", 30 );
					entityManager.persist( site3 );
					id3 = site3.getId();
				},

				// Revision 4
				entityManager -> {
					final StrIntTestEntity site2 = entityManager.find( StrIntTestEntity.class, id2 );
					final StrIntTestEntity site3 = entityManager.find( StrIntTestEntity.class, id3 );
					entityManager.remove( site2 );
					entityManager.remove( site3 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsPropertyEqQuery() {
		List revs_id1 = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.add( AuditEntity.id().eq( id1 ) )
				.getResultList();

		assertThat( revs_id1, CollectionMatchers.hasSize( 2 ) );
		assertThat( ( (Object[]) revs_id1.get( 0 ))[0], equalTo( new StrIntTestEntity( "a", 10, id1 ) ) );
		assertThat( ( (Object[]) revs_id1.get( 1 ))[0], equalTo( new StrIntTestEntity( "a", 10, id1 ) ) );
	}

	@DynamicTest
	@TestForIssue(jiraKey = "HHH-7800")
	public void testMaximizeInDisjunction() {
		final AuditDisjunction disjunction = AuditEntity.disjunction()
				.add( AuditEntity.revisionNumber().maximize()
						.add( AuditEntity.id().eq( id2 ) )
						.add( AuditEntity.revisionType().ne( RevisionType.DEL ) ) )
				.add( AuditEntity.revisionNumber().maximize()
						.add( AuditEntity.id().eq( id3 ) )
						.add( AuditEntity.revisionType().ne( RevisionType.DEL ) ) );

		final List<?> beforeDeletionRevisions = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, false )
				.add( disjunction )
				.addOrder( AuditEntity.property( "id" ).asc() )
				.getResultList();

		assertThat( beforeDeletionRevisions, CollectionMatchers.hasSize( 2 ) );

		final Object[] result1 = (Object[]) beforeDeletionRevisions.get( 0 );
		final Object[] result2 = (Object[]) beforeDeletionRevisions.get( 1 );

		assertThat( result1[0], equalTo( new StrIntTestEntity( "b", 20, id2 ) ) );
		// Making sure that we have received an entity added at revision 3.
		assertThat( ( (SequenceIdRevisionEntity) result1[1] ).getId(), equalTo( 3 ) );
		assertThat( result1[2], equalTo( RevisionType.ADD ) );

		assertThat( result2[0], equalTo( new StrIntTestEntity( "c", 30, id3  ) ) );
		// Making sure that we have received an entity added at revision 3.
		assertThat( ( (SequenceIdRevisionEntity) result2[1] ).getId(), equalTo( 3 ) );
		assertThat( result2[2], equalTo( RevisionType.ADD ) );
	}
}