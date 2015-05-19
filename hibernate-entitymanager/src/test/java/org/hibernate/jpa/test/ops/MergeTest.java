/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.ops;

import java.util.Map;
import javax.persistence.EntityManager;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.cfg.Environment;
import org.hibernate.jpa.internal.EntityManagerFactoryImpl;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

/**
 * @author Gavin King
 * @author Hardy Ferentschik
 */
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
		int updates = ( int ) entityManagerFactory().getSessionFactory()
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

