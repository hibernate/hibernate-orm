/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.modifiedflags;

import java.util.List;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.components.Component1;
import org.hibernate.envers.test.entities.components.Component2;
import org.hibernate.envers.test.entities.components.ComponentTestEntity;
import org.hibernate.envers.test.tools.TestTools;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.hibernate.envers.test.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.envers.test.tools.TestTools.makeList;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class HasChangedComponents extends AbstractModifiedFlagsEntityTest {
	private Integer id1;
	private Integer id2;
	private Integer id3;
	private Integer id4;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {ComponentTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		ComponentTestEntity cte1 = new ComponentTestEntity( new Component1( "a", "b" ), new Component2( "x", "y" ) );
		ComponentTestEntity cte2 = new ComponentTestEntity(
				new Component1( "a2", "b2" ), new Component2(
				"x2",
				"y2"
		)
		);
		ComponentTestEntity cte3 = new ComponentTestEntity(
				new Component1( "a3", "b3" ), new Component2(
				"x3",
				"y3"
		)
		);
		ComponentTestEntity cte4 = new ComponentTestEntity( null, null );

		em.persist( cte1 );
		em.persist( cte2 );
		em.persist( cte3 );
		em.persist( cte4 );

		em.getTransaction().commit();

		// Revision 2
		em = getEntityManager();
		em.getTransaction().begin();

		cte1 = em.find( ComponentTestEntity.class, cte1.getId() );
		cte2 = em.find( ComponentTestEntity.class, cte2.getId() );
		cte3 = em.find( ComponentTestEntity.class, cte3.getId() );
		cte4 = em.find( ComponentTestEntity.class, cte4.getId() );

		cte1.setComp1( new Component1( "a'", "b'" ) );
		cte2.getComp1().setStr1( "a2'" );
		cte3.getComp2().setStr6( "y3'" );
		cte4.setComp1( new Component1() );
		cte4.getComp1().setStr1( "n" );
		cte4.setComp2( new Component2() );
		cte4.getComp2().setStr5( "m" );

		em.getTransaction().commit();

		// Revision 3
		em = getEntityManager();
		em.getTransaction().begin();

		cte1 = em.find( ComponentTestEntity.class, cte1.getId() );
		cte2 = em.find( ComponentTestEntity.class, cte2.getId() );
		cte3 = em.find( ComponentTestEntity.class, cte3.getId() );
		cte4 = em.find( ComponentTestEntity.class, cte4.getId() );

		cte1.setComp2( new Component2( "x'", "y'" ) );
		cte3.getComp1().setStr2( "b3'" );
		cte4.setComp1( null );
		cte4.setComp2( null );

		em.getTransaction().commit();

		// Revision 4
		em = getEntityManager();
		em.getTransaction().begin();

		cte2 = em.find( ComponentTestEntity.class, cte2.getId() );

		em.remove( cte2 );

		em.getTransaction().commit();

		id1 = cte1.getId();
		id2 = cte2.getId();
		id3 = cte3.getId();
		id4 = cte4.getId();
	}

	@Test
	public void testModFlagProperties() {
		assertEquals(
				TestTools.makeSet( "comp1_MOD" ),
				TestTools.extractModProperties(
						metadata().getEntityBinding(
								"org.hibernate.envers.test.entities.components.ComponentTestEntity_AUD"
						)
				)
		);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHasChangedNotAudited() throws Exception {
		queryForPropertyHasChanged( ComponentTestEntity.class, id1, "comp2" );
	}

	@Test
	public void testHasChangedId1() throws Exception {
		List list = queryForPropertyHasChanged( ComponentTestEntity.class, id1, "comp1" );
		assertEquals( 2, list.size() );
		assertEquals( makeList( 1, 2 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasNotChanged( ComponentTestEntity.class, id1, "comp1" );
		assertEquals( 0, list.size() );
	}

	@Test
	public void testHasChangedId2() throws Exception {
		List list = queryForPropertyHasChangedWithDeleted( ComponentTestEntity.class, id2, "comp1" );
		assertEquals( 3, list.size() );
		assertEquals( makeList( 1, 2, 4 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasNotChangedWithDeleted( ComponentTestEntity.class, id2, "comp1" );
		assertEquals( 0, list.size() );
	}

	@Test
	public void testHasChangedId3() throws Exception {
		List list = queryForPropertyHasChangedWithDeleted( ComponentTestEntity.class, id3, "comp1" );
		assertEquals( 2, list.size() );
		assertEquals( makeList( 1, 3 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasNotChangedWithDeleted( ComponentTestEntity.class, id3, "comp1" );
		assertEquals( 0, list.size() );
	}

	@Test
	public void testHasChangedId4() throws Exception {
		List list = queryForPropertyHasChangedWithDeleted( ComponentTestEntity.class, id4, "comp1" );
		assertEquals( 2, list.size() );
		assertEquals( makeList( 2, 3 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasNotChangedWithDeleted( ComponentTestEntity.class, id4, "comp1" );
		assertEquals( 1, list.size() );
		assertEquals( makeList( 1 ), extractRevisionNumbers( list ) );
	}
}
