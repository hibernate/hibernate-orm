/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.events;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.AutoFlushEvent;
import org.hibernate.event.spi.AutoFlushEventListener;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AutoFlushEventListenerTest extends BaseCoreFunctionalTestCase {

	private TheListener listener = new TheListener();

	@Test
	public void testAutoFlushRequired() {
		listener.events.clear();

		Session s = openSession();
		s.beginTransaction();

		s.persist( new Entity1() );
		assertEquals( 0, listener.events.size() );

		// An entity of this type was persisted; a flush is required
		session.createQuery( "select i from Entity1 i" )
				.setHibernateFlushMode( FlushMode.AUTO )
				.getResultList();
		assertEquals( 1, listener.events.size() );
		assertTrue( listener.events.get( 0 ).isFlushRequired() );

		s.getTransaction().commit();
		assertEquals( 1, listener.events.size() );
		s.close();
		assertEquals( 1, listener.events.size() );
	}

	@Test
	public void testAutoFlushNotRequired() {
		listener.events.clear();

		Session s = openSession();
		s.beginTransaction();

		s.persist( new Entity2() );
		assertEquals( 0, listener.events.size() );

		// No entity of this type was persisted; no flush is required
		session.createQuery( "select i from Entity1 i" )
				.setHibernateFlushMode( FlushMode.AUTO )
				.getResultList();
		assertEquals( 1, listener.events.size() );
		assertFalse( listener.events.get( 0 ).isFlushRequired() );

		s.getTransaction().commit();
		assertEquals( 1, listener.events.size() );
		s.close();
		assertEquals( 1, listener.events.size() );
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
						serviceRegistry.getService( EventListenerRegistry.class ).appendListeners(
								EventType.AUTO_FLUSH,
								listener
						);
					}

					@Override
					public void disintegrate(SessionFactoryImplementor sessionFactory,
							SessionFactoryServiceRegistry serviceRegistry) {
					}
				}
		);
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Entity1.class, Entity2.class };
	}

	@Entity(name = "Entity1")
	static class Entity1 {
		@Id
		@GeneratedValue
		private Integer id;

		public Entity1() {
		}
	}

	@Entity(name = "Entity2")
	static class Entity2 {
		@Id
		@GeneratedValue
		private Integer id;

		public Entity2() {
		}
	}

	private static class TheListener implements AutoFlushEventListener {
		private List<AutoFlushEvent> events = new ArrayList<>();

		@Override
		public void onAutoFlush(AutoFlushEvent event) throws HibernateException {
			events.add( event );
		}
	}
}
