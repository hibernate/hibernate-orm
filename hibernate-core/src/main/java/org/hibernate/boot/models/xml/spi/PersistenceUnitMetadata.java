/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models.xml.spi;

import java.util.EnumSet;

import org.hibernate.annotations.CascadeType;
import org.hibernate.property.access.spi.PropertyAccessStrategy;

import jakarta.persistence.AccessType;

/**
 * Aggregator of information from {@code entity-mappings/persistence-unit-metadata}
 * and {@code entity-mappings/persistence-unit-metadata/persistence-unit-defaults}
 * across all mapping XML files in the persistence-unit.
 *
 * @author Steve Ebersole
 */
public interface PersistenceUnitMetadata {
	/**
	 * Whether XML mappings are complete for the entire persistent unit.
	 * See {@code entity-mappings/persistence-unit-metadata/xml-mapping-metadata-complete}
	 */
	boolean areXmlMappingsComplete();

	/**
	 * Default schema in effect for the entire persistent unit.
	 * See {@code entity-mappings/persistence-unit-metadata/persistence-unit-defaults/schema}
	 */
	String getDefaultSchema();

	/**
	 * Default catalog in effect for the entire persistent unit.
	 * See {@code entity-mappings/persistence-unit-metadata/persistence-unit-defaults/catalog}
	 */
	String getDefaultCatalog();

	/**
	 * Default AccessType in effect for the entire persistence unit.
	 * See {@code entity-mappings/persistence-unit-metadata/persistence-unit-defaults/access}
	 */
	AccessType getAccessType();

	/**
	 * Name of the default {@link PropertyAccessStrategy} in effect for the entire persistence unit
	 * See {@code entity-mappings/persistence-unit-metadata/persistence-unit-defaults/default-access}
	 */
	String getDefaultAccessStrategyName();

	/**
	 * Cascades to apply by default for this persistence unit
	 * See {@code entity-mappings/persistence-unit-metadata/persistence-unit-defaults/default-cascade}
	 */
	EnumSet<CascadeType> getDefaultCascadeTypes();

	/**
	 * Whether to quote all database identifiers in the persistence unit
	 * See {@code entity-mappings/persistence-unit-metadata/persistence-unit-defaults/delimited-identifiers}
	 */
	boolean useQuotedIdentifiers();
}
