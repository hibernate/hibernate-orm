/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.spi;

import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.type.TimeZoneStorageStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.relational.ColumnOrderingStrategy;
import org.hibernate.boot.models.xml.spi.PersistenceUnitMetadata;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.dialect.TimeZoneSupport;
import org.hibernate.type.WrapperArrayHandling;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.CompositeUserType;

import jakarta.persistence.SharedCacheMode;

/**
 * Convenience base class for custom implementors of {@link MetadataBuildingOptions} using delegation.
 *
 * @author Gunnar Morling
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
public abstract class AbstractDelegatingMetadataBuildingOptions implements MetadataBuildingOptions, JpaOrmXmlPersistenceUnitDefaultAware {

	private final MetadataBuildingOptions delegate;

	public AbstractDelegatingMetadataBuildingOptions(MetadataBuildingOptions delegate) {
		this.delegate = delegate;
	}

	protected MetadataBuildingOptions delegate() {
		return delegate;
	}

	@Override
	public StandardServiceRegistry getServiceRegistry() {
		return delegate.getServiceRegistry();
	}

	@Override
	public MappingDefaults getMappingDefaults() {
		return delegate.getMappingDefaults();
	}

	@Override
	public TimeZoneStorageStrategy getDefaultTimeZoneStorage() {
		return delegate.getDefaultTimeZoneStorage();
	}

	@Override
	public TimeZoneSupport getTimeZoneSupport() {
		return delegate.getTimeZoneSupport();
	}

	@Override
	public WrapperArrayHandling getWrapperArrayHandling() {
		return delegate.getWrapperArrayHandling();
	}

	@Override
	public List<BasicTypeRegistration> getBasicTypeRegistrations() {
		return delegate.getBasicTypeRegistrations();
	}

	@Override
	public List<CompositeUserType<?>> getCompositeUserTypes() {
		return delegate.getCompositeUserTypes();
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return delegate.getTypeConfiguration();
	}

	@Override
	public ImplicitNamingStrategy getImplicitNamingStrategy() {
		return delegate.getImplicitNamingStrategy();
	}

	@Override
	public PhysicalNamingStrategy getPhysicalNamingStrategy() {
		return delegate.getPhysicalNamingStrategy();
	}

	@Override
	public ColumnOrderingStrategy getColumnOrderingStrategy() {
		return delegate.getColumnOrderingStrategy();
	}

	@Override
	public SharedCacheMode getSharedCacheMode() {
		return delegate.getSharedCacheMode();
	}

	@Override
	public AccessType getImplicitCacheAccessType() {
		return delegate.getImplicitCacheAccessType();
	}

	@Override
	public boolean isMultiTenancyEnabled() {
		return delegate.isMultiTenancyEnabled();
	}

	@Override
	public boolean ignoreExplicitDiscriminatorsForJoinedInheritance() {
		return delegate.ignoreExplicitDiscriminatorsForJoinedInheritance();
	}

	@Override
	public boolean createImplicitDiscriminatorsForJoinedInheritance() {
		return delegate.createImplicitDiscriminatorsForJoinedInheritance();
	}

	@Override
	public boolean shouldImplicitlyForceDiscriminatorInSelect() {
		return delegate.shouldImplicitlyForceDiscriminatorInSelect();
	}

	@Override
	public boolean useNationalizedCharacterData() {
		return delegate.useNationalizedCharacterData();
	}

	@Override
	public boolean isNoConstraintByDefault() {
		return delegate.isNoConstraintByDefault();
	}

	@Override
	public void apply(JpaOrmXmlPersistenceUnitDefaults jpaOrmXmlPersistenceUnitDefaults) {
		if ( delegate instanceof JpaOrmXmlPersistenceUnitDefaultAware persistenceUnitDefaultAware ) {
			persistenceUnitDefaultAware.apply( jpaOrmXmlPersistenceUnitDefaults );
		}
		else {
			throw new HibernateException(
					"AbstractDelegatingMetadataBuildingOptions delegate did not " +
							"implement JpaOrmXmlPersistenceUnitDefaultAware; " +
							"cannot delegate JpaOrmXmlPersistenceUnitDefaultAware#apply"
			);
		}
	}

	@Override
	public void apply(PersistenceUnitMetadata persistenceUnitMetadata) {
		if ( delegate instanceof JpaOrmXmlPersistenceUnitDefaultAware persistenceUnitDefaultAware ) {
			persistenceUnitDefaultAware.apply( persistenceUnitMetadata );
		}
		else {
			throw new HibernateException(
					"AbstractDelegatingMetadataBuildingOptions delegate did not " +
							"implement JpaOrmXmlPersistenceUnitDefaultAware; " +
							"cannot delegate JpaOrmXmlPersistenceUnitDefaultAware#apply"
			);
		}
	}

	@Override
	public String getSchemaCharset() {
		return delegate.getSchemaCharset();
	}

	@Override
	public boolean isXmlMappingEnabled() {
		return delegate.isXmlMappingEnabled();
	}

	@Override
	public boolean isAllowExtensionsInCdi() {
		return delegate.isAllowExtensionsInCdi();
	}

	@Override
	public boolean isXmlFormatMapperLegacyFormatEnabled() {
		return delegate.isXmlFormatMapperLegacyFormatEnabled();
	}
}
