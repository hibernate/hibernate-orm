/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.collection.set;

import java.util.HashSet;

import org.hibernate.CacheMode;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.collection.internal.PersistentSet;
import org.hibernate.stat.CollectionStatistics;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
public class PersistentSetTest extends SessionFactoryBasedFunctionalTest {
	@Override
	public String[] getHmbMappingFiles() {
		return new String[] { "collection/set/Mappings.hbm.xml" };
	}

	@Override
	protected void applySettings(StandardServiceRegistryBuilder builer) {
		builer.applySetting( AvailableSettings.GENERATE_STATISTICS, "true" );
		builer.applySetting( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true" );
		builer.applySetting( AvailableSettings.USE_QUERY_CACHE, "true" );
	}

	@Test
	public void testWriteMethodDirtying() {
		Parent parent = new Parent( "p1" );
		Child child = new Child( "c1" );
		parent.getChildren().add( child );
		child.setParent( parent );
		Child otherChild = new Child( "c2" );

		inTransaction(
				session -> {
					session.save( parent );
					session.flush();
					// at this point, the set on parent has now been replaced with a PersistentSet...
					PersistentSet children = (PersistentSet) parent.getChildren();

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

					assertTrue( children.retainAll( otherSet ) );
					assertTrue( children.isDirty() );
					assertTrue( children.isEmpty() );

					children.clear();
					session.delete( child );
					assertTrue( children.isDirty() );

					session.flush();

					children.clear();
					assertFalse( children.isDirty() );

					session.delete( parent );
				}
		);
	}

	@Test
	public void testCollectionMerging() {
		final Parent parent = new Parent( "p1" );
		Child child = new Child( "c1" );
		child.setParent( parent );
		parent.getChildren().add( child );

		inTransaction(
				session -> {
					session.save( parent );
				}
		);


		CollectionStatistics stats = sessionFactory().getStatistics()
				.getCollectionStatistics( Parent.class.getName() + ".children" );
		long recreateCount = stats.getRecreateCount();
		long updateCount = stats.getUpdateCount();

		Parent retrievedParent = inTransaction(
				session -> {
					return (Parent) session.merge( parent );
				}
		);

		assertEquals( 1, retrievedParent.getChildren().size() );
		assertEquals( recreateCount, stats.getRecreateCount() );
		assertEquals( updateCount, stats.getUpdateCount() );

		inTransaction(
				session -> {
					Parent savedParent = session.get( Parent.class, "p1" );
					assertEquals( 1, savedParent.getChildren().size() );
					session.delete( savedParent );
				}
		);

	}

	@Test
	public void testCollectiondirtyChecking() {
		final Parent parent = new Parent( "p1" );
		Child child = new Child( "c1" );
		parent.getChildren().add( child );
		child.setParent( parent );
		inTransaction(
				session -> {
					session.save( parent );
				}
		);


		CollectionStatistics stats = sessionFactory().getStatistics()
				.getCollectionStatistics( Parent.class.getName() + ".children" );
		long recreateCount = stats.getRecreateCount();
		long updateCount = stats.getUpdateCount();

		inTransaction(
				session -> {
					Parent savedParent = session.get( Parent.class, "p1" );
					assertEquals( 1, savedParent.getChildren().size() );
				}
		);

		assertEquals( 1, parent.getChildren().size() );
		assertEquals( recreateCount, stats.getRecreateCount() );
		assertEquals( updateCount, stats.getUpdateCount() );

		inTransaction(
				session -> {
					assertEquals( 1, parent.getChildren().size() );
					session.delete( parent );
				}
		);
	}

	@Test
	public void testCompositeElementWriteMethodDirtying() {
		Container container = new Container( "p1" );
		Container.Content c1 = new Container.Content( "c1" );
		container.getContents().add( c1 );
		Container.Content c2 = new Container.Content( "c2" );

		inTransaction(
				session -> {
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

					assertTrue( children.retainAll( otherSet ) );
					assertTrue( children.isDirty() );
					assertTrue( children.isEmpty() );

					children.clear();
					assertTrue( children.isDirty() );

					session.flush();

					children.clear();
					assertFalse( children.isDirty() );

					session.delete( container );
				}
		);
	}

	@Test
//	@FailureExpected(jiraKey = "HHH-2485")
	public void testCompositeElementMerging() {
		final Container container = new Container( "p1" );
		Container.Content c1 = new Container.Content( "c1" );
		container.getContents().add( c1 );
		inTransaction(
				session -> {
					session.save( container );
				}
		);

		CollectionStatistics stats = sessionFactory().getStatistics()
				.getCollectionStatistics( Container.class.getName() + ".contents" );
		long recreateCount = stats.getRecreateCount();
		long updateCount = stats.getUpdateCount();

		container.setName( "another name" );

		Container merged = inTransaction(
				session -> (Container) session.merge( container )
		);

		assertEquals( 1, merged.getContents().size() );
		assertEquals( recreateCount, stats.getRecreateCount() );
		assertEquals( updateCount, stats.getUpdateCount() );

		inTransaction( session -> {
			Container savedContainer = session.get( Container.class, container.getId() );
			assertEquals( 1, savedContainer.getContents().size() );
			session.delete( savedContainer );
		} );
	}

	@Test
//	@FailureExpected(jiraKey = "HHH-2485")
	public void testCompositeElementCollectionDirtyChecking() {
		final Container container = new Container( "p1" );
		Container.Content c1 = new Container.Content( "c1" );
		container.getContents().add( c1 );

		inTransaction(
				session -> {
					session.save( container );

				}
		);

		CollectionStatistics stats = sessionFactory().getStatistics()
				.getCollectionStatistics( Container.class.getName() + ".contents" );
		long recreateCount = stats.getRecreateCount();
		long updateCount = stats.getUpdateCount();

		Container savedContainer = inTransaction(
				session -> {
					Container result = session.get( Container.class, container.getId() );
					assertEquals( 1, result.getContents().size() );
					return result;
				}
		);

		assertEquals( 1, savedContainer.getContents().size() );
		assertEquals( recreateCount, stats.getRecreateCount() );
		assertEquals( updateCount, stats.getUpdateCount() );

		inTransaction(
				session -> {
					Container result = session.get( Container.class, container.getId() );
					assertEquals( 1, result.getContents().size() );
					session.delete( result );
				}
		);
	}

	@Test
	public void testLoadChildCheckParentContainsChildCache() {
		final Parent parent = new Parent( "p1" );
		final Child child = new Child( "c1" );
		child.setDescription( "desc1" );
		parent.getChildren().add( child );
		child.setParent( parent );
		Child otherChild = new Child( "c2" );
		otherChild.setDescription( "desc2" );
		parent.getChildren().add( otherChild );
		otherChild.setParent( parent );

		inTransaction(
				session -> {
					session.save( parent );
				}
		);

		inTransaction(
				session -> {
					Parent savedParent = session.get( Parent.class, parent.getName() );
					assertTrue( savedParent.getChildren().contains( child ) );
					assertTrue( savedParent.getChildren().contains( otherChild ) );
				}
		);

		inTransaction(
				session -> {
					Child savedChild = session.get( Child.class, child.getName() );
					assertTrue( savedChild.getParent().getChildren().contains( savedChild ) );
					session.clear();

					// todo (6.0) : uncomment when criteria will be implemented
//					savedChild = session.createCriteria( Child.class, child.getName() )
//							.setCacheable( true )
//							.add( Restrictions.idEq( "c1" ) )
//							.uniqueResult();
//					assertTrue( child.getParent().getChildren().contains( savedChild ) );
//					assertTrue( child.getParent().getChildren().contains( otherChild ) );
//					session.clear();

					savedChild = (Child) session.createQuery( "from Child where name = 'c1'" )
							.setCacheable( true )
							.uniqueResult();
					assertTrue( child.getParent().getChildren().contains( savedChild ) );

					savedChild = (Child) session.createQuery( "from Child where name = 'c1'" )
							.setCacheable( true )
							.uniqueResult();
					assertTrue( savedChild.getParent().getChildren().contains( savedChild ) );

					session.delete( savedChild.getParent() );
				}
		);
	}

	@Test
	public void testLoadChildCheckParentContainsChildNoCache() {
		final Parent parent = new Parent( "p1" );
		final Child child = new Child( "c1" );
		parent.getChildren().add( child );
		child.setParent( parent );
		Child otherChild = new Child( "c2" );
		parent.getChildren().add( otherChild );
		otherChild.setParent( parent );

		inTransaction(
				session -> {
					session.save( parent );
				}
		);

		inTransaction(
				session -> {
					session.setCacheMode( CacheMode.IGNORE );
					Parent savedParent = session.get( Parent.class, parent.getName() );
					assertTrue( savedParent.getChildren().contains( child ) );
					assertTrue( savedParent.getChildren().contains( otherChild ) );
				}
		);

		inTransaction(
				session -> {
					session.setCacheMode( CacheMode.IGNORE );

					Child savedChild = session.get( Child.class, child.getName() );
					assertTrue( child.getParent().getChildren().contains( savedChild ) );
					session.clear();

					// todo (6.0) : uncomment when criteria will be implemented
//					savedChild = (Child) session.createCriteria( Child.class, child.getName() )
//							.add( Restrictions.idEq( "c1" ) )
//							.uniqueResult();
//					assertTrue( savedChild.getParent().getChildren().contains( savedChild ) );
//					assertTrue( savedChild.getParent().getChildren().contains( otherChild ) );
//					session.clear();

					savedChild = (Child) session.createQuery( "from Child where name = 'c1'" ).uniqueResult();
					assertTrue( child.getParent().getChildren().contains( savedChild ) );

					session.delete( savedChild.getParent() );

				}
		);
	}
}
