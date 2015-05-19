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
import org.hibernate.envers.test.entities.onetomany.detached.DoubleListJoinColumnBidirectionalRefEdEntity1;
import org.hibernate.envers.test.entities.onetomany.detached.DoubleListJoinColumnBidirectionalRefEdEntity2;
import org.hibernate.envers.test.entities.onetomany.detached.DoubleListJoinColumnBidirectionalRefIngEntity;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.hibernate.envers.test.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.envers.test.tools.TestTools.makeList;

/**
 * Test for a double "fake" bidirectional mapping where one side uses @OneToMany+@JoinColumn
 * (and thus owns the relation), and the other uses a @ManyToOne(insertable=false, updatable=false).
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class HasChangedDoubleJoinColumnBidirectionalList extends AbstractModifiedFlagsEntityTest {
	private Integer ed1_1_id;
	private Integer ed2_1_id;
	private Integer ed1_2_id;
	private Integer ed2_2_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				DoubleListJoinColumnBidirectionalRefIngEntity.class,
				DoubleListJoinColumnBidirectionalRefEdEntity1.class,
				DoubleListJoinColumnBidirectionalRefEdEntity2.class
		};
	}

	@Test
	@Priority(10)
	public void createData() {
		EntityManager em = getEntityManager();

		DoubleListJoinColumnBidirectionalRefEdEntity1 ed1_1 = new DoubleListJoinColumnBidirectionalRefEdEntity1(
				"ed1_1",
				null
		);
		DoubleListJoinColumnBidirectionalRefEdEntity1 ed1_2 = new DoubleListJoinColumnBidirectionalRefEdEntity1(
				"ed1_2",
				null
		);

		DoubleListJoinColumnBidirectionalRefEdEntity2 ed2_1 = new DoubleListJoinColumnBidirectionalRefEdEntity2(
				"ed2_1",
				null
		);
		DoubleListJoinColumnBidirectionalRefEdEntity2 ed2_2 = new DoubleListJoinColumnBidirectionalRefEdEntity2(
				"ed2_2",
				null
		);

		DoubleListJoinColumnBidirectionalRefIngEntity ing1 = new DoubleListJoinColumnBidirectionalRefIngEntity( "coll1" );
		DoubleListJoinColumnBidirectionalRefIngEntity ing2 = new DoubleListJoinColumnBidirectionalRefIngEntity( "coll2" );

		// Revision 1 (ing1: ed1_1, ed2_1, ing2: ed1_2, ed2_2)
		em.getTransaction().begin();

		ing1.getReferences1().add( ed1_1 );
		ing1.getReferences2().add( ed2_1 );

		ing2.getReferences1().add( ed1_2 );
		ing2.getReferences2().add( ed2_2 );

		em.persist( ed1_1 );
		em.persist( ed1_2 );
		em.persist( ed2_1 );
		em.persist( ed2_2 );
		em.persist( ing1 );
		em.persist( ing2 );

		em.getTransaction().commit();

		// Revision 2 (ing1: ed1_1, ed1_2, ed2_1, ed2_2)
		em.getTransaction().begin();

		ing1 = em.find( DoubleListJoinColumnBidirectionalRefIngEntity.class, ing1.getId() );
		ing2 = em.find( DoubleListJoinColumnBidirectionalRefIngEntity.class, ing2.getId() );
		ed1_1 = em.find( DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_1.getId() );
		ed1_2 = em.find( DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_2.getId() );
		ed2_1 = em.find( DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_1.getId() );
		ed2_2 = em.find( DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_2.getId() );

		ing2.getReferences1().clear();
		ing2.getReferences2().clear();

		ing1.getReferences1().add( ed1_2 );
		ing1.getReferences2().add( ed2_2 );

		em.getTransaction().commit();
		em.clear();

		// Revision 3 (ing1: ed1_1, ed1_2, ed2_1, ed2_2)
		em.getTransaction().begin();

		ing1 = em.find( DoubleListJoinColumnBidirectionalRefIngEntity.class, ing1.getId() );
		ing2 = em.find( DoubleListJoinColumnBidirectionalRefIngEntity.class, ing2.getId() );
		ed1_1 = em.find( DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_1.getId() );
		ed1_2 = em.find( DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_2.getId() );
		ed2_1 = em.find( DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_1.getId() );
		ed2_2 = em.find( DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_2.getId() );

		ed1_1.setData( "ed1_1 bis" );
		ed2_2.setData( "ed2_2 bis" );

		em.getTransaction().commit();
		em.clear();

		// Revision 4 (ing1: ed2_2, ing2: ed2_1, ed1_1, ed1_2)
		em.getTransaction().begin();

		ing1 = em.find( DoubleListJoinColumnBidirectionalRefIngEntity.class, ing1.getId() );
		ing2 = em.find( DoubleListJoinColumnBidirectionalRefIngEntity.class, ing2.getId() );
		ed1_1 = em.find( DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_1.getId() );
		ed1_2 = em.find( DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_2.getId() );
		ed2_1 = em.find( DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_1.getId() );
		ed2_2 = em.find( DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_2.getId() );

		ing1.getReferences1().clear();
		ing2.getReferences1().add( ed1_1 );
		ing2.getReferences1().add( ed1_2 );

		ing1.getReferences2().remove( ed2_1 );
		ing2.getReferences2().add( ed2_1 );

		em.getTransaction().commit();
		em.clear();

		//

		ing1_id = ing1.getId();
		ing2_id = ing2.getId();

		ed1_1_id = ed1_1.getId();
		ed1_2_id = ed1_2.getId();
		ed2_1_id = ed2_1.getId();
		ed2_2_id = ed2_2.getId();
	}

	@Test
	public void testOwnerHasChanged() throws Exception {
		List list = queryForPropertyHasChanged(
				DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_1_id,
				"owner"
		);
		assertEquals( 2, list.size() );
		assertEquals( makeList( 1, 4 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasNotChanged(
				DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_1_id,
				"owner"
		);
		assertEquals( 1, list.size() );
		assertEquals( makeList( 3 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasChanged(
				DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_2_id,
				"owner"
		);
		assertEquals( 3, list.size() );
		assertEquals( makeList( 1, 2, 4 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasNotChanged(
				DoubleListJoinColumnBidirectionalRefEdEntity1.class, ed1_2_id,
				"owner"
		);
		assertEquals( 0, list.size() );
	}

	@Test
	public void testOwnerSecEntityHasChanged() throws Exception {
		List list = queryForPropertyHasChanged(
				DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_1_id,
				"owner"
		);
		assertEquals( 2, list.size() );
		assertEquals( makeList( 1, 4 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasNotChanged(
				DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_1_id,
				"owner"
		);
		assertEquals( 0, list.size() );

		list = queryForPropertyHasChanged(
				DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_2_id,
				"owner"
		);
		assertEquals( 2, list.size() );
		assertEquals( makeList( 1, 2 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasNotChanged(
				DoubleListJoinColumnBidirectionalRefEdEntity2.class, ed2_2_id,
				"owner"
		);
		assertEquals( 1, list.size() );
		assertEquals( makeList( 3 ), extractRevisionNumbers( list ) );
	}

	@Test
	public void testReferences1HasChanged() throws Exception {
		List list = queryForPropertyHasChanged(
				DoubleListJoinColumnBidirectionalRefIngEntity.class, ing1_id,
				"references1"
		);
		assertEquals( 3, list.size() );
		assertEquals( makeList( 1, 2, 4 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasChanged(
				DoubleListJoinColumnBidirectionalRefIngEntity.class, ing2_id,
				"references1"
		);
		assertEquals( 3, list.size() );
		assertEquals( makeList( 1, 2, 4 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasNotChanged(
				DoubleListJoinColumnBidirectionalRefIngEntity.class, ing1_id,
				"references1"
		);
		assertEquals( 0, list.size() );

		list = queryForPropertyHasNotChanged(
				DoubleListJoinColumnBidirectionalRefIngEntity.class, ing2_id,
				"references1"
		);
		assertEquals( 0, list.size() );
	}

	@Test
	public void testReferences2HasChanged() throws Exception {
		List list = queryForPropertyHasChanged(
				DoubleListJoinColumnBidirectionalRefIngEntity.class, ing1_id,
				"references2"
		);
		assertEquals( 3, list.size() );
		assertEquals( makeList( 1, 2, 4 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasChanged(
				DoubleListJoinColumnBidirectionalRefIngEntity.class, ing2_id,
				"references2"
		);
		assertEquals( 3, list.size() );
		assertEquals( makeList( 1, 2, 4 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasNotChanged(
				DoubleListJoinColumnBidirectionalRefIngEntity.class, ing1_id,
				"references2"
		);
		assertEquals( 0, list.size() );

		list = queryForPropertyHasNotChanged(
				DoubleListJoinColumnBidirectionalRefIngEntity.class, ing2_id,
				"references2"
		);
		assertEquals( 0, list.size() );
	}
}