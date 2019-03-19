/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.modifiedflags;

import java.util.List;

import org.hibernate.envers.test.support.domains.onetoone.bidirectional.BiRefEdEntity;
import org.hibernate.envers.test.support.domains.onetoone.bidirectional.BiRefIngEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class HasChangedBidirectionalTest2 extends AbstractModifiedFlagsEntityTest {
	private Integer ed1_id;
	private Integer ed2_id;
	private Integer ing1_id;
	private Integer ing2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { BiRefEdEntity.class, BiRefIngEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					final BiRefEdEntity ed1 = new BiRefEdEntity( 1, "data_ed_1" );
					final BiRefEdEntity ed2 = new BiRefEdEntity( 2, "data_ed_2" );

					entityManager.persist( ed1 );
					entityManager.persist( ed2 );

					this.ed1_id = ed1.getId();
					this.ed2_id = ed2.getId();
				},

				// Revision 2
				entityManager -> {
					final BiRefIngEntity ing1 = new BiRefIngEntity( 3, "data_ing_1" );
					final BiRefIngEntity ing2 = new BiRefIngEntity( 4, "data_ing_2" );

					final BiRefEdEntity ed1 = entityManager.find( BiRefEdEntity.class, ed1_id );
					ing1.setReference( ed1 );

					entityManager.persist( ing1 );
					entityManager.persist( ing2 );

					this.ing1_id = ing1.getId();
					this.ing2_id = ing2.getId();
				},

				// Revision 3
				entityManager -> {
					final BiRefEdEntity ed1 = entityManager.find( BiRefEdEntity.class, ed1_id );
					final BiRefIngEntity ing1 = entityManager.find( BiRefIngEntity.class, ing1_id );
					final BiRefIngEntity ing2 = entityManager.find( BiRefIngEntity.class, ing2_id );

					ing1.setReference( null );
					ing2.setReference( ed1 );
				},

				// Revision 4
				entityManager -> {
					final BiRefEdEntity ed2 = entityManager.find( BiRefEdEntity.class, ed2_id );
					final BiRefIngEntity ing1 = entityManager.find( BiRefIngEntity.class, ing1_id );
					final BiRefIngEntity ing2 = entityManager.find( BiRefIngEntity.class, ing2_id );
					
					ing1.setReference( ed2 );
					ing2.setReference( null );
				}
		);
	}

	@DynamicTest
	public void testHasChanged() throws Exception {
		final List referencingList1 = queryForPropertyHasChanged( BiRefEdEntity.class, ed1_id, "referencing" );
		assertThat( extractRevisions( referencingList1 ), contains( 2, 3, 4 ) );

		final List referencingList2 = queryForPropertyHasChanged( BiRefEdEntity.class, ed2_id, "referencing" );
		assertThat( extractRevisions( referencingList2 ), contains( 4 ) );
	}
}