//$Id: MergeTest.java 11037 2007-01-09 16:04:16Z steve.ebersole@jboss.com $
package org.hibernate.test.nonflushedchanges;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.Test;

import org.hibernate.Hibernate;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.criterion.Projections;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.test.tm.SimpleJtaTransactionManagerImpl;

/**
 * @author Gavin King, Gail Badner (adapted this from "ops" tests version)
 */
public class MergeTest extends AbstractOperationTestCase {

	public MergeTest(String str) {
		super( str );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( MergeTest.class );
	}

	public void testMergeStaleVersionFails() throws Exception {

		clearCounts();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		VersionedEntity entity = new VersionedEntity( "entity", "entity" );
		s.persist( entity );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		// make the detached 'entity' reference stale...
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		VersionedEntity entity2 = ( VersionedEntity ) s.get( VersionedEntity.class, entity.getId() );
		entity2.setName( "entity-name" );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		// now try to reattch it
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		try {
			s.merge( entity );
			s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
			entity = ( VersionedEntity ) getOldToNewEntityRefMap().get( entity );
			SimpleJtaTransactionManagerImpl.getInstance().commit();
			fail( "was expecting staleness error" );
		}
		catch ( StaleObjectStateException expected ) {
			// expected outcome...
		}
		finally {
			SimpleJtaTransactionManagerImpl.getInstance().rollback();
		}
	}

	public void testMergeBidiPrimayKeyOneToOne() throws Exception {
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		Person p = new Person( "steve" );
		new PersonalDetails( "I have big feet", p );
		s.persist( p );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		clearCounts();

		p.getDetails().setSomePersonalDetail( p.getDetails().getSomePersonalDetail() + " and big hands too" );
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		p = ( Person ) s.merge( p );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		p = ( Person ) getOldToNewEntityRefMap().get( p );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertInsertCount( 0 );
		assertUpdateCount( 1 );
		assertDeleteCount( 0 );

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		s.delete( p );
		SimpleJtaTransactionManagerImpl.getInstance().commit();
	}

	public void testMergeBidiForeignKeyOneToOne() throws Exception {
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		Person p = new Person( "steve" );
		Address a = new Address( "123 Main", "Austin", "US", p );
		s.persist( a );
		s.persist( p );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		clearCounts();

		p.getAddress().setStreetAddress( "321 Main" );
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		p = ( Person ) s.merge( p );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertInsertCount( 0 );
		assertUpdateCount( 0 ); // no cascade
		assertDeleteCount( 0 );

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		s.delete( a );
		s.delete( p );
		SimpleJtaTransactionManagerImpl.getInstance().commit();
	}

	public void testNoExtraUpdatesOnMerge() throws Exception {
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		Node node = new Node( "test" );
		s.persist( node );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		clearCounts();

		// node is now detached, but we have made no changes.  so attempt to merge it
		// into this new session; this should cause no updates...
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		node = ( Node ) s.merge( node );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertUpdateCount( 0 );
		assertInsertCount( 0 );

		///////////////////////////////////////////////////////////////////////
		// as a control measure, now update the node while it is detached and
		// make sure we get an update as a result...
		node.setDescription( "new description" );
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		node = ( Node ) s.merge( node );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		SimpleJtaTransactionManagerImpl.getInstance().commit();
		assertUpdateCount( 1 );
		assertInsertCount( 0 );
		///////////////////////////////////////////////////////////////////////

		cleanup();
	}

	public void testNoExtraUpdatesOnMergeWithCollection() throws Exception {
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		Node parent = new Node( "parent" );
		Node child = new Node( "child" );
		parent.getChildren().add( child );
		child.setParent( parent );
		s.persist( parent );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		clearCounts();

		// parent is now detached, but we have made no changes.  so attempt to merge it
		// into this new session; this should cause no updates...
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		parent = ( Node ) s.merge( parent );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertUpdateCount( 0 );
		assertInsertCount( 0 );

		///////////////////////////////////////////////////////////////////////
		// as a control measure, now update the node while it is detached and
		// make sure we get an update as a result...
		( ( Node ) parent.getChildren().iterator().next() ).setDescription( "child's new description" );
		parent.addChild( new Node( "second child" ) );
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		parent = ( Node ) s.merge( parent );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		SimpleJtaTransactionManagerImpl.getInstance().commit();
		assertUpdateCount( 1 );
		assertInsertCount( 1 );
		///////////////////////////////////////////////////////////////////////

		cleanup();
	}

