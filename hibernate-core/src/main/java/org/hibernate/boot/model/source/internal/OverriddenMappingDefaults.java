/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal;

import java.util.EnumSet;

import org.hibernate.annotations.CascadeType;
import org.hibernate.boot.spi.EffectiveMappingDefaults;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.CollectionClassification;

/**
 * Represents a "nested level" in the mapping defaults stack.
 *
 * @author Steve Ebersole
 */
public class OverriddenMappingDefaults implements EffectiveMappingDefaults {
	private final String implicitSchemaName;
	private final String implicitCatalogName;
	private final boolean implicitlyQuoteIdentifiers;
	private final String implicitIdColumnName;
	private final String implicitTenantIdColumnName;
	private final String implicitDiscriminatorColumnName;
	private final String implicitPackageName;
	private final boolean autoImportEnabled;
	private final jakarta.persistence.AccessType implicitPropertyAccessType;
	private final String implicitPropertyAccessorName;
	private final boolean entitiesImplicitlyLazy;
	private final boolean pluralAttributesImplicitlyLazy;
	private final AccessType implicitCacheAccessType;
	private final EnumSet<CascadeType> cascadeTypes;
	private final CollectionClassification implicitListClassification;

	public OverriddenMappingDefaults(
			String implicitSchemaName,
			String implicitCatalogName,
			boolean implicitlyQuoteIdentifiers,
			String implicitIdColumnName,
			String implicitTenantIdColumnName,
			String implicitDiscriminatorColumnName,
			String implicitPackageName,
			boolean autoImportEnabled,
			EnumSet<CascadeType> cascadeTypes,
			jakarta.persistence.AccessType implicitPropertyAccessType,
			String implicitPropertyAccessorName,
			boolean entitiesImplicitlyLazy,
			boolean pluralAttributesImplicitlyLazy,
			AccessType implicitCacheAccessType,
			CollectionClassification implicitListClassification) {
		this.implicitSchemaName = implicitSchemaName;
		this.implicitCatalogName = implicitCatalogName;
		this.implicitlyQuoteIdentifiers = implicitlyQuoteIdentifiers;
		this.implicitIdColumnName = implicitIdColumnName;
		this.implicitTenantIdColumnName = implicitTenantIdColumnName;
		this.implicitDiscriminatorColumnName = implicitDiscriminatorColumnName;
		this.implicitPackageName = implicitPackageName;
		this.autoImportEnabled = autoImportEnabled;
		this.cascadeTypes = cascadeTypes;
		this.implicitPropertyAccessType = implicitPropertyAccessType;
		this.implicitPropertyAccessorName = implicitPropertyAccessorName;
		this.entitiesImplicitlyLazy = entitiesImplicitlyLazy;
		this.pluralAttributesImplicitlyLazy = pluralAttributesImplicitlyLazy;
		this.implicitCacheAccessType = implicitCacheAccessType;
		this.implicitListClassification = implicitListClassification;
	}

	@Override
	public String getDefaultSchemaName() {
		return implicitSchemaName;
	}

	@Override
	public String getDefaultCatalogName() {
		return implicitCatalogName;
	}

	@Override
	public boolean isDefaultQuoteIdentifiers() {
		return implicitlyQuoteIdentifiers;
	}

	@Override
	public String getDefaultIdColumnName() {
		return implicitIdColumnName;
	}

	@Override
	public String getDefaultDiscriminatorColumnName() {
		return implicitDiscriminatorColumnName;
	}

	@Override
	public String getDefaultTenantIdColumnName() {
		return implicitTenantIdColumnName;
	}

	@Override
	public String getDefaultPackageName() {
		return implicitPackageName;
	}

	@Override
	public boolean isDefaultAutoImport() {
		return autoImportEnabled;
	}

	@Override
	public EnumSet<CascadeType> getDefaultCascadeTypes() {
		return cascadeTypes;
	}

	@Override
	public jakarta.persistence.AccessType getDefaultPropertyAccessType() {
		return implicitPropertyAccessType;
	}

	@Override
	public String getDefaultAccessStrategyName() {
		return implicitPropertyAccessorName;
	}

	@Override
	public boolean isDefaultEntityLaziness() {
		return entitiesImplicitlyLazy;
	}

	@Override
	public boolean isDefaultCollectionLaziness() {
		return pluralAttributesImplicitlyLazy;
	}

	@Override
	public AccessType getDefaultCacheAccessType() {
		return implicitCacheAccessType;
	}

	@Override
	public CollectionClassification getDefaultListClassification() {
		return implicitListClassification;
	}

	public static class Builder {
		private String implicitSchemaName;
		private String implicitCatalogName;
		private boolean implicitlyQuoteIdentifiers;
		private String implicitIdColumnName;
		private String implicitTenantIdColumnName;
		private String implicitDiscriminatorColumnName;
		private String implicitPackageName;
		private boolean autoImportEnabled;
		private EnumSet<CascadeType> implicitCascadeTypes;
		private jakarta.persistence.AccessType implicitPropertyAccessType;
		private String implicitPropertyAccessorName;
		private boolean entitiesImplicitlyLazy;
		private boolean pluralAttributesImplicitlyLazy;
		private AccessType implicitCacheAccessType;
		private CollectionClassification implicitListClassification;

