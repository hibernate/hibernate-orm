/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.test.integration.components.collections;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.components.Component1;
import org.hibernate.envers.test.entities.components.ComponentSetTestEntity;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Felix Feisst
 */
public class CollectionOfComponents extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;
	private Integer id2;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {ComponentSetTestEntity.class, Component1.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		ComponentSetTestEntity cte1 = new ComponentSetTestEntity();

		ComponentSetTestEntity cte2 = new ComponentSetTestEntity();
		cte2.getComps().add( new Component1( "string1", null ) );

		em.persist( cte2 );
		em.persist( cte1 );

		em.getTransaction().commit();

		// Revision 2
		em = getEntityManager();
		em.getTransaction().begin();

		cte1 = em.find( ComponentSetTestEntity.class, cte1.getId() );

		cte1.getComps().add( new Component1( "a", "b" ) );

		em.getTransaction().commit();

		id1 = cte1.getId();
		id2 = cte2.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assertEquals( Arrays.asList( 1, 2 ), getAuditReader().getRevisions( ComponentSetTestEntity.class, id1 ) );
	}

	@Test
	public void testHistoryOfId1() {
		assertEquals( 0, getAuditReader().find( ComponentSetTestEntity.class, id1, 1 ).getComps().size() );

		Set<Component1> comps1 = getAuditReader().find( ComponentSetTestEntity.class, id1, 2 ).getComps();
		assertEquals( 1, comps1.size() );
		assertTrue( comps1.contains( new Component1( "a", "b" ) ) );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8968")
	public void testCollectionOfEmbeddableWithNullValue() {
		final Component1 componentV1 = new Component1( "string1", null );
		final ComponentSetTestEntity entityV1 = getAuditReader().find( ComponentSetTestEntity.class, id2, 1 );
		assertEquals( "Expected a component", Collections.singleton( componentV1 ), entityV1.getComps() );
	}
}