/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.event.collection;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.event.spi.AbstractCollectionEvent;
import org.hibernate.orm.test.event.collection.association.unidirectional.ParentWithCollectionOfEntities;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * These tests are known to fail. When the functionality is corrected, the
 * corresponding method will be moved into AbstractCollectionEventTest.
 *
 * @author Gail Badner
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/event/collection/association/unidirectional/onetomany/UnidirectionalOneToManySetMapping.hbm.xml"
)
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNoColumnInsert.class)
public class BrokenCollectionEventTest {

	@AfterEach
	protected void cleanupTest(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			s.getSessionFactory().getSchemaManager().truncateMappedObjects();
		} );
	}

	public ParentWithCollection createParent(String name) {
		return new ParentWithCollectionOfEntities( name );
	}

	public Collection createCollection() {
		return new HashSet<>();
	}

	@Test
	@FailureExpected(jiraKey = "unknown")
	public void testUpdateDetachedParentNoChildrenToNull(SessionFactoryScope scope) {
		CollectionListeners listeners = new CollectionListeners( scope.getSessionFactory() );
		ParentWithCollection parent = createParentWithNoChildren( scope, "parent" );
		listeners.clear();
		assertEquals( 0, parent.getChildren().size() );
		var oldCollection = scope.fromTransaction( s -> {
			Collection oc = parent.getChildren();
			parent.newChildren( null );
			s.merge( parent );
			return oc;
		} );
		int index = 0;
		checkResult( listeners, listeners.getPreCollectionRemoveListener(), parent, oldCollection, index++ );
		checkResult( listeners, listeners.getPostCollectionRemoveListener(), parent, oldCollection, index++ );
		// pre- and post- collection recreate events should be created when updating an entity with a "null" collection
		checkResult( listeners, listeners.getPreCollectionRecreateListener(), parent, index++ );
		checkResult( listeners, listeners.getPostCollectionRecreateListener(), parent, index++ );
		checkNumberOfResults( listeners, index );
	}

	@Test
	@FailureExpected(jiraKey = "unknown")
	public void testSaveParentNullChildren(SessionFactoryScope scope) {
		CollectionListeners listeners = new CollectionListeners( scope.getSessionFactory() );
		ParentWithCollection parent = createParentWithNullChildren( scope, "parent" );
		assertNull( parent.getChildren() );
		int index = 0;
		// pre- and post- collection recreate events should be created when creating an entity with a "null" collection
		checkResult( listeners, listeners.getPreCollectionRecreateListener(), parent, index++ );
		checkResult( listeners, listeners.getPostCollectionRecreateListener(), parent, index++ );
		checkNumberOfResults( listeners, index );
		listeners.clear();
		var p = scope.fromTransaction( s -> {
			return (ParentWithCollection) s.get( parent.getClass(), parent.getId() );
		} );
		assertNotNull( p.getChildren() );
		checkNumberOfResults( listeners, 0 );
	}

	@Test
	@FailureExpected(jiraKey = "unknown")
	public void testUpdateParentNoChildrenToNull(SessionFactoryScope scope) {
		CollectionListeners listeners = new CollectionListeners( scope.getSessionFactory() );
		ParentWithCollection parent = createParentWithNoChildren( scope, "parent" );
		listeners.clear();
		assertEquals( 0, parent.getChildren().size() );
		Long id = parent.getId();

		var data = scope.fromTransaction( s -> {
			var p = (ParentWithCollection) s.get( ParentWithCollection.class, id );
			Collection oldCollection = p.getChildren();
			p.newChildren( null );
			return List.of( p, oldCollection );
		} );
		int index = 0;
		Collection oldCollection = (Collection) data.get( 1 );
		parent = (ParentWithCollection) data.get( 0 );
		if ( ((PersistentCollection) oldCollection).wasInitialized() ) {
			checkResult( listeners, listeners.getInitializeCollectionListener(), parent, oldCollection, index++ );
		}
		checkResult( listeners, listeners.getPreCollectionRemoveListener(), parent, oldCollection, index++ );
		checkResult( listeners, listeners.getPostCollectionRemoveListener(), parent, oldCollection, index++ );
		// pre- and post- collection recreate events should be created when updating an entity with a "null" collection
		checkResult( listeners, listeners.getPreCollectionRecreateListener(), parent, index++ );
		checkResult( listeners, listeners.getPostCollectionRecreateListener(), parent, index++ );
		checkNumberOfResults( listeners, index );
	}

	private ParentWithCollection createParentWithNullChildren(SessionFactoryScope scope, String parentName) {
		return scope.fromTransaction( s -> {
			ParentWithCollection parent = createParent( parentName );
			s.persist( parent );
			return parent;
		} );
	}

	private ParentWithCollection createParentWithNoChildren(SessionFactoryScope scope, String parentName) {
		return scope.fromTransaction( s -> {
			ParentWithCollection parent = createParent( parentName );
			parent.setChildren( createCollection() );
			s.persist( parent );
			return parent;
		} );
	}

	protected void checkResult(CollectionListeners listeners,
							CollectionListeners.Listener listenerExpected,
							ParentWithCollection parent,
							int index) {
		checkResult( listeners, listenerExpected, parent, parent.getChildren(), index );
	}

	protected void checkResult(CollectionListeners listeners,
							CollectionListeners.Listener listenerExpected,
							Entity ownerExpected,
							Collection collExpected,
							int index) {
		assertSame( listenerExpected, listeners.getListenersCalled().get( index ) );
		assertSame(
				ownerExpected,
				((AbstractCollectionEvent) listeners.getEvents().get( index )).getAffectedOwnerOrNull()
		);
		assertEquals(
				ownerExpected.getId(),
				((AbstractCollectionEvent) listeners.getEvents().get( index )).getAffectedOwnerIdOrNull()
		);
		assertEquals(
				ownerExpected.getClass().getName(),
				((AbstractCollectionEvent) listeners.getEvents().get( index )).getAffectedOwnerEntityName()
		);
		assertSame(
				collExpected, ((AbstractCollectionEvent) listeners.getEvents().get( index )).getCollection()
		);
	}

	private void checkNumberOfResults(CollectionListeners listeners, int nEventsExpected) {
		assertEquals( nEventsExpected, listeners.getListenersCalled().size() );
		assertEquals( nEventsExpected, listeners.getEvents().size() );
	}
}
