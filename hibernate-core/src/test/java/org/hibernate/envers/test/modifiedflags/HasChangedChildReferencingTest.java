/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.envers.test.modifiedflags;

import java.util.List;

import org.hibernate.envers.test.support.domains.inheritance.joined.childrelation.ChildIngEntity;
import org.hibernate.envers.test.support.domains.inheritance.joined.childrelation.ParentNotIngEntity;
import org.hibernate.envers.test.support.domains.inheritance.joined.childrelation.ReferencedEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
@Disabled("NYI - Joined Inheritance Support")
public class HasChangedChildReferencingTest extends AbstractModifiedFlagsEntityTest {
	private Integer re_id1;
	private Integer re_id2;
	private Integer child_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { ChildIngEntity.class, ParentNotIngEntity.class, ReferencedEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					final ReferencedEntity re1 = new ReferencedEntity( 1 );
					final ReferencedEntity re2 = new ReferencedEntity( 10 );
					entityManager.persist( re1 );
					entityManager.persist( re2 );

					this.re_id1 = re1.getId();
					this.re_id2 = re2.getId();
				},

				// Revision 2
				entityManager -> {
					final ReferencedEntity re1 = entityManager.find( ReferencedEntity.class, re_id1 );

					ChildIngEntity cie = new ChildIngEntity( 100, "y", 1L );
					cie.setReferenced( re1 );
					entityManager.persist( cie );

					this.child_id = cie.getId();
				},

				// Revision 3
				entityManager -> {
					final ReferencedEntity re2 = entityManager.find( ReferencedEntity.class, re_id2 );
					final ChildIngEntity cie = entityManager.find( ChildIngEntity.class, child_id );
					cie.setReferenced( re2 );
				}
		);
	}

	@DynamicTest
	public void testReferencedEntityHasChanged() {
		final List referencing1ChangedList = queryForPropertyHasChanged( ReferencedEntity.class, re_id1, "referencing" );
		assertThat( extractRevisions( referencing1ChangedList ), contains( 2, 3 ) );

		final List referencing1NotChangedList = queryForPropertyHasNotChanged( ReferencedEntity.class, re_id1, "referencing" );
		assertThat( extractRevisions( referencing1NotChangedList ), contains( 1 ) );

		final List referencing2ChangedList = queryForPropertyHasChanged( ReferencedEntity.class, re_id2, "referencing" );
		assertThat( extractRevisions( referencing2ChangedList ), contains( 3 ) );
	}

}