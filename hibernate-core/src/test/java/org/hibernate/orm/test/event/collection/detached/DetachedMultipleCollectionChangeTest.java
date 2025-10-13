/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.event.collection.detached;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.event.spi.AbstractCollectionEvent;
import org.hibernate.event.spi.PostCollectionRecreateEvent;
import org.hibernate.event.spi.PreCollectionRemoveEvent;
import org.hibernate.event.spi.PreCollectionUpdateEvent;
import org.hibernate.orm.test.event.collection.Entity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * Test HHH-6361: Collection events may contain wrong stored snapshot after
 * merging a detached entity into the persistencecontext.
 *
 * @author Erik-Berndt Scheper
 */
@JiraKey(value = "HHH-6361")
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/event/collection/detached/MultipleCollectionBagMapping.hbm.xml"
)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.IMPLICIT_NAMING_STRATEGY, value = "legacy-jpa"),
				@Setting(name = Environment.DEFAULT_LIST_SEMANTICS, value = "bag"), // CollectionClassification.BAG
		}
)
@SessionFactory
public class DetachedMultipleCollectionChangeTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) throws Exception {
		scope.dropData();
	}

	@Test
	public void testMergeMultipleCollectionChangeEvents(SessionFactoryScope scope) {
		MultipleCollectionListeners listeners = new MultipleCollectionListeners(
				scope.getSessionFactory() );
		listeners.clear();
		int eventCount = 0;

		List<MultipleCollectionRefEntity1> oldRefentities1 = new ArrayList<MultipleCollectionRefEntity1>();
		List<MultipleCollectionRefEntity2> oldRefentities2 = new ArrayList<MultipleCollectionRefEntity2>();

		final AtomicReference<MultipleCollectionEntity> mce = new AtomicReference<>( new MultipleCollectionEntity() );
		scope.inTransaction( s -> {
			mce.get().setText( "MultipleCollectionEntity-1" );
			s.persist( mce.get() );
		} );

		checkListener( listeners, listeners.getPreCollectionRecreateListener(),
				mce.get(), oldRefentities1, eventCount++ );
		checkListener( listeners, listeners.getPostCollectionRecreateListener(),
				mce.get(), oldRefentities1, eventCount++ );
		checkListener( listeners, listeners.getPreCollectionRecreateListener(),
				mce.get(), oldRefentities2, eventCount++ );
		checkListener( listeners, listeners.getPostCollectionRecreateListener(),
				mce.get(), oldRefentities2, eventCount++ );
		checkEventCount( listeners, eventCount );


		Long mceId1 = mce.get().getId();
		assertNotNull( mceId1 );

		// add new entities to both collections

		MultipleCollectionEntity prevMce = mce.get().deepCopy();
		oldRefentities1 = prevMce.getRefEntities1();
		oldRefentities2 = prevMce.getRefEntities2();

		listeners.clear();
		eventCount = 0;

		MultipleCollectionRefEntity1 re1_1 = new MultipleCollectionRefEntity1();
		MultipleCollectionRefEntity1 re1_2 = new MultipleCollectionRefEntity1();

		mce.set( scope.fromTransaction( s -> {
			re1_1.setText( "MultipleCollectionRefEntity1-1" );
			re1_1.setMultipleCollectionEntity( mce.get() );

			re1_2.setText( "MultipleCollectionRefEntity1-2" );
			re1_2.setMultipleCollectionEntity( mce.get() );

			mce.get().addRefEntity1( re1_1 );
			mce.get().addRefEntity1( re1_2 );

			return s.merge( mce.get() );

		} ) );

		checkListener( listeners, listeners.getInitializeCollectionListener(),
				mce.get(), null, eventCount++ );
		checkListener( listeners, listeners.getPreCollectionUpdateListener(),
				mce.get(), oldRefentities1, eventCount++ );
		checkListener( listeners, listeners.getPostCollectionUpdateListener(),
				mce.get(), mce.get().getRefEntities1(), eventCount++ );

		MultipleCollectionRefEntity2 re2_1 = new MultipleCollectionRefEntity2();
		MultipleCollectionRefEntity2 re2_2 = new MultipleCollectionRefEntity2();

		mce.set( scope.fromTransaction( s -> {
			re2_1.setText( "MultipleCollectionRefEntity2-1" );
			re2_1.setMultipleCollectionEntity( mce.get() );

			re2_2.setText( "MultipleCollectionRefEntity2-2" );
			re2_2.setMultipleCollectionEntity( mce.get() );

			mce.get().addRefEntity2( re2_1 );
			mce.get().addRefEntity2( re2_2 );

			return s.merge( mce.get() );
		} ) );

		checkListener( listeners, listeners.getInitializeCollectionListener(),
				mce.get(), null, eventCount++ );
		checkListener( listeners, listeners.getPreCollectionUpdateListener(),
				mce.get(), oldRefentities2, eventCount++ );
		checkListener( listeners, listeners.getPostCollectionUpdateListener(),
				mce.get(), mce.get().getRefEntities2(), eventCount++ );
		checkEventCount( listeners, eventCount );

		for ( MultipleCollectionRefEntity1 refEnt1 : mce.get().getRefEntities1() ) {
			assertNotNull( refEnt1.getId() );
		}
		for ( MultipleCollectionRefEntity2 refEnt2 : mce.get().getRefEntities2() ) {
			assertNotNull( refEnt2.getId() );
		}

		// remove and add entities in both collections

		prevMce = mce.get().deepCopy();
		oldRefentities1 = prevMce.getRefEntities1();
		oldRefentities2 = prevMce.getRefEntities2();

		listeners.clear();
		eventCount = 0;

		mce.set( scope.fromTransaction( s -> {
			assertEquals( 2, mce.get().getRefEntities1().size() );
			assertEquals( 2, mce.get().getRefEntities2().size() );

			mce.get().removeRefEntity1( re1_2 );

			MultipleCollectionRefEntity1 re1_3 = new MultipleCollectionRefEntity1();
			re1_3.setText( "MultipleCollectionRefEntity1-3" );
			re1_3.setMultipleCollectionEntity( mce.get() );
			mce.get().addRefEntity1( re1_3 );

			return s.merge( mce.get() );

		} ) );

		checkListener( listeners, listeners.getInitializeCollectionListener(),
				mce.get(), null, eventCount++ );
		checkListener( listeners, listeners.getPreCollectionUpdateListener(),
				mce.get(), oldRefentities1, eventCount++ );
		checkListener( listeners, listeners.getPostCollectionUpdateListener(),
				mce.get(), mce.get().getRefEntities1(), eventCount++ );

		mce.set( scope.fromTransaction( s -> {

			mce.get().removeRefEntity2( re2_2 );

			MultipleCollectionRefEntity2 re2_3 = new MultipleCollectionRefEntity2();
			re2_3.setText( "MultipleCollectionRefEntity2-3" );
			re2_3.setMultipleCollectionEntity( mce.get() );
			mce.get().addRefEntity2( re2_3 );

			return s.merge( mce.get() );

		} ) );

		checkListener( listeners, listeners.getInitializeCollectionListener(),
				mce.get(), null, eventCount++ );
		checkListener( listeners, listeners.getPreCollectionUpdateListener(),
				mce.get(), oldRefentities2, eventCount++ );
		checkListener( listeners, listeners.getPostCollectionUpdateListener(),
				mce.get(), mce.get().getRefEntities2(), eventCount++ );

		checkEventCount( listeners, eventCount );
	}

	protected void checkListener(
			MultipleCollectionListeners listeners,
			MultipleCollectionListeners.Listener listenerExpected,
			Entity ownerExpected,
			List<? extends Entity> expectedCollectionEntrySnapshot,
			int index) {
		AbstractCollectionEvent event = listeners
				.getEvents().get( index );

		assertSame( listenerExpected, listeners.getListenersCalled().get( index ) );
		assertEquals( ownerExpected, event.getAffectedOwnerOrNull() );
		assertEquals( ownerExpected.getId(), event.getAffectedOwnerIdOrNull() );
		assertEquals( ownerExpected.getClass().getName(),
				event.getAffectedOwnerEntityName() );

		if ( event instanceof PreCollectionUpdateEvent ) {
			Serializable snapshot = listeners.getSnapshots().get( index );
			assertEquals( expectedCollectionEntrySnapshot, snapshot );
		}
		if ( event instanceof PreCollectionRemoveEvent ) {
			Serializable snapshot = listeners.getSnapshots().get( index );
			assertEquals( expectedCollectionEntrySnapshot, snapshot );
		}
		if ( event instanceof PostCollectionRecreateEvent ) {
			Serializable snapshot = listeners.getSnapshots().get( index );
			assertEquals( expectedCollectionEntrySnapshot, snapshot );
		}

	}

	private void checkEventCount(MultipleCollectionListeners listeners,
								int nEventsExpected) {
		assertEquals( nEventsExpected, listeners.getListenersCalled().size() );
		assertEquals( nEventsExpected, listeners.getEvents().size() );
	}

}
