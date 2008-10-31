//$Id$
package org.hibernate.ejb.test.ops;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.ejb.test.EJB3TestCase;

/**
 * @author Gavin King
 */
public class MergeTest extends EJB3TestCase {

	public MergeTest(String str) {
		super( str );
	}

	public void testMergeTree() {

		clearCounts();

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Node root = new Node( "root" );
		Node child = new Node( "child" );
		root.addChild( child );
		s.persist( root );
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
		s.merge( root );
		tx.commit();
		s.close();

		assertInsertCount( 1 );
		assertUpdateCount( 2 );

	}

	public void testMergeTreeWithGeneratedId() {

		clearCounts();

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		NumberedNode root = new NumberedNode( "root" );
		NumberedNode child = new NumberedNode( "child" );
		root.addChild( child );
		s.persist( root );
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
		s.merge( root );
		tx.commit();
		s.close();

		assertInsertCount( 1 );
		assertUpdateCount( 2 );

	}

	private void clearCounts() {
		getSessions().getStatistics().clear();
	}

	private void assertInsertCount(int count) {
		int inserts = (int) getSessions().getStatistics().getEntityInsertCount();
		assertEquals( count, inserts );
	}

	private void assertUpdateCount(int count) {
		int updates = (int) getSessions().getStatistics().getEntityUpdateCount();
		assertEquals( count, updates );
	}

	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
		cfg.setProperty( Environment.STATEMENT_BATCH_SIZE, "0" );
	}

	protected String[] getMappings() {
		return new String[]{"ops/Node.hbm.xml"};
	}

	public static Test suite() {
		return new TestSuite( MergeTest.class );
	}

}

