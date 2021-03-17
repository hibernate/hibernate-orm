/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.ops;

import javax.persistence.PersistenceException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;
import org.hibernate.dialect.AbstractHANADialect;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.SkipForDialect;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Gavin King
 */
@RequiresDialectFeature(DialectChecks.SupportsNoColumnInsert.class)
public class MergeTest extends AbstractOperationTestCase {
	@Test
	public void testMergeStaleVersionFails() throws Exception {
		Session s = openSession();
        s.beginTransaction();
		VersionedEntity entity = new VersionedEntity( "entity", "entity" );
		s.persist( entity );
		s.getTransaction().commit();
		s.close();

		// make the detached 'entity' reference stale...
		s = openSession();
        s.beginTransaction();
		VersionedEntity entity2 = ( VersionedEntity ) s.get( VersionedEntity.class, entity.getId() );
		entity2.setName( "entity-name" );
		s.getTransaction().commit();
		s.close();

		// now try to reattch it
		s = openSession();
		s.beginTransaction();
		try {
			s.merge( entity );
			s.getTransaction().commit();
			fail( "was expecting staleness error" );
		}
		catch (PersistenceException e){
			// expected
			assertTyping( StaleObjectStateException.class, e.getCause());
		}
		finally {
			s.getTransaction().rollback();
			s.close();
		}
	}

