/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.spi;

import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.archive.scan.spi.ScanOptions;
import org.hibernate.boot.archive.scan.spi.Scanner;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.ColumnOrderingStrategy;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.type.BasicType;
import org.hibernate.usertype.UserType;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.SharedCacheMode;

/**
 * Convenience base class for custom implementors of {@link MetadataBuilderImplementor} using delegation.
 *
 * @author Gunnar Morling
 *
 * @param <T> The specific subclass; Allows subclasses to narrow the return type of the contract methods
 *            to a specialization of {@link MetadataBuilderImplementor}.
 */
public abstract class AbstractDelegatingMetadataBuilderImplementor<T extends MetadataBuilderImplementor>  implements MetadataBuilderImplementor {

	private final MetadataBuilderImplementor delegate;

	protected MetadataBuilderImplementor delegate() {
		return delegate;
	}

	public AbstractDelegatingMetadataBuilderImplementor(MetadataBuilderImplementor delegate) {
		this.delegate = delegate;
	}

	/**
	 * Returns a specific implementation. See the <a
	 * href="http://www.angelikalanger.com/GenericsFAQ/FAQSections/ProgrammingIdioms.html#FAQ206">What is the
	 * "getThis trick?"</a>.
	 */
	protected abstract T getThis();

	@Override
	public MetadataBuilder applyImplicitSchemaName(String implicitSchemaName) {
		delegate.applyImplicitSchemaName( implicitSchemaName );
		return getThis();
	}

	@Override
	public MetadataBuilder applyImplicitCatalogName(String implicitCatalogName) {
		delegate.applyImplicitCatalogName( implicitCatalogName );
		return getThis();
	}

	@Override
	public MetadataBuilder applyImplicitNamingStrategy(ImplicitNamingStrategy namingStrategy) {
		delegate.applyImplicitNamingStrategy( namingStrategy );
		return getThis();
	}

	@Override
	public MetadataBuilder applyPhysicalNamingStrategy(PhysicalNamingStrategy namingStrategy) {
		delegate.applyPhysicalNamingStrategy( namingStrategy );
		return getThis();
	}

	@Override
	public MetadataBuilder applyColumnOrderingStrategy(ColumnOrderingStrategy columnOrderingStrategy) {
		delegate.applyColumnOrderingStrategy( columnOrderingStrategy );
		return getThis();
	}

	@Override
	public MetadataBuilder applySharedCacheMode(SharedCacheMode cacheMode) {
		delegate.applySharedCacheMode( cacheMode );
		return getThis();
	}

	@Override
	public MetadataBuilder applyAccessType(AccessType accessType) {
		delegate.applyAccessType( accessType );
		return getThis();
	}

	@Override
	public MetadataBuilder applyIndexView(Object jandexView) {
		delegate.applyIndexView( jandexView );
		return getThis();
	}

	@Override
	public MetadataBuilder applyScanOptions(ScanOptions scanOptions) {
		delegate.applyScanOptions( scanOptions );
		return getThis();
	}

	@Override
	public MetadataBuilder applyScanEnvironment(ScanEnvironment scanEnvironment) {
		delegate.applyScanEnvironment( scanEnvironment );
		return getThis();
	}

	@Override
	public MetadataBuilder applyScanner(Scanner scanner) {
		delegate.applyScanner( scanner );
		return getThis();
	}

	@Override
	public MetadataBuilder applyArchiveDescriptorFactory(ArchiveDescriptorFactory factory) {
		delegate.applyArchiveDescriptorFactory( factory );
		return getThis();
	}

	@Override
	public MetadataBuilder enableExplicitDiscriminatorsForJoinedSubclassSupport(boolean enabled) {
		delegate.enableExplicitDiscriminatorsForJoinedSubclassSupport( enabled );
		return getThis();
	}

	@Override
	public MetadataBuilder enableImplicitDiscriminatorsForJoinedSubclassSupport(boolean enabled) {
		delegate.enableImplicitDiscriminatorsForJoinedSubclassSupport( enabled );
		return getThis();
	}

	@Override
	public MetadataBuilder enableImplicitForcingOfDiscriminatorsInSelect(boolean supported) {
		delegate.enableImplicitForcingOfDiscriminatorsInSelect( supported );
		return getThis();
	}

	@Override
	public MetadataBuilder enableGlobalNationalizedCharacterDataSupport(boolean enabled) {
		delegate.enableGlobalNationalizedCharacterDataSupport( enabled );
		return getThis();
	}

	@Override
	public MetadataBuilder applyBasicType(BasicType<?> type) {
		delegate.applyBasicType( type );
		return getThis();
	}

	@Override
	public MetadataBuilder applyBasicType(BasicType<?> type, String... keys) {
		delegate.applyBasicType( type, keys );
		return getThis();
	}

	@Override
	public MetadataBuilder applyBasicType(UserType<?> type, String... keys) {
		delegate.applyBasicType( type, keys );
		return getThis();
	}

	@Override
	public MetadataBuilder applyTypes(TypeContributor typeContributor) {
		delegate.applyTypes( typeContributor );
		return getThis();
	}

	@Override
	public MetadataBuilder applyCacheRegionDefinition(CacheRegionDefinition cacheRegionDefinition) {
		delegate.applyCacheRegionDefinition( cacheRegionDefinition );
		return getThis();
	}

	@Override
	public MetadataBuilder applyTempClassLoader(ClassLoader tempClassLoader) {
		delegate.applyTempClassLoader( tempClassLoader );
		return getThis();
	}

	@Override
	public MetadataBuilder applyFunctions(FunctionContributor functionContributor) {
		delegate.applyFunctions( functionContributor );
		return this;
	}

	@Override
	public MetadataBuilder applySqlFunction(String functionName, SqmFunctionDescriptor function) {
		delegate.applySqlFunction( functionName, function );
		return getThis();
	}

	@Override
	public MetadataBuilder applyAuxiliaryDatabaseObject(AuxiliaryDatabaseObject auxiliaryDatabaseObject) {
		delegate.applyAuxiliaryDatabaseObject( auxiliaryDatabaseObject );
		return getThis();
	}

	@Override
	public MetadataBuilder applyAttributeConverter(ConverterDescriptor descriptor) {
		delegate.applyAttributeConverter( descriptor );
		return getThis();
	}

	@Override
	public <O, R> MetadataBuilder applyAttributeConverter(Class<? extends AttributeConverter<O, R>> attributeConverterClass) {
		delegate.applyAttributeConverter( attributeConverterClass );
		return getThis();
	}

	@Override
	public <O,R> MetadataBuilder applyAttributeConverter(Class<? extends AttributeConverter<O,R>> attributeConverterClass, boolean autoApply) {
		delegate.applyAttributeConverter( attributeConverterClass, autoApply );
		return getThis();
	}

	@Override
	public MetadataBuilder applyAttributeConverter(AttributeConverter attributeConverter) {
		delegate.applyAttributeConverter( attributeConverter );
		return getThis();
	}

	@Override
	public MetadataBuilder applyAttributeConverter(AttributeConverter attributeConverter, boolean autoApply) {
		delegate.applyAttributeConverter( attributeConverter, autoApply );
		return getThis();
	}

	@Override
	public MetadataBuildingOptions getMetadataBuildingOptions() {
		return delegate.getMetadataBuildingOptions();
	}

	@Override
	public Metadata build() {
		return delegate.build();
	}
}
