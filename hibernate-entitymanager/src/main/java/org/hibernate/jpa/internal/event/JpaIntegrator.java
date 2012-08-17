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
package org.hibernate.jpa.internal.event;

import javax.enterprise.inject.spi.BeanManager;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.CascadeStyles;
import org.hibernate.engine.spi.CascadingAction;
import org.hibernate.engine.spi.CascadingActions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.DuplicationStrategy;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.internal.event.core.HibernateEntityManagerEventListener;
import org.hibernate.jpa.internal.event.core.JpaAutoFlushEventListener;
import org.hibernate.jpa.internal.event.core.JpaDeleteEventListener;
import org.hibernate.jpa.internal.event.core.JpaFlushEntityEventListener;
import org.hibernate.jpa.internal.event.core.JpaFlushEventListener;
import org.hibernate.jpa.internal.event.core.JpaMergeEventListener;
import org.hibernate.jpa.internal.event.core.JpaPersistEventListener;
import org.hibernate.jpa.internal.event.core.JpaPersistOnFlushEventListener;
import org.hibernate.jpa.internal.event.core.JpaPostDeleteEventListener;
import org.hibernate.jpa.internal.event.core.JpaPostInsertEventListener;
import org.hibernate.jpa.internal.event.core.JpaPostLoadEventListener;
import org.hibernate.jpa.internal.event.core.JpaPostUpdateEventListener;
import org.hibernate.jpa.internal.event.core.JpaSaveEventListener;
import org.hibernate.jpa.internal.event.core.JpaSaveOrUpdateEventListener;
import org.hibernate.jpa.internal.event.jpa.BeanManagerListenerFactory;
import org.hibernate.jpa.internal.event.jpa.CallbackProcessor;
import org.hibernate.jpa.internal.event.jpa.CallbackProcessorImpl;
import org.hibernate.jpa.internal.event.jpa.CallbackRegistryConsumer;
import org.hibernate.jpa.internal.event.jpa.CallbackRegistry;
import org.hibernate.jpa.internal.event.jpa.LegacyCallbackProcessor;
import org.hibernate.jpa.internal.event.jpa.ListenerFactory;
import org.hibernate.jpa.internal.event.jpa.StandardListenerFactory;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.secure.internal.JACCPreDeleteEventListener;
import org.hibernate.secure.internal.JACCPreInsertEventListener;
import org.hibernate.secure.internal.JACCPreLoadEventListener;
import org.hibernate.secure.internal.JACCPreUpdateEventListener;
import org.hibernate.secure.internal.JACCSecurityListener;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * Hibernate EntityManager specific Integrator, performing JPA setup.
 *
 * @author Steve Ebersole
 */
public class JpaIntegrator implements Integrator {
	private ListenerFactory jpaListenerFactory;
	private CallbackProcessor callbackProcessor;
	private CallbackRegistry callbackRegistry;

	private static final DuplicationStrategy JPA_DUPLICATION_STRATEGY = new DuplicationStrategy() {
		@Override
		public boolean areMatch(Object listener, Object original) {
			return listener.getClass().equals( original.getClass() ) &&
					HibernateEntityManagerEventListener.class.isInstance( original );
		}

		@Override
		public Action getAction() {
			return Action.KEEP_ORIGINAL;
		}
	};

	private static final DuplicationStrategy JACC_DUPLICATION_STRATEGY = new DuplicationStrategy() {
		@Override
		public boolean areMatch(Object listener, Object original) {
			return listener.getClass().equals( original.getClass() ) &&
					JACCSecurityListener.class.isInstance( original );
		}

		@Override
		public Action getAction() {
			return Action.KEEP_ORIGINAL;
		}
	};

