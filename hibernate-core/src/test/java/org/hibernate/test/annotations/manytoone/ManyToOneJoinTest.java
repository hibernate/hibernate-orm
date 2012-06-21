/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.manytoone;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Emmanuel Bernard
 */
public class ManyToOneJoinTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testManyToOneJoinTable() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		ForestType forest = new ForestType();
		forest.setName( "Original forest" );
		s.persist( forest );
		TreeType tree = new TreeType();
		tree.setForestType( forest );
		tree.setAlternativeForestType( forest );
		tree.setName( "just a tree");
		s.persist( tree );
		s.flush();
		s.clear();
		tree = (TreeType) s.get(TreeType.class, tree.getId() );
		assertNotNull( tree.getForestType() );
		assertNotNull( tree.getAlternativeForestType() );
		s.clear();
		forest = (ForestType) s.get( ForestType.class, forest.getId() );
		assertEquals( 1, forest.getTrees().size() );
		assertEquals( tree.getId(), forest.getTrees().iterator().next().getId() );
		tx.rollback();
		s.close();
	}

	@Test
	public void testOneToOneJoinTable() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		ForestType forest = new ForestType();
		forest.setName( "Original forest" );
		s.persist( forest );
		BiggestForest forestRepr = new BiggestForest();
		forestRepr.setType( forest );
		forest.setBiggestRepresentative( forestRepr );
		s.persist( forestRepr );
		s.flush();
		s.clear();
		forest = (ForestType) s.get( ForestType.class, forest.getId() );
		assertNotNull( forest.getBiggestRepresentative() );
		assertEquals( forest, forest.getBiggestRepresentative().getType() );
		tx.rollback();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				BiggestForest.class,
				ForestType.class,
				TreeType.class
		};
	}
}
