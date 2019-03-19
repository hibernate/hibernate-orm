/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.modifiedflags;

import org.hibernate.envers.test.support.domains.basic.StrTestEntity;
import org.hibernate.envers.test.support.domains.collections.mapkey.IdMapKeyEntity;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class HasChangedIdMapKeyTest extends AbstractModifiedFlagsEntityTest {
	private Integer imke_id;
	private Integer ste1_id;
	private Integer ste2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { IdMapKeyEntity.class, StrTestEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					final StrTestEntity ste1 = new StrTestEntity( "x" );
					final StrTestEntity ste2 = new StrTestEntity( "y" );
					entityManager.persist( ste1 );
					entityManager.persist( ste2 );

					final IdMapKeyEntity imke = new IdMapKeyEntity();
					imke.getIdmap().put( ste1.getId(), ste1 );
					entityManager.persist( imke );

					this.imke_id = imke.getId();
					this.ste1_id = ste1.getId();
					this.ste2_id = ste2.getId();
				},

				// Revision 2
				entityManager -> {
					final StrTestEntity ste2 = entityManager.find( StrTestEntity.class, ste2_id );
					final IdMapKeyEntity imke = entityManager.find( IdMapKeyEntity.class, imke_id );

					imke.getIdmap().put( ste2_id, ste2 );
				}
		);
	}

	@DynamicTest
	public void testHasChanged() {
		assertThat(
				extractRevisions( queryForPropertyHasChanged( IdMapKeyEntity.class, imke_id, "idmap" ) ),
				contains( 1, 2 )
		);

		assertThat(
				queryForPropertyHasNotChanged( IdMapKeyEntity.class, imke_id, "idmap" ),
				CollectionMatchers.isEmpty()
		);
	}
}