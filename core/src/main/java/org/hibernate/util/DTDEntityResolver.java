/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.util;

import java.io.InputStream;
import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * An {@link EntityResolver} implementation which attempts to resolve
 * various systemId URLs to local classpath lookups<ol>
 * <li>Any systemId URL beginning with <tt>http://www.hibernate.org/dtd/</tt> is
 * searched for as a classpath resource in the classloader which loaded the
 * Hibernate classes.</li>
 * <li>Any systemId URL using <tt>classpath</tt> as the scheme (i.e. starting
 * with <tt>classpath://</tt> is searched for as a classpath resource using first
 * the current thread context classloader and then the classloader which loaded
 * the Hibernate classes.
 * </ol>
 * <p/>
 * Any entity references which cannot be resolved in relation to the above
 * rules result in returning null, which should force the SAX reader to
 * handle the entity reference in its default manner.
 *
 * @author Markus Meissner
 * @author Gavin King
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public class DTDEntityResolver implements EntityResolver, Serializable {

	private static final Logger log = LoggerFactory.getLogger( DTDEntityResolver.class );

	private static final String HIBERNATE_NAMESPACE = "http://www.hibernate.org/dtd/";
	private static final String OLD_HIBERNATE_NAMESPACE = "http://hibernate.sourceforge.net/";
	private static final String USER_NAMESPACE = "classpath://";

	public InputSource resolveEntity(String publicId, String systemId) {
		InputSource source = null; // returning null triggers default behavior
		if ( systemId != null ) {
			log.debug( "trying to resolve system-id [" + systemId + "]" );
			if ( systemId.startsWith( HIBERNATE_NAMESPACE ) ) {
				log.debug( "recognized hibernate namespace; attempting to resolve on classpath under org/hibernate/" );
				source = resolveOnClassPath( publicId, systemId, HIBERNATE_NAMESPACE );
			}
			else if ( systemId.startsWith( OLD_HIBERNATE_NAMESPACE ) ) {
				log.warn(
						"recognized obsolete hibernate namespace " + OLD_HIBERNATE_NAMESPACE + ". Use namespace "
								+ HIBERNATE_NAMESPACE + " instead. Refer to Hibernate 3.6 Migration Guide!"
				);
				log.debug( "attempting to resolve on classpath under org/hibernate/" );
				source = resolveOnClassPath( publicId, systemId, OLD_HIBERNATE_NAMESPACE );
			}
			else if ( systemId.startsWith( USER_NAMESPACE ) ) {
				log.debug( "recognized local namespace; attempting to resolve on classpath" );
				String path = systemId.substring( USER_NAMESPACE.length() );
				InputStream stream = resolveInLocalNamespace( path );
				if ( stream == null ) {
					log.debug( "unable to locate [" + systemId + "] on classpath" );
				}
				else {
					log.debug( "located [" + systemId + "] in classpath" );
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
			log.debug( "unable to locate [" + systemId + "] on classpath" );
			if ( systemId.substring( namespace.length() ).indexOf( "2.0" ) > -1 ) {
				log.error( "Don't use old DTDs, read the Hibernate 3.x Migration Guide!" );
			}
		}
		else {
			log.debug( "located [" + systemId + "] in classpath" );
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
