/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.modifiedflags;

import java.util.List;

import org.hibernate.envers.test.support.domains.collections.StringMapEntity;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class HasChangedStringMapTest extends AbstractModifiedFlagsEntityTest {
	private Integer sme1_id;
	private Integer sme2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { StringMapEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1 (sme1: initially empty, sme2: initially 1 mapping)
				entityManager -> {
					final StringMapEntity sme1 = new StringMapEntity();
					final StringMapEntity sme2 = new StringMapEntity();

					sme2.getStrings().put( "1", "a" );

					entityManager.persist( sme1 );
					entityManager.persist( sme2 );

					sme1_id = sme1.getId();
					sme2_id = sme2.getId();
				},

				// Revision 2 (sme1: adding 2 mappings, sme2: no changes)
				entityManager -> {
					final StringMapEntity sme1 = entityManager.find( StringMapEntity.class, sme1_id );
					final StringMapEntity sme2 = entityManager.find( StringMapEntity.class, sme2_id );

					sme1.getStrings().put( "1", "a" );
					sme1.getStrings().put( "2", "b" );
				},

				// Revision 3 (sme1: removing an existing mapping, sme2: replacing a value)
				entityManager -> {
					final StringMapEntity sme1 = entityManager.find( StringMapEntity.class, sme1_id );
					final StringMapEntity sme2 = entityManager.find( StringMapEntity.class, sme2_id );

					sme1.getStrings().remove( "1" );
					sme2.getStrings().put( "1", "b" );
				},

				// No revision (sme1: removing a non-existing mapping, sme2: replacing with the same value)
				entityManager -> {
					final StringMapEntity sme1 = entityManager.find( StringMapEntity.class, sme1_id );
					final StringMapEntity sme2 = entityManager.find( StringMapEntity.class, sme2_id );

					sme1.getStrings().remove( "3" );
					sme2.getStrings().put( "1", "b" );
				}
		);
	}

	@DynamicTest
	public void testHasChanged() {
		List changes1 = queryForPropertyHasChanged( StringMapEntity.class, sme1_id, "strings" );
		assertThat( extractRevisions( changes1 ), contains( 1, 2, 3 ) );

		List changes2 = queryForPropertyHasChanged( StringMapEntity.class, sme2_id, "strings" );
		assertThat( extractRevisions( changes2 ), contains( 1, 3 ) );

		List nonChanges1 = queryForPropertyHasNotChanged( StringMapEntity.class, sme1_id, "strings" );
		assertThat( nonChanges1, CollectionMatchers.isEmpty() );

		// In revision 2, there was no version generated for sme2_id
		List nonChanges2 = queryForPropertyHasNotChanged( StringMapEntity.class, sme2_id, "strings" );
		assertThat( nonChanges2, CollectionMatchers.isEmpty() );
	}
}