/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.collections;

import java.util.List;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.collections.StringListEntity;
import org.hibernate.envers.test.support.domains.collections.StringMapEntity;
import org.hibernate.envers.test.support.domains.collections.StringSetEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;

/**
 * @author Chris Cranford
 */
@Disabled("NYI - Requires implementation of StateArrayContributor#replace")
@TestForIssue(jiraKey = "HHH-11901")
public class CollectionNullValueTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer mapId;
	private Integer listId;
	private Integer setId;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { StringMapEntity.class, StringListEntity.class, StringSetEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		// Persist map with null values
		mapId = entityManagerFactoryScope().inTransaction(
				entityManager -> {
					final StringMapEntity sme = new StringMapEntity();
					sme.getStrings().put( "A", "B" );
					sme.getStrings().put( "B", null );
					entityManager.persist( sme );

					return sme.getId();
				} );

		// Update map with null values
		entityManagerFactoryScope().inTransaction(
				entityManager -> {
					final StringMapEntity sme = entityManager.find( StringMapEntity.class, mapId );
					sme.getStrings().put( "C", null );
					sme.getStrings().put( "D", "E" );
					sme.getStrings().remove( "A" );
					entityManager.merge( sme );
				} );

		// Persist list with null values
		listId = entityManagerFactoryScope().inTransaction(
				entityManager -> {
					final StringListEntity sle = new StringListEntity();
					sle.getStrings().add( "A" );
					sle.getStrings().add( null );
					entityManager.persist( sle );

					return sle.getId();
				} );

		// Update list with null values
		entityManagerFactoryScope().inTransaction(
				entityManager -> {
					final StringListEntity sle = entityManager.find( StringListEntity.class, listId );
					sle.getStrings().add( null );
					sle.getStrings().add( "D" );
					sle.getStrings().remove( "A" );
					entityManager.merge( sle );
				} );

		// Persist set with null values
		setId = entityManagerFactoryScope().inTransaction(
				entityManager -> {
					final StringSetEntity sse = new StringSetEntity();
					sse.getStrings().add( "A" );
					sse.getStrings().add( null );
					entityManager.persist( sse );

					return sse.getId();
				} );

		// Update set with null values
		entityManagerFactoryScope().inTransaction(
				entityManager -> {
					final StringSetEntity sse = entityManager.find( StringSetEntity.class, setId );
					sse.getStrings().add( null );
					sse.getStrings().add( "D" );
					sse.getStrings().remove( "A" );
					entityManager.merge( sse );
				} );
	}

	@DynamicTest
	public void testStringMapHistory() {
		final List<Number> revisions = getAuditReader().getRevisions( StringMapEntity.class, mapId );
		assertThat( revisions, contains( 1, 2 ) );

		final StringMapEntity rev1 = getAuditReader().find( StringMapEntity.class, mapId, 1 );
		assertThat( rev1.getStrings(), hasEntry( "A", "B" ) );

		final StringMapEntity rev2 = getAuditReader().find( StringMapEntity.class, mapId, 2 );
		assertThat( rev2.getStrings(), hasEntry( "D", "E" ) );
	}

	@DynamicTest
	public void testStringListHistory() {
		final List<Number> revisions = getAuditReader().getRevisions( StringListEntity.class, listId );
		assertThat( revisions, contains( 3, 4 ) );

		final StringListEntity rev3 = getAuditReader().find( StringListEntity.class, listId, 3 );
		assertThat( rev3.getStrings(), contains( "A" ) );

		// NOTE: the only reason this assertion expects a null element is because the collection is indexed.
		// ORM will return a list that consists of { null, "D" } and Envers should effectively mimic that.
		final StringListEntity rev4 = getAuditReader().find( StringListEntity.class, listId, 4 );
		assertThat( rev4.getStrings(), contains( null, "D" ) );
	}

	@DynamicTest
	public void testStringSetHistory() {
		final List<Number> revisions = getAuditReader().getRevisions( StringSetEntity.class, setId );
		assertThat( revisions, contains( 5, 6 ) );

		final StringSetEntity rev5 = getAuditReader().find( StringSetEntity.class, setId, 5 );
		assertThat( rev5.getStrings(), contains( "A" ) );

		final StringSetEntity rev6 = getAuditReader().find( StringSetEntity.class, setId, 6 );
		assertThat( rev6.getStrings(), contains( "D" ) );
	}

}
