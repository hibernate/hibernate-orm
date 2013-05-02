package org.hibernate.envers.test.integration.modifiedflags;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.onetomany.ListRefEdEntity;
import org.hibernate.envers.test.entities.onetomany.ListRefIngEntity;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.hibernate.envers.test.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.envers.test.tools.TestTools.makeList;
import static org.junit.Assert.assertEquals;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class HasChangedMergeTest extends AbstractModifiedFlagsEntityTest {
	private Integer parent1Id = null;
	private Integer child1Id = null;

	private Integer parent2Id = null;
	private Integer child2Id = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {ListRefEdEntity.class, ListRefIngEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1 - data preparation
		em.getTransaction().begin();
		ListRefEdEntity parent1 = new ListRefEdEntity( 1, "initial data" );
		parent1.setReffering( new ArrayList<ListRefIngEntity>() ); // Empty collection is not the same as null reference.
		ListRefEdEntity parent2 = new ListRefEdEntity( 2, "initial data" );
		parent2.setReffering( new ArrayList<ListRefIngEntity>() );
		em.persist( parent1 );
		em.persist( parent2 );
		em.getTransaction().commit();

		// Revision 2 - inserting new child entity and updating parent
		em.getTransaction().begin();
		parent1 = em.find( ListRefEdEntity.class, parent1.getId() );
		ListRefIngEntity child1 = new ListRefIngEntity( 1, "initial data", parent1 );
		em.persist( child1 );
		parent1.setData( "updated data" );
		parent1 = em.merge( parent1 );
		em.getTransaction().commit();

		// Revision 3 - updating parent, flushing and adding new child
		em.getTransaction().begin();
		parent2 = em.find( ListRefEdEntity.class, parent2.getId() );
		parent2.setData( "updated data" );
		parent2 = em.merge( parent2 );
		em.flush();
		ListRefIngEntity child2 = new ListRefIngEntity( 2, "initial data", parent2 );
		em.persist( child2 );
		em.getTransaction().commit();

		parent1Id = parent1.getId();
		child1Id = child1.getId();

		parent2Id = parent2.getId();
		child2Id = child2.getId();

		em.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7948")
	public void testOneToManyInsertChildUpdateParent() {
		List list = queryForPropertyHasChanged( ListRefEdEntity.class, parent1Id, "data" );
		assertEquals( 2, list.size() );
		assertEquals( makeList( 1, 2 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasChanged( ListRefEdEntity.class, parent1Id, "reffering" );
		assertEquals( 2, list.size() );
		assertEquals( makeList( 1, 2 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasChanged( ListRefIngEntity.class, child1Id, "reference" );
		assertEquals( 1, list.size() );
		assertEquals( makeList( 2 ), extractRevisionNumbers( list ) );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7948")
	public void testOneToManyUpdateParentInsertChild() {
		List list = queryForPropertyHasChanged( ListRefEdEntity.class, parent2Id, "data" );
		assertEquals( 2, list.size() );
		assertEquals( makeList( 1, 3 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasChanged( ListRefEdEntity.class, parent2Id, "reffering" );
		assertEquals( 2, list.size() );
		assertEquals( makeList( 1, 3 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasChanged( ListRefIngEntity.class, child2Id, "reference" );
		assertEquals( 1, list.size() );
		assertEquals( makeList( 3 ), extractRevisionNumbers( list ) );
	}
}
