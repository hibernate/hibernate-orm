/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.modifiedflags;

import java.util.List;

import org.hibernate.envers.test.support.domains.collections.mapkey.ComponentMapKeyEntity;
import org.hibernate.envers.test.support.domains.components.Component1;
import org.hibernate.envers.test.support.domains.components.Component2;
import org.hibernate.envers.test.support.domains.components.ComponentTestEntity;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class HasChangedComponentMapKeyTest extends AbstractModifiedFlagsEntityTest {
	private Integer cmke_id;

	private Integer cte1_id;
	private Integer cte2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { ComponentMapKeyEntity.class, ComponentTestEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		final Component1 component1 = new Component1( "x1", "y2" );
		final Component2 component2 = new Component2( "a1", "b2" );

		inTransactions(
				// Revision 1 (initially 1 mapping)
				entityManager -> {
					final ComponentMapKeyEntity imke = new ComponentMapKeyEntity();

					final ComponentTestEntity cte1 = new ComponentTestEntity( component1, component2 );
					final ComponentTestEntity cte2 = new ComponentTestEntity( component1, component2 );

					entityManager.persist( cte1 );
					entityManager.persist( cte2 );

					imke.getIdmap().put( cte1.getComp1(), cte1 );
					entityManager.persist( imke );

					this.cmke_id = imke.getId();
					this.cte1_id = cte1.getId();
					this.cte2_id = cte2.getId();
				},

				// Revision 2
				entityManager -> {
					final ComponentTestEntity cte2 = entityManager.find( ComponentTestEntity.class, cte2_id );
					final ComponentMapKeyEntity imke = entityManager.find( ComponentMapKeyEntity.class, cmke_id );
					imke.getIdmap().put( cte2.getComp1(), cte2 );
				}
		);
	}

	@DynamicTest
	public void testHasChangedMapEntity() {
		final List idmapChangedList = queryForPropertyHasChanged( ComponentMapKeyEntity.class, cmke_id, "idmap" );
		assertThat( extractRevisions( idmapChangedList ), contains( 1, 2 ) );

		final List idmapNotChangedList = queryForPropertyHasNotChanged( ComponentMapKeyEntity.class, cmke_id, "idmap" );
		assertThat( idmapNotChangedList, CollectionMatchers.isEmpty() );
	}

	@DynamicTest
	public void testHasChangedComponentEntity() {
		final List changedList1 = queryForPropertyHasChanged( ComponentTestEntity.class, cte1_id, "comp1" );
		assertThat( extractRevisions( changedList1 ), contains( 1 ) );

		final List notChangedList1 = queryForPropertyHasNotChanged( ComponentTestEntity.class, cte1_id, "comp1" );
		assertThat( notChangedList1, CollectionMatchers.isEmpty() );

		final List changedList2 = queryForPropertyHasChanged( ComponentTestEntity.class, cte2_id, "comp1" );
		assertThat( extractRevisions( changedList2 ), contains( 1 ) );

		final List notChangedList2 = queryForPropertyHasNotChanged( ComponentTestEntity.class, cte2_id, "comp1" );
		assertThat( notChangedList2, CollectionMatchers.isEmpty() );
	}
}
