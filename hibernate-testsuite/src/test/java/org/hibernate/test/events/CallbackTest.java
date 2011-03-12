/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.test.events;

import java.util.Set;

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;
import org.hibernate.event.DeleteEventListener;
import org.hibernate.event.Destructible;
import org.hibernate.event.Initializable;
import org.hibernate.event.DeleteEvent;
import org.hibernate.testing.junit.functional.FunctionalTestCase;

/**
 * CallbackTest implementation
 *
 * @author Steve Ebersole
 */
public class CallbackTest extends FunctionalTestCase {
	private TestingObserver observer = new TestingObserver();
	private TestingListener listener = new TestingListener();

	public CallbackTest(String string) {
		super( string );
	}

	public String[] getMappings() {
		return new String[0];
	}

	public void configure(Configuration cfg) {
		cfg.setSessionFactoryObserver( observer );
		cfg.getEventListeners().setDeleteEventListeners( new DeleteEventListener[] { listener } );
	}

	public void testCallbacks() {
		assertTrue( "observer not notified of creation", observer.creationCount == 1 );
		assertTrue( "listener not notified of creation", listener.initCount == 1 );

		sfi().close();

		assertTrue( "observer not notified of close", observer.closedCount == 1 );
		assertTrue( "listener not notified of close", listener.destoryCount == 1 );
	}

	private static class TestingObserver implements SessionFactoryObserver {
		private int creationCount = 0;
		private int closedCount = 0;

		public void sessionFactoryCreated(SessionFactory factory) {
			creationCount++;
		}

		public void sessionFactoryClosed(SessionFactory factory) {
			closedCount++;
		}
	}

	private static class TestingListener implements DeleteEventListener, Initializable, Destructible {
		private int initCount = 0;
		private int destoryCount = 0;

		public void initialize(Configuration cfg) {
			initCount++;
		}

		public void cleanup() {
			destoryCount++;
		}

		public void onDelete(DeleteEvent event) throws HibernateException {
		}

		public void onDelete(DeleteEvent event, Set transientEntities) throws HibernateException {
		}
	}
}
