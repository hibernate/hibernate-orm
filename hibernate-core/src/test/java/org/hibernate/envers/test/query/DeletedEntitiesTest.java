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
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrIntTestEntity;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class DeletedEntitiesTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id2;

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
					final StrIntTestEntity site2 = new StrIntTestEntity( "b", 11 );

					entityManager.persist( site1 );
					entityManager.persist( site2 );

					id2 = site2.getId();
				},

				// Revision 2
				entityManager -> {
					final StrIntTestEntity site2 = entityManager.find( StrIntTestEntity.class, id2 );
					entityManager.remove( site2 );
				}
		);
	}

	@DynamicTest
	public void testProjectionsInEntitiesAtRevision() {
		assertThat(
				getAuditReader().createQuery().forEntitiesAtRevision( StrIntTestEntity.class, 1 ).getResultList(),
				CollectionMatchers.hasSize( 2 )
		);

		assertThat(
				getAuditReader().createQuery().forEntitiesAtRevision( StrIntTestEntity.class, 2 ).getResultList(),
				CollectionMatchers.hasSize( 1 )
		);

		assertThat(
				getAuditReader().createQuery().forEntitiesAtRevision( StrIntTestEntity.class, 1 )
						.addProjection( AuditEntity.id().count() )
						.getResultList()
						.get( 0 )
				,
				equalTo( 2L )
		);

		assertThat(
				getAuditReader().createQuery().forEntitiesAtRevision( StrIntTestEntity.class, 2 )
						.addProjection( AuditEntity.id().count() )
						.getResultList()
						.get( 0 )
				,
				equalTo( 1L )
		);
	}

	@DynamicTest
	public void testRevisionsOfEntityWithoutDelete() {
		List result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, false )
				.add( AuditEntity.id().eq( id2 ) )
				.getResultList();

		assertThat( result, CollectionMatchers.hasSize( 1 ) );

		final Object[] objectArray = (Object[]) result.get( 0 );
		assertThat( objectArray[0], equalTo( new StrIntTestEntity( "b", 11, id2 ) ) );
		assertThat( objectArray[1], instanceOf( SequenceIdRevisionEntity.class ) );
		assertThat( ( (SequenceIdRevisionEntity) objectArray[1] ).getId(), equalTo( 1 ) );
		assertThat( objectArray[2], equalTo( RevisionType.ADD ) );
	}
}
