/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.jndi.internal;

import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.event.EventContext;
import javax.naming.event.NamespaceChangeListener;

import org.hibernate.cfg.Environment;
import org.hibernate.engine.jndi.JndiException;
import org.hibernate.engine.jndi.JndiNameException;
import org.hibernate.engine.jndi.spi.JndiService;
import org.hibernate.internal.CoreMessageLogger;

import org.jboss.logging.Logger;

/**
 * Standard implementation of JNDI services.
 *
 * @author Steve Ebersole
 */
public class JndiServiceImpl implements JndiService {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			JndiServiceImpl.class.getName()
	);

	private final Hashtable initialContextSettings;

	/**
	 * Constructs a JndiServiceImpl
	 *
	 * @param configurationValues Map of configuration settings, some of which apply to JNDI support.
	 */
	public JndiServiceImpl(Map configurationValues) {
		this.initialContextSettings = extractJndiProperties( configurationValues );
	}

	/**
	 * Given a hodgepodge of properties, extract out the ones relevant for JNDI interaction.
	 *
	 * @param configurationValues The map of config values
	 *
	 * @return The extracted JNDI specific properties.
	 */
	@SuppressWarnings({ "unchecked" })
	public static Properties extractJndiProperties(Map configurationValues) {
		final Properties jndiProperties = new Properties();

		for ( Map.Entry entry : (Set<Map.Entry>) configurationValues.entrySet() ) {
			if ( !String.class.isInstance( entry.getKey() ) ) {
				continue;
			}
			final String propertyName = (String) entry.getKey();
			final Object propertyValue = entry.getValue();
			if ( propertyName.startsWith( Environment.JNDI_PREFIX ) ) {
				// write the IntialContextFactory class and provider url to the result only if they are
				// non-null; this allows the environmental defaults (if any) to remain in effect
				if ( Environment.JNDI_CLASS.equals( propertyName ) ) {
					if ( propertyValue != null ) {
						jndiProperties.put( Context.INITIAL_CONTEXT_FACTORY, propertyValue );
					}
				}
				else if ( Environment.JNDI_URL.equals( propertyName ) ) {
					if ( propertyValue != null ) {
						jndiProperties.put( Context.PROVIDER_URL, propertyValue );
					}
				}
				else {
					final String passThruPropertyname = propertyName.substring( Environment.JNDI_PREFIX.length() + 1 );
					jndiProperties.put( passThruPropertyname, propertyValue );
				}
			}
		}

		return jndiProperties;
	}

	@Override
	public Object locate(String jndiName) {
		final InitialContext initialContext = buildInitialContext();
		final Name name = parseName( jndiName, initialContext );
		try {
			return initialContext.lookup( name );
		}
		catch ( NamingException e ) {
			throw new JndiException( "Unable to lookup JNDI name [" + jndiName + "]", e );
		}
		finally {
			cleanUp( initialContext );
		}
	}

	private InitialContext buildInitialContext() {
		try {
			return initialContextSettings.size() == 0 ? new InitialContext() : new InitialContext( initialContextSettings );
		}
		catch ( NamingException e ) {
			throw new JndiException( "Unable to open InitialContext", e );
		}
	}

	private Name parseName(String jndiName, Context context) {
		try {
			return context.getNameParser( "" ).parse( jndiName );
		}
		catch ( InvalidNameException e ) {
			throw new JndiNameException( "JNDI name [" + jndiName + "] was not valid", e );
		}
		catch ( NamingException e ) {
			throw new JndiException( "Error parsing JNDI name [" + jndiName + "]", e );
		}
	}

	private void cleanUp(InitialContext initialContext) {
		try {
			initialContext.close();
		}
		catch ( NamingException e ) {
			LOG.unableToCloseInitialContext(e.toString());
		}
	}

	@Override
	public void bind(String jndiName, Object value) {
		final InitialContext initialContext = buildInitialContext();
		final Name name = parseName( jndiName, initialContext );
		try {
			bind( name, value, initialContext );
		}
		finally {
			cleanUp( initialContext );
		}
	}

	private void bind(Name name, Object value, Context context) {
		try {
			LOG.tracef( "Binding : %s", name );
			context.rebind( name, value );
		}
		catch ( Exception initialException ) {
			// We had problems doing a simple bind operation.
			if ( name.size() == 1 ) {
				// if the jndi name had only 1 component there is nothing more we can do...
				throw new JndiException( "Error performing bind [" + name + "]", initialException );
			}

			// Otherwise, there is a good chance this may have been caused by missing intermediate contexts.  So we
			// attempt to create those missing intermediate contexts and bind again
			Context intermediateContextBase = context;
			while ( name.size() > 1 ) {
				final String intermediateContextName = name.get( 0 );

				Context intermediateContext = null;
				try {
					LOG.tracev( "Intermediate lookup: {0}", intermediateContextName );
					intermediateContext = (Context) intermediateContextBase.lookup( intermediateContextName );
				}
				catch ( NameNotFoundException handledBelow ) {
					// ok as we will create it below if not found
				}
				catch ( NamingException e ) {
					throw new JndiException( "Unanticipated error doing intermediate lookup", e );
				}

				if ( intermediateContext != null ) {
					LOG.tracev( "Found intermediate context: {0}", intermediateContextName );
				}
				else {
					LOG.tracev( "Creating sub-context: {0}", intermediateContextName );
					try {
						intermediateContext = intermediateContextBase.createSubcontext( intermediateContextName );
					}
					catch ( NamingException e ) {
						throw new JndiException( "Error creating intermediate context [" + intermediateContextName + "]", e );
					}
				}
				intermediateContextBase = intermediateContext;
				name = name.getSuffix( 1 );
			}
			LOG.tracev( "Binding : {0}", name );
			try {
				intermediateContextBase.rebind( name, value );
			}
			catch ( NamingException e ) {
				throw new JndiException( "Error performing intermediate bind [" + name + "]", e );
			}
		}
		LOG.debugf( "Bound name: %s", name );
	}

	@Override
	public void unbind(String jndiName) {
		final InitialContext initialContext = buildInitialContext();
		final Name name = parseName( jndiName, initialContext );
		try {
			initialContext.unbind( name );
		}
		catch (Exception e) {
			throw new JndiException( "Error performing unbind [" + name + "]", e );
		}
		finally {
			cleanUp( initialContext );
		}
	}

	@Override
	public void addListener(String jndiName, NamespaceChangeListener listener) {
		final InitialContext initialContext = buildInitialContext();
		final Name name = parseName( jndiName, initialContext );
		try {
			( (EventContext) initialContext ).addNamingListener( name, EventContext.OBJECT_SCOPE, listener );
		}
		catch (Exception e) {
			throw new JndiException( "Unable to bind listener to namespace [" + name + "]", e );
		}
		finally {
			cleanUp( initialContext );
		}
	}

}
