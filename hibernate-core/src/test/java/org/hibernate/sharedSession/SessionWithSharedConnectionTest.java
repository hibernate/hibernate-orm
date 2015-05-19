/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sharedSession;

import org.hibernate.IrrelevantEntity;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.persister.entity.EntityPersister;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class SessionWithSharedConnectionTest extends BaseCoreFunctionalTestCase {
	@Test
	@TestForIssue( jiraKey = "HHH-7090" )
	public void testSharedTransactionContextSessionClosing() {
		Session session = sessionFactory().openSession();
		session.getTransaction().begin();

		Session secondSession = session.sessionWithOptions()
				.transactionContext()
				.openSession();
		secondSession.createCriteria( IrrelevantEntity.class ).list();

		//the list should have registered and then released a JDBC resource
		assertFalse(
				((SessionImplementor) secondSession)
						.getJdbcCoordinator()
						.getResourceRegistry()
						.hasRegisteredResources()
		);

		assertTrue( session.isOpen() );
		assertTrue( secondSession.isOpen() );

		assertSame( session.getTransaction(), secondSession.getTransaction() );

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
	@TestForIssue( jiraKey = "HHH-7090" )
	public void testSharedTransactionContextAutoClosing() {
		Session session = sessionFactory().openSession();
		session.getTransaction().begin();

		// COMMIT ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		Session secondSession = session.sessionWithOptions()
				.transactionContext()
				.autoClose( true )
				.openSession();

		// directly assert state of the second session
		assertTrue( ((SessionImplementor) secondSession).isAutoCloseSessionEnabled() );
		assertTrue( ((SessionImplementor) secondSession).shouldAutoClose() );

		// now commit the transaction and make sure that does not close the sessions
		session.getTransaction().commit();
		assertFalse( ((SessionImplementor) session).isClosed() );
		assertTrue( ((SessionImplementor) secondSession).isClosed() );

		session.close();
		assertTrue( ((SessionImplementor) session).isClosed() );
		assertTrue( ((SessionImplementor) secondSession).isClosed() );


		// ROLLBACK ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		session = sessionFactory().openSession();
		session.getTransaction().begin();

		secondSession = session.sessionWithOptions()
				.transactionContext()
				.autoClose( true )
				.openSession();

		// directly assert state of the second session
		assertTrue( ((SessionImplementor) secondSession).isAutoCloseSessionEnabled() );
		assertTrue( ((SessionImplementor) secondSession).shouldAutoClose() );

		// now rollback the transaction and make sure that does not close the sessions
		session.getTransaction().rollback();
		assertFalse( ((SessionImplementor) session).isClosed() );
		assertTrue( ((SessionImplementor) secondSession).isClosed() );

		session.close();
		assertTrue( ((SessionImplementor) session).isClosed() );
		assertTrue( ((SessionImplementor) secondSession).isClosed() );

	}

//	@Test
//	@TestForIssue( jiraKey = "HHH-7090" )
//	public void testSharedTransactionContextAutoJoining() {
//		Session session = sessionFactory().openSession();
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
	@TestForIssue( jiraKey = "HHH-7090" )
	public void testSharedTransactionContextFlushBeforeCompletion() {
		Session session = sessionFactory().openSession();
		session.getTransaction().begin();

		Session secondSession = session.sessionWithOptions()
				.transactionContext()
				.flushBeforeCompletion( true )
				.autoClose( true )
				.openSession();

		// directly assert state of the second session
//		assertTrue( ((SessionImplementor) secondSession).isFlushBeforeCompletionEnabled() );

		// now try it out
		Integer id = (Integer) secondSession.save( new IrrelevantEntity() );
		session.getTransaction().commit();
		assertFalse( ((SessionImplementor) session).isClosed() );
		assertTrue( ((SessionImplementor) secondSession).isClosed() );

		session.close();
		assertTrue( ((SessionImplementor) session).isClosed() );
		assertTrue( ((SessionImplementor) secondSession).isClosed() );

		session = sessionFactory().openSession();
		session.getTransaction().begin();
		IrrelevantEntity it = (IrrelevantEntity) session.byId( IrrelevantEntity.class ).load( id );
		assertNotNull( it );
		session.delete( it );
		session.getTransaction().commit();
		session.close();
	}
	
	@Test
	@TestForIssue( jiraKey = "HHH-7239" )
	public void testChildSessionCallsAfterTransactionAction() throws Exception {
		Session session = openSession();

		final String postCommitMessage = "post commit was called";
		
		EventListenerRegistry eventListenerRegistry = sessionFactory().getServiceRegistry().getService(EventListenerRegistry.class);
		//register a post commit listener
		eventListenerRegistry.appendListeners(
				EventType.POST_COMMIT_INSERT,
				new PostInsertEventListener() {
					@Override
					public void onPostInsert(PostInsertEvent event) {
						((IrrelevantEntity) event.getEntity()).setName( postCommitMessage );
					}

					@Override
					public boolean requiresPostCommitHanding(EntityPersister persister) {
						return true;
					}
				}
		);
		
		session.getTransaction().begin();
		
		IrrelevantEntity irrelevantEntityMainSession = new IrrelevantEntity();
		irrelevantEntityMainSession.setName( "main session" );
		session.save( irrelevantEntityMainSession );
		
		//open secondary session to also insert an entity
		Session secondSession = session.sessionWithOptions()
				.connection()
				.flushBeforeCompletion( true )
				.autoClose( true )
				.openSession();

		IrrelevantEntity irrelevantEntitySecondarySession = new IrrelevantEntity();
		irrelevantEntitySecondarySession.setName( "secondary session" );
		secondSession.save( irrelevantEntitySecondarySession );

		session.getTransaction().commit();
		
		//both entities should have their names updated to the postCommitMessage value
		assertEquals(postCommitMessage, irrelevantEntityMainSession.getName());
		assertEquals(postCommitMessage, irrelevantEntitySecondarySession.getName());
	}

	@Test
	@TestForIssue( jiraKey = "HHH-7239" )
	public void testChildSessionTwoTransactions() throws Exception {
		Session session = openSession();
		
		session.getTransaction().begin();
		
		//open secondary session with managed options
		Session secondarySession = session.sessionWithOptions()
				.connection()
				.flushBeforeCompletion( true )
				.autoClose( true )
				.openSession();
		
		//the secondary session should be automatically closed after the commit
		session.getTransaction().commit();
		
		assertFalse( secondarySession.isOpen() );

		//should be able to create a new transaction and carry on using the original session
		session.getTransaction().begin();
		session.getTransaction().commit();
	}
	
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { IrrelevantEntity.class };
	}
}
