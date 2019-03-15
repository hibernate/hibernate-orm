/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.modifiedflags;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.envers.test.support.domains.onetomany.ListRefEdEntity;
import org.hibernate.envers.test.support.domains.onetomany.ListRefIngEntity;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class HasChangedMergeTest extends AbstractModifiedFlagsEntityTest {
	private Integer parent1Id = null;
	private Integer child1Id = null;

	private Integer parent2Id = null;
	private Integer child2Id = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { ListRefEdEntity.class, ListRefIngEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1 - data preparation
				entityManager -> {
					final ListRefEdEntity parent1 = new ListRefEdEntity( 1, "initial data" );
					// Empty collection is not the same as null reference.
					parent1.setReffering( new ArrayList<>() );
					ListRefEdEntity parent2 = new ListRefEdEntity( 2, "initial data" );
					parent2.setReffering( new ArrayList<>() );

					entityManager.persist( parent1 );
					entityManager.persist( parent2 );

					this.parent1Id = parent1.getId();
					this.parent2Id = parent2.getId();
				},

				// Revision 2 - inserting new child entity and updating parent
				entityManager -> {
					final ListRefEdEntity parent1 = entityManager.find( ListRefEdEntity.class, parent1Id );
					ListRefIngEntity child1 = new ListRefIngEntity( 1, "initial data", parent1 );

					entityManager.persist( child1 );
					this.child1Id = child1.getId();

					parent1.setData( "updated data" );
					entityManager.merge( parent1 );
				},

				// Revision 3 - Updating parent, flushing and adding new child
				entityManager -> {
					ListRefEdEntity parent2 = entityManager.find( ListRefEdEntity.class, parent2Id );
					parent2.setData( "updated data" );
					parent2 = entityManager.merge( parent2 );
					entityManager.flush();

					final ListRefIngEntity child2 = new ListRefIngEntity( 2, "initial data", parent2 );
					entityManager.persist( child2 );
					this.child2Id = child2.getId();
				}
		);
	}

	@DynamicTest
	@TestForIssue(jiraKey = "HHH-7948")
	public void testOneToManyInsertChildUpdateParent() {
		List list = queryForPropertyHasChanged( ListRefEdEntity.class, parent1Id, "data" );
		assertThat( extractRevisions( list ), contains( 1, 2 ) );

		list = queryForPropertyHasChanged( ListRefEdEntity.class, parent1Id, "reffering" );
		assertThat( extractRevisions( list ), contains( 1, 2 ) );

		list = queryForPropertyHasChanged( ListRefIngEntity.class, child1Id, "reference" );
		assertThat( extractRevisions( list ), contains( 2 ) );
	}

	@DynamicTest
	@TestForIssue(jiraKey = "HHH-7948")
	public void testOneToManyUpdateParentInsertChild() {
		List list = queryForPropertyHasChanged( ListRefEdEntity.class, parent2Id, "data" );
		assertThat( extractRevisions( list ), contains( 1, 3 ) );

		list = queryForPropertyHasChanged( ListRefEdEntity.class, parent2Id, "reffering" );
		assertThat( extractRevisions( list ), contains( 1, 3 ) );

		list = queryForPropertyHasChanged( ListRefIngEntity.class, child2Id, "reference" );
		assertThat( extractRevisions( list ), contains( 3 ) );
	}
}
