/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.collections.embeddable;

import java.util.Collections;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.collections.embeddable.EmbeddableMapEntity;
import org.hibernate.envers.test.support.domains.components.PartialAuditedComponent;
import org.hibernate.envers.test.support.domains.components.PartialAuditedNestedComponent;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;

/**
 * @author Kristoffer Lundberg (kristoffer at cambio dot se)
 */
@TestForIssue(jiraKey = "HHH-6613")
public class EmbeddableMapTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer eme1_id = null;
	private Integer eme2_id = null;

	private final PartialAuditedNestedComponent c4_1 = new PartialAuditedNestedComponent( "c41", "c41_value", "c41_description" );
	private final PartialAuditedNestedComponent c4_2 = new PartialAuditedNestedComponent( "c42", "c42_value2", "c42_description" );
	private final PartialAuditedComponent c3_1 = new PartialAuditedComponent( "c31", c4_1, c4_2 );
	private final PartialAuditedComponent c3_2 = new PartialAuditedComponent( "c32", c4_1, c4_2 );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {EmbeddableMapEntity.class};
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1 (eme1: initialy empty, eme2: initialy 1 mapping)
				entityManager -> {
					final EmbeddableMapEntity eme1 = new EmbeddableMapEntity();
					final EmbeddableMapEntity eme2 = new EmbeddableMapEntity();
					eme2.getComponentMap().put( "1", c3_1 );

					entityManager.persist( eme1 );
					entityManager.persist( eme2 );

					this.eme1_id = eme1.getId();
					this.eme2_id = eme2.getId();
				},

				// Revision 2 (eme1: adding 2 mappings, eme2: no changes)
				entityManager -> {
					final EmbeddableMapEntity eme1 = entityManager.find( EmbeddableMapEntity.class, eme1_id );
					final EmbeddableMapEntity eme2 = entityManager.find( EmbeddableMapEntity.class, eme2_id );
					eme1.getComponentMap().put( "1", c3_1 );
					eme1.getComponentMap().put( "2", c3_2 );
				},

				// Revision 3 (eme1: removing an existing mapping, eme2: replacing a value)
				entityManager -> {
					final EmbeddableMapEntity eme1 = entityManager.find( EmbeddableMapEntity.class, eme1_id );
					final EmbeddableMapEntity eme2 = entityManager.find( EmbeddableMapEntity.class, eme2_id );
					eme1.getComponentMap().remove( "1" );
					eme2.getComponentMap().put( "1", c3_2 );
				},

				// No revision (eme1: removing a non-existing mapping, eme2: replacing with the same value)
				entityManager -> {
					final EmbeddableMapEntity eme1 = entityManager.find( EmbeddableMapEntity.class, eme1_id );
					final EmbeddableMapEntity eme2 = entityManager.find( EmbeddableMapEntity.class, eme2_id );
					eme1.getComponentMap().remove( "3" );
					eme2.getComponentMap().put( "1", c3_2 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( EmbeddableMapEntity.class, eme1_id ), contains( 1, 2, 3 ) );
		assertThat( getAuditReader().getRevisions( EmbeddableMapEntity.class, eme2_id ), contains( 1, 3 ) );
	}

	@DynamicTest
	public void testHistoryOfEme1() {
		EmbeddableMapEntity rev1 = getAuditReader().find( EmbeddableMapEntity.class, eme1_id, 1 );
		EmbeddableMapEntity rev2 = getAuditReader().find( EmbeddableMapEntity.class, eme1_id, 2 );
		EmbeddableMapEntity rev3 = getAuditReader().find( EmbeddableMapEntity.class, eme1_id, 3 );
		EmbeddableMapEntity rev4 = getAuditReader().find( EmbeddableMapEntity.class, eme1_id, 4 );

		assertThat( rev1.getComponentMap(), equalTo( Collections.EMPTY_MAP ) );

		assertThat( rev2.getComponentMap().size(), equalTo( 2 ) );
		assertThat( rev2.getComponentMap(), hasEntry( "1", c3_1 ) );
		assertThat( rev2.getComponentMap(), hasEntry( "2", c3_2 ) );

		assertThat( rev3.getComponentMap().size(), equalTo( 1 ) );
		assertThat( rev3.getComponentMap(), hasEntry( "2", c3_2 ) );

		assertThat( rev4.getComponentMap().size(), equalTo( 1 ) );
		assertThat( rev4.getComponentMap(), hasEntry( "2", c3_2 ) );
	}

	@DynamicTest
	public void testHistoryOfEme2() {
		EmbeddableMapEntity rev1 = getAuditReader().find( EmbeddableMapEntity.class, eme2_id, 1 );
		EmbeddableMapEntity rev2 = getAuditReader().find( EmbeddableMapEntity.class, eme2_id, 2 );
		EmbeddableMapEntity rev3 = getAuditReader().find( EmbeddableMapEntity.class, eme2_id, 3 );
		EmbeddableMapEntity rev4 = getAuditReader().find( EmbeddableMapEntity.class, eme2_id, 4 );

		assertThat( rev1.getComponentMap().size(), equalTo( 1 ) );
		assertThat( rev1.getComponentMap(), hasEntry( "1", c3_1 ) );

		assertThat( rev2.getComponentMap().size(), equalTo( 1 ) );
		assertThat( rev2.getComponentMap(), hasEntry( "1", c3_1 ) );

		assertThat( rev3.getComponentMap().size(), equalTo( 1 ) );
		assertThat( rev3.getComponentMap(), hasEntry( "1", c3_2 ) );

		assertThat( rev4.getComponentMap().size(), equalTo( 1 ) );
		assertThat( rev4.getComponentMap(), hasEntry( "1", c3_2 ) );
	}
}