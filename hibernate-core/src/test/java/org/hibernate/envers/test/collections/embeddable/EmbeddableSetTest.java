/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.collections.embeddable;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.collections.EmbeddableSetEntity;
import org.hibernate.envers.test.support.domains.components.PartialAuditedComponent;
import org.hibernate.envers.test.support.domains.components.PartialAuditedNestedComponent;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * @author Kristoffer Lundberg (kristoffer at cambio dot se)
 */
@TestForIssue(jiraKey = "HHH-6613")
public class EmbeddableSetTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer ese1_id = null;

	private final PartialAuditedNestedComponent c4_1 = new PartialAuditedNestedComponent( "c41", "c41_value", "c41_description" );
	private final PartialAuditedNestedComponent c4_2 = new PartialAuditedNestedComponent( "c42", "c42_value2", "c42_description" );
	private final PartialAuditedComponent c3_1 = new PartialAuditedComponent( "c31", c4_1, c4_2 );
	private final PartialAuditedComponent c3_2 = new PartialAuditedComponent( "c32", c4_1, c4_2 );
	private final PartialAuditedComponent c3_3 = new PartialAuditedComponent( "c33", c4_1, c4_2 );
	private final PartialAuditedComponent c3_4 = new PartialAuditedComponent( "c34", c4_1, c4_2 );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EmbeddableSetEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1 (ese1: initially two elements)
				entityManager -> {
					EmbeddableSetEntity ese1 = new EmbeddableSetEntity();
					ese1.getComponentSet().add( c3_1 );
					ese1.getComponentSet().add( c3_3 );
					entityManager.persist( ese1 );

					this.ese1_id = ese1.getId();
				},

				// Revision (still 1) (ese1: removing non-existing element)
				entityManager -> {
					EmbeddableSetEntity ese1 = entityManager.find( EmbeddableSetEntity.class, ese1_id );
					ese1.getComponentSet().remove( c3_2 );
				},

				// Revision 2 (ese1: adding one element)
				entityManager -> {
					EmbeddableSetEntity ese1 = entityManager.find( EmbeddableSetEntity.class, ese1_id );
					ese1.getComponentSet().add( c3_2 );
				},

				// Revision (still 2) (ese1: adding one existing element)
				entityManager -> {
					EmbeddableSetEntity ese1 = entityManager.find( EmbeddableSetEntity.class, ese1_id );
					ese1.getComponentSet().add( c3_1 );
				},

				// Revision 3 (ese1: removing one existing element)
				entityManager -> {
					EmbeddableSetEntity ese1 = entityManager.find( EmbeddableSetEntity.class, ese1_id );
					ese1.getComponentSet().remove( c3_2 );
				},

				// Revision 4 (ese1: adding two elements)
				entityManager -> {
					EmbeddableSetEntity ese1 = entityManager.find( EmbeddableSetEntity.class, ese1_id );
					ese1.getComponentSet().add( c3_2 );
					ese1.getComponentSet().add( c3_4 );
				},

				// Revision 5 (ese1: removing two elements)
				entityManager -> {
					EmbeddableSetEntity ese1 = entityManager.find( EmbeddableSetEntity.class, ese1_id );
					ese1.getComponentSet().remove( c3_2 );
					ese1.getComponentSet().remove( c3_4 );
				},

				// Revision 6 (ese1: removing and adding two elements)
				entityManager -> {
					EmbeddableSetEntity ese1 = entityManager.find( EmbeddableSetEntity.class, ese1_id );
					ese1.getComponentSet().remove( c3_1 );
					ese1.getComponentSet().remove( c3_3 );
					ese1.getComponentSet().add( c3_2 );
					ese1.getComponentSet().add( c3_4 );
				},

				// Revision 7 (ese1: adding one element)
				entityManager -> {
					EmbeddableSetEntity ese1 = entityManager.find( EmbeddableSetEntity.class, ese1_id );
					ese1.getComponentSet().add( c3_1 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat(
				getAuditReader().getRevisions( EmbeddableSetEntity.class, ese1_id ),
				contains( 1, 2, 3, 4, 5, 6, 7 )
		);
	}

	@DynamicTest
	public void testHistoryOfEse1() {
		EmbeddableSetEntity rev1 = getAuditReader().find( EmbeddableSetEntity.class, ese1_id, 1 );
		EmbeddableSetEntity rev2 = getAuditReader().find( EmbeddableSetEntity.class, ese1_id, 2 );
		EmbeddableSetEntity rev3 = getAuditReader().find( EmbeddableSetEntity.class, ese1_id, 3 );
		EmbeddableSetEntity rev4 = getAuditReader().find( EmbeddableSetEntity.class, ese1_id, 4 );
		EmbeddableSetEntity rev5 = getAuditReader().find( EmbeddableSetEntity.class, ese1_id, 5 );
		EmbeddableSetEntity rev6 = getAuditReader().find( EmbeddableSetEntity.class, ese1_id, 6 );
		EmbeddableSetEntity rev7 = getAuditReader().find( EmbeddableSetEntity.class, ese1_id, 7 );

		assertThat( rev1.getComponentSet(), containsInAnyOrder( c3_1, c3_3 ) );
		assertThat( rev2.getComponentSet(), containsInAnyOrder( c3_1, c3_2, c3_3 ) );
		assertThat( rev3.getComponentSet(), containsInAnyOrder( c3_1, c3_3 ) );
		assertThat( rev4.getComponentSet(), containsInAnyOrder( c3_1, c3_2, c3_3, c3_4 ) );
		assertThat( rev5.getComponentSet(), containsInAnyOrder( c3_1, c3_3 ) );
		assertThat( rev6.getComponentSet(), containsInAnyOrder( c3_2, c3_4 ) );
		assertThat( rev7.getComponentSet(), containsInAnyOrder( c3_2, c3_4, c3_1 ) );;
	}
}