	public void testNoExtraUpdatesOnMergeVersioned() throws Exception {
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		VersionedEntity entity = new VersionedEntity( "entity", "entity" );
		s.persist( entity );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		clearCounts();

		// entity is now detached, but we have made no changes.  so attempt to merge it
		// into this new session; this should cause no updates...
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		VersionedEntity mergedEntity = ( VersionedEntity ) s.merge( entity );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		mergedEntity = ( VersionedEntity ) getOldToNewEntityRefMap().get( mergedEntity );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertUpdateCount( 0 );
		assertInsertCount( 0 );
		assertEquals( "unexpected version increment", entity.getVersion(), mergedEntity.getVersion() );


		///////////////////////////////////////////////////////////////////////
		// as a control measure, now update the node while it is detached and
		// make sure we get an update as a result...
		entity.setName( "new name" );
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		entity = ( VersionedEntity ) s.merge( entity );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		SimpleJtaTransactionManagerImpl.getInstance().commit();
		assertUpdateCount( 1 );
		assertInsertCount( 0 );
		///////////////////////////////////////////////////////////////////////

		cleanup();
	}

	public void testNoExtraUpdatesOnMergeVersionedWithCollection() throws Exception {
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		VersionedEntity parent = new VersionedEntity( "parent", "parent" );
		VersionedEntity child = new VersionedEntity( "child", "child" );
		parent.getChildren().add( child );
		child.setParent( parent );
		s.persist( parent );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		clearCounts();

		// parent is now detached, but we have made no changes.  so attempt to merge it
		// into this new session; this should cause no updates...
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		VersionedEntity mergedParent = ( VersionedEntity ) s.merge( parent );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		mergedParent = ( VersionedEntity ) getOldToNewEntityRefMap().get( mergedParent );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertUpdateCount( 0 );
		assertInsertCount( 0 );
		assertEquals( "unexpected parent version increment", parent.getVersion(), mergedParent.getVersion() );
		VersionedEntity mergedChild = ( VersionedEntity ) mergedParent.getChildren().iterator().next();
		assertEquals( "unexpected child version increment", child.getVersion(), mergedChild.getVersion() );

		///////////////////////////////////////////////////////////////////////
		// as a control measure, now update the node while it is detached and
		// make sure we get an update as a result...
		mergedParent.setName( "new name" );
		mergedParent.getChildren().add( new VersionedEntity( "child2", "new child" ) );
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		parent = ( VersionedEntity ) s.merge( mergedParent );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		parent = ( VersionedEntity ) getOldToNewEntityRefMap().get( parent );
		SimpleJtaTransactionManagerImpl.getInstance().commit();
		assertUpdateCount( 1 );
		assertInsertCount( 1 );
		///////////////////////////////////////////////////////////////////////

		cleanup();
	}

	public void testNoExtraUpdatesOnPersistentMergeVersionedWithCollection() throws Exception {
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		VersionedEntity parent = new VersionedEntity( "parent", "parent" );
		VersionedEntity child = new VersionedEntity( "child", "child" );
		parent.getChildren().add( child );
		child.setParent( parent );
		s.persist( parent );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		clearCounts();

		// parent is now detached, but we have made no changes. so attempt to merge it
		// into this new session; this should cause no updates...
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		// load parent so that merge will follow entityIsPersistent path
		VersionedEntity persistentParent = ( VersionedEntity ) s.get( VersionedEntity.class, parent.getId() );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		persistentParent = ( VersionedEntity ) getOldToNewEntityRefMap().get( persistentParent );
		// load children
		VersionedEntity persistentChild = ( VersionedEntity ) persistentParent.getChildren().iterator().next();
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		persistentParent = ( VersionedEntity ) getOldToNewEntityRefMap().get( persistentParent );
		VersionedEntity mergedParent = ( VersionedEntity ) s.merge( persistentParent ); // <-- This merge leads to failure
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		mergedParent = ( VersionedEntity ) getOldToNewEntityRefMap().get( mergedParent );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertUpdateCount( 0 );
		assertInsertCount( 0 );
		assertEquals( "unexpected parent version increment", parent.getVersion(), mergedParent.getVersion() );
		VersionedEntity mergedChild = ( VersionedEntity ) mergedParent.getChildren().iterator().next();
		assertEquals( "unexpected child version increment", child.getVersion(), mergedChild.getVersion() );

		///////////////////////////////////////////////////////////////////////
		// as a control measure, now update the node once it is loaded and
		// make sure we get an update as a result...
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		persistentParent = ( VersionedEntity ) s.get( VersionedEntity.class, parent.getId() );
		persistentParent.setName( "new name" );
		persistentParent.getChildren().add( new VersionedEntity( "child2", "new child" ) );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		persistentParent = ( VersionedEntity ) getOldToNewEntityRefMap().get( persistentParent );
		persistentParent = ( VersionedEntity ) s.merge( persistentParent );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		SimpleJtaTransactionManagerImpl.getInstance().commit();
		assertUpdateCount( 1 );
		assertInsertCount( 1 );
		///////////////////////////////////////////////////////////////////////

		// cleanup();
	}

