/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.flush;

import org.hibernate.Hibernate;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.ActionQueue;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistryFunctionalTesting;
import org.hibernate.testing.orm.junit.ServiceRegistryProducer;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Gail Badner
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey("HHH-6960")
@ServiceRegistryFunctionalTesting
@DomainModel(annotatedClasses = { Author.class, Book.class, Publisher.class, UnrelatedEntity.class })
@SessionFactory
public class TestAutoFlushBeforeQueryExecution implements ServiceRegistryProducer {

	@Test
	public void testAutoflushIsRequired(SessionFactoryScope factoryScope) {
		final var publisherId = factoryScope.fromTransaction( (session) -> {
			Publisher publisher = new Publisher();
			publisher.setName( "name" );
			session.persist( publisher );
			Assertions.assertEquals( 1, session.createQuery( "from Publisher p", Publisher.class ).list().size(),
					"autoflush entity create" );
			publisher.setName( "name" );
			Assertions.assertEquals( 1,
					session.createQuery( "from Publisher p where p.name='name'", Publisher.class ).list().size(),
					"autoflush entity update" );
			return publisher.getId();
		} );

		factoryScope.inTransaction( (session) -> {
			var publisher = session.find( Publisher.class, publisherId );
			Assertions.assertTrue( publisher.getAuthors().isEmpty() );

			final PersistenceContext persistenceContext = session.getPersistenceContext();
			final ActionQueue actionQueue = session.getActionQueue();
			Assertions.assertEquals( 1, persistenceContext.getCollectionEntriesSize() );
			Assertions.assertEquals( 1, persistenceContext.getCollectionsByKey().size() );
			Assertions.assertTrue( persistenceContext.getCollectionEntries().containsKey( publisher.getAuthors() ) );
			Assertions.assertTrue(
					persistenceContext.getCollectionsByKey().values().contains( publisher.getAuthors() ) );
			Assertions.assertEquals( 0, actionQueue.numberOfCollectionRemovals() );

			Author author1 = new Author( );
			author1.setPublisher( publisher );
			publisher.getAuthors().add( author1 );
			Assertions.assertTrue(
					session.createQuery( "select a from Publisher p join p.authors a" ).list().size() == 1,
					"autoflush collection update" );
			Assertions.assertEquals( 2, persistenceContext.getCollectionEntriesSize() );
			Assertions.assertEquals( 2, persistenceContext.getCollectionsByKey().size() );
			Assertions.assertTrue( persistenceContext.getCollectionEntries().containsKey( publisher.getAuthors() ) );
			Assertions.assertTrue( persistenceContext.getCollectionEntries().containsKey( author1.getBooks() ) );
			Assertions.assertTrue(
					persistenceContext.getCollectionsByKey().values().contains( publisher.getAuthors() ) );
			Assertions.assertTrue( persistenceContext.getCollectionsByKey().values().contains( author1.getBooks() ) );
			Assertions.assertEquals( 0, actionQueue.numberOfCollectionRemovals() );

			author1.setPublisher( null );
			session.remove( author1 );
			publisher.getAuthors().clear();
			Assertions.assertEquals( 0, actionQueue.numberOfCollectionRemovals() );
			Assertions.assertEquals( 0,
					session.createQuery( "select a from Publisher p join p.authors a" ).list().size(),
					"autoflush collection update" );
			Assertions.assertEquals( 1, persistenceContext.getCollectionEntriesSize() );
			Assertions.assertEquals( 1, persistenceContext.getCollectionsByKey().size() );
			Assertions.assertTrue( persistenceContext.getCollectionEntries().containsKey( publisher.getAuthors() ) );
			Assertions.assertTrue(
					persistenceContext.getCollectionsByKey().values().contains( publisher.getAuthors() ) );
			Assertions.assertEquals( 0, actionQueue.numberOfCollectionRemovals() );

			Set<Author> authorsOld = publisher.getAuthors();
			publisher.setAuthors( new HashSet<Author>() );
			Author author2 = new Author( );
			author2.setName( "author2" );
			author2.setPublisher( publisher );
			publisher.getAuthors().add( author2 );
			List<Publisher> results = session.createQuery( "select a from Publisher p join p.authors a" ).list();
			Assertions.assertEquals( 1, results.size() );
			Assertions.assertEquals( 2, persistenceContext.getCollectionEntriesSize() );
			Assertions.assertEquals( 2, persistenceContext.getCollectionsByKey().size() );
			Assertions.assertTrue( persistenceContext.getCollectionEntries().containsKey( publisher.getAuthors() ) );
			Assertions.assertTrue( persistenceContext.getCollectionEntries().containsKey( author2.getBooks() ) );
			Assertions.assertTrue(
					persistenceContext.getCollectionsByKey().values().contains( publisher.getAuthors() ) );
			Assertions.assertTrue( persistenceContext.getCollectionsByKey().values().contains( author2.getBooks() ) );
			Assertions.assertEquals( 0, actionQueue.numberOfCollectionRemovals() );

			session.remove(publisher);
			Assertions.assertEquals( 0, session.createQuery( "from Publisher p" ).list().size(), "autoflush delete" );
		} );
	}

