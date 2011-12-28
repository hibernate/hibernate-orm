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
package org.hibernate.internal;

import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.event.NamespaceChangeListener;
import javax.naming.event.NamingEvent;
import javax.naming.event.NamingExceptionEvent;
import javax.naming.spi.ObjectFactory;

import org.jboss.logging.Logger;

import org.hibernate.SessionFactory;
import org.hibernate.service.jndi.JndiException;
import org.hibernate.service.jndi.JndiNameException;
import org.hibernate.service.jndi.spi.JndiService;

/**
 * A registry of all {@link SessionFactory} instances for the same classloader as this class.
 *
 * This registry is used for serialization/deserialization as well as JNDI binding.
 *
 * @author Steve Ebersole
 */
public class SessionFactoryRegistry {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			SessionFactoryRegistry.class.getName()
	);

	public static final SessionFactoryRegistry INSTANCE = new SessionFactoryRegistry();

	private final ConcurrentHashMap<String, SessionFactory> sessionFactoryMap = new ConcurrentHashMap<String, SessionFactory>();
	private final ConcurrentHashMap<String,String> nameUuidXref = new ConcurrentHashMap<String, String>();

	public SessionFactoryRegistry() {
		LOG.debugf( "Initializing SessionFactoryRegistry : %s", this );
	}

	public void addSessionFactory(
			String uuid,
			String name,
			boolean isNameAlsoJndiName,
			SessionFactory instance,
			JndiService jndiService) {
		if ( uuid == null ) {
			throw new IllegalArgumentException( "SessionFactory UUID cannot be null" );
		}

        LOG.debugf( "Registering SessionFactory: %s (%s)", uuid, name == null ? "<unnamed>" : name );
		sessionFactoryMap.put( uuid, instance );
		if ( name != null ) {
			nameUuidXref.put( name, uuid );
		}

		if ( name == null || ! isNameAlsoJndiName ) {
			LOG.debug( "Not binding SessionFactory to JNDI, no JNDI name configured" );
			return;
		}

		LOG.debugf( "Attempting to bind SessionFactory [%s] to JNDI", name );

		try {
			jndiService.bind( name, instance );
			LOG.factoryBoundToJndiName( name );
			try {
				jndiService.addListener( name, LISTENER );
			}
			catch (Exception e) {
				LOG.couldNotBindJndiListener();
			}
		}
		catch (JndiNameException e) {
			LOG.invalidJndiName( name, e );
		}
		catch (JndiException e) {
			LOG.unableToBindFactoryToJndi( e );
		}
	}

	public void removeSessionFactory(
			String uuid,
			String name,
			boolean isNameAlsoJndiName,
			JndiService jndiService) {
		if ( name != null ) {
			nameUuidXref.remove( name );

			if ( isNameAlsoJndiName ) {
				try {
					LOG.tracef( "Unbinding SessionFactory from JNDI : %s", name );
					jndiService.unbind( name );
					LOG.factoryUnboundFromJndiName( name );
				}
				catch ( JndiNameException e ) {
					LOG.invalidJndiName( name, e );
				}
				catch ( JndiException e ) {
					LOG.unableToUnbindFactoryFromJndi( e );
				}
			}
		}

		sessionFactoryMap.remove( uuid );
	}

	public SessionFactory getNamedSessionFactory(String name) {
        LOG.debugf( "Lookup: name=%s", name );
		final String uuid = nameUuidXref.get( name );
		return getSessionFactory( uuid );
	}

	public SessionFactory getSessionFactory(String uuid) {
		LOG.debugf( "Lookup: uid=%s", uuid );
		final SessionFactory sessionFactory = sessionFactoryMap.get( uuid );
		if ( sessionFactory == null && LOG.isDebugEnabled() ) {
			LOG.debugf( "Not found: %s", uuid );
			LOG.debugf( sessionFactoryMap.toString() );
		}
		return sessionFactory;
	}

	/**
	 * Implementation of {@literal JNDI} {@link javax.naming.event.NamespaceChangeListener} contract to listener for context events
	 * and react accordingly if necessary
	 */
	private final NamespaceChangeListener LISTENER = new NamespaceChangeListener() {
		@Override
		public void objectAdded(NamingEvent evt) {
            LOG.debugf("A factory was successfully bound to name: %s", evt.getNewBinding().getName());
		}

		@Override
		public void objectRemoved(NamingEvent evt) {
			final String jndiName = evt.getOldBinding().getName();
            LOG.factoryUnboundFromName( jndiName );

			final String uuid = nameUuidXref.remove( jndiName );
			if ( uuid == null ) {
				// serious problem... but not sure what to do yet
			}
			sessionFactoryMap.remove( uuid );
		}

		@Override
		public void objectRenamed(NamingEvent evt) {
			final String oldJndiName = evt.getOldBinding().getName();
			final String newJndiName = evt.getNewBinding().getName();

            LOG.factoryJndiRename( oldJndiName, newJndiName );

			final String uuid = nameUuidXref.remove( oldJndiName );
			nameUuidXref.put( newJndiName, uuid );
		}

		@Override
		public void namingExceptionThrown(NamingExceptionEvent evt) {
			//noinspection ThrowableResultOfMethodCallIgnored
            LOG.namingExceptionAccessingFactory(evt.getException());
		}
	};

	public static class ObjectFactoryImpl implements ObjectFactory {
		@Override
		public Object getObjectInstance(Object reference, Name name, Context nameCtx, Hashtable<?, ?> environment)
				throws Exception {
			LOG.debugf( "JNDI lookup: %s", name );
			final String uuid = (String) ( (Reference) reference ).get( 0 ).getContent();
			LOG.tracef( "Resolved to UUID = %s", uuid );
			return INSTANCE.getSessionFactory( uuid );
		}
	}
}
