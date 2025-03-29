/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.event.collection;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.event.spi.AbstractCollectionEvent;
import org.hibernate.orm.test.event.collection.association.bidirectional.manytomany.ChildWithBidirectionalManyToMany;
import org.hibernate.orm.test.event.collection.association.unidirectional.ParentWithCollectionOfEntities;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * These tests are known to fail. When the functionality is corrected, the
 * corresponding method will be moved into AbstractCollectionEventTest.
 *
 * @author Gail Badner
 */
@RequiresDialectFeature(DialectChecks.SupportsNoColumnInsert.class)
public class BrokenCollectionEventTest extends BaseCoreFunctionalTestCase {


	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

	@Override
	public String[] getMappings() {
		return new String[] { "event/collection/association/unidirectional/onetomany/UnidirectionalOneToManySetMapping.hbm.xml" };
	}

	@Override
	protected void cleanupTest() {
		ParentWithCollection dummyParent = createParent( "dummyParent" );
		dummyParent.setChildren( createCollection() );
		Child dummyChild = dummyParent.addChild( "dummyChild" );
		inTransaction(
				s -> {
					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<? extends Child> childrenCriteria = criteriaBuilder.createQuery( dummyChild.getClass() );
					childrenCriteria.from( dummyChild.getClass() );
					List children = s.createQuery( childrenCriteria ).list();
//					List children = s.createCriteria( dummyChild.getClass() ).list();

					CriteriaQuery<? extends ParentWithCollection> parentsCriteria = criteriaBuilder.createQuery( dummyParent.getClass() );
					childrenCriteria.from( dummyParent.getClass() );
					List parents = s.createQuery( parentsCriteria ).list();

//					List children = s.createCriteria( dummyChild.getClass() ).list();
//					List parents = s.createCriteria( dummyParent.getClass() ).list();
					for ( Iterator it = parents.iterator(); it.hasNext(); ) {
						ParentWithCollection parent = ( ParentWithCollection ) it.next();
						parent.clearChildren();
						s.remove( parent );
					}
					for ( Iterator it = children.iterator(); it.hasNext(); ) {
						s.remove( it.next() );
					}
				}
		);
	}

	public ParentWithCollection createParent(String name) {
		return new ParentWithCollectionOfEntities( name );
	}

	public Collection createCollection() {
		return new HashSet();
	}

	@Test
	@FailureExpected( jiraKey = "unknown" )
	public void testUpdateDetachedParentNoChildrenToNull() {
		CollectionListeners listeners = new CollectionListeners( sessionFactory() );
		ParentWithCollection parent = createParentWithNoChildren( "parent" );
		listeners.clear();
		assertEquals( 0, parent.getChildren().size() );
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Collection oldCollection = parent.getChildren();
		parent.newChildren( null );
		s.merge( parent );
		tx.commit();
		s.close();
		int index = 0;
		checkResult( listeners, listeners.getPreCollectionRemoveListener(), parent, oldCollection, index++ );
		checkResult( listeners, listeners.getPostCollectionRemoveListener(), parent, oldCollection, index++ );
		// pre- and post- collection recreate events should be created when updating an entity with a "null" collection
		checkResult( listeners, listeners.getPreCollectionRecreateListener(), parent, index++ );
		checkResult( listeners, listeners.getPostCollectionRecreateListener(), parent, index++ );
		checkNumberOfResults( listeners, index );
	}

	// The following fails for the same reason as testUpdateDetachedParentNoChildrenToNullFailureExpected
	// When that issue is fixed, this one should also be fixed and moved into AbstractCollectionEventTest.
	/*
	public void testUpdateDetachedParentOneChildToNullFailureExpected() {
		CollectionListeners listeners = new CollectionListeners( sessionFactory() );
		ParentWithCollection parent = createParentWithOneChild( "parent", "child" );
		Child oldChild = ( Child ) parent.getChildren().iterator().next();
		assertEquals( 1, parent.getChildren().size() );
		listeners.clear();
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Collection oldCollection = parent.getChildren();
		parent.newChildren( null );
		s.update( parent );
		tx.commit();
		s.close();
		int index = 0;
		checkResult( listeners, listeners.getPreCollectionRemoveListener(), parent, oldCollection, index++ );
		checkResult( listeners, listeners.getPostCollectionRemoveListener(), parent, oldCollection, index++ );
		if ( oldChild instanceof ChildWithBidirectionalManyToMany ) {
			checkResult( listeners, listeners.getPreCollectionUpdateListener(), ( ChildWithBidirectionalManyToMany ) oldChild, index++ );
			checkResult( listeners, listeners.getPostCollectionUpdateListener(), ( ChildWithBidirectionalManyToMany ) oldChild, index++ );
		}
		// pre- and post- collection recreate events should be created when updating an entity with a "null" collection
		checkResult( listeners, listeners.getPreCollectionRecreateListener(), parent, index++ );
		checkResult( listeners, listeners.getPostCollectionRecreateListener(), parent, index++ );
		checkNumberOfResults( listeners, index );
	}
	*/

