/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.spi;

import java.util.EnumSet;

import org.hibernate.annotations.CascadeType;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.models.xml.spi.PersistenceUnitMetadata;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.metamodel.CollectionClassification;

/**
 * Defaults which are in effect for each mapping.
 * A combination of global settings and XML-specific settings
 *
 * @see MappingDefaults
 * @see PersistenceUnitMetadata
 * @see JaxbEntityMappingsImpl
 *
 * @author Steve Ebersole
 */
public interface EffectiveMappingDefaults {
	String DEFAULT_IDENTIFIER_COLUMN_NAME = "id";
	String DEFAULT_TENANT_IDENTIFIER_COLUMN_NAME = "tenant_id";
	String DEFAULT_DISCRIMINATOR_COLUMN_NAME = "class";

	/**
	 * The default database catalog name to use
	 *
	 * @see MappingDefaults#getImplicitCatalogName()
	 * @see PersistenceUnitMetadata#getDefaultCatalog()
	 */
	String getDefaultCatalogName();

	/**
	 * The default database schema name to use
	 *
	 * @see MappingDefaults#getImplicitCatalogName()
	 * @see PersistenceUnitMetadata#getDefaultCatalog()
	 */
	String getDefaultSchemaName();

	/**
	 * Whether database identifiers be quoted by default
	 *
	 * @see MappingDefaults#shouldImplicitlyQuoteIdentifiers()
	 * @see PersistenceUnitMetadata#useQuotedIdentifiers()
	 *
	 */
	boolean isDefaultQuoteIdentifiers();

	/**
	 * The default column name to use for the identifier column if none specified in
	 * the mapping.
	 * Falls back to {@value #DEFAULT_IDENTIFIER_COLUMN_NAME}.
	 */
	String getDefaultIdColumnName();

	/**
	 * The default column name to use for the discriminator column if none specified
	 * in the mapping.
	 * Falls back to {@value #DEFAULT_DISCRIMINATOR_COLUMN_NAME}.
	 */
	String getDefaultDiscriminatorColumnName();

	/**
	 * The default column name to use for the tenant identifier column if none is
	 * specified in the mapping.
	 * Falls back to {@value #DEFAULT_TENANT_IDENTIFIER_COLUMN_NAME}.
	 */
	String getDefaultTenantIdColumnName();

	/**
	 * The default package name to use if none specified in XML mappings.
	 * Useful when all (or most) domain classes are in a single package.
	 *
	 * @see MappingDefaults#getImplicitPackageName()
	 * @see JaxbEntityMappingsImpl#getPackage()
	 */
	String getDefaultPackageName();

	/**
	 * Whether auto-importing of entity names (for queries) is enabled.
	 *
	 * @see MappingDefaults#isAutoImportEnabled()
	 * @see JaxbEntityMappingsImpl#isAutoImport()
	 */
	boolean isDefaultAutoImport();

	/**
	 * The default cascade styles to apply to associations.
	 *
	 * @see MappingDefaults#getImplicitCascadeStyleName()
	 * @see PersistenceUnitMetadata#getDefaultCascadeTypes()
	 * @see JaxbEntityMappingsImpl#getDefaultCascade()
	 */
	EnumSet<CascadeType> getDefaultCascadeTypes();

	/**
	 * The default AccessType to use if not specified in the mapping.
	 *
	 * @see PersistenceUnitMetadata#getAccessType()
	 */
	jakarta.persistence.AccessType getDefaultPropertyAccessType();

	/**
	 * The default {@link org.hibernate.property.access.spi.PropertyAccessStrategy} name to use if
	 * none specified in the mapping.
	 *
	 * @see #getDefaultPropertyAccessType
	 * @see MappingDefaults#getImplicitPropertyAccessorName()
	 * @see JaxbEntityMappingsImpl#getAttributeAccessor()
	 */
	String getDefaultAccessStrategyName();

	/**
	 * Whether singular associations (many-to-one, one-to-one) are lazy by default if not specified in the mapping.
	 *
	 * @see MappingDefaults#areEntitiesImplicitlyLazy()
	 * @see JaxbEntityMappingsImpl#isDefaultLazy()
	 */
	boolean isDefaultEntityLaziness();

	/**
	 * Whether plural attributes are lazy by default if not specified in the mapping.
	 *
	 * @see MappingDefaults#areCollectionsImplicitlyLazy() ()
	 * @see JaxbEntityMappingsImpl#isDefaultLazy()
	 */
	boolean isDefaultCollectionLaziness();

	/**
	 * The default cache access strategy to use if none is specified
	 *
	 * @see MappingDefaults#getImplicitCacheAccessType()
	 */
	AccessType getDefaultCacheAccessType();

	/**
	 * @deprecated No longer supported
	 */
	@Deprecated
	default CollectionClassification getDefaultListClassification() {
		return CollectionClassification.LIST;
	}
}
