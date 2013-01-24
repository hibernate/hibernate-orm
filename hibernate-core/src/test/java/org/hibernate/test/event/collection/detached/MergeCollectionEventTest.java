/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.event.collection.detached;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.event.spi.AbstractCollectionEvent;
import org.hibernate.event.spi.PostCollectionRecreateEvent;
import org.hibernate.event.spi.PostCollectionUpdateEvent;
import org.hibernate.event.spi.PreCollectionRecreateEvent;
import org.hibernate.event.spi.PreCollectionRemoveEvent;
import org.hibernate.event.spi.PreCollectionUpdateEvent;

import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-7928" )
public class MergeCollectionEventTest extends BaseCoreFunctionalTestCase {

	private AggregatedCollectionEventListener.IntegratorImpl collectionListenerIntegrator =
			new AggregatedCollectionEventListener.IntegratorImpl();

	@Before
	public void resetListener() {
		collectionListenerIntegrator.getListener().reset();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Character.class, Alias.class };
	}

	@Override
	protected void prepareBootstrapRegistryBuilder(BootstrapServiceRegistryBuilder builder) {
		super.prepareBootstrapRegistryBuilder( builder );
		builder.with( collectionListenerIntegrator );
	}

	@Override
	protected void cleanupTestData() throws Exception {
		Session s = openSession();
		s.beginTransaction();
		List<Alias> aliases = s.createQuery( "from Alias" ).list();
		for ( Alias alias : aliases ) {
			for ( Character character : alias.getCharacters() ) {
				character.getAliases().clear();
			}
			alias.getCharacters().clear();
		}
		s.flush();
		s.createQuery( "delete Alias" ).executeUpdate();
		s.createQuery( "delete Character" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testCollectionEventHandlingOnMerge() {
		final AggregatedCollectionEventListener listener = collectionListenerIntegrator.getListener();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// This first bit really is just preparing the entities.  There is generally no collection
		// events of real interest during this part

		Session s = openSession();
		s.beginTransaction();
		Character paul = new Character( 1, "Paul Atreides" );
		s.save( paul );
		s.getTransaction().commit();
		s.close();

		assertEquals( 2, listener.getEventEntryList().size() );
		checkListener( 0, PreCollectionRecreateEvent.class, paul, Collections.EMPTY_LIST );
		checkListener( 1, PostCollectionRecreateEvent.class, paul, Collections.EMPTY_LIST );

		listener.reset();

		s = openSession();
		s.beginTransaction();
		Character paulo = new Character( 2, "Paulo Atreides" );
		s.save( paulo );
		s.getTransaction().commit();
		s.close();

		assertEquals( 2, listener.getEventEntryList().size() );
		checkListener( 0, PreCollectionRecreateEvent.class, paulo, Collections.EMPTY_LIST );
		checkListener( 1, PostCollectionRecreateEvent.class, paulo, Collections.EMPTY_LIST );

		listener.reset();

		s = openSession();
		s.beginTransaction();
		Alias alias1 = new Alias( 1, "Paul Muad'Dib" );
		s.save( alias1 );
		s.getTransaction().commit();
		s.close();

		assertEquals( 2, listener.getEventEntryList().size() );
		checkListener( 0, PreCollectionRecreateEvent.class, alias1, Collections.EMPTY_LIST );
		checkListener( 1, PostCollectionRecreateEvent.class, alias1, Collections.EMPTY_LIST );

		listener.reset();

		s = openSession();
		s.beginTransaction();
		Alias alias2 = new Alias( 2, "Usul" );
		s.save( alias2 );
		s.getTransaction().commit();
		s.close();

		assertEquals( 2, listener.getEventEntryList().size() );
		checkListener( 0, PreCollectionRecreateEvent.class, alias2, Collections.EMPTY_LIST );
		checkListener( 1, PostCollectionRecreateEvent.class, alias2, Collections.EMPTY_LIST );


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// at this point we can start setting up the associations and checking collection events
		// of "real interest"

		listener.reset();

		paul.associateAlias( alias1 );
		paul.associateAlias( alias2 );

		paulo.associateAlias( alias1 );
		paulo.associateAlias( alias2 );

		s = openSession();
		s.beginTransaction();
		s.merge( alias1 );

		assertEquals( 0, listener.getEventEntryList().size() );

		// this is where HHH-7928 (problem with HHH-6361 fix) shows up...
		s.flush();

		assertEquals( 8, listener.getEventEntryList().size() ); // 4 collections x 2 events per
		checkListener( 0, PreCollectionUpdateEvent.class, alias1, Collections.EMPTY_LIST );
		checkListener( 1, PostCollectionUpdateEvent.class, alias1, alias1.getCharacters() );
		checkListener( 2, PreCollectionUpdateEvent.class, paul, Collections.EMPTY_LIST );
		checkListener( 3, PostCollectionUpdateEvent.class, paul, paul.getAliases() );
		checkListener( 4, PreCollectionUpdateEvent.class, alias2, Collections.EMPTY_LIST );
		checkListener( 5, PostCollectionUpdateEvent.class, alias2, alias2.getCharacters() );
		checkListener( 6, PreCollectionUpdateEvent.class, paulo, Collections.EMPTY_LIST );
		checkListener( 7, PostCollectionUpdateEvent.class, paulo, paul.getAliases() );

		List<Character> alias1CharactersSnapshot = copy( alias1.getCharacters() );
		List<Character> alias2CharactersSnapshot = copy( alias2.getCharacters() );

		listener.reset();

		s.merge( alias2 );

		assertEquals( 0, listener.getEventEntryList().size() );

		s.flush();

		assertEquals( 8, listener.getEventEntryList().size() ); // 4 collections x 2 events per
		checkListener( 0, PreCollectionUpdateEvent.class, alias1, alias1CharactersSnapshot );
		checkListener( 1, PostCollectionUpdateEvent.class, alias1, alias1CharactersSnapshot );
//		checkListener( 2, PreCollectionUpdateEvent.class, paul, Collections.EMPTY_LIST );
//		checkListener( 3, PostCollectionUpdateEvent.class, paul, paul.getAliases() );
		checkListener( 4, PreCollectionUpdateEvent.class, alias2, alias2CharactersSnapshot );
		checkListener( 5, PostCollectionUpdateEvent.class, alias2, alias2.getCharacters() );
//		checkListener( 6, PreCollectionUpdateEvent.class, paulo, Collections.EMPTY_LIST );
//		checkListener( 7, PostCollectionUpdateEvent.class, paulo, paul.getAliases() );

		s.getTransaction().commit();
		s.close();

//
//		checkListener(listeners, listeners.getInitializeCollectionListener(),
//					  mce, null, eventCount++);
//		checkListener(listeners, listeners.getPreCollectionUpdateListener(),
//					  mce, oldRefentities1, eventCount++);
//		checkListener(listeners, listeners.getPostCollectionUpdateListener(),
//					  mce, mce.getRefEntities1(), eventCount++);

	}


	protected void checkListener(
			int eventIndex,
			Class<? extends AbstractCollectionEvent> expectedEventType,
			Identifiable expectedOwner,
			List<? extends Identifiable> expectedCollectionEntrySnapshot) {
		final AggregatedCollectionEventListener.EventEntry eventEntry
				= collectionListenerIntegrator.getListener().getEventEntryList().get( eventIndex );
		final AbstractCollectionEvent event = eventEntry.getEvent();

		assertTyping( expectedEventType, event );

// because of the merge graphs, the instances are likely different.  just base check on type and id
//		assertEquals( expectedOwner, event.getAffectedOwnerOrNull() );
		assertEquals( expectedOwner.getClass().getName(), event.getAffectedOwnerEntityName() );
		assertEquals( expectedOwner.getId(), event.getAffectedOwnerIdOrNull() );

		if ( event instanceof PreCollectionUpdateEvent
				|| event instanceof PreCollectionRemoveEvent
				|| event instanceof PostCollectionRecreateEvent ) {
			List<Identifiable> snapshot = (List) eventEntry.getSnapshotAtTimeOfEventHandling();
			assertEquals( expectedCollectionEntrySnapshot.size(), snapshot.size() );
			for ( int i = 0; i < expectedCollectionEntrySnapshot.size(); i++ ) {
				Identifiable expected = expectedCollectionEntrySnapshot.get( i );
				Identifiable found = snapshot.get( i );
				assertEquals( expected.getClass().getName(), found.getClass().getName() );
				assertEquals( expected.getId(), found.getId() );
			}
		}
	}

	private <T> List<T> copy(List<T> source) {
		ArrayList<T> copy = new ArrayList<T>( source.size() );
		copy.addAll( source );
		return copy;
	}
}