	public void testPersistThenMergeInSameTxnWithVersion() throws Exception {
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		VersionedEntity entity = new VersionedEntity( "test", "test" );
		s.persist( entity );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		s.merge( new VersionedEntity( "test", "test-2" ) );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );

		try {
			// control operation...
			s.saveOrUpdate( new VersionedEntity( "test", "test-3" ) );
			fail( "saveOrUpdate() should fail here" );
		}
		catch ( NonUniqueObjectException expected ) {
			// expected behavior
		}

		SimpleJtaTransactionManagerImpl.getInstance().commit();

		cleanup();
	}

	public void testPersistThenMergeInSameTxnWithTimestamp() throws Exception {
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		TimestampedEntity entity = new TimestampedEntity( "test", "test" );
		s.persist( entity );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		s.merge( new TimestampedEntity( "test", "test-2" ) );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );

		try {
			// control operation...
			s.saveOrUpdate( new TimestampedEntity( "test", "test-3" ) );
			fail( "saveOrUpdate() should fail here" );
		}
		catch ( NonUniqueObjectException expected ) {
			// expected behavior
		}

		SimpleJtaTransactionManagerImpl.getInstance().commit();

		cleanup();
	}

	public void testMergeDeepTree() throws Exception {

		clearCounts();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		Node root = new Node( "root" );
		Node child = new Node( "child" );
		Node grandchild = new Node( "grandchild" );
		root.addChild( child );
		child.addChild( grandchild );
		s.merge( root );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		grandchild.setDescription( "the grand child" );
		Node grandchild2 = new Node( "grandchild2" );
		child.addChild( grandchild2 );

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		s.merge( root );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertInsertCount( 1 );
		assertUpdateCount( 1 );
		clearCounts();

		Node child2 = new Node( "child2" );
		Node grandchild3 = new Node( "grandchild3" );
		child2.addChild( grandchild3 );
		root.addChild( child2 );

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		s.merge( root );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		s.delete( grandchild );
		s.delete( grandchild2 );
		s.delete( grandchild3 );
		s.delete( child );
		s.delete( child2 );
		s.delete( root );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

	}

	public void testMergeDeepTreeWithGeneratedId() throws Exception {

		clearCounts();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		NumberedNode root = new NumberedNode( "root" );
		NumberedNode child = new NumberedNode( "child" );
		NumberedNode grandchild = new NumberedNode( "grandchild" );
		root.addChild( child );
		child.addChild( grandchild );
		root = ( NumberedNode ) s.merge( root );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( NumberedNode ) getOldToNewEntityRefMap().get( root );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		child = ( NumberedNode ) root.getChildren().iterator().next();
		grandchild = ( NumberedNode ) child.getChildren().iterator().next();
		grandchild.setDescription( "the grand child" );
		NumberedNode grandchild2 = new NumberedNode( "grandchild2" );
		child.addChild( grandchild2 );

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		root = ( NumberedNode ) s.merge( root );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( NumberedNode ) getOldToNewEntityRefMap().get( root );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertInsertCount( 1 );
		assertUpdateCount( 1 );
		clearCounts();

		getSessions().evict( NumberedNode.class );

		NumberedNode child2 = new NumberedNode( "child2" );
		NumberedNode grandchild3 = new NumberedNode( "grandchild3" );
		child2.addChild( grandchild3 );
		root.addChild( child2 );

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		root = ( NumberedNode ) s.merge( root );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( NumberedNode ) getOldToNewEntityRefMap().get( root );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		s.createQuery( "delete from NumberedNode where name like 'grand%'" ).executeUpdate();
		s.createQuery( "delete from NumberedNode where name like 'child%'" ).executeUpdate();
		s.createQuery( "delete from NumberedNode" ).executeUpdate();
		SimpleJtaTransactionManagerImpl.getInstance().commit();

	}

	public void testMergeTree() throws Exception {

		clearCounts();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		Node root = new Node( "root" );
		Node child = new Node( "child" );
		root.addChild( child );
		s.persist( root );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( Node ) getOldToNewEntityRefMap().get( root );
		child = ( Node ) root.getChildren().iterator().next();
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertInsertCount( 2 );
		clearCounts();

		root.setDescription( "The root node" );
		child.setDescription( "The child node" );

		Node secondChild = new Node( "second child" );

		root.addChild( secondChild );

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		s.merge( root );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertInsertCount( 1 );
		assertUpdateCount( 2 );

		cleanup();
	}

	public void testMergeTreeWithGeneratedId() throws Exception {

		clearCounts();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		NumberedNode root = new NumberedNode( "root" );
		NumberedNode child = new NumberedNode( "child" );
		root.addChild( child );
		s.persist( root );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( NumberedNode ) getOldToNewEntityRefMap().get( root );
		child = ( NumberedNode ) root.getChildren().iterator().next();
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertInsertCount( 2 );
		clearCounts();

		root.setDescription( "The root node" );
		child.setDescription( "The child node" );

		NumberedNode secondChild = new NumberedNode( "second child" );

		root.addChild( secondChild );

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		s.merge( root );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertInsertCount( 1 );
		assertUpdateCount( 2 );

		cleanup();
	}

	public void testMergeManaged() throws Exception {

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		NumberedNode root = new NumberedNode( "root" );
		s.persist( root );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( NumberedNode ) getOldToNewEntityRefMap().get( root );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		clearCounts();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		NumberedNode child = new NumberedNode( "child" );
		root = ( NumberedNode ) s.merge( root );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( NumberedNode ) getOldToNewEntityRefMap().get( root );
		root.addChild( child );
		assertSame( root, s.merge( root ) );
		Object mergedChild = root.getChildren().iterator().next();
		assertNotSame( mergedChild, child );
		assertTrue( s.contains( mergedChild ) );
		assertFalse( s.contains( child ) );
		assertEquals( root.getChildren().size(), 1 );
		assertTrue( root.getChildren().contains( mergedChild ) );
		//assertNotSame( mergedChild, s.merge(child) ); //yucky :(
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( NumberedNode ) getOldToNewEntityRefMap().get( root );
		mergedChild = root.getChildren().iterator().next();
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertInsertCount( 1 );
		assertUpdateCount( 0 );

		assertEquals( root.getChildren().size(), 1 );
		assertTrue( root.getChildren().contains( mergedChild ) );

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		assertEquals(
				s.createCriteria( NumberedNode.class )
						.setProjection( Projections.rowCount() )
						.uniqueResult(),
				new Long( 2 )
		);

		SimpleJtaTransactionManagerImpl.getInstance().commit();

		cleanup();
	}

	public void testMergeManagedUninitializedCollection() throws Exception {

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		NumberedNode root = new NumberedNode( "root" );
		root.addChild( new NumberedNode( "child" ) );
		s.persist( root );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		clearCounts();

		NumberedNode newRoot = new NumberedNode( "root" );
		newRoot.setId( root.getId() );

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		root = ( NumberedNode ) s.get( NumberedNode.class, root.getId() );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( NumberedNode ) getOldToNewEntityRefMap().get( root );
		Set managedChildren = root.getChildren();
		assertFalse( Hibernate.isInitialized( managedChildren ) );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( NumberedNode ) getOldToNewEntityRefMap().get( root );
		managedChildren = root.getChildren();
		newRoot.setChildren( managedChildren );
		assertSame( root, s.merge( newRoot ) );
		assertSame( managedChildren, root.getChildren() );
		assertFalse( Hibernate.isInitialized( managedChildren ) );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertInsertCount( 0 );
		assertUpdateCount( 0 );
		assertDeleteCount( 0 );

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		assertEquals(
				s.createCriteria( NumberedNode.class )
						.setProjection( Projections.rowCount() )
						.uniqueResult(),
				new Long( 2 )
		);
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		cleanup();
	}

	public void testMergeManagedInitializedCollection() throws Exception {

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		NumberedNode root = new NumberedNode( "root" );
		root.addChild( new NumberedNode( "child" ) );
		s.persist( root );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		clearCounts();

		NumberedNode newRoot = new NumberedNode( "root" );
		newRoot.setId( root.getId() );

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		root = ( NumberedNode ) s.get( NumberedNode.class, root.getId() );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( NumberedNode ) getOldToNewEntityRefMap().get( root );
		Set managedChildren = root.getChildren();
		Hibernate.initialize( managedChildren );
		assertTrue( Hibernate.isInitialized( managedChildren ) );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		root = ( NumberedNode ) getOldToNewEntityRefMap().get( root );
		managedChildren = root.getChildren();
		newRoot.setChildren( managedChildren );
		assertSame( root, s.merge( newRoot ) );
		assertSame( managedChildren, root.getChildren() );
		assertTrue( Hibernate.isInitialized( managedChildren ) );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertInsertCount( 0 );
		assertUpdateCount( 0 );
		assertDeleteCount( 0 );

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		assertEquals(
				s.createCriteria( NumberedNode.class )
						.setProjection( Projections.rowCount() )
						.uniqueResult(),
				new Long( 2 )
		);
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		cleanup();
	}

	public void testRecursiveMergeTransient() throws Exception {

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		Employer jboss = new Employer();
		Employee gavin = new Employee();
		jboss.setEmployees( new ArrayList() );
		jboss.getEmployees().add( gavin );
		s.merge( jboss );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		s.flush();
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		jboss = ( Employer ) s.createQuery( "from Employer e join fetch e.employees" ).uniqueResult();
		assertTrue( Hibernate.isInitialized( jboss.getEmployees() ) );
		assertEquals( 1, jboss.getEmployees().size() );
		s.clear();
		s.merge( jboss.getEmployees().iterator().next() );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		cleanup();
	}

	public void testDeleteAndMerge() throws Exception {
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		Employer jboss = new Employer();
		s.persist( jboss );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		Employer otherJboss;
		otherJboss = ( Employer ) s.get( Employer.class, jboss.getId() );
		s.delete( otherJboss );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		jboss.setVers( new Integer( 1 ) );
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		s.merge( jboss );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		cleanup();
	}

	public void testMergeManyToManyWithCollectionDeference() throws Exception {
		// setup base data...
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		Competition competition = new Competition();
		competition.getCompetitors().add( new Competitor( "Name" ) );
		competition.getCompetitors().add( new Competitor() );
		competition.getCompetitors().add( new Competitor() );
		s.persist( competition );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		// the competition graph is now detached:
		//   1) create a new List reference to represent the competitors
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		List newComp = new ArrayList();
		Competitor originalCompetitor = ( Competitor ) competition.getCompetitors().get( 0 );
		originalCompetitor.setName( "Name2" );
		newComp.add( originalCompetitor );
		newComp.add( new Competitor() );
		//   2) set that new List reference unto the Competition reference
		competition.setCompetitors( newComp );
		//   3) attempt the merge
		Competition competition2 = ( Competition ) s.merge( competition );
		s = applyNonFlushedChangesToNewSessionCloseOldSession( s );
		Competition competition2copy = ( Competition ) getOldToNewEntityRefMap().get( competition2 );

		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertFalse( competition == competition2 );
		assertFalse( competition2 == competition2copy );
		assertFalse( competition.getCompetitors() == competition2.getCompetitors() );
		assertEquals( 2, competition2.getCompetitors().size() );
		assertEquals( 2, competition2copy.getCompetitors().size() );

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		competition = ( Competition ) s.get( Competition.class, competition.getId() );
		assertEquals( 2, competition.getCompetitors().size() );
		s.delete( competition );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		cleanup();
	}

	private void cleanup() throws Exception {
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		s.createQuery( "delete from NumberedNode where parent is not null" ).executeUpdate();
		s.createQuery( "delete from NumberedNode" ).executeUpdate();

		s.createQuery( "delete from Node where parent is not null" ).executeUpdate();
		s.createQuery( "delete from Node" ).executeUpdate();

		s.createQuery( "delete from VersionedEntity where parent is not null" ).executeUpdate();
		s.createQuery( "delete from VersionedEntity" ).executeUpdate();
		s.createQuery( "delete from TimestampedEntity" ).executeUpdate();

		s.createQuery( "delete from Competitor" ).executeUpdate();
		s.createQuery( "delete from Competition" ).executeUpdate();

		Iterator itr = s.createQuery( "from Employer" ).list().iterator();
		while ( itr.hasNext() ) {
			final Employer employer = ( Employer ) itr.next();
			s.delete( employer );
		}

		SimpleJtaTransactionManagerImpl.getInstance().commit();
	}
}

