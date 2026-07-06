/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal;

import java.util.List;

import jakarta.annotation.Nonnull;
import org.hibernate.HibernateException;
import org.hibernate.boot.spi.BasicTypeRegistration;
import org.hibernate.boot.spi.GlobalMappingDefaults;
import org.hibernate.boot.spi.JpaOrmXmlPersistenceUnitDefaultAware;
import org.hibernate.type.TimeZoneStorageStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.relational.ColumnOrderingStrategy;
import org.hibernate.boot.mapping.internal.xml.PersistenceUnitMetadata;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.dialect.TimeZoneSupport;
import org.hibernate.type.WrapperArrayHandling;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.CompositeUserType;

import jakarta.persistence.SharedCacheMode;

/**
 * Convenience base class for custom implementors of {@link MappingResolutionOptions} using delegation.
 *
 * @author Gunnar Morling
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
public abstract class AbstractDelegatingMappingResolutionOptions
		implements MappingResolutionOptions, JpaOrmXmlPersistenceUnitDefaultAware {

	private final MappingResolutionOptions delegate;

	public AbstractDelegatingMappingResolutionOptions(MappingResolutionOptions delegate) {
		this.delegate = delegate;
	}

	protected MappingResolutionOptions delegate() {
		return delegate;
	}

	@Override
	public StandardServiceRegistry getServiceRegistry() {
		return delegate.getServiceRegistry();
	}

	@Override
	public GlobalMappingDefaults getMappingDefaults() {
		return delegate.getMappingDefaults();
	}

	@Override
	@Nonnull
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
					"AbstractDelegatingMappingResolutionOptions delegate did not " +
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
					"AbstractDelegatingMappingResolutionOptions delegate did not " +
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
