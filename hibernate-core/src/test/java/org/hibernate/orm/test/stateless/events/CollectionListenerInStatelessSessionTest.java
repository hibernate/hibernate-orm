/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stateless.events;

import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostCollectionRecreateEvent;
import org.hibernate.event.spi.PostCollectionRecreateEventListener;
import org.hibernate.event.spi.PostCollectionRemoveEvent;
import org.hibernate.event.spi.PostCollectionRemoveEventListener;
import org.hibernate.event.spi.PreCollectionRecreateEvent;
import org.hibernate.event.spi.PreCollectionRecreateEventListener;
import org.hibernate.event.spi.PreCollectionRemoveEvent;
import org.hibernate.event.spi.PreCollectionRemoveEventListener;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = {EntityA.class, EntityB.class})
@SessionFactory
class CollectionListenerInStatelessSessionTest {

	@Test
	void statelessInsert(SessionFactoryScope scope) {
		var registry = scope.getSessionFactory().getEventListenerRegistry();
		var preRecreate = new MyPreCollectionRecreateEventListener();
		var preRemove = new MyPreCollectionRemoveEventListener();
		var postRecreate = new MyPostCollectionRecreateEventListener();
		var postRemove = new MyPostCollectionRemoveEventListener();
		registry.getEventListenerGroup(EventType.PRE_COLLECTION_RECREATE)
				.appendListener( preRecreate );
		registry.getEventListenerGroup(EventType.PRE_COLLECTION_REMOVE)
				.appendListener( preRemove );
		registry.getEventListenerGroup(EventType.POST_COLLECTION_RECREATE)
				.appendListener( postRecreate );
		registry.getEventListenerGroup(EventType.POST_COLLECTION_REMOVE)
				.appendListener( postRemove );

		scope.inStatelessTransaction(statelessSession -> {
			EntityA a = new EntityA();
			EntityB b = new EntityB();
			a.children.add(b);
			statelessSession.insert( b );
			statelessSession.insert( a );
			statelessSession.delete( a );
			statelessSession.delete( b );
		});

		assertEquals(1, preRecreate.called);
		assertEquals(1, preRemove.called);
		assertEquals(1, postRecreate.called);
		assertEquals(1, postRemove.called);
	}

}

class MyPreCollectionRecreateEventListener implements PreCollectionRecreateEventListener {

	int called = 0;

	@Override
	public void onPreRecreateCollection(PreCollectionRecreateEvent event) {
		called++;
	}

}

class MyPreCollectionRemoveEventListener implements PreCollectionRemoveEventListener {

	int called = 0;

	@Override
	public void onPreRemoveCollection(PreCollectionRemoveEvent event) {
		called++;
	}

}

class MyPostCollectionRecreateEventListener implements PostCollectionRecreateEventListener {

	int called = 0;

	@Override
	public void onPostRecreateCollection(PostCollectionRecreateEvent event) {
		called++;
	}

}

class MyPostCollectionRemoveEventListener implements PostCollectionRemoveEventListener {

	int called = 0;

	@Override
	public void onPostRemoveCollection(PostCollectionRemoveEvent event) {
		called++;
	}

}
