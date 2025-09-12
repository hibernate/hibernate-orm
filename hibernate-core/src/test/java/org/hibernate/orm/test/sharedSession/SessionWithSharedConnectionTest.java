/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sharedSession;

import org.hibernate.FlushMode;
import org.hibernate.IrrelevantEntity;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.internal.SessionImpl;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import java.lang.reflect.Field;
import java.util.Collection;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = IrrelevantEntity.class)
@SessionFactory
public class SessionWithSharedConnectionTest {
	@Test
	@JiraKey( value = "HHH-7090" )
	public void testSharedTransactionContextSessionClosing(SessionFactoryScope scope) {
		Session session = scope.getSessionFactory().openSession();
		session.getTransaction().begin();

		Session secondSession = session.sessionWithOptions()
				.connection()
				.openSession();
		CriteriaBuilder criteriaBuilder = secondSession.getCriteriaBuilder();
		CriteriaQuery<IrrelevantEntity> criteria = criteriaBuilder.createQuery( IrrelevantEntity.class );
		criteria.from( IrrelevantEntity.class );
		session.createQuery( criteria ).list();
//		secondSession.createCriteria( IrrelevantEntity.class ).list();

		//the list should have registered and then released a JDBC resource
		assertFalse( ((SessionImplementor) secondSession)
				.getJdbcCoordinator().getLogicalConnection().getResourceRegistry()
				.hasRegisteredResources() );

		assertTrue( session.isOpen() );
		assertTrue( secondSession.isOpen() );

		Assertions.assertSame( session.getTransaction(), secondSession.getTransaction() );

		session.getTransaction().commit();

		assertTrue( session.isOpen() );
		assertTrue( secondSession.isOpen() );

		secondSession.close();
		assertTrue( session.isOpen() );
		assertFalse( secondSession.isOpen() );

		session.close();
		assertFalse( session.isOpen() );
		assertFalse( secondSession.isOpen() );
	}

	@Test
	@JiraKey( value = "HHH-7090" )
	public void testSharedTransactionContextAutoClosing(SessionFactoryScope scope) {
		Session session = scope.getSessionFactory().openSession();
		session.getTransaction().begin();

		// COMMIT ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		Session secondSession = session.sessionWithOptions()
				.connection()
				.autoClose( true )
				.openSession();

		// directly assert state of the second session
		assertTrue( ((SessionImpl) secondSession).isAutoCloseSessionEnabled() );
		assertTrue( ((SessionImpl) secondSession).shouldAutoClose() );

		// now commit the transaction and make sure that does not close the sessions
		session.getTransaction().commit();
		assertFalse( ((SessionImplementor) session).isClosed() );
		assertTrue( ((SessionImplementor) secondSession).isClosed() );

		session.close();
		assertTrue( ((SessionImplementor) session).isClosed() );
		assertTrue( ((SessionImplementor) secondSession).isClosed() );


		// ROLLBACK ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		session = scope.getSessionFactory().openSession();
		session.getTransaction().begin();

		secondSession = session.sessionWithOptions()
				.connection()
				.autoClose( true )
				.openSession();

		// directly assert state of the second session
		assertTrue( ((SessionImpl) secondSession).isAutoCloseSessionEnabled() );
		assertTrue( ((SessionImpl) secondSession).shouldAutoClose() );

		// now rollback the transaction and make sure that does not close the sessions
		session.getTransaction().rollback();
		assertFalse( ((SessionImplementor) session).isClosed() );
		assertTrue( ((SessionImplementor) secondSession).isClosed() );

		session.close();
		assertTrue( ((SessionImplementor) session).isClosed() );
		assertTrue( ((SessionImplementor) secondSession).isClosed() );

	}

//	@Test
//	@JiraKey( value = "HHH-7090" )
//	public void testSharedTransactionContextAutoJoining() {
//		Session session = scope.getSessionFactory().openSession();
//		session.getTransaction().begin();
//
//		Session secondSession = session.sessionWithOptions()
//				.transactionContext()
//				.autoJoinTransactions( true )
//				.openSession();
//
//		// directly assert state of the second session
//		assertFalse( ((SessionImplementor) secondSession).shouldAutoJoinTransaction() );
//
//		secondSession.close();
//		session.close();
//	}

