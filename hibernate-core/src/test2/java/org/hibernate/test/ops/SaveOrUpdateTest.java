/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.ops;

import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.criterion.Projections;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Gavin King
 */
public class SaveOrUpdateTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected void addSettings(Map settings) {
		settings.put( AvailableSettings.GENERATE_STATISTICS, "true" );
		settings.put( AvailableSettings.STATEMENT_BATCH_SIZE, "0" );
	}

	@Override
	public String[] getMappings() {
		return new String[] {"ops/Node.hbm.xml"};
	}

	@Override
	protected String getCacheConcurrencyStrategy() {
		return "nonstrict-read-write";
	}

	@Test
	public void testSaveOrUpdateDeepTree() {
		clearCounts();

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Node root = new Node( "root" );
		Node child = new Node( "child" );
		Node grandchild = new Node( "grandchild" );
		root.addChild( child );
		child.addChild( grandchild );
		s.saveOrUpdate( root );
		tx.commit();
		s.close();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		grandchild.setDescription( "the grand child" );
		Node grandchild2 = new Node( "grandchild2" );
		child.addChild( grandchild2 );

		s = openSession();
		tx = s.beginTransaction();
		s.saveOrUpdate( root );
		tx.commit();
		s.close();

		assertInsertCount( 1 );
		assertUpdateCount( 1 );
		clearCounts();

		Node child2 = new Node( "child2" );
		Node grandchild3 = new Node( "grandchild3" );
		child2.addChild( grandchild3 );
		root.addChild( child2 );

		s = openSession();
		tx = s.beginTransaction();
		s.saveOrUpdate( root );
		tx.commit();
		s.close();

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		tx = s.beginTransaction();
		s.delete( grandchild );
		s.delete( grandchild2 );
		s.delete( grandchild3 );
		s.delete( child );
		s.delete( child2 );
		s.delete( root );
		tx.commit();
		s.close();
	}

	@Test
	public void testSaveOrUpdateDeepTreeWithGeneratedId() {
		boolean instrumented = PersistentAttributeInterceptable.class.isAssignableFrom( NumberedNode.class );
		clearCounts();

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		NumberedNode root = new NumberedNode( "root" );
		NumberedNode child = new NumberedNode( "child" );
		NumberedNode grandchild = new NumberedNode( "grandchild" );
		root.addChild( child );
		child.addChild( grandchild );
		s.saveOrUpdate( root );
		tx.commit();
		s.close();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		child = ( NumberedNode ) root.getChildren().iterator().next();
		grandchild = ( NumberedNode ) child.getChildren().iterator().next();
		grandchild.setDescription( "the grand child" );
		NumberedNode grandchild2 = new NumberedNode( "grandchild2" );
		child.addChild( grandchild2 );

		s = openSession();
		tx = s.beginTransaction();
		s.saveOrUpdate( root );
		tx.commit();
		s.close();

		assertInsertCount( 1 );
		assertUpdateCount( instrumented ? 1 : 3 );
		clearCounts();

		NumberedNode child2 = new NumberedNode( "child2" );
		NumberedNode grandchild3 = new NumberedNode( "grandchild3" );
		child2.addChild( grandchild3 );
		root.addChild( child2 );

		s = openSession();
		tx = s.beginTransaction();
		s.saveOrUpdate( root );
		tx.commit();
		s.close();

		assertInsertCount( 2 );
		assertUpdateCount( instrumented ? 0 : 4 );
		clearCounts();

		s = openSession();
		tx = s.beginTransaction();
		s.createQuery( "delete from NumberedNode where name like 'grand%'" ).executeUpdate();
		s.createQuery( "delete from NumberedNode where name like 'child%'" ).executeUpdate();
		s.createQuery( "delete from NumberedNode" ).executeUpdate();
		tx.commit();
		s.close();
	}

	@Test
	public void testSaveOrUpdateTree() {
		clearCounts();

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Node root = new Node( "root" );
		Node child = new Node( "child" );
		root.addChild( child );
		s.saveOrUpdate( root );
		tx.commit();
		s.close();

		assertInsertCount( 2 );
		clearCounts();

		root.setDescription( "The root node" );
		child.setDescription( "The child node" );

		Node secondChild = new Node( "second child" );

		root.addChild( secondChild );

		s = openSession();
		tx = s.beginTransaction();
		s.saveOrUpdate( root );
		tx.commit();
		s.close();

		assertInsertCount( 1 );
		assertUpdateCount( 2 );

		s = openSession();
		tx = s.beginTransaction();
		s.createQuery( "delete from Node where parent is not null" ).executeUpdate();
		s.createQuery( "delete from Node" ).executeUpdate();
		tx.commit();
		s.close();
	}

	@Test
	public void testSaveOrUpdateTreeWithGeneratedId() {
		clearCounts();

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		NumberedNode root = new NumberedNode( "root" );
		NumberedNode child = new NumberedNode( "child" );
		root.addChild( child );
		s.saveOrUpdate( root );
		tx.commit();
		s.close();

		assertInsertCount( 2 );
		clearCounts();

		root.setDescription( "The root node" );
		child.setDescription( "The child node" );

		NumberedNode secondChild = new NumberedNode( "second child" );

		root.addChild( secondChild );

		s = openSession();
		tx = s.beginTransaction();
		s.saveOrUpdate( root );
		tx.commit();
		s.close();

		assertInsertCount( 1 );
		assertUpdateCount( 2 );

		s = openSession();
		tx = s.beginTransaction();
		s.createQuery( "delete from NumberedNode where parent is not null" ).executeUpdate();
		s.createQuery( "delete from NumberedNode" ).executeUpdate();
		tx.commit();
		s.close();
	}

	@Test
	public void testSaveOrUpdateManaged() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		NumberedNode root = new NumberedNode( "root" );
		s.saveOrUpdate( root );
		tx.commit();

		tx = s.beginTransaction();
		NumberedNode child = new NumberedNode( "child" );
		root.addChild( child );
		s.saveOrUpdate( root );
		assertFalse( s.contains( child ) );
		s.flush();
		assertTrue( s.contains( child ) );
		tx.commit();

		assertTrue( root.getChildren().contains( child ) );
		assertEquals( root.getChildren().size(), 1 );

		tx = s.beginTransaction();
		assertEquals(
				Long.valueOf( 2 ),
				s.createCriteria( NumberedNode.class )
						.setProjection( Projections.rowCount() )
						.uniqueResult()
		);
		s.delete( root );
		s.delete( child );
		tx.commit();
		s.close();
	}


	@Test
	public void testSaveOrUpdateGot() {
		clearCounts();

		boolean instrumented = PersistentAttributeInterceptable.class.isAssignableFrom( NumberedNode.class );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		NumberedNode root = new NumberedNode( "root" );
		s.saveOrUpdate( root );
		tx.commit();
		s.close();

		assertInsertCount( 1 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		tx = s.beginTransaction();
		s.saveOrUpdate( root );
		tx.commit();
		s.close();

		assertInsertCount( 0 );
		assertUpdateCount( instrumented ? 0 : 1 );

		s = openSession();
		tx = s.beginTransaction();
		root = ( NumberedNode ) s.get( NumberedNode.class, new Long( root.getId() ) );
		Hibernate.initialize( root.getChildren() );
		tx.commit();
		s.close();

		clearCounts();

		s = openSession();
		tx = s.beginTransaction();
		NumberedNode child = new NumberedNode( "child" );
		root.addChild( child );
		s.saveOrUpdate( root );
		assertTrue( s.contains( child ) );
		tx.commit();

		assertInsertCount( 1 );
		assertUpdateCount( instrumented ? 0 : 1 );

		tx = s.beginTransaction();
		assertEquals(
				s.createCriteria( NumberedNode.class )
						.setProjection( Projections.rowCount() )
						.uniqueResult(),
		        new Long( 2 )
		);
		s.delete( root );
		s.delete( child );
		tx.commit();
		s.close();
	}

	@Test
	public void testSaveOrUpdateGotWithMutableProp() {
		clearCounts();

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Node root = new Node( "root" );
		s.saveOrUpdate( root );
		tx.commit();
		s.close();

		assertInsertCount( 1 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		tx = s.beginTransaction();
		s.saveOrUpdate( root );
		tx.commit();
		s.close();

		assertInsertCount( 0 );
		assertUpdateCount( 0 );

		s = openSession();
		tx = s.beginTransaction();
		root = ( Node ) s.get( Node.class, "root" );
		Hibernate.initialize( root.getChildren() );
		tx.commit();
		s.close();

		clearCounts();

		s = openSession();
		tx = s.beginTransaction();
		Node child = new Node( "child" );
		root.addChild( child );
		s.saveOrUpdate( root );
		assertTrue( s.contains( child ) );
		tx.commit();

		assertInsertCount( 1 );
		assertUpdateCount( 1 ); //note: will fail here if no second-level cache

		tx = s.beginTransaction();
		assertEquals(
				s.createCriteria( Node.class )
						.setProjection( Projections.rowCount() )
						.uniqueResult(),
		        new Long( 2 )
		);
		s.delete( root );
		s.delete( child );
		tx.commit();
		s.close();
	}

	@Test
	public void testEvictThenSaveOrUpdate() {
		Session s = openSession();
		s.getTransaction().begin();
		Node parent = new Node( "1:parent" );
		Node child = new Node( "2:child" );
		Node grandchild = new Node( "3:grandchild" );
		parent.addChild( child );
		child.addChild( grandchild );
		s.saveOrUpdate( parent );
		s.getTransaction().commit();
		s.close();

		Session s1 = openSession();
		s1.getTransaction().begin();
		child = ( Node ) s1.load( Node.class, "2:child" );
		assertTrue( s1.contains( child ) );
		assertFalse( Hibernate.isInitialized( child ) );
		assertTrue( s1.contains( child.getParent() ) );
		assertTrue( Hibernate.isInitialized( child ) );
		assertFalse( Hibernate.isInitialized( child.getChildren() ) );
		assertFalse( Hibernate.isInitialized( child.getParent() ) );
		assertTrue( s1.contains( child ) );
		s1.evict( child );
		assertFalse( s1.contains( child ) );
		assertTrue( s1.contains( child.getParent() ) );

		Session s2 = openSession();
		try {
			s2.getTransaction().begin();
			s2.saveOrUpdate( child );
			fail();
		}
		catch ( HibernateException ex ) {
			// expected because parent is connected to s1
		}
		finally {
			s2.getTransaction().rollback();
		}
		s2.close();

		s1.evict( child.getParent() );
		assertFalse( s1.contains( child.getParent() ) );

		s2 = openSession();
		s2.getTransaction().begin();
		s2.saveOrUpdate( child );
		assertTrue( s2.contains( child ) );
		assertFalse( s1.contains( child ) );
		assertTrue( s2.contains( child.getParent() ) );
		assertFalse( s1.contains( child.getParent() ) );
		assertFalse( Hibernate.isInitialized( child.getChildren() ) );
		assertFalse( Hibernate.isInitialized( child.getParent() ) );
		assertEquals( 1, child.getChildren().size() );
		assertEquals( "1:parent", child.getParent().getName() );
		assertTrue( Hibernate.isInitialized( child.getChildren() ) );
		assertFalse( Hibernate.isInitialized( child.getParent() ) );
		assertNull( child.getParent().getDescription() );
		assertTrue( Hibernate.isInitialized( child.getParent() ) );

		s1.getTransaction().commit();
		s2.getTransaction().commit();
		s1.close();
		s2.close();

		s = openSession();
		s.beginTransaction();
		s.delete( s.get( Node.class, "3:grandchild" ) );
		s.delete( s.get( Node.class, "2:child" ) );
		s.delete( s.get( Node.class, "1:parent" ) );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testSavePersistentEntityWithUpdate() {
		clearCounts();

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		NumberedNode root = new NumberedNode( "root" );
		root.setName( "a name" );
		s.saveOrUpdate( root );
		tx.commit();
		s.close();

		assertInsertCount( 1 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		tx = s.beginTransaction();
		root = ( NumberedNode ) s.get( NumberedNode.class, root.getId() );
		assertEquals( "a name", root.getName() );
		root.setName( "a new name" );
		s.save( root );
		tx.commit();
		s.close();

		assertInsertCount( 0 );
		assertUpdateCount( 1 );
		clearCounts();

		s = openSession();
		tx = s.beginTransaction();
		root = ( NumberedNode ) s.get( NumberedNode.class, root.getId() );
		assertEquals( "a new name", root.getName() );
		s.delete( root );
		tx.commit();
		s.close();
	}

	private void clearCounts() {
		sessionFactory().getStatistics().clear();
	}

	private void assertInsertCount(int count) {
		int inserts = ( int ) sessionFactory().getStatistics().getEntityInsertCount();
		assertEquals( count, inserts );
	}

	private void assertUpdateCount(int count) {
		int updates = ( int ) sessionFactory().getStatistics().getEntityUpdateCount();
		assertEquals( count, updates );
	}
}

