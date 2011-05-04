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
package org.hibernate.ejb.event;

import java.util.Iterator;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.cfg.Configuration;
import org.hibernate.ejb.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.secure.JACCPreDeleteEventListener;
import org.hibernate.secure.JACCPreInsertEventListener;
import org.hibernate.secure.JACCPreLoadEventListener;
import org.hibernate.secure.JACCPreUpdateEventListener;
import org.hibernate.secure.JACCSecurityListener;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.event.service.spi.DuplicationStrategy;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * Prepare the HEM-specific event listeners.
 * 
 * @author Steve Ebersole
 */
public class JpaIntegrator implements Integrator {
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
		final EventListenerRegistry eventListenerRegistry = serviceRegistry.getService( EventListenerRegistry.class );

		boolean isSecurityEnabled = configuration.getProperties().containsKey( AvailableSettings.JACC_ENABLED );

		eventListenerRegistry.addDuplicationStrategy( JPA_DUPLICATION_STRATEGY );
		eventListenerRegistry.addDuplicationStrategy( JACC_DUPLICATION_STRATEGY );

		// op listeners
		eventListenerRegistry.setListeners( EventType.AUTO_FLUSH, EJB3AutoFlushEventListener.INSTANCE );
		eventListenerRegistry.setListeners( EventType.DELETE, new EJB3DeleteEventListener() );
		eventListenerRegistry.setListeners( EventType.FLUSH_ENTITY, new EJB3FlushEntityEventListener() );
		eventListenerRegistry.setListeners( EventType.FLUSH, EJB3FlushEventListener.INSTANCE );
		eventListenerRegistry.setListeners( EventType.MERGE, new EJB3MergeEventListener() );
		eventListenerRegistry.setListeners( EventType.PERSIST, new EJB3PersistEventListener() );
		eventListenerRegistry.setListeners( EventType.PERSIST_ONFLUSH, new EJB3PersistOnFlushEventListener() );
		eventListenerRegistry.setListeners( EventType.SAVE, new EJB3SaveEventListener() );
		eventListenerRegistry.setListeners( EventType.SAVE_UPDATE, new EJB3SaveOrUpdateEventListener() );

		// pre op listeners
		if ( isSecurityEnabled ) {
			eventListenerRegistry.prependListeners( EventType.PRE_DELETE, new JACCPreDeleteEventListener() );
			eventListenerRegistry.prependListeners( EventType.PRE_INSERT, new JACCPreInsertEventListener() );
			eventListenerRegistry.prependListeners( EventType.PRE_UPDATE, new JACCPreUpdateEventListener() );
			eventListenerRegistry.prependListeners( EventType.PRE_LOAD, new JACCPreLoadEventListener() );
		}

		// post op listeners
		eventListenerRegistry.prependListeners( EventType.POST_DELETE, new EJB3PostDeleteEventListener() );
		eventListenerRegistry.prependListeners( EventType.POST_INSERT, new EJB3PostInsertEventListener() );
		eventListenerRegistry.prependListeners( EventType.POST_LOAD, new EJB3PostLoadEventListener() );
		eventListenerRegistry.prependListeners( EventType.POST_UPDATE, new EJB3PostUpdateEventListener() );

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

		final EntityCallbackHandler callbackHandler = new EntityCallbackHandler();
		Iterator classes = configuration.getClassMappings();
		ReflectionManager reflectionManager = configuration.getReflectionManager();
		while ( classes.hasNext() ) {
			PersistentClass clazz = (PersistentClass) classes.next();
			if ( clazz.getClassName() == null ) {
				//we can have non java class persisted by hibernate
				continue;
			}
			try {
				callbackHandler.add( reflectionManager.classForName( clazz.getClassName(), this.getClass() ), reflectionManager );
			}
			catch (ClassNotFoundException e) {
				throw new MappingException( "entity class not found: " + clazz.getNodeName(), e );
			}
		}

		for ( EventType eventType : EventType.values() ) {
			final EventListenerGroup eventListenerGroup = eventListenerRegistry.getEventListenerGroup( eventType );
			for ( Object listener : eventListenerGroup.listeners() ) {
				if ( CallbackHandlerConsumer.class.isInstance( listener ) ) {
					( (CallbackHandlerConsumer) listener ).setCallbackHandler( callbackHandler );
				}
			}
		}
	}

	@Override
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
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
