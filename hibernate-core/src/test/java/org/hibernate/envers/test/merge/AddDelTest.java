/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.merge;

import org.hibernate.envers.test.EnversSessionFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;
import org.hibernate.envers.test.support.domains.merge.GivenIdStrEntity;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-6753")
public class AddDelTest extends EnversSessionFactoryBasedFunctionalTest {
	private Integer strEntityId;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { StrTestEntity.class, GivenIdStrEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				session -> {
					final GivenIdStrEntity entity = new GivenIdStrEntity( 1, "data" );
					session.save( entity );
				},

				// Revision 2
				session -> {
					final StrTestEntity strEntity = new StrTestEntity( "another data" );
					session.save( strEntity ); // Just to create second revision.

					final GivenIdStrEntity entity = session.get( GivenIdStrEntity.class, 1 );
					session.delete( entity ); // First try to remove the entity.
					session.save( entity ); // Then save it.

					this.strEntityId = strEntity.getId();
				},

				// Revision 3
				session -> {
					final GivenIdStrEntity entity = session.get( GivenIdStrEntity.class, 1 );
					session.delete( entity ); // First try to remove the entity.
					entity.setData( "modified data" ); // Then change it's state.
					session.save( entity ); // Finally save it.
				}
		);
	}

	@DynamicTest
	public void testRevisionsCountOfGivenIdStrEntity() {
		// Revision 2 has not changed entity's state.
		assertThat( getAuditReader().getRevisions( GivenIdStrEntity.class, 1 ), contains( 1, 3 ) );
		assertThat( getAuditReader().getRevisions( StrTestEntity.class, strEntityId ), contains( 2 ) );
	}

	@DynamicTest
	public void testHistoryOfGivenIdStrEntity() {
		final GivenIdStrEntity rev1 = new GivenIdStrEntity( 1, "data" );
		final GivenIdStrEntity rev3 = new GivenIdStrEntity( 1, "modified data" );

		assertThat( getAuditReader().find( GivenIdStrEntity.class, 1, 1 ), equalTo( rev1 ) );
		assertThat( getAuditReader().find( GivenIdStrEntity.class, 1, 3 ), equalTo( rev3 ) );
	}

	@DynamicTest
	public void testHistoryOfStrTestEntity() {
		final StrTestEntity rev2 = new StrTestEntity( strEntityId, "another data" );

		assertThat( getAuditReader().find( StrTestEntity.class, strEntityId, 2 ), equalTo( rev2 ) );
	}
}
