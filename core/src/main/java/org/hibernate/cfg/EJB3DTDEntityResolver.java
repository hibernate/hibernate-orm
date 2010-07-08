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
 */
package org.hibernate.cfg;

import java.io.InputStream;

import org.hibernate.util.DTDEntityResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolve JPA xsd files locally
 *
 * @author Emmanuel Bernard
 */
public class EJB3DTDEntityResolver extends DTDEntityResolver {
	public static final EntityResolver INSTANCE = new EJB3DTDEntityResolver();

	private final Logger log = LoggerFactory.getLogger( EJB3DTDEntityResolver.class );

	boolean resolved = false;

	/**
	 * Persistence.xml has been resolved locally
	 * @return true if it has
	 */
	public boolean isResolved() {
		return resolved;
	}

	public InputSource resolveEntity(String publicId, String systemId) {
		log.trace("Resolving XML entity {} : {}", publicId, systemId);
		InputSource is = super.resolveEntity( publicId, systemId );
		if ( is == null ) {
			if ( systemId != null ) {
				if ( systemId.endsWith( "orm_1_0.xsd" ) ) {
					InputStream dtdStream = getStreamFromClasspath( "orm_1_0.xsd" );
					final InputSource source = buildInputSource( publicId, systemId, dtdStream, false );
					if (source != null) return source;
				}
				else if ( systemId.endsWith( "orm_2_0.xsd" ) ) {
					InputStream dtdStream = getStreamFromClasspath( "orm_2_0.xsd" );
					final InputSource source = buildInputSource( publicId, systemId, dtdStream, false );
					if (source != null) return source;
				}
				else if ( systemId.endsWith( "persistence_1_0.xsd" ) ) {
					InputStream dtdStream = getStreamFromClasspath( "persistence_1_0.xsd" );
					final InputSource source = buildInputSource( publicId, systemId, dtdStream, true );
					if (source != null) return source;
				}
				else if ( systemId.endsWith( "persistence_2_0.xsd" ) ) {
					InputStream dtdStream = getStreamFromClasspath( "persistence_2_0.xsd" );
					final InputSource source = buildInputSource( publicId, systemId, dtdStream, true );
					if (source != null) return source;
				}
			}
		}
		else {
			resolved = true;
			return is;
		}
		//use the default behavior
		return null;
	}

	private InputSource buildInputSource(String publicId, String systemId, InputStream dtdStream, boolean resolved) {
		if ( dtdStream == null ) {
			log.trace( "unable to locate [{}] on classpath", systemId );
			return null;
		}
		else {
			log.trace( "located [{}] in classpath", systemId );
			InputSource source = new InputSource( dtdStream );
			source.setPublicId( publicId );
			source.setSystemId( systemId );
			this.resolved = resolved;
			return source;
		}
	}

	private InputStream getStreamFromClasspath(String fileName) {
		log.trace(
							"recognized JPA ORM namespace; attempting to resolve on classpath under org/hibernate/ejb"
		);
		String path = "org/hibernate/ejb/" + fileName;
		InputStream dtdStream = resolveInHibernateNamespace( path );
		return dtdStream;
	}
}