		public Builder(EffectiveMappingDefaults parentDefaults) {
			this.implicitSchemaName = parentDefaults.getDefaultSchemaName();
			this.implicitCatalogName = parentDefaults.getDefaultCatalogName();
			this.implicitlyQuoteIdentifiers = parentDefaults.isDefaultQuoteIdentifiers();
			this.implicitIdColumnName = parentDefaults.getDefaultIdColumnName();
			this.implicitTenantIdColumnName = parentDefaults.getDefaultTenantIdColumnName();
			this.implicitDiscriminatorColumnName = parentDefaults.getDefaultDiscriminatorColumnName();
			this.implicitPackageName = parentDefaults.getDefaultPackageName();
			this.autoImportEnabled = parentDefaults.isDefaultAutoImport();

			this.implicitCascadeTypes = parentDefaults.getDefaultCascadeTypes();
			this.implicitPropertyAccessType = parentDefaults.getDefaultPropertyAccessType();
			this.implicitPropertyAccessorName = parentDefaults.getDefaultAccessStrategyName();
			this.entitiesImplicitlyLazy = parentDefaults.isDefaultEntityLaziness();
			this.pluralAttributesImplicitlyLazy = parentDefaults.isDefaultCollectionLaziness();
			this.implicitCacheAccessType = parentDefaults.getDefaultCacheAccessType();
		}

		public Builder setImplicitSchemaName(String implicitSchemaName) {
			if ( StringHelper.isNotEmpty( implicitSchemaName ) ) {
				this.implicitSchemaName = implicitSchemaName;
			}
			return this;
		}

		public Builder setImplicitCatalogName(String implicitCatalogName) {
			if ( StringHelper.isNotEmpty( implicitCatalogName ) ) {
				this.implicitCatalogName = implicitCatalogName;
			}
			return this;
		}

		public Builder setImplicitlyQuoteIdentifiers(boolean implicitlyQuoteIdentifiers) {
			this.implicitlyQuoteIdentifiers = implicitlyQuoteIdentifiers;
			return this;
		}

		public Builder setImplicitIdColumnName(String implicitIdColumnName) {
			if ( StringHelper.isNotEmpty( implicitIdColumnName ) ) {
				this.implicitIdColumnName = implicitIdColumnName;
			}
			return this;
		}

		public Builder setImplicitTenantIdColumnName(String implicitTenantIdColumnName) {
			if ( StringHelper.isNotEmpty( implicitTenantIdColumnName ) ) {
				this.implicitTenantIdColumnName = implicitTenantIdColumnName;
			}
			return this;
		}

		public Builder setImplicitDiscriminatorColumnName(String implicitDiscriminatorColumnName) {
			if ( StringHelper.isNotEmpty( implicitDiscriminatorColumnName ) ) {
				this.implicitDiscriminatorColumnName = implicitDiscriminatorColumnName;
			}
			return this;
		}

		public Builder setImplicitPackageName(String implicitPackageName) {
			if ( StringHelper.isNotEmpty( implicitPackageName ) ) {
				this.implicitPackageName = implicitPackageName;
			}
			return this;
		}

		public Builder setAutoImportEnabled(boolean autoImportEnabled) {
			this.autoImportEnabled = autoImportEnabled;
			return this;
		}

		public Builder setImplicitCascadeTypes(EnumSet<CascadeType> implicitCascadeTypes) {
			if (implicitCascadeTypes != null) {
				this.implicitCascadeTypes = implicitCascadeTypes;
			}
			return this;
		}

		public Builder setImplicitPropertyAccessType(jakarta.persistence.AccessType accessType) {
			if ( accessType != null ) {
				this.implicitPropertyAccessType = accessType;
			}
			return this;
		}

		public Builder setImplicitPropertyAccessorName(String implicitPropertyAccessorName) {
			if ( StringHelper.isNotEmpty( implicitPropertyAccessorName ) ) {
				this.implicitPropertyAccessorName = implicitPropertyAccessorName;
			}
			return this;
		}

		public Builder setEntitiesImplicitlyLazy(boolean entitiesImplicitlyLazy) {
			this.entitiesImplicitlyLazy = entitiesImplicitlyLazy;
			return this;
		}

		public Builder setPluralAttributesImplicitlyLazy(boolean pluralAttributesImplicitlyLazy) {
			this.pluralAttributesImplicitlyLazy = pluralAttributesImplicitlyLazy;
			return this;
		}

		public Builder setImplicitCacheAccessType(AccessType implicitCacheAccessType) {
			if ( implicitCacheAccessType != null ) {
				this.implicitCacheAccessType = implicitCacheAccessType;
			}
			return this;
		}

		public CollectionClassification getImplicitListClassification() {
			return implicitListClassification;
		}

		public Builder setImplicitListClassification(CollectionClassification implicitListClassification) {
			if ( implicitListClassification != null ) {
				this.implicitListClassification = implicitListClassification;
			}
			return this;
		}

		public OverriddenMappingDefaults build() {
			return new OverriddenMappingDefaults(
					implicitSchemaName,
					implicitCatalogName,
					implicitlyQuoteIdentifiers,
					implicitIdColumnName,
					implicitTenantIdColumnName,
					implicitDiscriminatorColumnName,
					implicitPackageName,
					autoImportEnabled,
					implicitCascadeTypes,
					implicitPropertyAccessType,
					implicitPropertyAccessorName,
					entitiesImplicitlyLazy,
					pluralAttributesImplicitlyLazy,
					implicitCacheAccessType,
					implicitListClassification
			);
		}
	}
}
