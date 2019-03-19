/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.envers.test.modifiedflags;

import java.util.List;

import org.hibernate.envers.test.support.domains.inheritance.joined.ChildEntity;
import org.hibernate.envers.test.support.domains.inheritance.joined.ParentEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
@Disabled("NYI - Joined Inheritance Support")
public class HasChangedChildAuditingTest extends AbstractModifiedFlagsEntityTest {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { ChildEntity.class, ParentEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					final ChildEntity child = new ChildEntity( 1, "x", 1L );
					entityManager.persist( child );

					this.id1 = child.getId();
				},

				// Revision 2
				entityManager -> {
					final ChildEntity child = entityManager.find( ChildEntity.class, this.id1 );
					child.setData( "y" );
					child.setNumVal( 2L );
				}
		);
	}

	@DynamicTest
	public void testChildHasChanged() {
		final List child1DataChangedList = queryForPropertyHasChanged( ChildEntity.class, id1, "data" );
		assertThat( extractRevisions( child1DataChangedList ), contains( 1, 2 ) );

		final List child1NumValChangedList = queryForPropertyHasChanged( ChildEntity.class, id1, "numVal" );
		assertThat( extractRevisions( child1NumValChangedList ), contains( 1, 2 ) );

		final List child1DataNotChangedList = queryForPropertyHasNotChanged( ChildEntity.class, id1, "data" );
		assertThat( child1DataNotChangedList, CollectionMatchers.isEmpty() );

		final List child1NumValNotChangedList = queryForPropertyHasNotChanged( ChildEntity.class, id1, "numVal" );
		assertThat( child1NumValNotChangedList, CollectionMatchers.isEmpty() );
	}

	@DynamicTest
	public void testParentHasChanged() {
		final List parentChangedList = queryForPropertyHasChanged( ParentEntity.class, id1, "data" );
		assertThat( extractRevisions( parentChangedList ), contains( 1, 2 ) );

		final List parentNotChangedList = queryForPropertyHasNotChanged( ParentEntity.class, id1, "data" );
		assertThat( parentNotChangedList, CollectionMatchers.isEmpty() );
	}
}