	@Test
	public void testMergeBidiPrimayKeyOneToOne() throws Exception {
		rebuildSessionFactory();
		Session s = openSession();
        s.beginTransaction();
		Person p = new Person( "steve" );
		new PersonalDetails( "I have big feet", p );
		s.persist( p );
		s.getTransaction().commit();
		s.close();

		clearCounts();

		p.getDetails().setSomePersonalDetail( p.getDetails().getSomePersonalDetail() + " and big hands too" );
		s = openSession();
        s.beginTransaction();
		p = ( Person ) s.merge( p );
		s.getTransaction().commit();
		s.close();

		assertInsertCount( 0 );
		assertUpdateCount( 1 );
		assertDeleteCount( 0 );

		s = openSession();
        s.beginTransaction();
		s.delete( p );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testMergeBidiForeignKeyOneToOne() throws Exception {
		Session s = openSession();
        s.beginTransaction();
		Person p = new Person( "steve" );
		Address a = new Address( "123 Main", "Austin", "US", p );
		s.persist( a );
		s.persist( p );
		s.getTransaction().commit();
		s.close();

		clearCounts();

		p.getAddress().setStreetAddress( "321 Main" );
		s = openSession();
        s.beginTransaction();
		p = ( Person ) s.merge( p );
		s.getTransaction().commit();
		s.close();

		assertInsertCount( 0 );
		assertUpdateCount( 0 ); // no cascade
		assertDeleteCount( 0 );

		s = openSession();
        s.beginTransaction();
		s.delete( a );
		s.delete( p );
		s.getTransaction().commit();
		s.close();
	}

	@Test
    @SuppressWarnings( {"UnusedAssignment"})
	public void testNoExtraUpdatesOnMerge() throws Exception {
		Session s = openSession();
        s.beginTransaction();
		Node node = new Node( "test" );
		s.persist( node );
		s.getTransaction().commit();
		s.close();

		clearCounts();

		// node is now detached, but we have made no changes.  so attempt to merge it
		// into this new session; this should cause no updates...
		s = openSession();
		s.beginTransaction();
		node = ( Node ) s.merge( node );
		s.getTransaction().commit();
		s.close();

		assertUpdateCount( 0 );
		assertInsertCount( 0 );

		///////////////////////////////////////////////////////////////////////
		// as a control measure, now update the node while it is detached and
		// make sure we get an update as a result...
		node.setDescription( "new description" );
		s = openSession();
		s.beginTransaction();
		node = ( Node ) s.merge( node );
		s.getTransaction().commit();
		s.close();
		assertUpdateCount( 1 );
		assertInsertCount( 0 );
		///////////////////////////////////////////////////////////////////////

		cleanup();
    }

	@Test
	@SuppressWarnings( {"unchecked", "UnusedAssignment"})
	public void testNoExtraUpdatesOnMergeWithCollection() throws Exception {
		Session s = openSession();
        s.beginTransaction();
		Node parent = new Node( "parent" );
		Node child = new Node( "child" );
		parent.getChildren().add( child );
		child.setParent( parent );
		s.persist( parent );
		s.getTransaction().commit();
		s.close();

		clearCounts();

		// parent is now detached, but we have made no changes.  so attempt to merge it
		// into this new session; this should cause no updates...
		s = openSession();
		s.beginTransaction();
		parent = ( Node ) s.merge( parent );
		s.getTransaction().commit();
		s.close();

		assertUpdateCount( 0 );
		assertInsertCount( 0 );

		///////////////////////////////////////////////////////////////////////
		// as a control measure, now update the node while it is detached and
		// make sure we get an update as a result...
		( ( Node ) parent.getChildren().iterator().next() ).setDescription( "child's new description" );
		parent.addChild( new Node( "second child" ) );
		s = openSession();
		s.beginTransaction();
		parent = ( Node ) s.merge( parent );
		s.getTransaction().commit();
		s.close();
		assertUpdateCount( 1 );
		assertInsertCount( 1 );
		///////////////////////////////////////////////////////////////////////

		cleanup();
	}

	@Test
    @SuppressWarnings( {"UnusedAssignment"})
	public void testNoExtraUpdatesOnMergeVersioned() throws Exception {
		Session s = openSession();
        s.beginTransaction();
		VersionedEntity entity = new VersionedEntity( "entity", "entity" );
		s.persist( entity );
		s.getTransaction().commit();
		s.close();

		clearCounts();

		// entity is now detached, but we have made no changes.  so attempt to merge it
		// into this new session; this should cause no updates...
		s = openSession();
		s.beginTransaction();
		VersionedEntity mergedEntity = ( VersionedEntity ) s.merge( entity );
		s.getTransaction().commit();
		s.close();

		assertUpdateCount( 0 );
		assertInsertCount( 0 );
        assertEquals( "unexpected version increment", entity.getVersion(), mergedEntity.getVersion() );


		///////////////////////////////////////////////////////////////////////
		// as a control measure, now update the node while it is detached and
		// make sure we get an update as a result...
		entity.setName( "new name" );
		s = openSession();
		s.beginTransaction();
		entity = ( VersionedEntity ) s.merge( entity );
		s.getTransaction().commit();
		s.close();
		assertUpdateCount( 1 );
		assertInsertCount( 0 );
		///////////////////////////////////////////////////////////////////////

		cleanup();
    }

	@Test
	@SuppressWarnings( {"unchecked", "UnusedAssignment"})
	public void testNoExtraUpdatesOnMergeVersionedWithCollection() throws Exception {
		Session s = openSession();
        s.beginTransaction();
		VersionedEntity parent = new VersionedEntity( "parent", "parent" );
		VersionedEntity child = new VersionedEntity( "child", "child" );
		parent.getChildren().add( child );
		child.setParent( parent );
		s.persist( parent );
		s.getTransaction().commit();
		s.close();

		clearCounts();

		// parent is now detached, but we have made no changes.  so attempt to merge it
		// into this new session; this should cause no updates...
		s = openSession();
		s.beginTransaction();
		VersionedEntity mergedParent = ( VersionedEntity ) s.merge( parent );
		s.getTransaction().commit();
		s.close();

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
		s = openSession();
		s.beginTransaction();
		parent = ( VersionedEntity ) s.merge( mergedParent );
		s.getTransaction().commit();
		s.close();
		assertUpdateCount( 1 );
		assertInsertCount( 1 );
		///////////////////////////////////////////////////////////////////////

		cleanup();
    }

	@Test
	@SuppressWarnings( {"unchecked", "UnusedAssignment", "UnusedDeclaration"})
	public void testNoExtraUpdatesOnPersistentMergeVersionedWithCollection() throws Exception {
		Session s = openSession();
		s.beginTransaction();
		VersionedEntity parent = new VersionedEntity( "parent", "parent" );
		VersionedEntity child = new VersionedEntity( "child", "child" );
		parent.getChildren().add( child );
		child.setParent( parent );
		s.persist( parent );
		s.getTransaction().commit();
		s.close();

		clearCounts();

		// parent is now detached, but we have made no changes. so attempt to merge it
		// into this new session; this should cause no updates...
		s = openSession();
		s.beginTransaction();
		// load parent so that merge will follow entityIsPersistent path
		VersionedEntity persistentParent = ( VersionedEntity ) s.get( VersionedEntity.class, parent.getId() );
		// load children
		VersionedEntity persistentChild = ( VersionedEntity ) persistentParent.getChildren().iterator().next();
		VersionedEntity mergedParent = ( VersionedEntity ) s.merge( persistentParent ); // <-- This merge leads to failure
		s.getTransaction().commit();
		s.close();

		assertUpdateCount( 0 );
		assertInsertCount( 0 );
		assertEquals( "unexpected parent version increment", parent.getVersion(), mergedParent.getVersion() );
		VersionedEntity mergedChild = ( VersionedEntity ) mergedParent.getChildren().iterator().next();
		assertEquals( "unexpected child version increment", child.getVersion(), mergedChild.getVersion() );

		///////////////////////////////////////////////////////////////////////
		// as a control measure, now update the node once it is loaded and
		// make sure we get an update as a result...
		s = openSession();
		s.beginTransaction();
		persistentParent = ( VersionedEntity ) s.get( VersionedEntity.class, parent.getId() );
		persistentParent.setName( "new name" );
		persistentParent.getChildren().add( new VersionedEntity( "child2", "new child" ) );
		persistentParent = ( VersionedEntity ) s.merge( persistentParent );
		s.getTransaction().commit();
		s.close();
		assertUpdateCount( 1 );
		assertInsertCount( 1 );
		///////////////////////////////////////////////////////////////////////

		// cleanup();
	}

	@Test
	public void testPersistThenMergeInSameTxnWithVersion() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		VersionedEntity entity = new VersionedEntity( "test", "test" );
		s.persist( entity );
		s.merge( new VersionedEntity( "test", "test-2" ) );

		try {
			// control operation...
			s.saveOrUpdate( new VersionedEntity( "test", "test-3" ) );
			fail( "saveOrUpdate() should fail here" );
		}
		catch( NonUniqueObjectException expected ) {
			// expected behavior
		}

		tx.commit();
		s.close();

		cleanup();
	}

