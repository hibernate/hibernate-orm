package org.hibernate.test.collection.set;

import java.util.HashSet;

import junit.framework.Test;

import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.collection.PersistentSet;
import org.hibernate.criterion.Restrictions;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.stat.CollectionStatistics;

/**
 * todo: describe PersistentSetTest
 *
 * @author Steve Ebersole
 */
public class PersistentSetTest extends FunctionalTestCase {
	public PersistentSetTest(String name) {
		super( name );
	}

	public String[] getMappings() {
		return new String[] { "collection/set/Mappings.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( PersistentSetTest.class );
	}

	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}

	public void testWriteMethodDirtying() {
		Parent parent = new Parent( "p1" );
		Child child = new Child( "c1" );
		parent.getChildren().add( child );
		child.setParent( parent );
		Child otherChild = new Child( "c2" );

		Session session = openSession();
		session.beginTransaction();
		session.save( parent );
		session.flush();
		// at this point, the set on parent has now been replaced with a PersistentSet...
		PersistentSet children = ( PersistentSet ) parent.getChildren();

		assertFalse( children.add( child ) );
		assertFalse( children.isDirty() );

		assertFalse( children.remove( otherChild ) );
		assertFalse( children.isDirty() );

		HashSet otherSet = new HashSet();
		otherSet.add( child );
		assertFalse( children.addAll( otherSet ) );
		assertFalse( children.isDirty() );

		assertFalse( children.retainAll( otherSet ) );
		assertFalse( children.isDirty() );

		otherSet = new HashSet();
		otherSet.add( otherChild );
		assertFalse( children.removeAll( otherSet ) );
		assertFalse( children.isDirty() );

		assertTrue( children.retainAll( otherSet ));
		assertTrue( children.isDirty() );
		assertTrue( children.isEmpty() );

		children.clear();
		session.delete( child );
		assertTrue( children.isDirty() );

		session.flush();

		children.clear();
		assertFalse( children.isDirty() );

		session.delete( parent );
		session.getTransaction().commit();
		session.close();
	}

	public void testCollectionMerging() {
		Session session = openSession();
		session.beginTransaction();
		Parent parent = new Parent( "p1" );
		Child child = new Child( "c1" );
		parent.getChildren().add( child );
		child.setParent( parent );
		session.save( parent );
		session.getTransaction().commit();
		session.close();

		CollectionStatistics stats =  sfi().getStatistics().getCollectionStatistics( Parent.class.getName() + ".children" );
		long recreateCount = stats.getRecreateCount();
		long updateCount = stats.getUpdateCount();

		session = openSession();
		session.beginTransaction();
		parent = ( Parent ) session.merge( parent );
		session.getTransaction().commit();
		session.close();

		assertEquals( 1, parent.getChildren().size() );
		assertEquals( recreateCount, stats.getRecreateCount() );
		assertEquals( updateCount, stats.getUpdateCount() );

		session = openSession();
		session.beginTransaction();
		parent = ( Parent ) session.get( Parent.class, "p1" );
		assertEquals( 1, parent.getChildren().size() );
		session.delete( parent );
		session.getTransaction().commit();
		session.close();
	}

	public void testCollectiondirtyChecking() {
		Session session = openSession();
		session.beginTransaction();
		Parent parent = new Parent( "p1" );
		Child child = new Child( "c1" );
		parent.getChildren().add( child );
		child.setParent( parent );
		session.save( parent );
		session.getTransaction().commit();
		session.close();

		CollectionStatistics stats =  sfi().getStatistics().getCollectionStatistics( Parent.class.getName() + ".children" );
		long recreateCount = stats.getRecreateCount();
		long updateCount = stats.getUpdateCount();

		session = openSession();
		session.beginTransaction();
		parent = ( Parent ) session.get( Parent.class, "p1" );
		assertEquals( 1, parent.getChildren().size() );
		session.getTransaction().commit();
		session.close();

		assertEquals( 1, parent.getChildren().size() );
		assertEquals( recreateCount, stats.getRecreateCount() );
		assertEquals( updateCount, stats.getUpdateCount() );

		session = openSession();
		session.beginTransaction();
		assertEquals( 1, parent.getChildren().size() );
		session.delete( parent );
		session.getTransaction().commit();
		session.close();
	}

