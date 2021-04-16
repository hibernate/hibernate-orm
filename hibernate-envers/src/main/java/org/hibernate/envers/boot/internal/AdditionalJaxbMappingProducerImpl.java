/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.internal;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.hibernate.HibernateException;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.jaxb.spi.XmlMappingOptions;
import org.hibernate.boot.model.source.internal.hbm.MappingDocument;
import org.hibernate.boot.spi.AdditionalJaxbMappingProducer;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.envers.boot.EnversBootLogger;
import org.hibernate.envers.configuration.internal.MappingCollector;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;

import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import static org.hibernate.cfg.AvailableSettings.XML_MAPPING_ENABLED;

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
		MetadataBuildingOptions metadataBuildingOptions = metadata.getMetadataBuildingOptions();
		final ServiceRegistry serviceRegistry = metadataBuildingOptions.getServiceRegistry();
		final EnversService enversService = serviceRegistry.getService( EnversService.class );

		if ( !enversService.isEnabled() ) {
			// short-circuit if envers integration has been disabled.
			return Collections.emptyList();
		}

		if ( !metadataBuildingOptions.getXmlMappingOptions().isEnabled() ) {
			throw new HibernateException( "Hibernate Envers currently requires XML mapping to be enabled."
					+ " Please don't disable setting `" + XML_MAPPING_ENABLED
					+ "`; alternatively disable Hibernate Envers." );
		}

		final ArrayList<MappingDocument> additionalMappingDocuments = new ArrayList<>();

		// atm we do not have distinct origin info for envers
		final Origin origin = new Origin( SourceType.OTHER, "envers" );

		final MappingCollector mappingCollector = (document) -> {
			logXml( document );

			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				final Writer w = new BufferedWriter( new OutputStreamWriter( baos, "UTF-8" ) );
				final XMLWriter xw = new XMLWriter( w, new OutputFormat( " ", true ) );
				xw.write( document );
				w.flush();
			}
			catch (IOException e) {
				throw new HibernateException( "Unable to bind Envers-generated XML", e );
			}

			ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream( baos.toByteArray() );
			BufferedInputStream bufferedInputStream = new BufferedInputStream( byteArrayInputStream );
			final Binding<JaxbHbmHibernateMapping> jaxbBinding = mappingBinder.bind( bufferedInputStream, origin );

			final JaxbHbmHibernateMapping jaxbRoot = jaxbBinding.getRoot();
			additionalMappingDocuments.add( new MappingDocument( "envers", jaxbRoot, origin, buildingContext ) );
		};

		enversService.initialize( metadata, mappingCollector );

		return additionalMappingDocuments;
	}

	private static void logXml(Document document) {
		if ( ! EnversBootLogger.DEBUG_ENABLED ) {
			return;
		}

		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		final Writer w = new PrintWriter( outputStream );

		try {
			final XMLWriter xw = new XMLWriter( w, new OutputFormat( " ", true ) );
			xw.write( document );
			w.flush();
		}
		catch (IOException e1) {
			throw new RuntimeException( "Error dumping enhanced class", e1 );
		}

		EnversBootLogger.BOOT_LOGGER.jaxbContribution( outputStream.toString() );

		log.tracef( "Envers-generate entity mapping -----------------------------\n%s", outputStream.toString() );
		log.trace( "------------------------------------------------------------" );
	}
}
