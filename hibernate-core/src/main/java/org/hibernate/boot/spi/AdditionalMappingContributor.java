/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.spi;

import org.hibernate.Incubating;
import org.hibernate.boot.jaxb.internal.MappingBinder;

/**
 * Contract allowing pluggable contributions of additional
 * mapping objects.
 *
 * Resolvable as a {@linkplain java.util.ServiceLoader Java service}.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface AdditionalMappingContributor {
	/**
	 * The name of this contributor.
	 */
	default String getContributorName() {
		return null;
	}

	/**
	 * Contribute the additional mappings
	 *
	 * @param contributions Collector of the contributions
	 * @param metadata Current (live) metadata
	 * @param jaxbBinder JAXB binding support for XML documents.  May be {@code null}
	 * if XML processing is {@linkplain MetadataBuildingOptions#isXmlMappingEnabled() disabled}
	 * @param buildingContext Access to useful contextual details
	 */
	void contribute(
			AdditionalMappingContributions contributions,
			InFlightMetadataCollector metadata,
			MappingBinder jaxbBinder,
			MetadataBuildingContext buildingContext);
}