	@Override
	@SuppressWarnings( {"unchecked"})
	public void integrate(
			Configuration configuration,
			SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {
		// first, register the JPA-specific persist cascade style
		CascadeStyles.registerCascadeStyle(
				"persist",
				new CascadeStyles.BaseCascadeStyle() {
					@Override
					public boolean doCascade(CascadingAction action) {
						return action == JpaPersistEventListener.PERSIST_SKIPLAZY
								|| action == CascadingActions.PERSIST_ON_FLUSH;
					}

					@Override
					public String toString() {
						return "STYLE_PERSIST_SKIPLAZY";
					}
				}
		);


		// then prepare listeners
		final EventListenerRegistry eventListenerRegistry = serviceRegistry.getService( EventListenerRegistry.class );

		boolean isSecurityEnabled = configuration.getProperties().containsKey( AvailableSettings.JACC_ENABLED );

		eventListenerRegistry.addDuplicationStrategy( JPA_DUPLICATION_STRATEGY );
		eventListenerRegistry.addDuplicationStrategy( JACC_DUPLICATION_STRATEGY );

		// op listeners
		eventListenerRegistry.setListeners( EventType.AUTO_FLUSH, JpaAutoFlushEventListener.INSTANCE );
		eventListenerRegistry.setListeners( EventType.DELETE, new JpaDeleteEventListener() );
		eventListenerRegistry.setListeners( EventType.FLUSH_ENTITY, new JpaFlushEntityEventListener() );
		eventListenerRegistry.setListeners( EventType.FLUSH, JpaFlushEventListener.INSTANCE );
		eventListenerRegistry.setListeners( EventType.MERGE, new JpaMergeEventListener() );
		eventListenerRegistry.setListeners( EventType.PERSIST, new JpaPersistEventListener() );
		eventListenerRegistry.setListeners( EventType.PERSIST_ONFLUSH, new JpaPersistOnFlushEventListener() );
		eventListenerRegistry.setListeners( EventType.SAVE, new JpaSaveEventListener() );
		eventListenerRegistry.setListeners( EventType.SAVE_UPDATE, new JpaSaveOrUpdateEventListener() );

		// pre op listeners
		if ( isSecurityEnabled ) {
			final String jaccContextId = configuration.getProperty( Environment.JACC_CONTEXTID );
			eventListenerRegistry.prependListeners( EventType.PRE_DELETE, new JACCPreDeleteEventListener(jaccContextId) );
			eventListenerRegistry.prependListeners( EventType.PRE_INSERT, new JACCPreInsertEventListener(jaccContextId) );
			eventListenerRegistry.prependListeners( EventType.PRE_UPDATE, new JACCPreUpdateEventListener(jaccContextId) );
			eventListenerRegistry.prependListeners( EventType.PRE_LOAD, new JACCPreLoadEventListener(jaccContextId) );
		}

		// post op listeners
		eventListenerRegistry.prependListeners( EventType.POST_DELETE, new JpaPostDeleteEventListener() );
		eventListenerRegistry.prependListeners( EventType.POST_INSERT, new JpaPostInsertEventListener() );
		eventListenerRegistry.prependListeners( EventType.POST_LOAD, new JpaPostLoadEventListener() );
		eventListenerRegistry.prependListeners( EventType.POST_UPDATE, new JpaPostUpdateEventListener() );

		for ( Map.Entry<?,?> entry : configuration.getProperties().entrySet() ) {
			if ( ! String.class.isInstance( entry.getKey() ) ) {
				continue;
			}
			final String propertyName = (String) entry.getKey();
			if ( ! propertyName.startsWith( AvailableSettings.EVENT_LISTENER_PREFIX ) ) {
				continue;
			}
			final String eventTypeName = propertyName.substring( AvailableSettings.EVENT_LISTENER_PREFIX.length() + 1 );
			final EventType eventType = EventType.resolveEventTypeByName( eventTypeName );
			final EventListenerGroup eventListenerGroup = eventListenerRegistry.getEventListenerGroup( eventType );
			for ( String listenerImpl : ( (String) entry.getValue() ).split( " ," ) ) {
				eventListenerGroup.appendListener( instantiate( listenerImpl, serviceRegistry ) );
			}
		}

		// handle JPA "entity listener classes"...

		this.callbackRegistry = new CallbackRegistry();
		final BeanManager beanManager = (BeanManager) configuration.getProperties().get( AvailableSettings.CDI_BEAN_MANAGER );
		this.jpaListenerFactory = beanManager == null
				? new StandardListenerFactory()
				: new BeanManagerListenerFactory( beanManager );
		this.callbackProcessor = new LegacyCallbackProcessor( jpaListenerFactory, configuration.getReflectionManager() );

		Iterator classes = configuration.getClassMappings();
		while ( classes.hasNext() ) {
			final PersistentClass clazz = (PersistentClass) classes.next();
			if ( clazz.getClassName() == null ) {
				// we can have non java class persisted by hibernate
				continue;
			}
			callbackProcessor.processCallbacksForEntity( clazz.getClassName(), callbackRegistry );
		}

		for ( EventType eventType : EventType.values() ) {
			final EventListenerGroup eventListenerGroup = eventListenerRegistry.getEventListenerGroup( eventType );
			for ( Object listener : eventListenerGroup.listeners() ) {
				if ( CallbackRegistryConsumer.class.isInstance( listener ) ) {
					( (CallbackRegistryConsumer) listener ).injectCallbackRegistry( callbackRegistry );
				}
			}
		}
	}

	@Override
	public void integrate(
			MetadataImplementor metadata,
			SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry ) {
		// first, register the JPA-specific persist cascade style
		CascadeStyles.registerCascadeStyle(
				"persist",
				new CascadeStyles.BaseCascadeStyle() {
					@Override
					public boolean doCascade(CascadingAction action) {
						return action == JpaPersistEventListener.PERSIST_SKIPLAZY
								|| action == CascadingActions.PERSIST_ON_FLUSH;
					}

					@Override
					public String toString() {
						return "STYLE_PERSIST_SKIPLAZY";
					}
				}
		);

		// then prepare listeners
        final EventListenerRegistry eventListenerRegistry = serviceRegistry.getService( EventListenerRegistry.class );

        boolean isSecurityEnabled = sessionFactory.getProperties().containsKey( AvailableSettings.JACC_ENABLED );

        eventListenerRegistry.addDuplicationStrategy( JPA_DUPLICATION_STRATEGY );
        eventListenerRegistry.addDuplicationStrategy( JACC_DUPLICATION_STRATEGY );

        // op listeners
        eventListenerRegistry.setListeners( EventType.AUTO_FLUSH, JpaAutoFlushEventListener.INSTANCE );
        eventListenerRegistry.setListeners( EventType.DELETE, new JpaDeleteEventListener() );
        eventListenerRegistry.setListeners( EventType.FLUSH_ENTITY, new JpaFlushEntityEventListener() );
        eventListenerRegistry.setListeners( EventType.FLUSH, JpaFlushEventListener.INSTANCE );
        eventListenerRegistry.setListeners( EventType.MERGE, new JpaMergeEventListener() );
        eventListenerRegistry.setListeners( EventType.PERSIST, new JpaPersistEventListener() );
        eventListenerRegistry.setListeners( EventType.PERSIST_ONFLUSH, new JpaPersistOnFlushEventListener() );
        eventListenerRegistry.setListeners( EventType.SAVE, new JpaSaveEventListener() );
        eventListenerRegistry.setListeners( EventType.SAVE_UPDATE, new JpaSaveOrUpdateEventListener() );

        // pre op listeners
        if ( isSecurityEnabled ) {
            final String jaccContextId = sessionFactory.getProperties().getProperty( Environment.JACC_CONTEXTID );
            eventListenerRegistry.prependListeners( EventType.PRE_DELETE, new JACCPreDeleteEventListener(jaccContextId) );
            eventListenerRegistry.prependListeners( EventType.PRE_INSERT, new JACCPreInsertEventListener(jaccContextId) );
            eventListenerRegistry.prependListeners( EventType.PRE_UPDATE, new JACCPreUpdateEventListener(jaccContextId) );
            eventListenerRegistry.prependListeners( EventType.PRE_LOAD, new JACCPreLoadEventListener(jaccContextId) );
        }

        // post op listeners
        eventListenerRegistry.prependListeners( EventType.POST_DELETE, new JpaPostDeleteEventListener() );
        eventListenerRegistry.prependListeners( EventType.POST_INSERT, new JpaPostInsertEventListener() );
        eventListenerRegistry.prependListeners( EventType.POST_LOAD, new JpaPostLoadEventListener() );
        eventListenerRegistry.prependListeners( EventType.POST_UPDATE, new JpaPostUpdateEventListener() );

        for ( Map.Entry<?,?> entry : sessionFactory.getProperties().entrySet() ) {
            if ( ! String.class.isInstance( entry.getKey() ) ) {
                continue;
            }
            final String propertyName = (String) entry.getKey();
            if ( ! propertyName.startsWith( AvailableSettings.EVENT_LISTENER_PREFIX ) ) {
                continue;
            }
            final String eventTypeName = propertyName.substring( AvailableSettings.EVENT_LISTENER_PREFIX.length() + 1 );
            final EventType eventType = EventType.resolveEventTypeByName( eventTypeName );
            final EventListenerGroup eventListenerGroup = eventListenerRegistry.getEventListenerGroup( eventType );
            for ( String listenerImpl : ( (String) entry.getValue() ).split( " ," ) ) {
                eventListenerGroup.appendListener( instantiate( listenerImpl, serviceRegistry ) );
            }
        }

		// handle JPA "entity listener classes"...

		this.callbackRegistry = new CallbackRegistry();
		final BeanManager beanManager = (BeanManager) sessionFactory.getProperties().get( AvailableSettings.CDI_BEAN_MANAGER );
		this.jpaListenerFactory = beanManager == null
				? new StandardListenerFactory()
				: new BeanManagerListenerFactory( beanManager );
		this.callbackProcessor = new CallbackProcessorImpl( jpaListenerFactory, metadata, serviceRegistry );

        for ( EntityBinding binding : metadata.getEntityBindings() ) {
			callbackProcessor.processCallbacksForEntity( binding, callbackRegistry );
        }

        for ( EventType eventType : EventType.values() ) {
            final EventListenerGroup eventListenerGroup = eventListenerRegistry.getEventListenerGroup( eventType );
            for ( Object listener : eventListenerGroup.listeners() ) {
                if ( CallbackRegistryConsumer.class.isInstance( listener ) ) {
                    ( (CallbackRegistryConsumer) listener ).injectCallbackRegistry( callbackRegistry );
                }
            }
        }
	}

	@Override
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		if ( callbackRegistry != null ) {
			callbackRegistry.release();
		}
		if ( callbackProcessor != null ) {
			callbackProcessor.release();
		}
		if ( jpaListenerFactory != null ) {
			jpaListenerFactory.release();
		}
	}

	private Object instantiate(String listenerImpl, ServiceRegistryImplementor serviceRegistry) {
		try {
			return serviceRegistry.getService( ClassLoaderService.class ).classForName( listenerImpl ).newInstance();
		}
		catch (Exception e) {
			throw new HibernateException( "Could not instantiate requested listener [" + listenerImpl + "]", e );
        }
    }
}
