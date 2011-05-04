/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cfg.search;

import org.jboss.logging.Logger;

import org.hibernate.AnnotationException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostCollectionRemoveEventListener;
import org.hibernate.event.spi.PostCollectionUpdateEventListener;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.cfg.Configuration;
import org.hibernate.event.spi.PostCollectionRecreateEventListener;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.event.service.spi.DuplicationStrategy;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * Integrates Hibernate Search into Hibernate Core by registering its needed listeners
 * <p/>
 * The note on the original (now removed) org.hibernate.cfg.search.HibernateSearchEventListenerRegister class indicated
 * that Search now uses a new means for this.  However that signature is relying on removed classes...
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
public class HibernateSearchIntegrator implements Integrator {
    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, HibernateSearchIntegrator.class.getName() );

	public static final String AUTO_REGISTER = "hibernate.search.autoregister_listeners";
	public static final String LISTENER_CLASS = "org.hibernate.search.event.FullTextIndexEventListener";

	@Override
	public void integrate(
			Configuration configuration,
			SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {
		final boolean registerListeners = ConfigurationHelper.getBoolean( AUTO_REGISTER, configuration.getProperties(), false );
		if ( !registerListeners ) {
			LOG.debug( "Skipping search event listener auto registration" );
			return;
		}

		final Class listenerClass = loadSearchEventListener( serviceRegistry );
		if ( listenerClass == null ) {
			LOG.debug( "Skipping search event listener auto registration - could not fid listener class" );
			return;
		}

		final Object listener = instantiateListener( listenerClass );

		EventListenerRegistry listenerRegistry = serviceRegistry.getService( EventListenerRegistry.class );

		listenerRegistry.addDuplicationStrategy( new DuplicationStrategyImpl( listenerClass ) );

		listenerRegistry.getEventListenerGroup( EventType.POST_INSERT ).appendListener( (PostInsertEventListener) listener );
		listenerRegistry.getEventListenerGroup( EventType.POST_UPDATE ).appendListener( (PostUpdateEventListener) listener );
		listenerRegistry.getEventListenerGroup( EventType.POST_DELETE ).appendListener( (PostDeleteEventListener) listener );
		listenerRegistry.getEventListenerGroup( EventType.POST_COLLECTION_RECREATE ).appendListener( (PostCollectionRecreateEventListener) listener );
		listenerRegistry.getEventListenerGroup( EventType.POST_COLLECTION_REMOVE ).appendListener( (PostCollectionRemoveEventListener) listener );
		listenerRegistry.getEventListenerGroup( EventType.POST_COLLECTION_UPDATE ).appendListener( (PostCollectionUpdateEventListener) listener );
	}

	private Class loadSearchEventListener(SessionFactoryServiceRegistry serviceRegistry) {
		try {
			return serviceRegistry.getService( ClassLoaderService.class ).classForName( LISTENER_CLASS );
		}
		catch (Exception e) {
			return null;
		}
	}

	private Object instantiateListener(Class listenerClass) {
		try {
			return listenerClass.newInstance();
		}
		catch (Exception e) {
			throw new AnnotationException( "Unable to instantiate Search event listener", e );
		}
	}

	public static class DuplicationStrategyImpl implements DuplicationStrategy {
		private final Class checkClass;

		public DuplicationStrategyImpl(Class checkClass) {
			this.checkClass = checkClass;
		}

		@Override
		public boolean areMatch(Object listener, Object original) {
			// not isAssignableFrom since the user could subclass
			return checkClass == original.getClass() && checkClass == listener.getClass();
		}

		@Override
		public Action getAction() {
			return Action.KEEP_ORIGINAL;
		}
	}

	@Override
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		// nothing to do here afaik
	}
}
