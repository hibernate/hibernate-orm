/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.collections.embeddable;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.collections.embeddable.EmbeddableListEntity1;
import org.hibernate.envers.test.support.domains.components.PartialAuditedComponent;
import org.hibernate.envers.test.support.domains.components.PartialAuditedNestedComponent;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * @author Kristoffer Lundberg (kristoffer at cambio dot se)
 */
@TestForIssue(jiraKey = "HHH-6613")
public class EmbeddableList1Test extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer ele1_id = null;

	private final PartialAuditedNestedComponent c4_1 = new PartialAuditedNestedComponent( "c41", "c41_value", "c41_description" );
	private final PartialAuditedNestedComponent c4_2 = new PartialAuditedNestedComponent( "c42", "c42_value2", "c42_description" );
	private final PartialAuditedComponent c3_1 = new PartialAuditedComponent( "c31", c4_1, c4_2 );
	private final PartialAuditedComponent c3_2 = new PartialAuditedComponent( "c32", c4_1, c4_2 );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EmbeddableListEntity1.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1 (ele1: initially 1 element in both collections)
				entityManager -> {
					final EmbeddableListEntity1 ele1 = new EmbeddableListEntity1();
					ele1.getComponentList().add( c3_1 );
					entityManager.persist( ele1 );

					ele1_id = ele1.getId();
				},

				// Revision (still 1) (ele1: removing non-existing element)
				entityManager -> {
					final EmbeddableListEntity1 ele1 = entityManager.find( EmbeddableListEntity1.class, ele1_id );
					ele1.getComponentList().remove( c3_2 );
				},

				// Revision 2 (ele1: adding one element)
				entityManager -> {
					final EmbeddableListEntity1 ele1 = entityManager.find( EmbeddableListEntity1.class, ele1_id );
					ele1.getComponentList().add( c3_2 );

				},

				// Revision 3 (ele1: adding one existing element)
				entityManager -> {
					final EmbeddableListEntity1 ele1 = entityManager.find( EmbeddableListEntity1.class, ele1_id );
					ele1.getComponentList().add( c3_1 );
				},

				// Revision 4 (ele1: removing one existing element)
				entityManager -> {
					final EmbeddableListEntity1 ele1 = entityManager.find( EmbeddableListEntity1.class, ele1_id );
					ele1.getComponentList().remove( c3_2 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( EmbeddableListEntity1.class, ele1_id ), contains( 1, 2, 3, 4 ) );
	}

	@DynamicTest
	public void testHistoryOfEle1() {
		EmbeddableListEntity1 rev1 = getAuditReader().find( EmbeddableListEntity1.class, ele1_id, 1 );
		EmbeddableListEntity1 rev2 = getAuditReader().find( EmbeddableListEntity1.class, ele1_id, 2 );
		EmbeddableListEntity1 rev3 = getAuditReader().find( EmbeddableListEntity1.class, ele1_id, 3 );
		EmbeddableListEntity1 rev4 = getAuditReader().find( EmbeddableListEntity1.class, ele1_id, 4 );

		assertThat( rev1.getComponentList(), contains( c3_1 ) );
		assertThat( rev2.getComponentList(), contains( c3_1, c3_2 ) );
		assertThat( rev3.getComponentList(), contains( c3_1, c3_2, c3_1 ) );
		assertThat( rev4.getComponentList(), contains( c3_1, c3_1 ) );
	}
}
