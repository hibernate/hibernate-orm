/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.spi;

import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.metamodel.CollectionClassification;

/**
 * Values to use as defaults in the absence of certain mapping information.
 *
 * @implSpec Designed with stacking in mind, such that the defaults can be overridden at
 *           various levels using simple wrapping and delegation. The "global" level is
 *           configuration settings.
 *
 * @author Steve Ebersole
 * @author Gail Badner
 *
 * @since 5.0
 */
public interface MappingDefaults {
	String DEFAULT_IDENTIFIER_COLUMN_NAME = "id";
	String DEFAULT_TENANT_IDENTIFIER_COLUMN_NAME = "tenant_id";
	String DEFAULT_DISCRIMINATOR_COLUMN_NAME = "class";
	String DEFAULT_CASCADE_NAME = "none";
	String DEFAULT_PROPERTY_ACCESS_NAME = "property";
	/**
	 * Identifies the database schema name to use if none specified in the mapping.
	 *
	 * @return The implicit schema name; may be {@code null}
	 *
	 * @see org.hibernate.cfg.MappingSettings#DEFAULT_SCHEMA
	 */
	String getImplicitSchemaName();

	/**
	 * Identifies the database catalog name to use if none specified in the mapping.
	 *
	 * @return The implicit catalog name; may be {@code null}
	 *
	 * @see org.hibernate.cfg.MappingSettings#DEFAULT_CATALOG
	 */
	String getImplicitCatalogName();

	/**
	 * Should all database identifiers encountered in this context be implicitly quoted?
	 *
	 * {@code true} indicates that all identifier encountered within this context should be
	 * quoted.  {@code false} indicates indicates that identifiers within this context are
	 * only quoted if explicitly quoted.
	 *
	 * @return {@code true}/{@code false}
	 */
	boolean shouldImplicitlyQuoteIdentifiers();

	/**
	 * Identifies the column name to use for the identifier column if none specified in
	 * the mapping.
	 *
	 * @return The implicit identifier column name
	 */
	String getImplicitIdColumnName();

	/**
	 * Identifies the column name to use for the tenant identifier column if none is
	 * specified in the mapping.
	 *
	 * @return The implicit tenant identifier column name
	 */
	String getImplicitTenantIdColumnName();

	/**
	 * Identifies the column name to use for the discriminator column if none specified
	 * in the mapping.
	 *
	 * @return The implicit discriminator column name
	 */
	String getImplicitDiscriminatorColumnName();

	/**
	 * Identifies the package name to use if none specified in the mapping.  Really only
	 * pertinent for {@code hbm.xml} mappings.
	 *
	 * @return The implicit package name.
	 */
	String getImplicitPackageName();

	/**
	 * Is auto-importing of entity (short) names enabled?
	 *
	 * @return {@code true} if auto-importing is enabled; {@code false} otherwise.
	 */
	boolean isAutoImportEnabled();

	/**
	 * Identifies the cascade style to apply to associations if none specified in the mapping.
	 *
	 * @return The implicit cascade style
	 */
	String getImplicitCascadeStyleName();

	/**
	 * The default {@link org.hibernate.property.access.spi.PropertyAccessStrategy} to use if none specified in the mapping.
	 *
	 * @see org.hibernate.property.access.spi.PropertyAccessStrategy
	 */
	String getImplicitPropertyAccessorName();

	/**
	 * Identifies whether singular associations (many-to-one, one-to-one) are lazy
	 * by default if not specified in the mapping.
	 *
	 * @return The implicit association laziness
	 */
	boolean areEntitiesImplicitlyLazy();

	/**
	 * Identifies whether plural attributes are lazy by default if not specified in the mapping.
	 *
	 * @return The implicit association laziness
	 */
	boolean areCollectionsImplicitlyLazy();

	/**
	 * The cache access type to use if none is specified
	 *
	 * @see org.hibernate.cfg.AvailableSettings#DEFAULT_CACHE_CONCURRENCY_STRATEGY
	 */
	AccessType getImplicitCacheAccessType();

	/**
	 * Collection semantics to be applied to {@link java.util.List} attributes
	 * with no explicit configuration
	 *
	 * @see org.hibernate.cfg.AvailableSettings#DEFAULT_LIST_SEMANTICS
	 */
	CollectionClassification getImplicitListClassification();
}
