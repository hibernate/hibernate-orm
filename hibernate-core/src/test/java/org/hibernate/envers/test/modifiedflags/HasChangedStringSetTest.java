/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.modifiedflags;

import java.util.List;

import org.hibernate.envers.test.support.domains.collections.StringSetEntity;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class HasChangedStringSetTest extends AbstractModifiedFlagsEntityTest {
	private Integer sse1_id;
	private Integer sse2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { StringSetEntity.class };
	}

	@DynamicTest
	public void prepareAuditData() {
		inTransactions(
				// Revision 1 (sse1: initially empty, sse2: initially 2 elements)
				entityManager -> {
					final StringSetEntity sse1 = new StringSetEntity();
					final StringSetEntity sse2 = new StringSetEntity();

					sse2.getStrings().add( "sse2_string1" );
					sse2.getStrings().add( "sse2_string2" );

					entityManager.persist( sse1 );
					entityManager.persist( sse2 );

					sse1_id = sse1.getId();
					sse2_id = sse2.getId();
				},

				// Revision 2 (sse1: adding 2 elements, sse2: adding an existing element)
				entityManager -> {
					final StringSetEntity sse1 = entityManager.find( StringSetEntity.class, sse1_id );
					final StringSetEntity sse2 = entityManager.find( StringSetEntity.class, sse2_id );

					sse1.getStrings().add( "sse1_string1" );
					sse1.getStrings().add( "sse1_string2" );

					sse2.getStrings().add( "sse2_string1" );
				},

				// Revision 3 (sse1: removing a non-existing element, sse2: removing one element)
				entityManager -> {
					final StringSetEntity sse1 = entityManager.find( StringSetEntity.class, sse1_id );
					final StringSetEntity sse2 = entityManager.find( StringSetEntity.class, sse2_id );

					sse1.getStrings().remove( "sse1_string3" );
					sse2.getStrings().remove( "sse2_string1" );
				}
		);
	}

	@DynamicTest
	public void testHasChanged() throws Exception {
		List changes1 = queryForPropertyHasChanged( StringSetEntity.class, sse1_id, "strings" );
		assertThat( extractRevisions( changes1 ), contains( 1, 2 ) );

		List changes2 = queryForPropertyHasChanged( StringSetEntity.class, sse2_id, "strings" );
		assertThat( extractRevisions( changes2 ), contains( 1, 3 ) );

		List nonChanges1 = queryForPropertyHasNotChanged( StringSetEntity.class, sse1_id, "strings" );
		assertThat( nonChanges1, CollectionMatchers.isEmpty() );

		// In revision 2, there was no version generated for sse2_id
		List nonChanges2 = queryForPropertyHasNotChanged( StringSetEntity.class, sse2_id, "strings" );
		assertThat( nonChanges2, CollectionMatchers.isEmpty() );
	}
}