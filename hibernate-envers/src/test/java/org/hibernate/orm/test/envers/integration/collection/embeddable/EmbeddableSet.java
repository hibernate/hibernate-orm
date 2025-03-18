/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.collection.embeddable;

import java.util.Arrays;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.collection.EmbeddableSetEntity;
import org.hibernate.orm.test.envers.entities.components.Component3;
import org.hibernate.orm.test.envers.entities.components.Component4;
import org.hibernate.orm.test.envers.tools.TestTools;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Kristoffer Lundberg (kristoffer at cambio dot se)
 */
@JiraKey(value = "HHH-6613")
public class EmbeddableSet extends BaseEnversJPAFunctionalTestCase {
	private Integer ese1_id = null;

	private final Component4 c4_1 = new Component4( "c41", "c41_value", "c41_description" );
	private final Component4 c4_2 = new Component4( "c42", "c42_value2", "c42_description" );
	private final Component3 c3_1 = new Component3( "c31", c4_1, c4_2 );
	private final Component3 c3_2 = new Component3( "c32", c4_1, c4_2 );
	private final Component3 c3_3 = new Component3( "c33", c4_1, c4_2 );
	private final Component3 c3_4 = new Component3( "c34", c4_1, c4_2 );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {EmbeddableSetEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		EmbeddableSetEntity ese1 = new EmbeddableSetEntity();

		// Revision 1 (ese1: initially two elements)
		em.getTransaction().begin();
		ese1.getComponentSet().add( c3_1 );
		ese1.getComponentSet().add( c3_3 );
		em.persist( ese1 );
		em.getTransaction().commit();

		// Revision (still 1) (ese1: removing non-existing element)
		em.getTransaction().begin();
		ese1 = em.find( EmbeddableSetEntity.class, ese1.getId() );
		ese1.getComponentSet().remove( c3_2 );
		em.getTransaction().commit();

		// Revision 2 (ese1: adding one element)
		em.getTransaction().begin();
		ese1 = em.find( EmbeddableSetEntity.class, ese1.getId() );
		ese1.getComponentSet().add( c3_2 );
		em.getTransaction().commit();

		// Revision (still 2) (ese1: adding one existing element)
		em.getTransaction().begin();
		ese1 = em.find( EmbeddableSetEntity.class, ese1.getId() );
		ese1.getComponentSet().add( c3_1 );
		em.getTransaction().commit();

		// Revision 3 (ese1: removing one existing element)
		em.getTransaction().begin();
		ese1 = em.find( EmbeddableSetEntity.class, ese1.getId() );
		ese1.getComponentSet().remove( c3_2 );
		em.getTransaction().commit();

		// Revision 4 (ese1: adding two elements)
		em.getTransaction().begin();
		ese1 = em.find( EmbeddableSetEntity.class, ese1.getId() );
		ese1.getComponentSet().add( c3_2 );
		ese1.getComponentSet().add( c3_4 );
		em.getTransaction().commit();

		// Revision 5 (ese1: removing two elements)
		em.getTransaction().begin();
		ese1 = em.find( EmbeddableSetEntity.class, ese1.getId() );
		ese1.getComponentSet().remove( c3_2 );
		ese1.getComponentSet().remove( c3_4 );
		em.getTransaction().commit();

		// Revision 6 (ese1: removing and adding two elements)
		em.getTransaction().begin();
		ese1 = em.find( EmbeddableSetEntity.class, ese1.getId() );
		ese1.getComponentSet().remove( c3_1 );
		ese1.getComponentSet().remove( c3_3 );
		ese1.getComponentSet().add( c3_2 );
		ese1.getComponentSet().add( c3_4 );
		em.getTransaction().commit();

		// Revision 7 (ese1: adding one element)
		em.getTransaction().begin();
		ese1 = em.find( EmbeddableSetEntity.class, ese1.getId() );
		ese1.getComponentSet().add( c3_1 );
		em.getTransaction().commit();

		ese1_id = ese1.getId();

		em.close();
	}

	@Test
	public void testRevisionsCounts() {
		assertEquals(
				Arrays.asList( 1, 2, 3, 4, 5, 6, 7 ), getAuditReader().getRevisions(
				EmbeddableSetEntity.class,
				ese1_id
		)
		);
	}

	@Test
	public void testHistoryOfEse1() {
		EmbeddableSetEntity rev1 = getAuditReader().find( EmbeddableSetEntity.class, ese1_id, 1 );
		EmbeddableSetEntity rev2 = getAuditReader().find( EmbeddableSetEntity.class, ese1_id, 2 );
		EmbeddableSetEntity rev3 = getAuditReader().find( EmbeddableSetEntity.class, ese1_id, 3 );
		EmbeddableSetEntity rev4 = getAuditReader().find( EmbeddableSetEntity.class, ese1_id, 4 );
		EmbeddableSetEntity rev5 = getAuditReader().find( EmbeddableSetEntity.class, ese1_id, 5 );
		EmbeddableSetEntity rev6 = getAuditReader().find( EmbeddableSetEntity.class, ese1_id, 6 );
		EmbeddableSetEntity rev7 = getAuditReader().find( EmbeddableSetEntity.class, ese1_id, 7 );

		assertEquals( TestTools.makeSet( c3_1, c3_3 ), rev1.getComponentSet() );
		assertEquals( TestTools.makeSet( c3_1, c3_2, c3_3 ), rev2.getComponentSet() );
		assertEquals( TestTools.makeSet( c3_1, c3_3 ), rev3.getComponentSet() );
		assertEquals( TestTools.makeSet( c3_1, c3_2, c3_3, c3_4 ), rev4.getComponentSet() );
		assertEquals( TestTools.makeSet( c3_1, c3_3 ), rev5.getComponentSet() );
		assertEquals( TestTools.makeSet( c3_2, c3_4 ), rev6.getComponentSet() );
		assertEquals( TestTools.makeSet( c3_2, c3_4, c3_1 ), rev7.getComponentSet() );
	}
}
