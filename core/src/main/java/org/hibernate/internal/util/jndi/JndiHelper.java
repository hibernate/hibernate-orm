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
package org.hibernate.internal.util.jndi;

import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.cfg.Environment;

public final class JndiHelper {
	private static final Logger log = LoggerFactory.getLogger( JndiHelper.class );

	private JndiHelper() {
	}

	/**
	 * Given a hodge-podge of properties, extract out the ones relevant for JNDI interaction.
	 *
	 * @param properties
	 * @return
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

	/**
	 * Do a JNDI lookup.  Mainly we are handling {@link NamingException}
	 *
	 * @param jndiName The namespace of the object to locate
	 * @param context The context in which to resolve the namespace.
	 *
	 * @return The located object; may be null.
	 *
	 * @throws JndiException if a {@link NamingException} occurs
	 */
	public static Object locate(String jndiName, Context context) {
		try {
			return context.lookup( jndiName );
		}
		catch ( NamingException e ) {
			throw new JndiException( "Unable to lookup JNDI name [" + jndiName + "]", e );
		}
	}

	/**
	 * Bind val to name in ctx, and make sure that all intermediate contexts exist.
	 *
	 * @param ctx the root context
	 * @param name the name as a string
	 * @param val the object to be bound
	 *
	 * @throws JndiException if a {@link NamingException} occurs
	 */
	public static void bind(String jndiName, Object value, Context context) {
		try {
			log.trace( "binding : " + jndiName );
			context.rebind( jndiName, value );
		}
		catch ( Exception initialException ) {
			// We had problems doing a simple bind operation.  This could very well be caused by missing intermediate
			// contexts, so we attempt to create those intermmediate contexts and bind again
			Name n = tokenizeName( jndiName, context );
			Context intermediateContextBase = context;
			while ( n.size() > 1 ) {
				final String intermediateContextName = n.get( 0 );

				Context intermediateContext = null;
				try {
					log.trace( "intermediate lookup: " + intermediateContextName );
					intermediateContext = (Context) intermediateContextBase.lookup( intermediateContextName );
				}
				catch ( NameNotFoundException handledBelow ) {
					// ok as we will create it below if not found
				}
				catch ( NamingException e ) {
					throw new JndiException( "Unaniticipated error doing intermediate lookup", e );
				}

				if ( intermediateContext != null ) {
					log.trace( "Found interediate context: " + intermediateContextName );
				}
				else {
					log.trace( "Creating subcontext: " + intermediateContextName );
					try {
						intermediateContext = intermediateContextBase.createSubcontext( intermediateContextName );
					}
					catch ( NamingException e ) {
						throw new JndiException( "Error creating intermediate context [" + intermediateContextName + "]", e );
					}
				}
				intermediateContextBase = intermediateContext;
				n = n.getSuffix( 1 );
			}
			log.trace( "binding: " + n );
			try {
				intermediateContextBase.rebind( n, value );
			}
			catch ( NamingException e ) {
				throw new JndiException( "Error performing intermediate bind [" + n + "]", e );
			}
		}
		log.debug( "Bound name: " + jndiName );
	}

	private static Name tokenizeName(String jndiName, Context context) {
		try {
			return context.getNameParser( "" ).parse( jndiName );
		}
		catch ( NamingException e ) {
			throw new JndiException( "Unable to tokenize JNDI name [" + jndiName + "]", e );
		}
	}





	// todo : remove these once we get the services in place and integrated into the SessionFactory






	public static InitialContext getInitialContext(Properties props) throws NamingException {

		Hashtable hash = extractJndiProperties(props);
		log.info("JNDI InitialContext properties:" + hash);
		try {
			return hash.size()==0 ?
					new InitialContext() :
					new InitialContext(hash);
		}
		catch (NamingException e) {
			log.error("Could not obtain initial context", e);
			throw e;
		}
	}

	/**
	 * Bind val to name in ctx, and make sure that all intermediate contexts exist.
	 *
	 * @param ctx the root context
	 * @param name the name as a string
	 * @param val the object to be bound
	 * @throws NamingException
	 */
	public static void bind(Context ctx, String name, Object val) throws NamingException {
		try {
			log.trace("binding: " + name);
			ctx.rebind(name, val);
		}
		catch (Exception e) {
			Name n = ctx.getNameParser("").parse(name);
			while ( n.size() > 1 ) {
				String ctxName = n.get(0);

				Context subctx=null;
				try {
					log.trace("lookup: " + ctxName);
					subctx = (Context) ctx.lookup(ctxName);
				}
				catch (NameNotFoundException nfe) {}

				if (subctx!=null) {
					log.debug("Found subcontext: " + ctxName);
					ctx = subctx;
				}
				else {
					log.info("Creating subcontext: " + ctxName);
					ctx = ctx.createSubcontext(ctxName);
				}
				n = n.getSuffix(1);
			}
			log.trace("binding: " + n);
			ctx.rebind(n, val);
		}
		log.debug("Bound name: " + name);
	}

}