	public void testCompositeElementWriteMethodDirtying() {
		Container container = new Container( "p1" );
		Container.Content c1 = new Container.Content( "c1" );
		container.getContents().add( c1 );
		Container.Content c2 = new Container.Content( "c2" );

		Session session = openSession();
		session.beginTransaction();
		session.save( container );
		session.flush();
		// at this point, the set on container has now been replaced with a PersistentSet...
		PersistentSet children = ( PersistentSet ) container.getContents();

		assertFalse( children.add( c1 ) );
		assertFalse( children.isDirty() );

		assertFalse( children.remove( c2 ) );
		assertFalse( children.isDirty() );

		HashSet otherSet = new HashSet();
		otherSet.add( c1 );
		assertFalse( children.addAll( otherSet ) );
		assertFalse( children.isDirty() );

		assertFalse( children.retainAll( otherSet ) );
		assertFalse( children.isDirty() );

		otherSet = new HashSet();
		otherSet.add( c2 );
		assertFalse( children.removeAll( otherSet ) );
		assertFalse( children.isDirty() );

		assertTrue( children.retainAll( otherSet ));
		assertTrue( children.isDirty() );
		assertTrue( children.isEmpty() );

		children.clear();
		assertTrue( children.isDirty() );

		session.flush();

		children.clear();
		assertFalse( children.isDirty() );

		session.delete( container );
		session.getTransaction().commit();
		session.close();
	}

	public void testCompositeElementMergingFailureExpected() {
		// HHH-2485
		Session session = openSession();
		session.beginTransaction();
		Container container = new Container( "p1" );
		Container.Content c1 = new Container.Content( "c1" );
		container.getContents().add( c1 );
		session.save( container );
		session.getTransaction().commit();
		session.close();

		CollectionStatistics stats =  sfi().getStatistics().getCollectionStatistics( Container.class.getName() + ".contents" );
		long recreateCount = stats.getRecreateCount();
		long updateCount = stats.getUpdateCount();

		container.setName( "another name" );

		session = openSession();
		session.beginTransaction();
		container = ( Container ) session.merge( container );
		session.getTransaction().commit();
		session.close();

		assertEquals( 1, container.getContents().size() );
		assertEquals( recreateCount, stats.getRecreateCount() );
		assertEquals( updateCount, stats.getUpdateCount() );

		session = openSession();
		session.beginTransaction();
		container = ( Container ) session.get( Container.class, container.getId() );
		assertEquals( 1, container.getContents().size() );
		session.delete( container );
		session.getTransaction().commit();
		session.close();
	}

	public void testCompositeElementCollectionDirtyCheckingFailureExpected() {
		// HHH-2485
		Session session = openSession();
		session.beginTransaction();
		Container container = new Container( "p1" );
		Container.Content c1 = new Container.Content( "c1" );
		container.getContents().add( c1 );
		session.save( container );
		session.getTransaction().commit();
		session.close();

		CollectionStatistics stats =  sfi().getStatistics().getCollectionStatistics( Container.class.getName() + ".contents" );
		long recreateCount = stats.getRecreateCount();
		long updateCount = stats.getUpdateCount();

		session = openSession();
		session.beginTransaction();
		container = ( Container ) session.get( Container.class, container.getId() );
		assertEquals( 1, container.getContents().size() );
		session.getTransaction().commit();
		session.close();

		assertEquals( 1, container.getContents().size() );
		assertEquals( recreateCount, stats.getRecreateCount() );
		assertEquals( updateCount, stats.getUpdateCount() );

		session = openSession();
		session.beginTransaction();
		container = ( Container ) session.get( Container.class, container.getId() );
		assertEquals( 1, container.getContents().size() );
		session.delete( container );
		session.getTransaction().commit();
		session.close();
	}

