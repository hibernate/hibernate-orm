package org.hibernate.envers.test.integration.modifiedflags;

import javax.persistence.EntityManager;
import java.util.List;

import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.collection.EmbeddableListEntity1;
import org.hibernate.envers.test.entities.components.Component3;
import org.hibernate.envers.test.entities.components.Component4;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.hibernate.envers.test.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.envers.test.tools.TestTools.makeList;
import static org.junit.Assert.assertEquals;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-6613")
public class HasChangedComponentCollection extends AbstractModifiedFlagsEntityTest {
	private Integer ele1_id = null;

	private final Component4 c4_1 = new Component4( "c41", "c41_value", "c41_description" );
	private final Component4 c4_2 = new Component4( "c42", "c42_value2", "c42_description" );
	private final Component3 c3_1 = new Component3( "c31", c4_1, c4_2 );
	private final Component3 c3_2 = new Component3( "c32", c4_1, c4_2 );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {EmbeddableListEntity1.class, Component3.class, Component4.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1 (ele1: initially 1 element in both collections)
		em.getTransaction().begin();
		EmbeddableListEntity1 ele1 = new EmbeddableListEntity1();
		ele1.setOtherData( "data" );
		ele1.getComponentList().add( c3_1 );
		em.persist( ele1 );
		em.getTransaction().commit();

		// Revision (still 1) (ele1: removing non-existing element)
		em.getTransaction().begin();
		ele1 = em.find( EmbeddableListEntity1.class, ele1.getId() );
		ele1.getComponentList().remove( c3_2 );
		em.getTransaction().commit();

		// Revision 2 (ele1: updating singular property and removing non-existing element)
		em.getTransaction().begin();
		ele1 = em.find( EmbeddableListEntity1.class, ele1.getId() );
		ele1.setOtherData( "modified" );
		ele1.getComponentList().remove( c3_2 );
		ele1 = em.merge( ele1 );
		em.getTransaction().commit();

		// Revision 3 (ele1: adding one element)
		em.getTransaction().begin();
		ele1 = em.find( EmbeddableListEntity1.class, ele1.getId() );
		ele1.getComponentList().add( c3_2 );
		em.getTransaction().commit();

		// Revision 4 (ele1: adding one existing element)
		em.getTransaction().begin();
		ele1 = em.find( EmbeddableListEntity1.class, ele1.getId() );
		ele1.getComponentList().add( c3_1 );
		em.getTransaction().commit();

		// Revision 5 (ele1: removing one existing element)
		em.getTransaction().begin();
		ele1 = em.find( EmbeddableListEntity1.class, ele1.getId() );
		ele1.getComponentList().remove( c3_2 );
		em.getTransaction().commit();

		// Revision 6 (ele1: changing singular property only)
		em.getTransaction().begin();
		ele1 = em.find( EmbeddableListEntity1.class, ele1.getId() );
		ele1.setOtherData( "another modification" );
		ele1 = em.merge( ele1 );
		em.getTransaction().commit();

		ele1_id = ele1.getId();

		em.close();
	}

	@Test
	public void testHasChangedEle() {
		List list = queryForPropertyHasChanged( EmbeddableListEntity1.class, ele1_id, "componentList" );
		assertEquals( 4, list.size() );
		assertEquals( makeList( 1, 3, 4, 5 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasChanged( EmbeddableListEntity1.class, ele1_id, "otherData" );
		assertEquals( 3, list.size() );
		assertEquals( makeList( 1, 2, 6 ), extractRevisionNumbers( list ) );
	}
}