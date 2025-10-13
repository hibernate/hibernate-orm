/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.events;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.DeleteContext;
import org.hibernate.event.spi.DeleteEvent;
import org.hibernate.event.spi.DeleteEventListener;
import org.hibernate.event.spi.EventType;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.JiraKeyGroup;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * CallbackTest implementation
 *
 * @author Steve Ebersole
 */
@JiraKeyGroup(value = {
		@JiraKey(value = "HHH-2884"),
		@JiraKey(value = "HHH-10674"),
		@JiraKey(value = "HHH-14541")
})
@org.hibernate.testing.orm.junit.SessionFactory(sessionFactoryConfigurer = CallbackTest.Configurer.class)
public class CallbackTest {
	private static TestingObserver observer = new TestingObserver();
	private static TestingListener listener = new TestingListener();

	@Test
	public void testCallbacks(SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		sessionFactory.getEventListenerRegistry().setListeners(
				EventType.DELETE,
				listener
		);
		listener.initialize();
		// test pre-assertions
		assert observer.closingCount == 0;
		assert observer.closedCount == 0;
		assertEquals( 1, observer.creationCount, "observer not notified of creation" );
		assertEquals( 1, listener.initCount, "listener not notified of creation" );

		sessionFactory.close();

		assertEquals( 1, observer.closingCount, "observer not notified of closing" );
		assertEquals( 1, observer.closedCount, "observer not notified of close" );
		assertEquals( 1, listener.destoryCount, "listener not notified of close" );
	}

	private static class TestingObserver implements SessionFactoryObserver {
		private int creationCount = 0;
		private int closedCount = 0;
		private int closingCount = 0;

		@Override
		public void sessionFactoryCreated(SessionFactory factory) {
			assertThat( factory.isClosed() ).isFalse();
			creationCount++;
		}

		@Override
		public void sessionFactoryClosing(SessionFactory factory) {
			// Test for HHH-14541
			assertThat( factory.isClosed() ).isFalse();
			closingCount++;
		}

		@Override
		public void sessionFactoryClosed(SessionFactory factory) {
			assertThat( factory.isClosed() ).isTrue();
			closedCount++;
			listener.cleanup();
		}
	}

	private static class TestingListener implements DeleteEventListener {
		private int initCount = 0;
		private int destoryCount = 0;

		public void initialize() {
			initCount++;
		}

		public void cleanup() {
			destoryCount++;
		}

		public void onDelete(DeleteEvent event) throws HibernateException {
		}

		public void onDelete(DeleteEvent event, DeleteContext transientEntities) throws HibernateException {
		}
	}

	public static class Configurer implements Consumer<SessionFactoryBuilder> {
		@Override
		public void accept(SessionFactoryBuilder sessionFactoryBuilder) {
			sessionFactoryBuilder.addSessionFactoryObservers( observer );
		}
	}
}
