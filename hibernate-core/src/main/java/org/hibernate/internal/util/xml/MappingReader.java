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
package org.hibernate.internal.util.xml;

import java.io.StringReader;

import org.dom4j.Document;
import org.dom4j.io.SAXReader;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.jboss.logging.Logger;

import org.hibernate.InvalidMappingException;
import org.hibernate.internal.CoreMessageLogger;

/**
 * Handles reading mapping documents, both {@code hbm} and {@code orm} varieties.
 *
 * @author Steve Ebersole
 */
public class MappingReader {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			MappingReader.class.getName()
	);

	public static final MappingReader INSTANCE = new MappingReader();

	/**
	 * Disallow direct instantiation.
	 * <p/>
	 * Eventually we perhaps need to have this configurable by the "configuration" and simply reference it
	 * from there (registry).  This would allow, for example, injection of the entity resolver to use as
	 * instance state.
	 */
	private MappingReader() {
	}

	public XmlDocument readMappingDocument(EntityResolver entityResolver, InputSource source, Origin origin) {
		// IMPL NOTE : this is the legacy logic as pulled from the old AnnotationConfiguration code

		Exception failure;

		ErrorLogger errorHandler = new ErrorLogger();

		SAXReader saxReader = new SAXReader();
		saxReader.setEntityResolver( entityResolver );
		saxReader.setErrorHandler( errorHandler );
		saxReader.setMergeAdjacentText( true );
		saxReader.setValidation( true );

		Document document = null;
		try {
			// first try with orm 2.1 xsd validation
			setValidationFor( saxReader, "orm_2_1.xsd" );
			document = saxReader.read( source );
			if ( errorHandler.hasErrors() ) {
				throw errorHandler.getErrors().get( 0 );
			}
			return new XmlDocumentImpl( document, origin.getType(), origin.getName() );
		}
		catch ( Exception e ) {
			if ( LOG.isDebugEnabled() ) {
				LOG.debugf( "Problem parsing XML using orm 2.1 xsd, trying 2.0 xsd : %s", e.getMessage() );
			}
			failure = e;
			errorHandler.reset();

			if ( document != null ) {
				// next try with orm 2.0 xsd validation
				try {
					setValidationFor( saxReader, "orm_2_0.xsd" );
					document = saxReader.read( new StringReader( document.asXML() ) );
					if ( errorHandler.hasErrors() ) {
						errorHandler.logErrors();
						throw errorHandler.getErrors().get( 0 );
					}
					return new XmlDocumentImpl( document, origin.getType(), origin.getName() );
				}
				catch ( Exception e2 ) {
					if ( LOG.isDebugEnabled() ) {
						LOG.debugf( "Problem parsing XML using orm 2.0 xsd, trying 1.0 xsd : %s", e2.getMessage() );
					}
					errorHandler.reset();

					if ( document != null ) {
						// next try with orm 1.0 xsd validation
						try {
							setValidationFor( saxReader, "orm_1_0.xsd" );
							document = saxReader.read( new StringReader( document.asXML() ) );
							if ( errorHandler.hasErrors() ) {
								errorHandler.logErrors();
								throw errorHandler.getErrors().get( 0 );
							}
							return new XmlDocumentImpl( document, origin.getType(), origin.getName() );
						}
						catch ( Exception e3 ) {
							if ( LOG.isDebugEnabled() ) {
								LOG.debugf( "Problem parsing XML using orm 1.0 xsd : %s", e3.getMessage() );
							}
						}
					}
				}
			}
		}
		throw new InvalidMappingException( "Unable to read XML", origin.getType(), origin.getName(), failure );
	}

	private void setValidationFor(SAXReader saxReader, String xsd) {
		try {
			saxReader.setFeature( "http://apache.org/xml/features/validation/schema", true );
			// saxReader.setFeature( "http://apache.org/xml/features/validation/dynamic", true );
			saxReader.setProperty(
					"http://apache.org/xml/properties/schema/external-schemaLocation",
					"http://java.sun.com/xml/ns/persistence/orm " + xsd
			);
		}
		catch ( SAXException e ) {
			saxReader.setValidation( false );
		}
	}

}
