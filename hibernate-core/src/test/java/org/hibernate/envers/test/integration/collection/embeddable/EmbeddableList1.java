/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.collection.embeddable;

import java.util.Arrays;
import java.util.Collections;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.collection.EmbeddableListEntity1;
import org.hibernate.envers.test.entities.components.Component3;
import org.hibernate.envers.test.entities.components.Component4;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Kristoffer Lundberg (kristoffer at cambio dot se)
 */
@TestForIssue(jiraKey = "HHH-6613")
public class EmbeddableList1 extends BaseEnversJPAFunctionalTestCase {
	private Integer ele1_id = null;

	private final Component4 c4_1 = new Component4( "c41", "c41_value", "c41_description" );
	private final Component4 c4_2 = new Component4( "c42", "c42_value2", "c42_description" );
	private final Component3 c3_1 = new Component3( "c31", c4_1, c4_2 );
	private final Component3 c3_2 = new Component3( "c32", c4_1, c4_2 );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {EmbeddableListEntity1.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		EmbeddableListEntity1 ele1 = new EmbeddableListEntity1();

		// Revision 1 (ele1: initially 1 element in both collections)
		em.getTransaction().begin();
		ele1.getComponentList().add( c3_1 );
		em.persist( ele1 );
		em.getTransaction().commit();

		// Revision (still 1) (ele1: removing non-existing element)
		em.getTransaction().begin();
		ele1 = em.find( EmbeddableListEntity1.class, ele1.getId() );
		ele1.getComponentList().remove( c3_2 );
		em.getTransaction().commit();

		// Revision 2 (ele1: adding one element)
		em.getTransaction().begin();
		ele1 = em.find( EmbeddableListEntity1.class, ele1.getId() );
		ele1.getComponentList().add( c3_2 );
		em.getTransaction().commit();

		// Revision 3 (ele1: adding one existing element)
		em.getTransaction().begin();
		ele1 = em.find( EmbeddableListEntity1.class, ele1.getId() );
		ele1.getComponentList().add( c3_1 );
		em.getTransaction().commit();

		// Revision 4 (ele1: removing one existing element)
		em.getTransaction().begin();
		ele1 = em.find( EmbeddableListEntity1.class, ele1.getId() );
		ele1.getComponentList().remove( c3_2 );
		em.getTransaction().commit();

		ele1_id = ele1.getId();

		em.close();
	}

	@Test
	public void testRevisionsCounts() {
		assertEquals(
				Arrays.asList( 1, 2, 3, 4 ), getAuditReader().getRevisions(
				EmbeddableListEntity1.class,
				ele1_id
		)
		);
	}

	@Test
	public void testHistoryOfEle1() {
		EmbeddableListEntity1 rev1 = getAuditReader().find( EmbeddableListEntity1.class, ele1_id, 1 );
		EmbeddableListEntity1 rev2 = getAuditReader().find( EmbeddableListEntity1.class, ele1_id, 2 );
		EmbeddableListEntity1 rev3 = getAuditReader().find( EmbeddableListEntity1.class, ele1_id, 3 );
		EmbeddableListEntity1 rev4 = getAuditReader().find( EmbeddableListEntity1.class, ele1_id, 4 );

		assertEquals( Collections.singletonList( c3_1 ), rev1.getComponentList() );
		assertEquals( Arrays.asList( c3_1, c3_2 ), rev2.getComponentList() );
		assertEquals( Arrays.asList( c3_1, c3_2, c3_1 ), rev3.getComponentList() );
		assertEquals( Arrays.asList( c3_1, c3_1 ), rev4.getComponentList() );
	}
}