	@Test
	@FailureExpected( jiraKey = "unknown" )
	public void testSaveParentNullChildren() {
		CollectionListeners listeners = new CollectionListeners( sessionFactory() );
		ParentWithCollection parent = createParentWithNullChildren( "parent" );
		assertNull( parent.getChildren() );
		int index = 0;
		// pre- and post- collection recreate events should be created when creating an entity with a "null" collection
		checkResult( listeners, listeners.getPreCollectionRecreateListener(), parent, index++ );
		checkResult( listeners, listeners.getPostCollectionRecreateListener(), parent, index++ );
		checkNumberOfResults( listeners, index );
		listeners.clear();
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		parent = ( ParentWithCollection ) s.get( parent.getClass(), parent.getId() );
		tx.commit();
		s.close();
		assertNotNull( parent.getChildren() );
		checkNumberOfResults( listeners, 0 );
	}

	@Test
	@FailureExpected( jiraKey = "unknown" )
	public void testUpdateParentNoChildrenToNull() {
		CollectionListeners listeners = new CollectionListeners( sessionFactory() );
		ParentWithCollection parent = createParentWithNoChildren( "parent" );
		listeners.clear();
		assertEquals( 0, parent.getChildren().size() );
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		parent = ( ParentWithCollection ) s.get( parent.getClass(), parent.getId() );
		Collection oldCollection = parent.getChildren();
		parent.newChildren( null );
		tx.commit();
		s.close();
		int index = 0;
		if ( ( (PersistentCollection) oldCollection ).wasInitialized() ) {
			checkResult( listeners, listeners.getInitializeCollectionListener(), parent, oldCollection, index++ );
		}
		checkResult( listeners, listeners.getPreCollectionRemoveListener(), parent, oldCollection, index++ );
		checkResult( listeners, listeners.getPostCollectionRemoveListener(), parent, oldCollection, index++ );
		// pre- and post- collection recreate events should be created when updating an entity with a "null" collection
		checkResult( listeners, listeners.getPreCollectionRecreateListener(), parent, index++ );
		checkResult( listeners, listeners.getPostCollectionRecreateListener(), parent, index++ );
		checkNumberOfResults( listeners, index );
	}


