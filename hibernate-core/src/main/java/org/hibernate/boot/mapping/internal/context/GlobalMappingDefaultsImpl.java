/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.context;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.GlobalMappingDefaults;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.config.spi.ConfigurationService;

import static org.hibernate.cfg.CacheSettings.DEFAULT_CACHE_CONCURRENCY_STRATEGY;
import static org.hibernate.cfg.MappingSettings.GLOBALLY_QUOTED_IDENTIFIERS;
import static org.hibernate.engine.config.spi.StandardConverters.BOOLEAN;

/// Standard mapping defaults used while building boot metadata.
///
/// @since 9.0
/// @author Steve Ebersole
public class GlobalMappingDefaultsImpl implements GlobalMappingDefaults {
	String implicitSchemaName;
	String implicitCatalogName;
	boolean implicitlyQuoteIdentifiers;

	boolean toOnesAreLazyByDefault = false;

	AccessType implicitCacheAccessType;

	public GlobalMappingDefaultsImpl(StandardServiceRegistry serviceRegistry) {
		final var configService = serviceRegistry.requireService( ConfigurationService.class );

		// AvailableSettings.DEFAULT_SCHEMA and AvailableSettings.DEFAULT_CATALOG
		// are taken into account later, at runtime, when rendering table/sequence names.
		// These fields are exclusively about mapping defaults,
		// overridden in XML mappings or metadata customizations.
		implicitSchemaName = null;
		implicitCatalogName = null;

		implicitlyQuoteIdentifiers = configService.getSetting(
				GLOBALLY_QUOTED_IDENTIFIERS,
				BOOLEAN,
				false
		);

		implicitCacheAccessType = configService.getSetting(
				DEFAULT_CACHE_CONCURRENCY_STRATEGY,
				value -> AccessType.fromExternalName( value.toString() )
		);
	}

	@Override
	public String getImplicitSchemaName() {
		return implicitSchemaName;
	}

	@Override
	public String getImplicitCatalogName() {
		return implicitCatalogName;
	}

	@Override
	public boolean shouldImplicitlyQuoteIdentifiers() {
		return implicitlyQuoteIdentifiers;
	}

	@Override
	public String getImplicitIdColumnName() {
		return DEFAULT_IDENTIFIER_COLUMN_NAME;
	}

	@Override
	public String getImplicitTenantIdColumnName() {
		return DEFAULT_TENANT_IDENTIFIER_COLUMN_NAME;
	}

	@Override
	public String getImplicitDiscriminatorColumnName() {
		return DEFAULT_DISCRIMINATOR_COLUMN_NAME;
	}

	@Override
	public String getImplicitPackageName() {
		return null;
	}

	@Override
	public boolean isAutoImportEnabled() {
		return true;
	}

	@Override
	public String getImplicitCascadeStyleName() {
		return DEFAULT_CASCADE_NAME;
	}

	@Override
	public String getImplicitPropertyAccessorName() {
		return DEFAULT_PROPERTY_ACCESS_NAME;
	}

	@Override
	public boolean areEntitiesImplicitlyLazy() {
		return toOnesAreLazyByDefault;
	}

	@Override
	public boolean areCollectionsImplicitlyLazy() {
		// for now, just hard-code
		return true;
	}

	@Override
	public AccessType getImplicitCacheAccessType() {
		return implicitCacheAccessType;
	}

	public void applyImplicitSchemaName(String implicitSchemaName) {
		this.implicitSchemaName = implicitSchemaName;
	}

	public void applyImplicitCatalogName(String implicitCatalogName) {
		this.implicitCatalogName = implicitCatalogName;
	}

	public void applyImplicitlyQuoteIdentifiers(boolean implicitlyQuoteIdentifiers) {
		this.implicitlyQuoteIdentifiers = implicitlyQuoteIdentifiers;
	}

	public void applyDefaultToOneFetchType(boolean lazy) {
		this.toOnesAreLazyByDefault = lazy;
	}

	public void applyImplicitCacheAccessType(AccessType implicitCacheAccessType) {
		this.implicitCacheAccessType = implicitCacheAccessType;
	}
}
