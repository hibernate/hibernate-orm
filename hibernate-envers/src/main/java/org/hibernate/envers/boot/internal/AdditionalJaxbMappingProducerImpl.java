/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.internal;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.hibernate.HibernateException;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.model.source.internal.hbm.MappingDocument;
import org.hibernate.boot.spi.AdditionalJaxbMappingProducer;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.envers.boot.EnversBootLogger;
import org.hibernate.envers.configuration.internal.MappingCollector;
import org.hibernate.service.ServiceRegistry;

import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import static org.hibernate.cfg.AvailableSettings.XML_MAPPING_ENABLED;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

/**
 * @author Steve Ebersole
 * @author Chris Cranford
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

		if ( !metadataBuildingOptions.isXmlMappingEnabled() ) {
			throw new HibernateException( "Hibernate Envers currently requires XML mapping to be enabled."
					+ " Please don't disable setting `" + XML_MAPPING_ENABLED
					+ "`; alternatively disable Hibernate Envers." );
		}

		final ArrayList<MappingDocument> additionalMappingDocuments = new ArrayList<>();

		// atm we do not have distinct origin info for envers
		final Origin origin = new Origin( SourceType.OTHER, "envers" );

		final MappingCollector mappingCollector = new MappingCollector() {
			@Override
			public void addDocument(JaxbHbmHibernateMapping mapping) {
				log.infof( "Adding JAXB document mapping" );
				try {
					JAXBContext context = JAXBContext.newInstance( JaxbHbmHibernateMapping.class );
					Marshaller marshaller = context.createMarshaller();
					marshaller.setProperty ( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );

					StringWriter sw = new StringWriter();
					marshaller.marshal( mapping, sw );

					EnversBootLogger.BOOT_LOGGER.jaxbContribution( sw.toString() );
					log.trace( "------------------------------------------------------------" );
				}
				catch (JAXBException e) {
					throw new RuntimeException( "Error dumping enhanced class", e );
				}
				additionalMappingDocuments.add( new MappingDocument( "envers", mapping, origin, buildingContext ) );
			}
		};

		enversService.initialize( metadata, mappingCollector );

		return additionalMappingDocuments;
	}
}
