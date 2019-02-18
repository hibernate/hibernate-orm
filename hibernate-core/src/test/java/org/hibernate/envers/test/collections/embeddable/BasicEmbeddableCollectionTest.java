/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.collections.embeddable;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.collections.embeddable.DarkCharacter;
import org.hibernate.envers.test.support.domains.collections.embeddable.Name;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-6613")
public class BasicEmbeddableCollectionTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private int id = -1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { DarkCharacter.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1 - empty element collection
				entityManager -> {
					DarkCharacter darkCharacter = new DarkCharacter( 1, 1 );
					entityManager.persist( darkCharacter );
					id = darkCharacter.getId();
				},

				// Revision 2 - adding collection element
				entityManager -> {
					DarkCharacter darkCharacter = entityManager.find( DarkCharacter.class, id );
					darkCharacter.getNames().add( new Name( "Action", "Hank" ) );
					entityManager.merge( darkCharacter );
				},

				// Revision 3 - adding another collection element
				entityManager -> {
					DarkCharacter darkCharacter = entityManager.find( DarkCharacter.class, id );
					darkCharacter.getNames().add( new Name( "Green", "Lantern" ) );
					entityManager.merge( darkCharacter );
				},

				// Revision 4 - removing single collection element
				entityManager -> {
					DarkCharacter darkCharacter = entityManager.find( DarkCharacter.class, id );
					darkCharacter.getNames().remove( new Name( "Action", "Hank" ) );
					entityManager.merge( darkCharacter );
				},

				// Revision 5 - removing all collection elements
				entityManager -> {
					DarkCharacter darkCharacter = entityManager.find( DarkCharacter.class, id );
					darkCharacter.getNames().clear();
					entityManager.merge( darkCharacter );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCount() {
		assertThat( getAuditReader().getRevisions( DarkCharacter.class, id ), contains( 1, 2, 3, 4, 5 ) );
	}

	@DynamicTest
	public void testHistoryOfCharacter() {
		DarkCharacter darkCharacter = new DarkCharacter( id, 1 );

		DarkCharacter ver1 = getAuditReader().find( DarkCharacter.class, id, 1 );
		assertThat( ver1, equalTo( darkCharacter ) );
		assertThat( ver1.getNames(), CollectionMatchers.isEmpty() );

		darkCharacter.getNames().add( new Name( "Action", "Hank" ) );
		DarkCharacter ver2 = getAuditReader().find( DarkCharacter.class, id, 2 );
		assertThat( ver2, equalTo( darkCharacter ) );
		assertThat( ver2.getNames(), equalTo( darkCharacter.getNames() ) );

		darkCharacter.getNames().add( new Name( "Green", "Lantern" ) );
		DarkCharacter ver3 = getAuditReader().find( DarkCharacter.class, id, 3 );
		assertThat( ver3, equalTo( darkCharacter ) );
		assertThat( ver3.getNames(), equalTo( darkCharacter.getNames() ) );

		darkCharacter.getNames().remove( new Name( "Action", "Hank" ) );
		DarkCharacter ver4 = getAuditReader().find( DarkCharacter.class, id, 4 );
		assertThat( ver4, equalTo( darkCharacter ) );
		assertThat( ver4.getNames(), equalTo( darkCharacter.getNames() ) );

		darkCharacter.getNames().clear();
		DarkCharacter ver5 = getAuditReader().find( DarkCharacter.class, id, 5 );
		assertThat( ver5, equalTo( darkCharacter ) );
		assertThat( ver5.getNames(), equalTo( darkCharacter.getNames() ) );
	}
}
