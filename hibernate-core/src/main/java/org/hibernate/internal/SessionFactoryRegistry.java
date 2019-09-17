/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.annotations.common.util.StringHelper;
import org.hibernate.engine.jndi.JndiException;
import org.hibernate.engine.jndi.JndiNameException;
import org.hibernate.engine.jndi.spi.JndiService;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * A registry of all {@link SessionFactory} instances for the same classloader as this class.
 * <p/>
 * This registry is used for serialization/deserialization as well as JNDI binding.
 *
 * @author Steve Ebersole
 */
public class SessionFactoryRegistry {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( SessionFactoryRegistry.class );

	/**
	 * Singleton access
	 */
	public static final SessionFactoryRegistry INSTANCE = new SessionFactoryRegistry();

	/**
	 * A map for mapping the UUID of a SessionFactory to the corresponding SessionFactory instance
	 */
	private final ConcurrentHashMap<String, SessionFactory> sessionFactoryMap = new ConcurrentHashMap<String, SessionFactory>();

	/**
	 * A cross-reference for mapping a SessionFactory name to its UUID.  Not all SessionFactories get named,
	 */
	private final ConcurrentHashMap<String, String> nameUuidXref = new ConcurrentHashMap<String, String>();

	private SessionFactoryRegistry() {
		LOG.debugf( "Initializing SessionFactoryRegistry : %s", this );
	}

	/**
	 * Adds a SessionFactory to the registry
	 *
	 * @param uuid The uuid under which to register the SessionFactory
	 * @param name The optional name under which to register the SessionFactory
	 * @param isNameAlsoJndiName Is name, if provided, also a JNDI name?
	 * @param instance The SessionFactory instance
	 * @param jndiService The JNDI service, so we can register a listener if name is a JNDI name
	 */
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

		if ( name == null || !isNameAlsoJndiName ) {
			LOG.debug( "Not binding SessionFactory to JNDI, no JNDI name configured" );
			return;
		}

		LOG.debugf( "Attempting to bind SessionFactory [%s] to JNDI", name );

		try {
			jndiService.bind( name, instance );
			LOG.factoryBoundToJndiName( name );
			try {
				jndiService.addListener( name, listener );
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

	/**
	 * Remove a previously added SessionFactory
	 *
	 * @param uuid The uuid
	 * @param name The optional name
	 * @param isNameAlsoJndiName Is name, if provided, also a JNDI name?
	 * @param jndiService The JNDI service
	 */
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
				catch (JndiNameException e) {
					LOG.invalidJndiName( name, e );
				}
				catch (JndiException e) {
					LOG.unableToUnbindFactoryFromJndi( e );
				}
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
	public SessionFactory getNamedSessionFactory(String name) {
		LOG.debugf( "Lookup: name=%s", name );
		final String uuid = nameUuidXref.get( name );
		// protect against NPE -- see HHH-8428
		return uuid == null ? null : getSessionFactory( uuid );
	}

	public SessionFactory getSessionFactory(String uuid) {
		LOG.debugf( "Lookup: uid=%s", uuid );
		final SessionFactory sessionFactory = sessionFactoryMap.get( uuid );
		if ( sessionFactory == null && LOG.isDebugEnabled() ) {
			LOG.debugf( "Not found: %s", uuid );
			LOG.debug( sessionFactoryMap.toString() );
		}
		return sessionFactory;
	}

	public SessionFactory findSessionFactory(String uuid, String name) {
		SessionFactory sessionFactory = getSessionFactory( uuid );
		if ( sessionFactory == null && StringHelper.isNotEmpty( name ) ) {
			sessionFactory = getNamedSessionFactory( name );
		}
		return sessionFactory;
	}

	/**
	 * Does this registry currently contain registrations?
	 *
	 * @return true/false
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
	 * Implementation of {@literal JNDI} {@link javax.naming.event.NamespaceChangeListener} contract to listener for context events
	 * and react accordingly if necessary
	 */
	private final NamespaceChangeListener listener = new NamespaceChangeListener() {
		@Override
		public void objectAdded(NamingEvent evt) {
			LOG.debugf( "A factory was successfully bound to name: %s", evt.getNewBinding().getName() );
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
			LOG.namingExceptionAccessingFactory( evt.getException() );
		}
	};

	public static class ObjectFactoryImpl implements ObjectFactory {
		@Override
		public Object getObjectInstance(Object reference, Name name, Context nameCtx, Hashtable<?, ?> environment) {
			LOG.debugf( "JNDI lookup: %s", name );
			final String uuid = (String) ( (Reference) reference ).get( 0 ).getContent();
			LOG.tracef( "Resolved to UUID = %s", uuid );
			return INSTANCE.getSessionFactory( uuid );
		}
	}
}