	public void testLoadChildCheckParentContainsChildCache() {
		Parent parent = new Parent( "p1" );
		Child child = new Child( "c1" );
		child.setDescription( "desc1" );
		parent.getChildren().add( child );
		child.setParent( parent );
		Child otherChild = new Child( "c2" );
		otherChild.setDescription( "desc2" );
		parent.getChildren().add( otherChild );
		otherChild.setParent( parent );

		Session session = openSession();
		session.beginTransaction();
		session.save( parent );
		session.getTransaction().commit();

		session = openSession();
		session.beginTransaction();
		parent = ( Parent ) session.get( Parent.class, parent.getName() );
		assertTrue( parent.getChildren().contains( child ) );
		assertTrue( parent.getChildren().contains( otherChild ) );
		session.getTransaction().commit();

		session = openSession();
		session.beginTransaction();

		child = ( Child ) session.get( Child.class, child.getName() );
		assertTrue( child.getParent().getChildren().contains( child ) );
		session.clear();

		child = ( Child ) session.createCriteria( Child.class, child.getName() )
				.setCacheable( true )
				.add( Restrictions.idEq( "c1" ) )
				.uniqueResult();
		assertTrue( child.getParent().getChildren().contains( child ) );
		assertTrue( child.getParent().getChildren().contains( otherChild ) );
		session.clear();

		child = ( Child ) session.createCriteria( Child.class, child.getName() )
				.setCacheable( true )
				.add( Restrictions.idEq( "c1" ) )
				.uniqueResult();
		assertTrue( child.getParent().getChildren().contains( child ) );
		assertTrue( child.getParent().getChildren().contains( otherChild ) );
		session.clear();

		child = ( Child ) session.createQuery( "from Child where name = 'c1'" )
				.setCacheable( true )
				.uniqueResult();
		assertTrue( child.getParent().getChildren().contains( child ) );

		child = ( Child ) session.createQuery( "from Child where name = 'c1'" )
				.setCacheable( true )
				.uniqueResult();
		assertTrue( child.getParent().getChildren().contains( child ) );

		session.delete( child.getParent() );
		session.getTransaction().commit();
		session.close();
	}

	public void testLoadChildCheckParentContainsChildNoCache() {
		Parent parent = new Parent( "p1" );
		Child child = new Child( "c1" );
		parent.getChildren().add( child );
		child.setParent( parent );
		Child otherChild = new Child( "c2" );
		parent.getChildren().add( otherChild );
		otherChild.setParent( parent );

		Session session = openSession();
		session.beginTransaction();
		session.save( parent );
		session.getTransaction().commit();

		session = openSession();
		session.beginTransaction();
		session.setCacheMode( CacheMode.IGNORE );
		parent = ( Parent ) session.get( Parent.class, parent.getName() );
		assertTrue( parent.getChildren().contains( child ) );
		assertTrue( parent.getChildren().contains( otherChild ) );
		session.getTransaction().commit();

		session = openSession();
		session.beginTransaction();
		session.setCacheMode( CacheMode.IGNORE );

		child = ( Child ) session.get( Child.class, child.getName() );
		assertTrue( child.getParent().getChildren().contains( child ) );
		session.clear();

		child = ( Child ) session.createCriteria( Child.class, child.getName() )
				.add( Restrictions.idEq( "c1" ) )
				.uniqueResult();
		assertTrue( child.getParent().getChildren().contains( child ) );
		assertTrue( child.getParent().getChildren().contains( otherChild ) );
		session.clear();

		child = ( Child ) session.createQuery( "from Child where name = 'c1'" ).uniqueResult();
		assertTrue( child.getParent().getChildren().contains( child ) );

		session.delete( child.getParent() );
		session.getTransaction().commit();
		session.close();
	}
}
