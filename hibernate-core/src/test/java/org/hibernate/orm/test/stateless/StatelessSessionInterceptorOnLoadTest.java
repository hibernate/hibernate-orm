package org.hibernate.orm.test.stateless;

import java.util.List;

import org.hibernate.Interceptor;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.type.Type;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test for HHH-20876: StatelessSession should call Interceptor#onLoad
 *
 * @author Chikumar
 */
@DomainModel(
		annotatedClasses = Document.class
)
@SessionFactory
public class StatelessSessionInterceptorOnLoadTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope){
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testOnLoadCalledWhenEntityLoaded(SessionFactoryScope scope) {
		final OnLoadTrackingInterceptor interceptor = new OnLoadTrackingInterceptor();

		// First, insert a document using a stateless session without interceptor
		scope.inStatelessTransaction(
				statelessSession -> {
					Document doc = new Document( "test content", "TestDoc" );
					statelessSession.insert( doc );
				}
		);

		// Now load it with an interceptor that tracks onLoad calls
		try (StatelessSession statelessSession = (StatelessSession) scope.getSessionFactory()
				.withStatelessOptions()
				.interceptor( interceptor )
				.open()) {

			Transaction tx = statelessSession.beginTransaction();

			// Load the document - this should trigger onLoad
			Document doc = (Document) statelessSession.get( Document.class.getName(), "TestDoc" );

			assertNotNull( doc, "Document should be loaded" );
			assertTrue( interceptor.onLoadCalled,
					"Interceptor.onLoad() should have been called when loading entity" );

			tx.commit();
		}
	}

	@Test
	public void testOnLoadCalledWithRefresh(SessionFactoryScope scope) {
		final OnLoadTrackingInterceptor interceptor = new OnLoadTrackingInterceptor();

		// Insert and then refresh
		try (StatelessSession statelessSession = (StatelessSession) scope.getSessionFactory()
				.withStatelessOptions()
				.interceptor( interceptor )
				.open()) {

			Transaction tx = statelessSession.beginTransaction();

			Document doc = new Document( "test content", "TestDoc2" );
			statelessSession.insert( doc );

			// Reset the flag
			interceptor.onLoadCalled = false;

			// Refresh should also trigger onLoad
			statelessSession.refresh( doc );

			assertTrue( interceptor.onLoadCalled,
					"Interceptor.onLoad() should have been called when refreshing entity" );

			tx.commit();
		}
	}

	@Test
	public void testOnLoadCalledWithGetMultiple(SessionFactoryScope scope) {
		final OnLoadCountingInterceptor interceptor = new OnLoadCountingInterceptor();

		// First, insert multiple documents using a stateless session without interceptor
		scope.inStatelessTransaction(
				statelessSession -> {
					statelessSession.insert( new Document( "content1", "Doc1" ) );
					statelessSession.insert( new Document( "content2", "Doc2" ) );
					statelessSession.insert( new Document( "content3", "Doc3" ) );
				}
		);

		// Now load them with an interceptor that tracks onLoad calls
		try (StatelessSession statelessSession = (StatelessSession) scope.getSessionFactory()
				.withStatelessOptions()
				.interceptor( interceptor )
				.open()) {

			Transaction tx = statelessSession.beginTransaction();

			// Load multiple documents - this should trigger onLoad for each
			var docs = statelessSession.getMultiple( Document.class, List.of( "Doc1", "Doc2", "Doc3" ) );

			assertNotNull( docs, "Documents should be loaded" );
			assertTrue( interceptor.onLoadCallCount == 3,
					"Interceptor.onLoad() should have been called 3 times when loading multiple entities, but was called "
					+ interceptor.onLoadCallCount + " times" );

			tx.commit();
		}
	}

	/**
	 * Simple interceptor that tracks whether onLoad was called
	 */
	private static class OnLoadTrackingInterceptor implements Interceptor {
		boolean onLoadCalled = false;

		@Override
		public boolean onLoad(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
			onLoadCalled = true;
			return false;
		}
	}

	/**
	 * Interceptor that counts how many times onLoad was called
	 */
	private static class OnLoadCountingInterceptor implements Interceptor {
		int onLoadCallCount = 0;

		@Override
		public boolean onLoad(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
			onLoadCallCount++;
			return false;
		}
	}
}
