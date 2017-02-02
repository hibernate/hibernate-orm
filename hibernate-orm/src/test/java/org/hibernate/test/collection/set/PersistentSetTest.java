/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.set;

import java.util.HashSet;
import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.collection.internal.PersistentSet;
import org.hibernate.criterion.Restrictions;
import org.hibernate.stat.CollectionStatistics;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class PersistentSetTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "collection/set/Mappings.hbm.xml" };
	}

	@Override
	protected void addSettings(Map settings) {
		super.addSettings( settings );
		settings.put( AvailableSettings.GENERATE_STATISTICS, "true" );
		settings.put( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true" );
		settings.put( AvailableSettings.USE_QUERY_CACHE, "true" );
	}

	@Test
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

	@Test
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

		CollectionStatistics stats =  sessionFactory().getStatistics().getCollectionStatistics( Parent.class.getName() + ".children" );
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

	@Test
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

		CollectionStatistics stats =  sessionFactory().getStatistics().getCollectionStatistics( Parent.class.getName() + ".children" );
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

	@Test
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
		PersistentSet children = (PersistentSet) container.getContents();

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

	@Test
	@FailureExpected( jiraKey = "HHH-2485" )
	public void testCompositeElementMerging() {
		Session session = openSession();
		session.beginTransaction();
		Container container = new Container( "p1" );
		Container.Content c1 = new Container.Content( "c1" );
		container.getContents().add( c1 );
		session.save( container );
		session.getTransaction().commit();
		session.close();

		CollectionStatistics stats =  sessionFactory().getStatistics().getCollectionStatistics( Container.class.getName() + ".contents" );
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

	@Test
	@FailureExpected( jiraKey = "HHH-2485" )
	public void testCompositeElementCollectionDirtyChecking() {
		Session session = openSession();
		session.beginTransaction();
		Container container = new Container( "p1" );
		Container.Content c1 = new Container.Content( "c1" );
		container.getContents().add( c1 );
		session.save( container );
		session.getTransaction().commit();
		session.close();

		CollectionStatistics stats =  sessionFactory().getStatistics().getCollectionStatistics( Container.class.getName() + ".contents" );
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

	@Test
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

	@Test
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
