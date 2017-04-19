/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi;

import org.jboss.jandex.IndexView;

/**
 * Contract for contributing to Metadata (InFlightMetadataCollector).
 *
 * This hook occurs just after all processing of all {@link org.hibernate.boot.MetadataSources},
 * and just before {@link org.hibernate.boot.spi.AdditionalJaxbMappingProducer}.
 *
 * @author Steve Ebersole
 *
 * @since 5.0
 */
public interface MetadataContributor {
	/**
	 * Perform the contributions.
	 *
	 * @param metadataCollector The metadata collector, representing the in-flight metadata being built
	 * @param jandexIndex The Jandex index
	 */
	public void contribute(InFlightMetadataCollector metadataCollector, IndexView jandexIndex);
}
