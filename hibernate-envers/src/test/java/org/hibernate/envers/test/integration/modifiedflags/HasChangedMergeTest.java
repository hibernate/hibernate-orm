package org.hibernate.envers.test.integration.modifiedflags;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;

import org.junit.Test;

import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.onetomany.ListRefEdEntity;
import org.hibernate.envers.test.entities.onetomany.ListRefIngEntity;
import org.hibernate.testing.TestForIssue;

import static org.hibernate.envers.test.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.envers.test.tools.TestTools.makeList;
import static org.junit.Assert.assertEquals;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class HasChangedMergeTest extends AbstractModifiedFlagsEntityTest {
	private Integer parentId = null;
	private Integer childId = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { ListRefEdEntity.class, ListRefIngEntity.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1 - data preparation
		em.getTransaction().begin();
		ListRefEdEntity parent = new ListRefEdEntity( 1, "initial data" );
		parent.setReffering( new ArrayList<ListRefIngEntity>() ); // Empty collection is not the same as null reference.
		em.persist( parent );
		em.getTransaction().commit();

		// Revision 2 - inserting new child entity and updating parent
		em.getTransaction().begin();
		parent = em.find( ListRefEdEntity.class, parent.getId() );
		ListRefIngEntity child = new ListRefIngEntity( 1, "initial data", parent );
		em.persist( child );
		parent.setData( "updated data" );
		parent = em.merge( parent );
		em.getTransaction().commit();

		parentId = parent.getId();
		childId = child.getId();

		em.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7948")
	public void testOneToManyInsertChildUpdateParent() {
		List list = queryForPropertyHasChanged( ListRefEdEntity.class, parentId, "data" );
		assertEquals( 2, list.size() );
		assertEquals( makeList( 1, 2 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasChanged( ListRefEdEntity.class, parentId, "reffering" );
		assertEquals( 2, list.size() );
		assertEquals( makeList( 1, 2 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasChanged( ListRefIngEntity.class, childId, "reference" );
		assertEquals( 1, list.size() );
		assertEquals( makeList( 2 ), extractRevisionNumbers( list ) );
	}
}