	@Test
	public void testPersistThenMergeInSameTxnWithTimestamp() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		TimestampedEntity entity = new TimestampedEntity( "test", "test" );
		s.persist( entity );
		s.merge( new TimestampedEntity( "test", "test-2" ) );

		try {
			// control operation...
			s.saveOrUpdate( new TimestampedEntity( "test", "test-3" ) );
			fail( "saveOrUpdate() should fail here" );
		}
		catch( NonUniqueObjectException expected ) {
			// expected behavior
		}

		tx.commit();
		s.close();

		cleanup();
	}

	@Test
	public void testMergeDeepTree() {
		clearCounts();

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Node root = new Node("root");
		Node child = new Node("child");
		Node grandchild = new Node("grandchild");
		root.addChild(child);
		child.addChild(grandchild);
		s.merge(root);
		tx.commit();
		s.close();

		assertInsertCount(3);
		assertUpdateCount(0);
		clearCounts();

		grandchild.setDescription("the grand child");
		Node grandchild2 = new Node("grandchild2");
		child.addChild( grandchild2 );

		s = openSession();
		tx = s.beginTransaction();
		s.merge(root);
		tx.commit();
		s.close();

		assertInsertCount(1);
		assertUpdateCount(1);
		clearCounts();

		Node child2 = new Node("child2");
		Node grandchild3 = new Node("grandchild3");
		child2.addChild( grandchild3 );
		root.addChild(child2);

		s = openSession();
		tx = s.beginTransaction();
		s.merge(root);
		tx.commit();
		s.close();

		assertInsertCount(2);
		assertUpdateCount(0);
		clearCounts();

		s = openSession();
		tx = s.beginTransaction();
		s.delete(grandchild);
		s.delete(grandchild2);
		s.delete(grandchild3);
		s.delete(child);
		s.delete(child2);
		s.delete(root);
		tx.commit();
		s.close();

	}

	@SuppressWarnings( {"UnusedAssignment"})
	@Test
	public void testMergeDeepTreeWithGeneratedId() {
		clearCounts();

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		NumberedNode root = new NumberedNode("root");
		NumberedNode child = new NumberedNode("child");
		NumberedNode grandchild = new NumberedNode("grandchild");
		root.addChild(child);
		child.addChild(grandchild);
		root = (NumberedNode) s.merge(root);
		tx.commit();
		s.close();

		assertInsertCount(3);
		assertUpdateCount(0);
		clearCounts();

		child = (NumberedNode) root.getChildren().iterator().next();
		grandchild = (NumberedNode) child.getChildren().iterator().next();
		grandchild.setDescription( "the grand child" );
		NumberedNode grandchild2 = new NumberedNode("grandchild2");
		child.addChild( grandchild2 );

		s = openSession();
		tx = s.beginTransaction();
		root = (NumberedNode) s.merge(root);
		tx.commit();
		s.close();

		assertInsertCount( 1 );
		assertUpdateCount(1);
		clearCounts();

		sessionFactory().getCache().evictEntityRegion( NumberedNode.class );

		NumberedNode child2 = new NumberedNode("child2");
		NumberedNode grandchild3 = new NumberedNode("grandchild3");
		child2.addChild( grandchild3 );
		root.addChild( child2 );

		s = openSession();
		tx = s.beginTransaction();
		root = (NumberedNode) s.merge( root );
		tx.commit();
		s.close();

		assertInsertCount(2);
		assertUpdateCount(0);
		clearCounts();

		s = openSession();
		tx = s.beginTransaction();
		s.createQuery("delete from NumberedNode where name like 'grand%'").executeUpdate();
		s.createQuery("delete from NumberedNode where name like 'child%'").executeUpdate();
		s.createQuery("delete from NumberedNode").executeUpdate();
		tx.commit();
		s.close();
	}

	@Test
	public void testMergeTree() {
		clearCounts();

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Node root = new Node("root");
		Node child = new Node("child");
		root.addChild(child);
		s.persist(root);
		tx.commit();
		s.close();

		assertInsertCount(2);
		clearCounts();

		root.setDescription("The root node");
		child.setDescription("The child node");

		Node secondChild = new Node("second child");

		root.addChild(secondChild);

		s = openSession();
		tx = s.beginTransaction();
		s.merge(root);
		tx.commit();
		s.close();

		assertInsertCount(1);
		assertUpdateCount(2);

		cleanup();
	}

	@Test
	public void testMergeTreeWithGeneratedId() {
		clearCounts();

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		NumberedNode root = new NumberedNode("root");
		NumberedNode child = new NumberedNode("child");
		root.addChild(child);
		s.persist(root);
		tx.commit();
		s.close();

		assertInsertCount(2);
		clearCounts();

		root.setDescription("The root node");
		child.setDescription("The child node");

		NumberedNode secondChild = new NumberedNode("second child");

		root.addChild(secondChild);

		s = openSession();
		tx = s.beginTransaction();
		s.merge(root);
		tx.commit();
		s.close();

		assertInsertCount(1);
		assertUpdateCount(2);

		cleanup();
	}

	@Test
	public void testMergeManaged() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		NumberedNode root = new NumberedNode("root");
		s.persist(root);
		tx.commit();

		clearCounts();

		tx = s.beginTransaction();
		NumberedNode child = new NumberedNode("child");
		root.addChild( child );
		assertSame( root, s.merge( root ) );
		Object mergedChild = root.getChildren().iterator().next();
		assertNotSame( mergedChild, child );
		assertTrue( s.contains( mergedChild ) );
		assertFalse( s.contains(child) );
		assertEquals( root.getChildren().size(), 1 );
		assertTrue( root.getChildren().contains(mergedChild) );
		//assertNotSame( mergedChild, s.merge(child) ); //yucky :(
		tx.commit();

		assertInsertCount( 1 );
		assertUpdateCount( 0 );

		assertEquals( root.getChildren().size(), 1 );
		assertTrue( root.getChildren().contains(mergedChild) );

		s.beginTransaction();
		assertEquals(
				Long.valueOf( 2 ),
				s.createCriteria( NumberedNode.class )
						.setProjection( Projections.rowCount() )
						.uniqueResult()
		);
		s.getTransaction().commit();
		s.close();

		cleanup();
	}

	@Test
	public void testMergeManagedUninitializedCollection() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		NumberedNode root = new NumberedNode( "root" );
		root.addChild( new NumberedNode( "child" ) );
		s.persist( root );
		tx.commit();
		s.close();

		clearCounts();

		NumberedNode newRoot = new NumberedNode( "root" );
		newRoot.setId( root.getId() );

		s = openSession();
		tx = s.beginTransaction();
		root = ( NumberedNode ) s.get( NumberedNode.class, root.getId() );
		Set managedChildren = root.getChildren();
		assertFalse( Hibernate.isInitialized( managedChildren ) );
		newRoot.setChildren( managedChildren );
		assertSame( root, s.merge( newRoot ) );
		assertSame( managedChildren, root.getChildren() );
		assertFalse( Hibernate.isInitialized( managedChildren ) );
		tx.commit();

		assertInsertCount( 0 );
		assertUpdateCount( 0 );
		assertDeleteCount( 0 );

		tx = s.beginTransaction();
		assertEquals(
				Long.valueOf( 2 ),
				s.createCriteria( NumberedNode.class )
						.setProjection( Projections.rowCount() )
						.uniqueResult()
		);
		tx.commit();

		s.close();

		cleanup();
	}

	@Test
	public void testMergeManagedInitializedCollection() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		NumberedNode root = new NumberedNode( "root" );
		root.addChild( new NumberedNode( "child" ) );
		s.persist(root);
		tx.commit();
		s.close();

		clearCounts();

		NumberedNode newRoot = new NumberedNode( "root" );
		newRoot.setId( root.getId() );

		s = openSession();
		tx = s.beginTransaction();
		root = ( NumberedNode ) s.get( NumberedNode.class, root.getId() );
		Set managedChildren = root.getChildren();
		Hibernate.initialize( managedChildren );
		assertTrue( Hibernate.isInitialized( managedChildren ) );
		newRoot.setChildren( managedChildren );
		assertSame( root, s.merge( newRoot ) );
		assertSame( managedChildren, root.getChildren() );
		assertTrue( Hibernate.isInitialized( managedChildren ) );
		tx.commit();

		assertInsertCount(0);
		assertUpdateCount(0);
		assertDeleteCount(0);

		tx = s.beginTransaction();
		assertEquals(
				Long.valueOf( 2 ),
				s.createCriteria(NumberedNode.class)
						.setProjection( Projections.rowCount() )
						.uniqueResult()
		);
		tx.commit();

		s.close();

		cleanup();
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	@SkipForDialect(value = AbstractHANADialect.class, comment = " HANA doesn't support tables consisting of only a single auto-generated column")
	public void testRecursiveMergeTransient() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Employer jboss = new Employer();
		Employee gavin = new Employee();
		jboss.setEmployees( new ArrayList() );
		jboss.getEmployees().add( gavin );
		s.merge( jboss );
		s.flush();
		jboss = (Employer) s.createQuery("from Employer e join fetch e.employees").uniqueResult();
		assertTrue( Hibernate.isInitialized( jboss.getEmployees() ) );
		assertEquals( 1, jboss.getEmployees().size() );
		s.clear();
		s.merge( jboss.getEmployees().iterator().next() );
		tx.commit();
		s.close();

		cleanup();
	}

	@Test
	public void testDeleteAndMerge() throws Exception {
		Session s = openSession();
		s.getTransaction().begin();
		Employer jboss = new Employer();
		s.persist( jboss );
		s.getTransaction().commit();
		s.clear();

		s.getTransaction().begin();
		Employer otherJboss;
		otherJboss = (Employer) s.get( Employer.class, jboss.getId() );
		s.delete( otherJboss );
		s.getTransaction().commit();
		s.clear();
		jboss.setVers( Integer.valueOf( 1 ) );
		s.getTransaction().begin();
		s.merge( jboss );
		s.getTransaction().commit();
		s.close();

		cleanup();
	}

	@SuppressWarnings( {"unchecked"})
	@Test
	@SkipForDialect(value = AbstractHANADialect.class, comment = " HANA doesn't support tables consisting of only a single auto-generated column")
	public void testMergeManyToManyWithCollectionDeference() throws Exception {
		// setup base data...
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Competition competition = new Competition();
		competition.getCompetitors().add( new Competitor( "Name" ) );
		competition.getCompetitors().add( new Competitor() );
		competition.getCompetitors().add( new Competitor() );
		s.persist( competition );
		tx.commit();
		s.close();

		// the competition graph is now detached:
		//   1) create a new List reference to represent the competitors
		s = openSession();
		tx = s.beginTransaction();
		List newComp = new ArrayList();
		Competitor originalCompetitor = ( Competitor ) competition.getCompetitors().get( 0 );
		originalCompetitor.setName( "Name2" );
		newComp.add( originalCompetitor );
		newComp.add( new Competitor() );
		//   2) set that new List reference unto the Competition reference
		competition.setCompetitors( newComp );
		//   3) attempt the merge
		Competition competition2 = ( Competition ) s.merge( competition );
		tx.commit();
		s.close();

		assertFalse( competition == competition2 );
		assertFalse( competition.getCompetitors() == competition2.getCompetitors() );
		assertEquals( 2, competition2.getCompetitors().size() );

		s = openSession();
		tx = s.beginTransaction();
		competition = ( Competition ) s.get( Competition.class, competition.getId() );
		assertEquals( 2, competition.getCompetitors().size() );
		s.delete( competition );
		tx.commit();
		s.close();

		cleanup();
	}

	@SuppressWarnings( {"unchecked"})
	private void cleanup() {
		Session s = openSession();
		s.beginTransaction();
		s.createQuery( "delete from NumberedNode where parent is not null" ).executeUpdate();
		s.createQuery( "delete from NumberedNode" ).executeUpdate();

		s.createQuery( "delete from Node where parent is not null" ).executeUpdate();
		s.createQuery( "delete from Node" ).executeUpdate();

		s.createQuery( "delete from VersionedEntity where parent is not null" ).executeUpdate();
		s.createQuery( "delete from VersionedEntity" ).executeUpdate();
		s.createQuery( "delete from TimestampedEntity" ).executeUpdate();

		s.createQuery( "delete from Competitor" ).executeUpdate();
		s.createQuery( "delete from Competition" ).executeUpdate();

		for ( Employer employer : (List<Employer>) s.createQuery( "from Employer" ).list() ) {
			s.delete( employer );
		}

		s.getTransaction().commit();
		s.close();
	}
}

