/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.spi;

import java.util.List;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.xml.spi.BindResult;

import org.jboss.jandex.IndexView;

/**
 * Contract for integrations that wish to provide additional mappings (in the form of
 * {@link org.hibernate.xml.spi.BindResult}.  This hook is performed after all other
 * mappings, annotations, etc have completed processing.
 *
 * @author Steve Ebersole
 */
public interface AdditionalJaxbRootProducer {
	/**
	 * Produce and return the list of additional mappings to be processed.
	 *
	 * @param metadataCollector The metadata (for access to binding information).
	 * @param context The context.
	 *
	 * @return List of additional mappings
	 *
	 * @see AdditionalJaxbRootProducerContext
	 */
	public List<BindResult> produceRoots(
			InFlightMetadataCollector metadataCollector,
			AdditionalJaxbRootProducerContext context);

	public interface AdditionalJaxbRootProducerContext {

		/**
		 * Gets the Jandex annotation index.
		 *
		 * @return the Jandex annotation index
		 */
		public IndexView getJandexIndex();

		/**
		 * Gets the service registry.
		 *
		 * @return The service registry.
		 */
		public StandardServiceRegistry getServiceRegistry();
	}
}
