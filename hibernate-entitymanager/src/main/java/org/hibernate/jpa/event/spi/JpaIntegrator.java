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
package org.hibernate.jpa.event.spi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.Configuration;
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
import org.hibernate.jpa.event.internal.core.HibernateEntityManagerEventListener;
import org.hibernate.jpa.event.internal.core.JpaAutoFlushEventListener;
import org.hibernate.jpa.event.internal.core.JpaDeleteEventListener;
import org.hibernate.jpa.event.internal.core.JpaFlushEntityEventListener;
import org.hibernate.jpa.event.internal.core.JpaFlushEventListener;
import org.hibernate.jpa.event.internal.core.JpaMergeEventListener;
import org.hibernate.jpa.event.internal.core.JpaPersistEventListener;
import org.hibernate.jpa.event.internal.core.JpaPersistOnFlushEventListener;
import org.hibernate.jpa.event.internal.core.JpaPostDeleteEventListener;
import org.hibernate.jpa.event.internal.core.JpaPostInsertEventListener;
import org.hibernate.jpa.event.internal.core.JpaPostLoadEventListener;
import org.hibernate.jpa.event.internal.core.JpaPostUpdateEventListener;
import org.hibernate.jpa.event.internal.core.JpaSaveEventListener;
import org.hibernate.jpa.event.internal.core.JpaSaveOrUpdateEventListener;
import org.hibernate.jpa.event.internal.jpa.CallbackProcessor;
import org.hibernate.jpa.event.internal.jpa.CallbackProcessorImpl;
import org.hibernate.jpa.event.internal.jpa.CallbackRegistryConsumer;
import org.hibernate.jpa.event.internal.jpa.CallbackRegistryImpl;
import org.hibernate.jpa.event.internal.jpa.StandardListenerFactory;
import org.hibernate.jpa.event.spi.jpa.ListenerFactory;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.binding.EntityBinding;
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
	private CallbackRegistryImpl callbackRegistry;

	private static final DuplicationStrategy JPA_DUPLICATION_STRATEGY = new JPADuplicationStrategy();

	@Override
	@SuppressWarnings( {"unchecked"})
	public void integrate(
			Configuration configuration,
			SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {
//		// first, register the JPA-specific persist cascade style
//		CascadeStyles.registerCascadeStyle(
//				"persist",
//                new PersistCascadeStyle()
//		);
//
//
//		// then prepare listeners
//		final EventListenerRegistry eventListenerRegistry = serviceRegistry.getService( EventListenerRegistry.class );
//
//		eventListenerRegistry.addDuplicationStrategy( JPA_DUPLICATION_STRATEGY );
//
//		// op listeners
//		eventListenerRegistry.setListeners( EventType.AUTO_FLUSH, JpaAutoFlushEventListener.INSTANCE );
//		eventListenerRegistry.setListeners( EventType.DELETE, new JpaDeleteEventListener() );
//		eventListenerRegistry.setListeners( EventType.FLUSH_ENTITY, new JpaFlushEntityEventListener() );
//		eventListenerRegistry.setListeners( EventType.FLUSH, JpaFlushEventListener.INSTANCE );
//		eventListenerRegistry.setListeners( EventType.MERGE, new JpaMergeEventListener() );
//		eventListenerRegistry.setListeners( EventType.PERSIST, new JpaPersistEventListener() );
//		eventListenerRegistry.setListeners( EventType.PERSIST_ONFLUSH, new JpaPersistOnFlushEventListener() );
//		eventListenerRegistry.setListeners( EventType.SAVE, new JpaSaveEventListener() );
//		eventListenerRegistry.setListeners( EventType.SAVE_UPDATE, new JpaSaveOrUpdateEventListener() );
//
//		// post op listeners
//		eventListenerRegistry.prependListeners( EventType.POST_DELETE, new JpaPostDeleteEventListener() );
//		eventListenerRegistry.prependListeners( EventType.POST_INSERT, new JpaPostInsertEventListener() );
//		eventListenerRegistry.prependListeners( EventType.POST_LOAD, new JpaPostLoadEventListener() );
//		eventListenerRegistry.prependListeners( EventType.POST_UPDATE, new JpaPostUpdateEventListener() );
//
//		for ( Map.Entry<?,?> entry : configuration.getProperties().entrySet() ) {
//			if ( ! String.class.isInstance( entry.getKey() ) ) {
//				continue;
//			}
//			final String propertyName = (String) entry.getKey();
//			if ( ! propertyName.startsWith( AvailableSettings.EVENT_LISTENER_PREFIX ) ) {
//				continue;
//			}
//			final String eventTypeName = propertyName.substring( AvailableSettings.EVENT_LISTENER_PREFIX.length() + 1 );
//			final EventType eventType = EventType.resolveEventTypeByName( eventTypeName );
//			final EventListenerGroup eventListenerGroup = eventListenerRegistry.getEventListenerGroup( eventType );
//			for ( String listenerImpl : ( (String) entry.getValue() ).split( " ," ) ) {
//				eventListenerGroup.appendListener( instantiate( listenerImpl, serviceRegistry ) );
//			}
//		}
//
//		// handle JPA "entity listener classes"...
//
//		this.callbackRegistry = new CallbackRegistryImpl();
//		final Object beanManagerRef = configuration.getProperties().get( AvailableSettings.CDI_BEAN_MANAGER );
//		this.jpaListenerFactory = beanManagerRef == null
//				? new StandardListenerFactory()
//				: buildBeanManagerListenerFactory( beanManagerRef );
//		this.callbackProcessor = new LegacyCallbackProcessor( jpaListenerFactory, configuration.getReflectionManager() );
//
//		Iterator classes = configuration.getClassMappings();
//		while ( classes.hasNext() ) {
//			final PersistentClass clazz = (PersistentClass) classes.next();
//			if ( clazz.getClassName() == null ) {
//				// we can have non java class persisted by hibernate
//				continue;
//			}
//			callbackProcessor.processCallbacksForEntity( clazz.getClassName(), callbackRegistry );
//		}
//
//		for ( EventType eventType : EventType.values() ) {
//			final EventListenerGroup eventListenerGroup = eventListenerRegistry.getEventListenerGroup( eventType );
//			for ( Object listener : eventListenerGroup.listeners() ) {
//				if ( CallbackRegistryConsumer.class.isInstance( listener ) ) {
//					( (CallbackRegistryConsumer) listener ).injectCallbackRegistry( callbackRegistry );
//				}
//			}
//		}
	}

	private static final String CDI_LISTENER_FACTORY_CLASS = "org.hibernate.jpa.event.internal.jpa.BeanManagerListenerFactory";

	private ListenerFactory buildBeanManagerListenerFactory(Object beanManagerRef) {
		try {
			// specifically using our classloader here...
			final Class beanManagerListenerFactoryClass = getClass().getClassLoader()
					.loadClass( CDI_LISTENER_FACTORY_CLASS );
			final Method beanManagerListenerFactoryBuilderMethod = beanManagerListenerFactoryClass.getMethod(
					"fromBeanManagerReference",
					Object.class
			);

			try {
				return (ListenerFactory) beanManagerListenerFactoryBuilderMethod.invoke( null, beanManagerRef );
			}
			catch (InvocationTargetException e) {
				throw e.getTargetException();
			}
		}
		catch (ClassNotFoundException e) {
			throw new HibernateException( "Could not locate BeanManagerListenerFactory class to handle CDI extensions", e );
		}
		catch (HibernateException e) {
			throw e;
		}
		catch (Throwable e) {
			throw new HibernateException( "Could not access BeanManagerListenerFactory class to handle CDI extensions", e );
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
				new PersistCascadeStyle()
		);

		// then prepare listeners
        final EventListenerRegistry eventListenerRegistry = serviceRegistry.getService( EventListenerRegistry.class );

        eventListenerRegistry.addDuplicationStrategy( JPA_DUPLICATION_STRATEGY );

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

		this.callbackRegistry = new CallbackRegistryImpl();
		final Object beanManagerRef = sessionFactory.getSessionFactoryOptions().getBeanManagerReference();
		this.jpaListenerFactory = beanManagerRef == null
				? new StandardListenerFactory()
				: buildBeanManagerListenerFactory( beanManagerRef );
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

    private static class PersistCascadeStyle extends CascadeStyles.BaseCascadeStyle {
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

    private static class JPADuplicationStrategy implements DuplicationStrategy {
        @Override
        public boolean areMatch(Object listener, Object original) {
            return listener.getClass().equals( original.getClass() ) &&
                    HibernateEntityManagerEventListener.class.isInstance( original );
        }

        @Override
        public Action getAction() {
            return Action.KEEP_ORIGINAL;
        }
    }
}
