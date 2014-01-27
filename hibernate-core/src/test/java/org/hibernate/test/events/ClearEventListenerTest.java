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
package org.hibernate.test.events;

import org.hibernate.Session;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.ClearEvent;
import org.hibernate.event.spi.ClearEventListener;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class ClearEventListenerTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testExplicitClear() {
		listener.callCount = 0;

		Session s = openSession();
		s.clear();
		assertEquals( 1, listener.callCount );
		s.close();
		assertEquals( 1, listener.callCount );
	}

	@Test
	public void testAutoClear() {
		listener.callCount = 0;

		Session s = openSession();
		( (SessionImplementor) s ).setAutoClear( true );
		s.beginTransaction();
		assertEquals( 0, listener.callCount );
		s.getTransaction().commit();
		assertEquals( 1, listener.callCount );
		s.close();
		assertEquals( 1, listener.callCount );
	}

	private TheListener listener = new TheListener();

	private static class TheListener implements ClearEventListener {
		private int callCount;

		@Override
		public void onClear(ClearEvent event) {
			callCount++;
		}
	}

	@Override
	protected void prepareBootstrapRegistryBuilder(BootstrapServiceRegistryBuilder builder) {
		super.prepareBootstrapRegistryBuilder( builder );
		builder.with(
				new Integrator() {

					@Override
					public void integrate(
							Configuration configuration,
							SessionFactoryImplementor sessionFactory,
							SessionFactoryServiceRegistry serviceRegistry) {
						integrate(serviceRegistry);
					}

					@Override
					public void integrate( MetadataImplementor metadata,
										   SessionFactoryImplementor sessionFactory,
										   SessionFactoryServiceRegistry serviceRegistry ) {
						integrate(serviceRegistry);
					}

					private void integrate( SessionFactoryServiceRegistry serviceRegistry ) {
						serviceRegistry.getService( EventListenerRegistry.class ).setListeners(
								EventType.CLEAR,
								listener
						);
					}

					@Override
					public void disintegrate(
							SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
					}
				}
		);
	}
}
