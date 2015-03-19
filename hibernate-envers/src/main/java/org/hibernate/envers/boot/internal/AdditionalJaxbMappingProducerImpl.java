/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
package org.hibernate.envers.boot.internal;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import javax.xml.transform.dom.DOMSource;

import org.hibernate.HibernateException;
import org.hibernate.boot.archive.internal.ByteArrayInputStreamAccess;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.model.source.internal.hbm.MappingDocument;
import org.hibernate.boot.spi.AdditionalJaxbMappingProducer;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.envers.configuration.internal.MappingCollector;
import org.hibernate.service.ServiceRegistry;

import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import org.dom4j.DocumentException;
import org.dom4j.io.DOMWriter;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.dom4j.Document;

/**
 * @author Steve Ebersole
 */
public class AdditionalJaxbMappingProducerImpl implements AdditionalJaxbMappingProducer {
	private static final Logger log = Logger.getLogger( AdditionalJaxbMappingProducerImpl.class );

	@Override
	public Collection<MappingDocument> produceAdditionalMappings(
			final MetadataImplementor metadata,
			IndexView jandexIndex,
			final MappingBinder mappingBinder,
			final MetadataBuildingContext buildingContext) {
		final ServiceRegistry serviceRegistry = metadata.getMetadataBuildingOptions().getServiceRegistry();
		final EnversService enversService = serviceRegistry.getService( EnversService.class );

		if ( !enversService.isEnabled() ) {
			// short-circuit if envers integration has been disabled.
			return Collections.emptyList();
		}

		final ArrayList<MappingDocument> additionalMappingDocuments = new ArrayList<MappingDocument>();

		// atm we do not have distinct origin info for envers
		final Origin origin = new Origin( SourceType.OTHER, "envers" );
//		final DOMWriter writer = new DOMWriter();

		final MappingCollector mappingCollector = new MappingCollector() {
			@Override
			public void addDocument(Document document) throws DocumentException {
				dump( document );

				// while the commented-out code here is more efficient (well, understanding that
				// this whole process is un-efficient)  it leads to un-decipherable messages when
				// we get mapping mapping errors from envers output.
//				final DOMSource domSource = new DOMSource( writer.write( document ) );
//				domSource.setSystemId( "envers" );
//				final Binding jaxbBinding = mappingBinder.bind( domSource, origin );

				// this form at least allows us to get better error messages
				final ByteArrayOutputStream baos = new ByteArrayOutputStream();
				final Writer w = new PrintWriter( baos );
				try {
					final XMLWriter xw = new XMLWriter( w, new OutputFormat( " ", true ) );
					xw.write( document );
					w.flush();
				}
				catch (IOException e) {
					throw new HibernateException( "Unable to bind Envers-generated XML", e );
				}

				ByteArrayInputStream bais = new ByteArrayInputStream( baos.toByteArray() );
				BufferedInputStream bis = new BufferedInputStream( bais );
				final Binding jaxbBinding = mappingBinder.bind( bis, origin );

				final JaxbHbmHibernateMapping jaxbRoot = (JaxbHbmHibernateMapping) jaxbBinding.getRoot();
				additionalMappingDocuments.add( new MappingDocument( jaxbRoot, origin, buildingContext ) );
			}
		};

		enversService.initialize( metadata, mappingCollector );

		return additionalMappingDocuments;
	}

	private static void dump(Document document) {
		if ( !log.isTraceEnabled() ) {
			return;
		}

		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final Writer w = new PrintWriter( baos );

		try {
			final XMLWriter xw = new XMLWriter( w, new OutputFormat( " ", true ) );
			xw.write( document );
			w.flush();
		}
		catch (IOException e1) {
			e1.printStackTrace();
		}

		log.tracef( "Envers-generate entity mapping -----------------------------\n%s", baos.toString() );
		log.trace( "------------------------------------------------------------" );
	}
}
