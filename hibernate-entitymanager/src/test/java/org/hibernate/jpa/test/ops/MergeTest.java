/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.test.ops;

import java.util.Map;
import javax.persistence.EntityManager;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.cfg.Environment;
import org.hibernate.jpa.internal.EntityManagerFactoryImpl;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.FailureExpectedWithNewUnifiedXsd;

/**
 * @author Gavin King
 * @author Hardy Ferentschik
 */
@FailureExpectedWithNewUnifiedXsd
public class MergeTest extends BaseEntityManagerFunctionalTestCase {
	@Test
	public void testMergeTree() {
		clearCounts();

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Node root = new Node( "root" );
		Node child = new Node( "child" );
		root.addChild( child );
		em.persist( root );
		em.getTransaction().commit();
		em.close();

		assertInsertCount( 2 );
		clearCounts();

		root.setDescription( "The root node" );
		child.setDescription( "The child node" );

		Node secondChild = new Node( "second child" );

		root.addChild( secondChild );

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.merge( root );
		em.getTransaction().commit();
		em.close();

		assertInsertCount( 1 );
		assertUpdateCount( 2 );
	}

	public void testMergeTreeWithGeneratedId() {
		clearCounts();

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		NumberedNode root = new NumberedNode( "root" );
		NumberedNode child = new NumberedNode( "child" );
		root.addChild( child );
		em.persist( root );
		em.getTransaction().commit();
		em.close();

		assertInsertCount( 2 );
		clearCounts();

		root.setDescription( "The root node" );
		child.setDescription( "The child node" );

		NumberedNode secondChild = new NumberedNode( "second child" );

		root.addChild( secondChild );

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.merge( root );
		em.getTransaction().commit();
		em.close();

		assertInsertCount( 1 );
		assertUpdateCount( 2 );
	}

	private void clearCounts() {
		( ( EntityManagerFactoryImpl ) entityManagerFactory() ).getSessionFactory().getStatistics().clear();
	}

	private void assertInsertCount(int count) {
		int inserts = ( int ) ( ( EntityManagerFactoryImpl ) entityManagerFactory() ).getSessionFactory()
				.getStatistics()
				.getEntityInsertCount();
		Assert.assertEquals( count, inserts );
	}

	private void assertUpdateCount(int count) {
		int updates = ( int ) ( (EntityManagerFactoryImpl) entityManagerFactory() ).getSessionFactory()
				.getStatistics()
				.getEntityUpdateCount();
		Assert.assertEquals( count, updates );
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	protected void addConfigOptions(Map options) {
		options.put( Environment.GENERATE_STATISTICS, "true" );
		options.put( Environment.STATEMENT_BATCH_SIZE, "0" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[0];
	}

	@Override
	protected String[] getMappings() {
		return new String[] { "org/hibernate/jpa/test/ops/Node.hbm.xml" };
	}
}

