/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.jpa.test.graphs.named.multiple;

import javax.persistence.AttributeNode;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import java.util.List;

import org.hibernate.testing.FailureExpectedWithNewMetamodel;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class NamedEntityGraphsTest  extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Person.class, Employee.class };
	}

	@Test
	@FailureExpectedWithNewMetamodel( jiraKey = "HHH-8933" )
	public void testIt() {
		EntityGraph graph = getOrCreateEntityManager().getEntityGraph( "abc" );
		assertNotNull( graph );
		graph = getOrCreateEntityManager().getEntityGraph( "xyz" );
		assertNotNull( graph );
	}

	@Test
	@FailureExpectedWithNewMetamodel( jiraKey = "HHH-8933" )
	public void testAttributeNodesAreAvailable() {
		EntityManager em = getOrCreateEntityManager();
			EntityGraph graph = em.getEntityGraph( "name_salary_graph" );
			assertNotNull( graph );

			List<AttributeNode<?>> list =  graph.getAttributeNodes();
			assertNotNull( list );
			assertTrue("expected list.size() is two but actual list size is " + list.size(), 2 == list.size() );

			AttributeNode attributeNode1 = list.get(0);
			AttributeNode attributeNode2 = list.get(1);
			assertNotNull( attributeNode1 );
			assertNotNull( attributeNode2 );

			assertTrue( "node1 attribute name is expected to be either 'name' or 'salary' but actually is "+attributeNode1.getAttributeName(),
					"name".equals(attributeNode1.getAttributeName()) || "salary".equals(attributeNode1.getAttributeName()));

			assertTrue( "node2 attribute name is expected to be either 'name' or 'salary' but actually is "+attributeNode2.getAttributeName(),
					"name".equals(attributeNode2.getAttributeName()) || "salary".equals(attributeNode2.getAttributeName()));
	}

}
