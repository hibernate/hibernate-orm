/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sharedSession;

import org.hibernate.FlushMode;
import org.hibernate.IrrelevantEntity;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = IrrelevantEntity.class)
@SessionFactory(useCollectingStatementInspector = true)
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
		var session = scope.getSessionFactory().openSession();
		session.getTransaction().begin();

		// COMMIT ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		var secondSession = (SessionImplementor) session.sessionWithOptions()
				.connection()
				.autoClose( true )
				.openSession();

		// directly assert state of the second session
		assertTrue( secondSession.isAutoCloseSessionEnabled() );
		assertTrue( ((SessionImpl) secondSession).shouldAutoClose() );

		// now commit the transaction and make sure that does not close the sessions
		session.getTransaction().commit();
		assertFalse( session.isClosed() );
		assertTrue( secondSession.isClosed() );

		session.close();
		assertTrue( session.isClosed() );
		assertTrue( secondSession.isClosed() );


		// ROLLBACK ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		session = scope.getSessionFactory().openSession();
		session.getTransaction().begin();

		secondSession = (SessionImplementor) session.sessionWithOptions()
				.connection()
				.autoClose( true )
				.openSession();

		// directly assert state of the second session
		assertTrue( secondSession.isAutoCloseSessionEnabled() );
		assertTrue( ((SessionImpl) secondSession).shouldAutoClose() );

		// now rollback the transaction and make sure that does not close the sessions
		session.getTransaction().rollback();
		assertFalse( session.isClosed() );
		assertTrue( secondSession.isClosed() );

		session.close();
		assertTrue( session.isClosed() );
		assertTrue( secondSession.isClosed() );

	}

	@Test
	@JiraKey( value = "HHH-7090" )
	public void testSharedTransactionContextFlushBeforeCompletion(SessionFactoryScope scope) {
		var session = scope.getSessionFactory().openSession();
		session.getTransaction().begin();

		var secondSession = (SessionImplementor) session.sessionWithOptions()
				.connection()
				.autoClose( true )
				.openSession();

		// now try it out
		IrrelevantEntity irrelevantEntity = new IrrelevantEntity();
		secondSession.persist( irrelevantEntity );
		Integer id = irrelevantEntity.getId();
		session.getTransaction().commit();
		assertFalse( session.isClosed() );
		assertTrue( secondSession.isClosed() );

		session.close();
		assertTrue( session.isClosed() );
		assertTrue( secondSession.isClosed() );

		session = scope.getSessionFactory().openSession();
		session.getTransaction().begin();
		IrrelevantEntity it = session.find( IrrelevantEntity.class, id );
		assertNotNull( it );
		session.remove( it );
		session.getTransaction().commit();
		session.close();
	}

	@Test
	@JiraKey( value = "HHH-7239" )
	public void testChildSessionCallsAfterTransactionAction(SessionFactoryScope scope) throws Exception {
		final var sqlCollector = scope.getCollectingStatementInspector();
		sqlCollector.clear();

		final var postCommitMessage = "post commit was called";

		var eventListenerRegistry = scope.getSessionFactory().getEventListenerRegistry();
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

		final var parentSession = scope.getSessionFactory().openSession();
		parentSession.beginTransaction();

		var mainEntity = new IrrelevantEntity();
		mainEntity.setName( "main session" );
		parentSession.persist( mainEntity );

		// open child session to also insert an entity
		var childSession = parentSession.sessionWithOptions()
				.connection()
				.autoClose( true )
				.openSession();

		var childEntity = new IrrelevantEntity();
		childEntity.setName( "secondary session" );
		childSession.persist( childEntity );

		parentSession.getTransaction().commit();

		assertThat( sqlCollector.getSqlQueries() ).hasSize( 3 );
		assertThat( sqlCollector.getSqlQueries().get( 0 ) ).startsWith( "select max(id) " );
		assertThat( sqlCollector.getSqlQueries().get( 1 ) ).startsWith( "insert" );
		assertThat( sqlCollector.getSqlQueries().get( 2 ) ).startsWith( "insert" );

		// both entities should have their names updated to the postCommitMessage value
		assertThat( mainEntity.getName() ).isEqualTo( postCommitMessage );
		assertThat( childEntity.getName() ).isEqualTo( postCommitMessage );
	}

	@Test
	@JiraKey( value = "HHH-7239" )
	public void testChildSessionTwoTransactions(SessionFactoryScope scope) {
		Session session = scope.getSessionFactory().openSession();

		session.getTransaction().begin();

		//open secondary session with managed options
		Session secondarySession = session.sessionWithOptions()
				.connection()
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
