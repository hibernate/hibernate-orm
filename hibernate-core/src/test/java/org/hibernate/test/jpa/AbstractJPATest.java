/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.jpa;

import java.io.Serializable;
import java.util.IdentityHashMap;
import javax.persistence.EntityNotFoundException;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.CascadingAction;
import org.hibernate.engine.spi.CascadingActions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.internal.DefaultAutoFlushEventListener;
import org.hibernate.event.internal.DefaultFlushEntityEventListener;
import org.hibernate.event.internal.DefaultFlushEventListener;
import org.hibernate.event.internal.DefaultPersistEventListener;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.AutoFlushEventListener;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.FlushEntityEventListener;
import org.hibernate.event.spi.FlushEventListener;
import org.hibernate.event.spi.PersistEventListener;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * An abstract test for all JPA spec related tests.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractJPATest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "jpa/Part.hbm.xml", "jpa/Item.hbm.xml", "jpa/MyEntity.hbm.xml" };
	}

	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.JPAQL_STRICT_COMPLIANCE, "true" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false" );
		cfg.setEntityNotFoundDelegate( new JPAEntityNotFoundDelegate() );
	}

	@Override
	protected void prepareBootstrapRegistryBuilder(BootstrapServiceRegistryBuilder builder) {
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
						EventListenerRegistry eventListenerRegistry = serviceRegistry.getService( EventListenerRegistry.class );
						eventListenerRegistry.setListeners( EventType.PERSIST, buildPersistEventListeners() );
						eventListenerRegistry.setListeners(
								EventType.PERSIST_ONFLUSH, buildPersisOnFlushEventListeners()
						);
						eventListenerRegistry.setListeners( EventType.AUTO_FLUSH, buildAutoFlushEventListeners() );
						eventListenerRegistry.setListeners( EventType.FLUSH, buildFlushEventListeners() );
						eventListenerRegistry.setListeners( EventType.FLUSH_ENTITY, buildFlushEntityEventListeners() );
					}

					@Override
					public void disintegrate(
							SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
					}
				}
		);
	}

	// mimic specific exception aspects of the JPA environment ~~~~~~~~~~~~~~~~

	private static class JPAEntityNotFoundDelegate implements EntityNotFoundDelegate {
		public void handleEntityNotFound(String entityName, Serializable id) {
			throw new EntityNotFoundException("Unable to find " + entityName  + " with id " + id);
		}
	}

	// mimic specific event aspects of the JPA environment ~~~~~~~~~~~~~~~~~~~~

	protected PersistEventListener[] buildPersistEventListeners() {
		return new PersistEventListener[] { new JPAPersistEventListener() };
	}

	protected PersistEventListener[] buildPersisOnFlushEventListeners() {
		return new PersistEventListener[] { new JPAPersistOnFlushEventListener() };
	}

	protected AutoFlushEventListener[] buildAutoFlushEventListeners() {
		return new AutoFlushEventListener[] { JPAAutoFlushEventListener.INSTANCE };
	}

	protected FlushEventListener[] buildFlushEventListeners() {
		return new FlushEventListener[] { JPAFlushEventListener.INSTANCE };
	}

	protected FlushEntityEventListener[] buildFlushEntityEventListeners() {
		return new FlushEntityEventListener[] { new JPAFlushEntityEventListener() };
	}

	public static class JPAPersistEventListener extends DefaultPersistEventListener {
		// overridden in JPA impl for entity callbacks...
	}

	public static class JPAPersistOnFlushEventListener extends JPAPersistEventListener {
		@Override
        protected CascadingAction getCascadeAction() {
			return CascadingActions.PERSIST_ON_FLUSH;
		}
	}

	public static class JPAAutoFlushEventListener extends DefaultAutoFlushEventListener {
		// not sure why EM code has this ...
		public static final AutoFlushEventListener INSTANCE = new JPAAutoFlushEventListener();

		@Override
        protected CascadingAction getCascadingAction() {
			return CascadingActions.PERSIST_ON_FLUSH;
		}

		@Override
        protected Object getAnything() {
			return new IdentityHashMap( 10 );
		}
	}

	public static class JPAFlushEventListener extends DefaultFlushEventListener {
		// not sure why EM code has this ...
		public static final FlushEventListener INSTANCE = new JPAFlushEventListener();

		@Override
        protected CascadingAction getCascadingAction() {
			return CascadingActions.PERSIST_ON_FLUSH;
		}

		@Override
        protected Object getAnything() {
			return new IdentityHashMap( 10 );
		}
	}

	public static class JPAFlushEntityEventListener extends DefaultFlushEntityEventListener {
		// in JPA, used mainly for preUpdate callbacks...
	}
}