	// The following two tests fail for the same reason as testUpdateParentNoChildrenToNullFailureExpected
	// When that issue is fixed, this one should also be fixed and moved into AbstractCollectionEventTest.
	/*
	public void testUpdateParentOneChildToNullFailureExpected() {
		CollectionListeners listeners = new CollectionListeners( sessionFactory() );
		ParentWithCollection parent = createParentWithOneChild( "parent", "child" );
		Child oldChild = ( Child ) parent.getChildren().iterator().next();
		assertEquals( 1, parent.getChildren().size() );
		listeners.clear();
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		parent = ( AbstractParentWithCollection ) s.get( parent.getClass(), parent.getId() );
		if ( oldChild instanceof ChildEntity ) {
			oldChild = ( Child ) s.get( oldChild.getClass(), ( ( ChildEntity ) oldChild ).getId() );
		}
		Collection oldCollection = parent.getChildren();
		parent.newChildren( null );
		tx.commit();
		s.close();
		int index = 0;
		if ( ( ( PersistentCollection ) oldCollection ).wasInitialized() ) {
			checkResult( listeners, listeners.getInitializeCollectionListener(), parent, oldCollection, index++ );
		}
		ChildWithBidirectionalManyToMany oldChildWithManyToMany = null;
		if ( oldChild instanceof ChildWithBidirectionalManyToMany ) {
			oldChildWithManyToMany = ( ChildWithBidirectionalManyToMany ) oldChild;
			if ( ( ( PersistentCollection ) oldChildWithManyToMany.getParents() ).wasInitialized() ) {
				checkResult( listeners, listeners.getInitializeCollectionListener(), oldChildWithManyToMany, index++ );
			}
		}
		checkResult( listeners, listeners.getPreCollectionRemoveListener(), parent, oldCollection, index++ );
		checkResult( listeners, listeners.getPostCollectionRemoveListener(), parent, oldCollection, index++ );
		if ( oldChildWithManyToMany != null ) {
			checkResult( listeners, listeners.getPreCollectionUpdateListener(), oldChildWithManyToMany, index++ );
			checkResult( listeners, listeners.getPostCollectionUpdateListener(), oldChildWithManyToMany, index++ );
		}
		// pre- and post- collection recreate events should be created when updating an entity with a "null" collection
		checkResult( listeners, listeners.getPreCollectionRecreateListener(), parent, index++ );
		checkResult( listeners, listeners.getPostCollectionRecreateListener(), parent, index++ );
		checkNumberOfResults( listeners, index );
	}

	public void testUpdateMergedParentOneChildToNullFailureExpected() {
		CollectionListeners listeners = new CollectionListeners( sessionFactory() );
		ParentWithCollection parent = createParentWithOneChild( "parent", "child" );
		assertEquals( 1, parent.getChildren().size() );
		listeners.clear();
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		parent = ( AbstractParentWithCollection ) s.merge( parent );
		Collection oldCollection = parent.getChildren();
		parent.newChildren( null );
		tx.commit();
		s.close();
		int index = 0;
		Child oldChild = ( Child ) oldCollection.iterator().next();
		ChildWithBidirectionalManyToMany oldChildWithManyToMany = null;
		if ( oldChild instanceof ChildWithBidirectionalManyToMany ) {
			oldChildWithManyToMany = ( ChildWithBidirectionalManyToMany ) oldChild;
			if ( ( ( PersistentCollection ) oldChildWithManyToMany.getParents() ).wasInitialized() ) {
		}
			checkResult( listeners, listeners.getInitializeCollectionListener(), oldChildWithManyToMany, index++ );
		}
		checkResult( listeners, listeners.getPreCollectionRemoveListener(), parent, oldCollection, index++ );
		checkResult( listeners, listeners.getPostCollectionRemoveListener(), parent, oldCollection, index++ );
		if ( oldChildWithManyToMany != null ) {
			checkResult( listeners, listeners.getPreCollectionUpdateListener(), oldChildWithManyToMany, index++ );
			checkResult( listeners, listeners.getPostCollectionUpdateListener(), oldChildWithManyToMany, index++ );
		}
		// pre- and post- collection recreate events should be created when updating an entity with a "null" collection
		checkResult( listeners, listeners.getPreCollectionRecreateListener(), parent, index++ );
		checkResult( listeners, listeners.getPostCollectionRecreateListener(), parent, index++ );
		checkNumberOfResults( listeners, index );
	}
	*/

	private ParentWithCollection createParentWithNullChildren(String parentName) {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		ParentWithCollection parent = createParent( parentName );
		s.persist( parent );
		tx.commit();
		s.close();
		return parent;
	}

	private ParentWithCollection createParentWithNoChildren(String parentName) {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		ParentWithCollection parent = createParent( parentName );
		parent.setChildren( createCollection() );
		s.persist( parent );
		tx.commit();
		s.close();
		return parent;
	}

	private ParentWithCollection createParentWithOneChild(String parentName, String ChildName) {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		ParentWithCollection parent = createParent( parentName );
		parent.setChildren( createCollection() );
		parent.addChild( ChildName );
		s.persist( parent );
		tx.commit();
		s.close();
		return parent;
	}

	protected void checkResult(CollectionListeners listeners,
							CollectionListeners.Listener listenerExpected,
							ParentWithCollection parent,
							int index) {
		checkResult( listeners, listenerExpected, parent, parent.getChildren(), index );
	}
	protected void checkResult(CollectionListeners listeners,
							CollectionListeners.Listener listenerExpected,
							ChildWithBidirectionalManyToMany child,
							int index) {
		checkResult( listeners, listenerExpected, child, child.getParents(), index );
	}

	protected void checkResult(CollectionListeners listeners,
							CollectionListeners.Listener listenerExpected,
							Entity ownerExpected,
							Collection collExpected,
							int index) {
		assertSame( listenerExpected, listeners.getListenersCalled().get( index ) );
		assertSame(
				ownerExpected,
				( ( AbstractCollectionEvent ) listeners.getEvents().get( index ) ).getAffectedOwnerOrNull()
		);
		assertEquals(
				ownerExpected.getId(),
				( ( AbstractCollectionEvent ) listeners.getEvents().get( index ) ).getAffectedOwnerIdOrNull()
		);
		assertEquals(
				ownerExpected.getClass().getName(),
				( (AbstractCollectionEvent) listeners.getEvents().get( index ) ).getAffectedOwnerEntityName()
		);
		assertSame(
				collExpected, ( ( AbstractCollectionEvent ) listeners.getEvents().get( index ) ).getCollection()
		);
	}

	private void checkNumberOfResults(CollectionListeners listeners, int nEventsExpected) {
		assertEquals( nEventsExpected, listeners.getListenersCalled().size() );
		assertEquals( nEventsExpected, listeners.getEvents().size() );
	}
}
