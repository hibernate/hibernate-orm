/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.xml;

import java.io.InputStream;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ConfigHelper;

import org.jboss.logging.Logger;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * An {@link EntityResolver} implementation which attempts to resolve
 * various systemId URLs to local classpath look ups<ol>
 * <li>Any systemId URL beginning with {@code http://www.hibernate.org/dtd/} is
 * searched for as a classpath resource in the classloader which loaded the
 * Hibernate classes.</li>
 * <li>Any systemId URL using {@code classpath} as the scheme (i.e. starting
 * with {@code classpath://} is searched for as a classpath resource using first
 * the current thread context classloader and then the classloader which loaded
 * the Hibernate classes.
 * </ol>
 * <p>
 * Any entity references which cannot be resolved in relation to the above
 * rules result in returning null, which should force the SAX reader to
 * handle the entity reference in its default manner.
 *
 * @author Markus Meissner
 * @author Gavin King
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 *
 * @deprecated Hibernate now uses StAX for XML processing and the role of this class is served
 * now by {@link org.hibernate.boot.jaxb.internal.stax.LocalXmlResourceResolver}
 */
@Deprecated
public class DTDEntityResolver implements EntityResolver, Serializable {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( MethodHandles.lookup(), CoreMessageLogger.class, DTDEntityResolver.class.getName() );

	private static final String HIBERNATE_NAMESPACE = "http://www.hibernate.org/dtd/";
	private static final String OLD_HIBERNATE_NAMESPACE = "http://hibernate.sourceforge.net/";
	private static final String USER_NAMESPACE = "classpath://";

	public InputSource resolveEntity(String publicId, String systemId) {
		InputSource source = null; // returning null triggers default behavior
		if ( systemId != null ) {
			LOG.debugf( "Trying to resolve system-id [%s]", systemId );
			if ( systemId.startsWith( HIBERNATE_NAMESPACE ) ) {
				LOG.debug( "Recognized hibernate namespace; attempting to resolve on classpath under org/hibernate/" );
				source = resolveOnClassPath( publicId, systemId, HIBERNATE_NAMESPACE );
			}
			else if ( systemId.startsWith( OLD_HIBERNATE_NAMESPACE ) ) {
				LOG.recognizedObsoleteHibernateNamespace( OLD_HIBERNATE_NAMESPACE, HIBERNATE_NAMESPACE );
				LOG.debug( "Attempting to resolve on classpath under org/hibernate/" );
				source = resolveOnClassPath( publicId, systemId, OLD_HIBERNATE_NAMESPACE );
			}
			else if ( systemId.startsWith( USER_NAMESPACE ) ) {
				LOG.debug( "Recognized local namespace; attempting to resolve on classpath" );
				String path = systemId.substring( USER_NAMESPACE.length() );
				InputStream stream = resolveInLocalNamespace( path );
				if ( stream == null ) {
					LOG.debugf( "Unable to locate [%s] on classpath", systemId );
				}
				else {
					LOG.debugf( "Located [%s] on classpath", systemId );
					source = new InputSource( stream );
					source.setPublicId( publicId );
					source.setSystemId( systemId );
				}
			}
		}
		return source;
	}

	private InputSource resolveOnClassPath(String publicId, String systemId, String namespace) {
		InputSource source = null;
		String path = "org/hibernate/" + systemId.substring( namespace.length() );
		InputStream dtdStream = resolveInHibernateNamespace( path );
		if ( dtdStream == null ) {
			LOG.debugf( "Unable to locate [%s] on classpath", systemId );
			if ( systemId.substring( namespace.length() ).contains("2.0") ) {
				LOG.usingOldDtd();
			}
		}
		else {
			LOG.debugf( "Located [%s] on classpath", systemId );
			source = new InputSource( dtdStream );
			source.setPublicId( publicId );
			source.setSystemId( systemId );
		}
		return source;
	}

	protected InputStream resolveInHibernateNamespace(String path) {
		return this.getClass().getClassLoader().getResourceAsStream( path );
	}

	protected InputStream resolveInLocalNamespace(String path) {
		try {
			return ConfigHelper.getUserResourceAsStream( path );
		}
		catch ( Throwable t ) {
			return null;
		}
	}
}
