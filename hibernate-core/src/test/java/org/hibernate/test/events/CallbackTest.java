/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.events;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.cfg.Configuration;
import org.hibernate.event.DeleteEvent;
import org.hibernate.event.DeleteEventListener;
import org.hibernate.event.Destructible;
import org.hibernate.event.EventListenerRegistration;
import org.hibernate.event.EventType;
import org.hibernate.event.Initializable;
import org.hibernate.service.StandardServiceInitiators;
import org.hibernate.service.event.spi.EventListenerRegistry;
import org.hibernate.service.internal.BasicServiceRegistryImpl;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.junit.Test;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;


/**
 * CallbackTest implementation
 *
 * @author Steve Ebersole
 */
public class CallbackTest extends BaseCoreFunctionalTestCase {
	private TestingObserver observer = new TestingObserver();
	private TestingListener listener = new TestingListener();

	public String[] getMappings() {
		return NO_MAPPINGS;
	}

	public void configure(Configuration cfg) {
		cfg.setSessionFactoryObserver( observer );
	}

	@Override
	protected void applyServices(BasicServiceRegistryImpl serviceRegistry) {
		super.applyServices( serviceRegistry );
		serviceRegistry.getService( StandardServiceInitiators.EventListenerRegistrationService.class ).attachEventListenerRegistration(
				new EventListenerRegistration() {
					@Override
					public void apply(
							ServiceRegistryImplementor serviceRegistry,
							Configuration configuration,
							Map<?, ?> configValues) {
						serviceRegistry.getService( EventListenerRegistry.class ).setListeners( EventType.DELETE, listener );
					}
				}
		);
	}

	@Test
	@FailureExpected( jiraKey = "HHH-5913", message = "Need to figure out how to initialize/destroy event listeners now")
	public void testCallbacks() {
		assertEquals( "observer not notified of creation", 1, observer.creationCount );
		assertEquals( "listener not notified of creation", 1, listener.initCount );

		sessionFactory().close();

		assertEquals( "observer not notified of close", 1, observer.closedCount );
		assertEquals( "listener not notified of close", 1, listener.destoryCount );
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
