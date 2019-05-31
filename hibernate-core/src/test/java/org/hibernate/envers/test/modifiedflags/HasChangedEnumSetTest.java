/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.modifiedflags;

import java.util.List;

import org.hibernate.envers.test.support.domains.collections.EnumSetEntity;
import org.hibernate.envers.test.support.domains.collections.EnumSetEntity.E1;
import org.hibernate.envers.test.support.domains.collections.EnumSetEntity.E2;
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
@Disabled("Attempts to instantiate E1 or E2 enum via ManagedBeanRegistry throwing unable to locate no-arg constructor for bean.")
public class HasChangedEnumSetTest extends AbstractModifiedFlagsEntityTest {
	private Integer sse1_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { EnumSetEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1 (sse1: initially 1 element)
				entityManager -> {
					EnumSetEntity sse1 = new EnumSetEntity();
					sse1.getEnums1().add( E1.X );
					sse1.getEnums2().add( E2.A );

					entityManager.persist( sse1 );
					sse1_id = sse1.getId();
				},

				// Revision 2 (sse1: adding 1 element/removing a non-existing element)
				entityManager -> {
					EnumSetEntity sse1 = entityManager.find( EnumSetEntity.class, sse1_id );

					sse1.getEnums1().add( E1.Y );
					sse1.getEnums2().remove( E2.B );
				},

				// Revision 3 (sse1: removing 1 element/adding an exisiting element)
				entityManager -> {
					EnumSetEntity sse1 = entityManager.find( EnumSetEntity.class, sse1_id );

					sse1.getEnums1().remove( E1.X );
					sse1.getEnums2().add( E2.A );
				}
		);
	}

	@DynamicTest
	public void testHasChanged() throws Exception {
		final List enums1Changes = queryForPropertyHasChanged( EnumSetEntity.class, sse1_id, "enums1" );
		assertThat( extractRevisions( enums1Changes ), contains( 1, 2, 3 ) );

		final List enums2Changes = queryForPropertyHasChanged( EnumSetEntity.class, sse1_id, "enums2" );
		assertThat( extractRevisions( enums2Changes ), contains( 1 ) );

		final List enums1NoChanges = queryForPropertyHasNotChanged( EnumSetEntity.class, sse1_id, "enums1" );
		assertThat( enums1NoChanges, CollectionMatchers.isEmpty() );

		final List enums2NoChanges = queryForPropertyHasNotChanged( EnumSetEntity.class, sse1_id, "enums2" );
		assertThat( extractRevisions( enums2NoChanges ), contains( 2, 3 ) );
	}
}