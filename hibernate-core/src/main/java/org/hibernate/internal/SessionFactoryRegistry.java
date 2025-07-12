/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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

import org.hibernate.SessionFactory;
import org.hibernate.engine.jndi.JndiException;
import org.hibernate.engine.jndi.JndiNameException;
import org.hibernate.engine.jndi.spi.JndiService;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import static org.hibernate.internal.util.StringHelper.isNotEmpty;

/**
 * A registry of all {@link SessionFactory} instances for the same classloader as this class.
 * <p>
 * This registry is used for serialization/deserialization as well as JNDI binding.
 *
 * @author Steve Ebersole
 */
public class SessionFactoryRegistry {
	private static final SessionFactoryRegistryMessageLogger LOG = SessionFactoryRegistryMessageLogger.INSTANCE;

	/**
	 * Singleton access
	 */
	public static final SessionFactoryRegistry INSTANCE = new SessionFactoryRegistry();

	/**
	 * A map for mapping the UUID of a SessionFactory to the corresponding SessionFactory instance
	 */
	private final ConcurrentHashMap<String, SessionFactoryImplementor> sessionFactoryMap = new ConcurrentHashMap<>();

	/**
	 * A cross-reference for mapping a SessionFactory name to its UUID.  Not all SessionFactories get named,
	 */
	private final ConcurrentHashMap<String, String> nameUuidXref = new ConcurrentHashMap<>();

	private SessionFactoryRegistry() {
		LOG.tracef( "Initializing SessionFactoryRegistry: %s", this );
	}

	/**
	 * Adds a SessionFactory to the registry
	 *
	 * @param uuid The uuid under which to register the SessionFactory
	 * @param name The optional name under which to register the SessionFactory
	 * @param jndiName An optional name to use for binding the SessionFactory into JNDI
	 * @param instance The SessionFactory instance
	 * @param jndiService The JNDI service, so we can register a listener if name is a JNDI name
	 */
	public void addSessionFactory(
			String uuid,
			String name,
			String jndiName,
			SessionFactoryImplementor instance,
			JndiService jndiService) {
		if ( uuid == null ) {
			throw new IllegalArgumentException( "SessionFactory UUID cannot be null" );
		}

		LOG.registeringSessionFactory( uuid, name == null ? "<unnamed>" : name );
		sessionFactoryMap.put( uuid, instance );
		if ( name != null ) {
			nameUuidXref.put( name, uuid );
		}

		if ( jndiName == null ) {
			LOG.notBindingSessionFactory();
			return;
		}

		bindToJndi( jndiName, instance, jndiService );
	}

	private void bindToJndi(String jndiName, SessionFactoryImplementor instance, JndiService jndiService) {
		try {
			LOG.attemptingToBindFactoryToJndi( jndiName );
			jndiService.bind( jndiName, instance );
			LOG.factoryBoundToJndiName( jndiName );
			try {
				jndiService.addListener( jndiName, listener );
			}
			catch (Exception e) {
				LOG.couldNotBindJndiListener();
			}
		}
		catch (JndiNameException e) {
			LOG.invalidJndiName( jndiName, e );
		}
		catch (JndiException e) {
			LOG.unableToBindFactoryToJndi( e );
		}
	}

	/**
	 * Remove a previously added SessionFactory
	 *
	 * @param uuid The uuid
	 * @param name The optional name
	 * @param jndiName An optional name to use for binding the SessionFactory nto JNDI
	 * @param jndiService The JNDI service
	 */
	public void removeSessionFactory(
			String uuid,
			String name,
			String jndiName,
			JndiService jndiService) {
		if ( name != null ) {
			nameUuidXref.remove( name );
		}

		if ( jndiName != null ) {
			try {
				LOG.attemptingToUnbindFactoryFromJndi( jndiName );
				jndiService.unbind( jndiName );
				LOG.factoryUnboundFromJndiName( jndiName );
			}
			catch (JndiNameException e) {
				LOG.invalidJndiName( jndiName, e );
			}
			catch (JndiException e) {
				LOG.unableToUnbindFactoryFromJndi( e );
			}
		}

		sessionFactoryMap.remove( uuid );
	}

	/**
	 * Get a registered SessionFactory by name
	 *
	 * @param name The name
	 *
	 * @return The SessionFactory
	 */
	public SessionFactoryImplementor getNamedSessionFactory(String name) {
		LOG.tracef( "Lookup: name=%s", name );
		final String uuid = nameUuidXref.get( name );
		// protect against NPE -- see HHH-8428
		return uuid == null ? null : getSessionFactory( uuid );
	}

	public SessionFactoryImplementor getSessionFactory(String uuid) {
		LOG.tracef( "Lookup: uid=%s", uuid );
		final SessionFactoryImplementor sessionFactory = sessionFactoryMap.get( uuid );
		if ( sessionFactory == null && LOG.isDebugEnabled() ) {
			LOG.debugf( "Not found: %s", uuid );
			LOG.debug( sessionFactoryMap.toString() );
		}
		return sessionFactory;
	}

	public SessionFactoryImplementor findSessionFactory(String uuid, String name) {
		final SessionFactoryImplementor sessionFactory = getSessionFactory( uuid );
		return sessionFactory == null && isNotEmpty( name )
				? getNamedSessionFactory( name )
				: sessionFactory;

	}

	/**
	 * Does this registry currently contain registrations?
	 */
	public boolean hasRegistrations() {
		return !sessionFactoryMap.isEmpty();
	}

	public void clearRegistrations() {
		nameUuidXref.clear();
		for ( SessionFactory factory : sessionFactoryMap.values() ) {
			try {
				factory.close();
			}
			catch (Exception ignore) {
			}
		}
		sessionFactoryMap.clear();
	}

	/**
	 * Implementation of {@literal JNDI} {@link NamespaceChangeListener} contract to listener for context events
	 * and react accordingly if necessary
	 */
	private final NamespaceChangeListener listener = new NamespaceChangeListener() {
		@Override
		public void objectAdded(NamingEvent evt) {
			if ( LOG.isDebugEnabled() ) {
				LOG.factoryBoundToJndi( evt.getNewBinding().getName() );
			}
		}

		@Override
		public void objectRemoved(NamingEvent evt) {
			final String jndiName = evt.getOldBinding().getName();
			LOG.factoryUnboundFromName( jndiName );

			final String uuid = nameUuidXref.remove( jndiName );
			//noinspection StatementWithEmptyBody
			if ( uuid == null ) {
				// serious problem... but not sure what to do yet
			}
			else {
				sessionFactoryMap.remove( uuid );
			}
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
			LOG.namingExceptionAccessingFactory( evt.getException() );
		}
	};

	public static class ObjectFactoryImpl implements ObjectFactory {
		@Override
		public Object getObjectInstance(Object reference, Name name, Context nameCtx, Hashtable<?, ?> environment) {
			LOG.tracef( "JNDI lookup: %s", name );
			final String uuid = (String) ( (Reference) reference ).get( 0 ).getContent();
			LOG.tracef( "Resolved to UUID = %s", uuid );
			return INSTANCE.getSessionFactory( uuid );
		}
	}
}
