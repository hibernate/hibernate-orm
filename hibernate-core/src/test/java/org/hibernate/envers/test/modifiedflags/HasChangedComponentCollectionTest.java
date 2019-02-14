/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.modifiedflags;

import org.hibernate.envers.test.support.domains.collections.embeddable.EmbeddableListEntity1;
import org.hibernate.envers.test.support.domains.components.PartialAuditedComponent;
import org.hibernate.envers.test.support.domains.components.PartialAuditedNestedComponent;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-6613")
public class HasChangedComponentCollectionTest extends AbstractModifiedFlagsEntityTest {
	private Integer ele1_id = null;

	private final PartialAuditedNestedComponent c4_1 = new PartialAuditedNestedComponent( "c41", "c41_value", "c41_description" );
	private final PartialAuditedNestedComponent c4_2 = new PartialAuditedNestedComponent( "c42", "c42_value2", "c42_description" );
	private final PartialAuditedComponent c3_1 = new PartialAuditedComponent( "c31", c4_1, c4_2 );
	private final PartialAuditedComponent c3_2 = new PartialAuditedComponent( "c32", c4_1, c4_2 );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { EmbeddableListEntity1.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				entityManager -> {
					final EmbeddableListEntity1 ele1 = new EmbeddableListEntity1();
					ele1.setOtherData( "data" );
					ele1.getComponentList().add( c3_1 );
					entityManager.persist( ele1 );

					ele1_id = ele1.getId();
				},

				// Revision (still 1) (ele1: removing non-existing element)
				entityManager -> {
					final EmbeddableListEntity1 ele1 = entityManager.find( EmbeddableListEntity1.class, ele1_id );
					ele1.getComponentList().remove( c3_2 );
				},

				// Revision 2 (ele1: updating singular property and removing non-existing element)
				entityManager -> {
					final EmbeddableListEntity1 ele1 = entityManager.find( EmbeddableListEntity1.class, ele1_id );
					ele1.setOtherData( "modified" );
					ele1.getComponentList().remove( c3_2 );
					entityManager.merge( ele1 );
				},

				// Revision 3 (ele1: adding one element)
				entityManager -> {
					final EmbeddableListEntity1 ele1 = entityManager.find( EmbeddableListEntity1.class, ele1_id );
					ele1.getComponentList().add( c3_2 );
				},

				// Revision 4 (ele1: adding one existing element)
				entityManager -> {
					final EmbeddableListEntity1 ele1 = entityManager.find( EmbeddableListEntity1.class, ele1_id );
					ele1.getComponentList().add( c3_1 );
				},

				// Revision 5 (ele1: removing one existing element)
				entityManager -> {
					final EmbeddableListEntity1 ele1 = entityManager.find( EmbeddableListEntity1.class, ele1_id );
					ele1.getComponentList().remove( c3_2 );
				},

				// Revision 6 (ele1: changing singular property only)
				entityManager -> {
					final EmbeddableListEntity1 ele1 = entityManager.find( EmbeddableListEntity1.class, ele1_id );
					ele1.setOtherData( "another modification" );
					entityManager.merge( ele1 );
				}
		);
	}

	@DynamicTest
	public void testHasChangedEle() {
		assertThat(
				getPropertyChangeRevisions( EmbeddableListEntity1.class, ele1_id, "componentList" ),
				contains( 1, 3, 4, 5 )
		);

		assertThat(
				getPropertyChangeRevisions( EmbeddableListEntity1.class, ele1_id, "otherData" ),
				contains( 1, 2, 6 )
		);
	}
}