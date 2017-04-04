/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.events;

import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.ClearEvent;
import org.hibernate.event.spi.ClearEventListener;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

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
