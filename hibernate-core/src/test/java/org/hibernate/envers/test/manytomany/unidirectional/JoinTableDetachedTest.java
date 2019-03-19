/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.manytomany.unidirectional;

import java.util.HashSet;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;
import org.hibernate.envers.test.support.domains.manytomany.unidirectional.JoinTableEntity;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-8087")
public class JoinTableDetachedTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Long collectionEntityId = null;
	private Integer element1Id = null;
	private Integer element2Id = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { JoinTableEntity.class, StrTestEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		// Detached collection elements
		final StrTestEntity element1 = new StrTestEntity( "str1" );
		final StrTestEntity element2 = new StrTestEntity( "str2" );

		// Revision 1 - Addition
		inTransaction(
				entityManager -> {
					final JoinTableEntity collectionEntity = new JoinTableEntity( "some data" );
					collectionEntity.getReferences().add( element1 );
					collectionEntity.getReferences().add( element2 );

					entityManager.persist( element1 );
					entityManager.persist( element2 );
					entityManager.persist( collectionEntity );

					this.collectionEntityId = collectionEntity.getId();
					this.element1Id = element1.getId();
					this.element2Id = element2.getId();
				}
		);

		// Revision 2 - Simple modification
		inTransaction(
				entityManager -> {
					final JoinTableEntity entity = entityManager.find( JoinTableEntity.class, collectionEntityId );
					entity.setData( "some other data" );
					entityManager.merge( entity );
				}
		);

		// Revision 3 - Remove detached object from collection
		inTransaction(
				entityManager -> {
					final JoinTableEntity entity = entityManager.find( JoinTableEntity.class, collectionEntityId );
					entity.getReferences().remove( element1 );
					entityManager.merge( entity );
				}
		);

		// Revision 4 - Replace the collection
		inTransaction(
				entityManager -> {
					final JoinTableEntity entity = entityManager.find( JoinTableEntity.class, collectionEntityId );
					entity.setReferences( new HashSet<>() );
					entityManager.merge( entity );
				}
		);

		// Revision 5 - Add to collection
		inTransaction(
				entityManager -> {
					final JoinTableEntity entity = entityManager.find( JoinTableEntity.class, collectionEntityId );
					entity.getReferences().add( element1 ) ;
					entityManager.merge( entity );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( JoinTableEntity.class, collectionEntityId ), contains( 1, 2, 3, 4, 5 ) );

		assertThat( getAuditReader().getRevisions( StrTestEntity.class, element1Id ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( StrTestEntity.class, element2Id ), contains( 1 ) );
	}

	@DynamicTest
	public void testHistoryOfCollectionEntity() {
		// Revision 1
		JoinTableEntity collectionEntity = new JoinTableEntity( collectionEntityId, "some data" );
		StrTestEntity element1 = new StrTestEntity( element1Id, "str1" );
		StrTestEntity element2 = new StrTestEntity( element2Id, "str2" );
		collectionEntity.getReferences().add( element1 );
		collectionEntity.getReferences().add( element2 );
		JoinTableEntity ver1 = getAuditReader().find( JoinTableEntity.class, collectionEntityId, 1 );
		assertThat( ver1, equalTo( collectionEntity ) );
		assertThat( ver1.getReferences(), equalTo( collectionEntity.getReferences() ) );

		// Revision 2
		collectionEntity.setData( "some other data" );
		JoinTableEntity ver2 = getAuditReader().find( JoinTableEntity.class, collectionEntityId, 2 );
		assertThat( ver2, equalTo( collectionEntity ) );
		assertThat( ver2.getReferences(), equalTo( collectionEntity.getReferences() ) );

		// Revision 3
		collectionEntity.getReferences().remove( element1 );
		JoinTableEntity ver3 = getAuditReader().find( JoinTableEntity.class, collectionEntityId, 3 );
		assertThat( ver3, equalTo( collectionEntity ) );
		assertThat( ver3.getReferences(), equalTo( collectionEntity.getReferences() ) );

		// Revision 4
		collectionEntity.setReferences( new HashSet<StrTestEntity>() );
		JoinTableEntity ver4 = getAuditReader().find( JoinTableEntity.class, collectionEntityId, 4 );
		assertThat( ver4, equalTo( collectionEntity ) );
		assertThat( ver4.getReferences(), equalTo( collectionEntity.getReferences() ) );

		// Revision 5
		collectionEntity.getReferences().add( element1 );
		JoinTableEntity ver5 = getAuditReader().find( JoinTableEntity.class, collectionEntityId, 5 );
		assertThat( ver5, equalTo( collectionEntity ) );
		assertThat( ver5.getReferences(), equalTo( collectionEntity.getReferences() ) );
	}
}
