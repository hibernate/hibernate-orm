package org.hibernate.boot.spi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.FetchType;
import org.hibernate.Remove;
import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.scan.spi.ScanningProvider;
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
@Remove
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
	@Nonnull
	protected abstract T getThis();

	@Override
	@Nonnull
	public MetadataBuilder applyImplicitSchemaName(@Nullable String implicitSchemaName) {
		delegate.applyImplicitSchemaName( implicitSchemaName );
		return getThis();
	}

	@Override
	@Nonnull
	public MetadataBuilder applyImplicitCatalogName(@Nullable String implicitCatalogName) {
		delegate.applyImplicitCatalogName( implicitCatalogName );
		return getThis();
	}

	@Override
	@Nonnull
	public MetadataBuilder applyImplicitNamingStrategy(@Nonnull ImplicitNamingStrategy namingStrategy) {
		delegate.applyImplicitNamingStrategy( namingStrategy );
		return getThis();
	}

	@Override
	@Nonnull
	public MetadataBuilder applyPhysicalNamingStrategy(@Nonnull PhysicalNamingStrategy namingStrategy) {
		delegate.applyPhysicalNamingStrategy( namingStrategy );
		return getThis();
	}

	@Override
	@Nonnull
	public MetadataBuilder applyColumnOrderingStrategy(@Nonnull ColumnOrderingStrategy columnOrderingStrategy) {
		delegate.applyColumnOrderingStrategy( columnOrderingStrategy );
		return getThis();
	}

	@Override
	@Nonnull
	public MetadataBuilder applySharedCacheMode(@Nonnull SharedCacheMode cacheMode) {
		delegate.applySharedCacheMode( cacheMode );
		return getThis();
	}

	@Override
	@Nonnull
	public MetadataBuilder applyAccessType(@Nullable AccessType accessType) {
		delegate.applyAccessType( accessType );
		return getThis();
	}

	@Override
	@Nonnull
	public MetadataBuilder applyIndexView(@Nullable Object jandexView) {
		delegate.applyIndexView( jandexView );
		return getThis();
	}

	@Override
	@Nonnull
	public MetadataBuilder applyScanning(@Nullable ScanningProvider scanningProvider) {
		delegate.applyScanning( scanningProvider );
		return getThis();
	}

	@Override
	@Nonnull
	public MetadataBuilder applyArchiveDescriptorFactory(@Nullable ArchiveDescriptorFactory factory) {
		delegate.applyArchiveDescriptorFactory( factory );
		return getThis();
	}

	@Override
	@Nonnull
	public MetadataBuilder enableExplicitDiscriminatorsForJoinedSubclassSupport(boolean enabled) {
		delegate.enableExplicitDiscriminatorsForJoinedSubclassSupport( enabled );
		return getThis();
	}

	@Override
	@Nonnull
	public MetadataBuilder enableImplicitDiscriminatorsForJoinedSubclassSupport(boolean enabled) {
		delegate.enableImplicitDiscriminatorsForJoinedSubclassSupport( enabled );
		return getThis();
	}

	@Override
	@Nonnull
	public MetadataBuilder enableImplicitForcingOfDiscriminatorsInSelect(boolean supported) {
		delegate.enableImplicitForcingOfDiscriminatorsInSelect( supported );
		return getThis();
	}

	@Override
	@Nonnull
	public MetadataBuilder enableGlobalNationalizedCharacterDataSupport(boolean enabled) {
		delegate.enableGlobalNationalizedCharacterDataSupport( enabled );
		return getThis();
	}

	@Override
	@Nonnull
	public MetadataBuilder applyDefaultToOneFetchType(@Nullable FetchType defaultToOneFetchType) {
		delegate.applyDefaultToOneFetchType( defaultToOneFetchType );
		return getThis();
	}

	@Override
	@Nonnull
	public MetadataBuilder applyBasicType(@Nonnull BasicType<?> type) {
		delegate.applyBasicType( type );
		return getThis();
	}

	@Override
	@Nonnull
	public MetadataBuilder applyBasicType(@Nonnull BasicType<?> type, @Nullable String... keys) {
		delegate.applyBasicType( type, keys );
		return getThis();
	}

	@Override
	@Nonnull
	public MetadataBuilder applyBasicType(@Nonnull UserType<?> type, @Nullable String... keys) {
		delegate.applyBasicType( type, keys );
		return getThis();
	}

	@Override
	@Nonnull
	public MetadataBuilder applyTypes(@Nonnull TypeContributor typeContributor) {
		delegate.applyTypes( typeContributor );
		return getThis();
	}

	@Override
	@Nonnull
	public MetadataBuilder applyCacheRegionDefinition(@Nonnull CacheRegionDefinition cacheRegionDefinition) {
		delegate.applyCacheRegionDefinition( cacheRegionDefinition );
		return getThis();
	}

	@Override
	@Nonnull
	public MetadataBuilder applyTempClassLoader(@Nullable ClassLoader tempClassLoader) {
		delegate.applyTempClassLoader( tempClassLoader );
		return getThis();
	}

	@Override
	@Nonnull
	public MetadataBuilder applyFunctions(@Nonnull FunctionContributor functionContributor) {
		delegate.applyFunctions( functionContributor );
		return this;
	}

	@Override
	@Nonnull
	public MetadataBuilder applySqlFunction(@Nonnull String functionName, @Nonnull SqmFunctionDescriptor function) {
		delegate.applySqlFunction( functionName, function );
		return getThis();
	}

	@Override
	@Nonnull
	public MetadataBuilder applyAuxiliaryDatabaseObject(@Nonnull AuxiliaryDatabaseObject auxiliaryDatabaseObject) {
		delegate.applyAuxiliaryDatabaseObject( auxiliaryDatabaseObject );
		return getThis();
	}

	@Override
	@Nonnull
	public MetadataBuilder applyAttributeConverter(@Nonnull ConverterDescriptor<?,?> descriptor) {
		delegate.applyAttributeConverter( descriptor );
		return getThis();
	}

	@Override
	@Nonnull
	public <O, R> MetadataBuilder applyAttributeConverter(
			@Nonnull Class<? extends AttributeConverter<O, R>> attributeConverterClass) {
		delegate.applyAttributeConverter( attributeConverterClass );
		return getThis();
	}

	@Override
	@Nonnull
	public <O,R> MetadataBuilder applyAttributeConverter(
			@Nonnull Class<? extends AttributeConverter<O,R>> attributeConverterClass,
			boolean autoApply) {
		delegate.applyAttributeConverter( attributeConverterClass, autoApply );
		return getThis();
	}

	@Override
	@Nonnull
	public MetadataBuilder applyAttributeConverter(@Nonnull AttributeConverter attributeConverter) {
		delegate.applyAttributeConverter( attributeConverter );
		return getThis();
	}

	@Override
	@Nonnull
	public MetadataBuilder applyAttributeConverter(@Nonnull AttributeConverter attributeConverter, boolean autoApply) {
		delegate.applyAttributeConverter( attributeConverter, autoApply );
		return getThis();
	}

	@Override
	public MetadataBuildingOptions getMetadataBuildingOptions() {
		return delegate.getMetadataBuildingOptions();
	}

	@Override
	@Nonnull
	public Metadata build() {
		return delegate.build();
	}
}
