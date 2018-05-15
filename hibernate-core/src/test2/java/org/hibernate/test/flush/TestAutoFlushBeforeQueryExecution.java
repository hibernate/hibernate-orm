/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.flush;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.engine.spi.ActionQueue;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.internal.SessionImpl;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
@TestForIssue( jiraKey = "HHH-6960" )
public class TestAutoFlushBeforeQueryExecution extends BaseCoreFunctionalTestCase {

	@Test
	public void testAutoflushIsRequired() {
		Session s = openSession();
		Transaction txn = s.beginTransaction();
		Publisher publisher = new Publisher();
		publisher.setName( "name" );
		s.save( publisher );
		assertTrue( "autoflush entity create", s.createQuery( "from Publisher p" ).list().size() == 1 );
		publisher.setName( "name" );
		assertTrue( "autoflush entity update", s.createQuery( "from Publisher p where p.name='name'" ).list().size() == 1 );
		txn.commit();
		s.close();

		s = openSession();
		txn = s.beginTransaction();
		publisher = (Publisher) s.get( Publisher.class, publisher.getId() );
		assertTrue( publisher.getAuthors().isEmpty() );

		final PersistenceContext persistenceContext = ( (SessionImplementor) s ).getPersistenceContext();
		final ActionQueue actionQueue = ( (SessionImpl) s ).getActionQueue();
		assertEquals( 1, persistenceContext.getCollectionEntries().size() );
		assertEquals( 1, persistenceContext.getCollectionsByKey().size() );
		assertTrue( persistenceContext.getCollectionEntries().containsKey( publisher.getAuthors() ) );
		assertTrue( persistenceContext.getCollectionsByKey().values().contains( publisher.getAuthors() ) );
		assertEquals( 0, actionQueue.numberOfCollectionRemovals() );

		Author author1 = new Author( );
		author1.setPublisher( publisher );
		publisher.getAuthors().add( author1 );
		assertTrue(
				"autoflush collection update",
				s.createQuery( "select a from Publisher p join p.authors a" ).list().size() == 1
		);
		assertEquals( 2, persistenceContext.getCollectionEntries().size() );
		assertEquals( 2, persistenceContext.getCollectionsByKey().size() );
		assertTrue( persistenceContext.getCollectionEntries().containsKey( publisher.getAuthors() ) );
		assertTrue( persistenceContext.getCollectionEntries().containsKey( author1.getBooks() ) );
		assertTrue( persistenceContext.getCollectionsByKey().values().contains( publisher.getAuthors() ) );
		assertTrue( persistenceContext.getCollectionsByKey().values().contains( author1.getBooks() ) );
		assertEquals( 0, actionQueue.numberOfCollectionRemovals() );

		author1.setPublisher( null );
		s.delete( author1 );
		publisher.getAuthors().clear();
		assertEquals( 0, actionQueue.numberOfCollectionRemovals() );
		assertTrue( "autoflush collection update",
				s.createQuery( "select a from Publisher p join p.authors a" ).list().size() == 0
		);
		assertEquals( 1, persistenceContext.getCollectionEntries().size() );
		assertEquals( 1, persistenceContext.getCollectionsByKey().size() );
		assertTrue( persistenceContext.getCollectionEntries().containsKey( publisher.getAuthors() ) );
		assertTrue( persistenceContext.getCollectionsByKey().values().contains( publisher.getAuthors() ) );
		assertEquals( 0, actionQueue.numberOfCollectionRemovals() );

		Set<Author> authorsOld = publisher.getAuthors();
		publisher.setAuthors( new HashSet<Author>() );
		Author author2 = new Author( );
		author2.setName( "author2" );
		author2.setPublisher( publisher );
		publisher.getAuthors().add( author2 );
		List results = s.createQuery( "select a from Publisher p join p.authors a" ).list();
		assertEquals( 1, results.size() );
		assertEquals( 2, persistenceContext.getCollectionEntries().size() );
		assertEquals( 2, persistenceContext.getCollectionsByKey().size() );
		assertTrue( persistenceContext.getCollectionEntries().containsKey( publisher.getAuthors() ) );
		assertTrue( persistenceContext.getCollectionEntries().containsKey( author2.getBooks() ) );
		assertTrue( persistenceContext.getCollectionsByKey().values().contains( publisher.getAuthors() ) );
		assertTrue( persistenceContext.getCollectionsByKey().values().contains( author2.getBooks() ) );
		assertEquals( 0, actionQueue.numberOfCollectionRemovals() );

		s.delete(publisher);
		assertTrue( "autoflush delete", s.createQuery( "from Publisher p" ).list().size()==0 );
		txn.commit();
		s.close();
	}

