/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.modifiedflags;

import java.util.List;

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
public class HasChangedComponentsTest extends AbstractModifiedFlagsEntityTest {
	private Integer id1;
	private Integer id2;
	private Integer id3;
	private Integer id4;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { ComponentTestEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					final Component1 componentAb = new Component1( "a", "b" );
					final Component2 componentXy = new Component2( "x", "y" );

					final Component1 componentAb2 = new Component1( "a2", "b2" );
					final Component2 componentXy2 = new Component2( "x2", "y2" );

					final Component1 componentAb3 = new Component1( "a3", "b3" );
					final Component2 componentXy3 = new Component2( "x3", "y3" );

					final ComponentTestEntity cte1 = new ComponentTestEntity( componentAb, componentXy );
					final ComponentTestEntity cte2 = new ComponentTestEntity( componentAb2, componentXy2 );
					final ComponentTestEntity cte3 = new ComponentTestEntity( componentAb3, componentXy3 );
					final ComponentTestEntity cte4 = new ComponentTestEntity( null, null );

					entityManager.persist( cte1 );
					entityManager.persist( cte2 );
					entityManager.persist( cte3 );
					entityManager.persist( cte4 );

					id1 = cte1.getId();
					id2 = cte2.getId();
					id3 = cte3.getId();
					id4 = cte4.getId();
				},

				// Revision 2
				entityManager -> {
					final ComponentTestEntity cte1 = entityManager.find( ComponentTestEntity.class, id1 );
					final ComponentTestEntity cte2 = entityManager.find( ComponentTestEntity.class, id2 );
					final ComponentTestEntity cte3 = entityManager.find( ComponentTestEntity.class, id3 );
					final ComponentTestEntity cte4 = entityManager.find( ComponentTestEntity.class, id4 );

					cte1.setComp1( new Component1( "a'", "b'" ) );
					cte2.getComp1().setStr1( "a2'" );
					cte3.getComp2().setStr6( "y3'" );
					cte4.setComp1( new Component1() );
					cte4.getComp1().setStr1( "n" );
					cte4.setComp2( new Component2() );
					cte4.getComp2().setStr5( "m" );
				},

				// Revision 3
				entityManager -> {
					final ComponentTestEntity cte1 = entityManager.find( ComponentTestEntity.class, id1 );
					final ComponentTestEntity cte2 = entityManager.find( ComponentTestEntity.class, id2 );
					final ComponentTestEntity cte3 = entityManager.find( ComponentTestEntity.class, id3 );
					final ComponentTestEntity cte4 = entityManager.find( ComponentTestEntity.class, id4 );

					cte1.setComp2( new Component2( "x'", "y'" ) );
					cte3.getComp1().setStr2( "b3'" );
					cte4.setComp1( null );
					cte4.setComp2( null );
				},

				// Revision 4
				entityManager -> {
					final ComponentTestEntity cte2 = entityManager.find( ComponentTestEntity.class, id2 );
					entityManager.remove( cte2 );
				}
		);
	}

	@DynamicTest
	public void testModFlagProperties() {
		assertThat(
				extractModifiedPropertyNames(
						ComponentTestEntity.class.getName() + "_AUD",
						"_MOD"
				),
				contains( "comp1_MOD" )
		);
	}

	@DynamicTest(expected = IllegalArgumentException.class)
	public void testHasChangedNotAudited() {
		queryForPropertyHasChanged( ComponentTestEntity.class, id1, "comp2" );
	}

	@DynamicTest
	public void testHasChangedId1() {
		final List changedList = queryForPropertyHasChanged( ComponentTestEntity.class, id1, "comp1" );
		assertThat( extractRevisions( changedList ), contains( 1, 2 ) );

		final List notChangedList = queryForPropertyHasNotChanged( ComponentTestEntity.class, id1, "comp1" );
		assertThat( notChangedList, CollectionMatchers.isEmpty() );
	}

	@DynamicTest
	public void testHasChangedId2() {
		final List changedList = queryForPropertyHasChangedWithDeleted( ComponentTestEntity.class, id2, "comp1" );
		assertThat( extractRevisions( changedList ), contains( 1, 2, 4 ) );

		final List notChangedList = queryForPropertyHasNotChangedWithDeleted( ComponentTestEntity.class, id2, "comp1" );
		assertThat( notChangedList, CollectionMatchers.isEmpty() );
	}

	@DynamicTest
	public void testHasChangedId3() {
		final List changedList = queryForPropertyHasChangedWithDeleted( ComponentTestEntity.class, id3, "comp1" );
		assertThat( extractRevisions( changedList ), contains( 1, 3 ) );

		final List notChangedList = queryForPropertyHasNotChangedWithDeleted( ComponentTestEntity.class, id3, "comp1" );
		assertThat( notChangedList, CollectionMatchers.isEmpty() );
	}

	@DynamicTest
	public void testHasChangedId4() {
		final List changedList = queryForPropertyHasChangedWithDeleted( ComponentTestEntity.class, id4, "comp1" );
		assertThat( extractRevisions( changedList ), contains( 2, 3 ) );

		final List notChangedList = queryForPropertyHasNotChangedWithDeleted( ComponentTestEntity.class, id4, "comp1" );
		assertThat( extractRevisions( notChangedList ), contains( 1 ) );
	}
}