	@Test
	@JiraKey( value = "HHH-7090" )
	public void testSharedTransactionContextFlushBeforeCompletion(SessionFactoryScope scope) {
		Session session = scope.getSessionFactory().openSession();
		session.getTransaction().begin();

		Session secondSession = session.sessionWithOptions()
				.connection()
//				.flushBeforeCompletion( true )
				.autoClose( true )
				.openSession();

		// directly assert state of the second session
//		assertTrue( ((SessionImplementor) secondSession).isFlushBeforeCompletionEnabled() );

		// now try it out
		IrrelevantEntity irrelevantEntity = new IrrelevantEntity();
		secondSession.persist( irrelevantEntity );
		Integer id = irrelevantEntity.getId();
		session.getTransaction().commit();
		assertFalse( ((SessionImplementor) session).isClosed() );
		assertTrue( ((SessionImplementor) secondSession).isClosed() );

		session.close();
		assertTrue( ((SessionImplementor) session).isClosed() );
		assertTrue( ((SessionImplementor) secondSession).isClosed() );

		session = scope.getSessionFactory().openSession();
		session.getTransaction().begin();
		IrrelevantEntity it = session.byId( IrrelevantEntity.class ).load( id );
		assertNotNull( it );
		session.remove( it );
		session.getTransaction().commit();
		session.close();
	}

	@Test
	@JiraKey( value = "HHH-7239" )
	public void testChildSessionCallsAfterTransactionAction(SessionFactoryScope scope) throws Exception {
		Session session = scope.getSessionFactory().openSession();

		final String postCommitMessage = "post commit was called";

		EventListenerRegistry eventListenerRegistry = scope.getSessionFactory().getEventListenerRegistry();
		//register a post commit listener
		eventListenerRegistry.appendListeners(
				EventType.POST_COMMIT_INSERT,
				new PostInsertEventListener() {
					@Override
					public void onPostInsert(PostInsertEvent event) {
						((IrrelevantEntity) event.getEntity()).setName( postCommitMessage );
					}

					@Override
					public boolean requiresPostCommitHandling(EntityPersister persister) {
						return true;
					}
				}
		);

		session.getTransaction().begin();

		IrrelevantEntity irrelevantEntityMainSession = new IrrelevantEntity();
		irrelevantEntityMainSession.setName( "main session" );
		session.persist( irrelevantEntityMainSession );

		//open secondary session to also insert an entity
		Session secondSession = session.sessionWithOptions()
				.connection()
//				.flushBeforeCompletion( true )
				.autoClose( true )
				.openSession();

		IrrelevantEntity irrelevantEntitySecondarySession = new IrrelevantEntity();
		irrelevantEntitySecondarySession.setName( "secondary session" );
		secondSession.persist( irrelevantEntitySecondarySession );

		session.getTransaction().commit();

		//both entities should have their names updated to the postCommitMessage value
		assertEquals( postCommitMessage, irrelevantEntityMainSession.getName() );
		assertEquals( postCommitMessage, irrelevantEntitySecondarySession.getName() );
	}

	@Test
	@JiraKey( value = "HHH-7239" )
	public void testChildSessionTwoTransactions(SessionFactoryScope scope) {
		Session session = scope.getSessionFactory().openSession();

		session.getTransaction().begin();

		//open secondary session with managed options
		Session secondarySession = session.sessionWithOptions()
				.connection()
//				.flushBeforeCompletion( true )
				.autoClose( true )
				.openSession();

		//the secondary session should be automatically closed after the commit
		session.getTransaction().commit();

		assertFalse( secondarySession.isOpen() );

		//should be able to create a new transaction and carry on using the original session
		session.getTransaction().begin();
		session.getTransaction().commit();
	}

	@Test
	@JiraKey(value = "HHH-11830")
	public void testSharedSessionTransactionObserver(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Field field = null;
			Class<?> clazz = session.getTransactionCoordinator().getClass();
			while ( clazz != null ) {
				try {
					field = clazz.getDeclaredField( "observers" );
					field.setAccessible( true );
					break;
				}
				catch (NoSuchFieldException e) {
					clazz = clazz.getSuperclass();
				}
				catch (Exception e) {
					throw new IllegalStateException( e );
				}
			}
			assertNotNull( field, "Observers field was not found" );

			try {
				//Some of these collections could be lazily initialize: check for null before invoking size()
				final Collection<?> collection = (Collection<?>) field.get( session.getTransactionCoordinator() );
				assertTrue( collection == null || collection.isEmpty() );

				//open secondary sessions with managed options and immediately close
				Session secondarySession;
				for ( int i = 0; i < 10; i++ ) {
					secondarySession = session.sessionWithOptions()
							.connection()
							.flushMode( FlushMode.COMMIT )
							.autoClose( true )
							.openSession();

					//when the shared session is opened it should register an observer
					assertEquals( 1,
							( (Collection<?>) field.get( session.getTransactionCoordinator() ) ).size() );

					//observer should be released
					secondarySession.close();

					assertEquals( 0,
							( (Collection<?>) field.get( session.getTransactionCoordinator() ) ).size() );
				}
			}
			catch (Exception e) {
				throw new IllegalStateException( e );
			}
		} );
	}

}