	@Test
	public void testAutoflushIsNotRequiredWithUnrelatedCollectionChange() {
		Session s = openSession();
		Transaction txn = s.beginTransaction();
		Publisher publisher = new Publisher();
		publisher.setName( "name" );
		s.save( publisher );
		assertTrue( "autoflush entity create", s.createQuery( "from Publisher p" ).list().size() == 1 );
		publisher.setName( "name" );
		assertTrue( "autoflush entity update", s.createQuery( "from Publisher p where p.name='name'" ).list().size() == 1 );
		UnrelatedEntity unrelatedEntity = new UnrelatedEntity( );
		s.save( unrelatedEntity );
		txn.commit();
		s.close();

		s = openSession();
		txn = s.beginTransaction();
		unrelatedEntity = (UnrelatedEntity) s.get( UnrelatedEntity.class, unrelatedEntity.getId() );
		publisher = (Publisher) s.get( Publisher.class, publisher.getId() );
		assertTrue( publisher.getAuthors().isEmpty() );

		final PersistenceContext persistenceContext = ( (SessionImplementor) s ).getPersistenceContext();
		final ActionQueue actionQueue = ( (SessionImpl) s ).getActionQueue();
		assertEquals( 1, persistenceContext.getCollectionEntries().size() );
		assertEquals( 1, persistenceContext.getCollectionsByKey().size() );
		assertTrue( persistenceContext.getCollectionEntries().containsKey( publisher.getAuthors() ) );
		assertTrue( persistenceContext.getCollectionsByKey().values().contains( publisher.getAuthors() ) );
		assertEquals( 0, actionQueue.numberOfCollectionRemovals() );

		Author author1 = new Author( );
		author1.setPublisher( publisher );
		publisher.getAuthors().add( author1 );
		assertTrue(	s.createQuery( "from UnrelatedEntity" ).list().size() == 1 );
		assertEquals( 2, persistenceContext.getCollectionEntries().size() );
		assertEquals( 1, persistenceContext.getCollectionsByKey().size() );
		assertTrue( persistenceContext.getCollectionEntries().containsKey( publisher.getAuthors() ) );
		assertTrue( persistenceContext.getCollectionEntries().containsKey( author1.getBooks() ) );
		assertTrue( persistenceContext.getCollectionsByKey().values().contains( publisher.getAuthors() ) );
		assertEquals( 0, actionQueue.numberOfCollectionRemovals() );

		author1.setPublisher( null );
		s.delete( author1 );
		publisher.getAuthors().clear();
		assertEquals( 0, actionQueue.numberOfCollectionRemovals() );
		assertTrue( s.createQuery( "from UnrelatedEntity" ).list().size() == 1 );
		assertEquals( 2, persistenceContext.getCollectionEntries().size() );
		assertEquals( 1, persistenceContext.getCollectionsByKey().size() );
		assertTrue( persistenceContext.getCollectionEntries().containsKey( publisher.getAuthors() ) );
		assertTrue( persistenceContext.getCollectionEntries().containsKey( author1.getBooks() ) );
		assertTrue( persistenceContext.getCollectionsByKey().values().contains( publisher.getAuthors() ) );
		assertEquals( 0, actionQueue.numberOfCollectionRemovals() );

		Set<Author> authorsOld = publisher.getAuthors();
		publisher.setAuthors( new HashSet<Author>() );
		Author author2 = new Author( );
		author2.setName( "author2" );
		author2.setPublisher( publisher );
		publisher.getAuthors().add( author2 );
		List results = s.createQuery( "from UnrelatedEntity" ).list();
		assertEquals( 1, results.size() );
		assertEquals( 4, persistenceContext.getCollectionEntries().size() );
		assertEquals( 1, persistenceContext.getCollectionsByKey().size() );
		assertTrue( persistenceContext.getCollectionEntries().containsKey( publisher.getAuthors() ) );
		assertTrue( persistenceContext.getCollectionEntries().containsKey( author2.getBooks() ) );
		assertTrue( persistenceContext.getCollectionEntries().containsKey( authorsOld ) );
		assertTrue( persistenceContext.getCollectionEntries().containsKey( author1.getBooks() ) );
		assertTrue( persistenceContext.getCollectionsByKey().values().contains( authorsOld ) );
		assertEquals( 0, actionQueue.numberOfCollectionRemovals() );

		s.flush();
		assertEquals( 2, persistenceContext.getCollectionEntries().size() );
		assertEquals( 2, persistenceContext.getCollectionsByKey().size() );
		assertTrue( persistenceContext.getCollectionEntries().containsKey( publisher.getAuthors() ) );
		assertTrue( persistenceContext.getCollectionEntries().containsKey( author2.getBooks() ) );
		assertTrue( persistenceContext.getCollectionsByKey().values().contains( publisher.getAuthors() ) );
		assertTrue( persistenceContext.getCollectionsByKey().values().contains( author2.getBooks() ) );
		assertEquals( 0, actionQueue.numberOfCollectionRemovals() );

		s.delete(publisher);
		assertTrue( "autoflush delete", s.createQuery( "from UnrelatedEntity" ).list().size()==1 );
		s.delete( unrelatedEntity );
		txn.commit();
		s.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Author.class, Book.class, Publisher.class, UnrelatedEntity.class };
	}

	@Override
	protected void prepareBootstrapRegistryBuilder(BootstrapServiceRegistryBuilder builder) {
		super.prepareBootstrapRegistryBuilder( builder );
		builder.applyIntegrator(
				new Integrator() {
					@Override
					public void integrate(
							Metadata metadata,
							SessionFactoryImplementor sessionFactory,
							SessionFactoryServiceRegistry serviceRegistry) {
						integrate( serviceRegistry );
					}

					private void integrate(SessionFactoryServiceRegistry serviceRegistry) {
						serviceRegistry.getService( EventListenerRegistry.class )
								.getEventListenerGroup( EventType.PRE_UPDATE )
								.appendListener( InitializingPreUpdateEventListener.INSTANCE );
					}

					@Override
					public void disintegrate(
							SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
					}
				}
		);
	}

	public static class InitializingPreUpdateEventListener implements PreUpdateEventListener {
		public static final InitializingPreUpdateEventListener INSTANCE = new InitializingPreUpdateEventListener();

		private boolean executed = false;
		private boolean foundAny = false;

		@Override
		public boolean onPreUpdate(PreUpdateEvent event) {
			executed = true;

			final Object[] oldValues = event.getOldState();
			final String[] properties = event.getPersister().getPropertyNames();

			// Iterate through all fields of the updated object
			for ( int i = 0; i < properties.length; i++ ) {
				if ( oldValues != null && oldValues[i] != null ) {
					if ( ! Hibernate.isInitialized( oldValues[i] ) ) {
						// force any proxies and/or collections to initialize to illustrate HHH-2763
						foundAny = true;
						Hibernate.initialize( oldValues );
					}
				}
			}
			return true;
		}
	}
}