	@Test
	public void testAutoflushIsNotRequiredWithUnrelatedCollectionChange(SessionFactoryScope factoryScope) {
		record Ids (Long publisherId, Long unrelatedEntityId ) {
		}

		final Ids ids = factoryScope.fromTransaction( (session) -> {
			Publisher publisher = new Publisher();
			publisher.setName( "name" );
			session.persist( publisher );
			Assertions.assertEquals( 1, session.createQuery( "from Publisher p" ).list().size(),
					"autoflush entity create" );
			publisher.setName( "name" );
			Assertions.assertEquals( 1, session.createQuery( "from Publisher p where p.name='name'" ).list().size(),
					"autoflush entity update" );
			UnrelatedEntity unrelatedEntity = new UnrelatedEntity( );
			session.persist( unrelatedEntity );
			session.flush();

			return new Ids( publisher.getId(), unrelatedEntity.getId() );
		} );

		factoryScope.inTransaction( (s) -> {
			var unrelatedEntity = s.find( UnrelatedEntity.class, ids.unrelatedEntityId );
			var publisher = s.find( Publisher.class, ids.publisherId );
			Assertions.assertTrue( publisher.getAuthors().isEmpty() );

			final PersistenceContext persistenceContext = s.getPersistenceContext();
			final ActionQueue actionQueue = s.getActionQueue();
			Assertions.assertEquals( 1, persistenceContext.getCollectionEntriesSize() );
			Assertions.assertEquals( 1, persistenceContext.getCollectionsByKey().size() );
			Assertions.assertTrue( persistenceContext.getCollectionEntries().containsKey( publisher.getAuthors() ) );
			Assertions.assertTrue(
					persistenceContext.getCollectionsByKey().values().contains( publisher.getAuthors() ) );
			Assertions.assertEquals( 0, actionQueue.numberOfCollectionRemovals() );

			Author author1 = new Author( );
			author1.setPublisher( publisher );
			publisher.getAuthors().add( author1 );
			Assertions.assertEquals( 1, s.createQuery( "from UnrelatedEntity" ).list().size() );
			Assertions.assertEquals( 2, persistenceContext.getCollectionEntriesSize() );
			Assertions.assertEquals( 1, persistenceContext.getCollectionsByKey().size() );
			Assertions.assertTrue( persistenceContext.getCollectionEntries().containsKey( publisher.getAuthors() ) );
			Assertions.assertTrue( persistenceContext.getCollectionEntries().containsKey( author1.getBooks() ) );
			Assertions.assertTrue(
					persistenceContext.getCollectionsByKey().values().contains( publisher.getAuthors() ) );
			Assertions.assertEquals( 0, actionQueue.numberOfCollectionRemovals() );

			author1.setPublisher( null );
			s.remove( author1 );
			publisher.getAuthors().clear();
			Assertions.assertEquals( 0, actionQueue.numberOfCollectionRemovals() );
			Assertions.assertEquals( 1, s.createQuery( "from UnrelatedEntity" ).list().size() );
			Assertions.assertEquals( 2, persistenceContext.getCollectionEntriesSize() );
			Assertions.assertEquals( 1, persistenceContext.getCollectionsByKey().size() );
			Assertions.assertTrue( persistenceContext.getCollectionEntries().containsKey( publisher.getAuthors() ) );
			Assertions.assertTrue( persistenceContext.getCollectionEntries().containsKey( author1.getBooks() ) );
			Assertions.assertTrue(
					persistenceContext.getCollectionsByKey().values().contains( publisher.getAuthors() ) );
			Assertions.assertEquals( 0, actionQueue.numberOfCollectionRemovals() );

			Set<Author> authorsOld = publisher.getAuthors();
			publisher.setAuthors( new HashSet<Author>() );
			Author author2 = new Author( );
			author2.setName( "author2" );
			author2.setPublisher( publisher );
			publisher.getAuthors().add( author2 );
			List<UnrelatedEntity> results = s.createQuery( "from UnrelatedEntity" ).list();
			Assertions.assertEquals( 1, results.size() );
			Assertions.assertEquals( 4, persistenceContext.getCollectionEntriesSize() );
			Assertions.assertEquals( 1, persistenceContext.getCollectionsByKey().size() );
			Assertions.assertTrue( persistenceContext.getCollectionEntries().containsKey( publisher.getAuthors() ) );
			Assertions.assertTrue( persistenceContext.getCollectionEntries().containsKey( author2.getBooks() ) );
			Assertions.assertTrue( persistenceContext.getCollectionEntries().containsKey( authorsOld ) );
			Assertions.assertTrue( persistenceContext.getCollectionEntries().containsKey( author1.getBooks() ) );
			Assertions.assertTrue( persistenceContext.getCollectionsByKey().values().contains( authorsOld ) );
			Assertions.assertEquals( 0, actionQueue.numberOfCollectionRemovals() );

			s.flush();
			Assertions.assertEquals( 2, persistenceContext.getCollectionEntriesSize() );
			Assertions.assertEquals( 2, persistenceContext.getCollectionsByKey().size() );
			Assertions.assertTrue( persistenceContext.getCollectionEntries().containsKey( publisher.getAuthors() ) );
			Assertions.assertTrue( persistenceContext.getCollectionEntries().containsKey( author2.getBooks() ) );
			Assertions.assertTrue(
					persistenceContext.getCollectionsByKey().values().contains( publisher.getAuthors() ) );
			Assertions.assertTrue( persistenceContext.getCollectionsByKey().values().contains( author2.getBooks() ) );
			Assertions.assertEquals( 0, actionQueue.numberOfCollectionRemovals() );

			s.remove(publisher);
			Assertions.assertEquals( 1, s.createQuery( "from UnrelatedEntity" ).list().size(), "autoflush delete" );
			s.remove( unrelatedEntity );
		} );
	}

	@Override
	public void prepareBootstrapRegistryBuilder(BootstrapServiceRegistryBuilder builder) {
		builder.applyIntegrator(
				new Integrator() {
					@Override
					public void integrate(
							Metadata metadata,
							BootstrapContext bootstrapContext,
							SessionFactoryImplementor sessionFactory) {
						integrate( sessionFactory );
					}

					private void integrate(SessionFactoryImplementor sessionFactory) {
						sessionFactory.getEventListenerRegistry()
								.getEventListenerGroup( EventType.PRE_UPDATE )
								.appendListener( InitializingPreUpdateEventListener.INSTANCE );
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
			// Don't veto updates
			return false;
		}
	}
}
