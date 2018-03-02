/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi;

import javax.persistence.AttributeConverter;
import javax.persistence.SharedCacheMode;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.archive.scan.spi.ScanOptions;
import org.hibernate.boot.archive.scan.spi.Scanner;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.model.IdGeneratorStrategyInterpreter;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.AttributeConverterDefinition;
import org.hibernate.cfg.MetadataSourceType;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.type.BasicType;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserType;

import org.jboss.jandex.IndexView;

/**
 * Convenience base class for custom implementors of {@link MetadataBuilderImplementor} using delegation.
 *
 * @author Gunnar Morling
 *
 * @param <T> The type of a specific sub-class; Allows sub-classes to narrow down the return-type of the contract methods
 * to a specialization of {@link MetadataBuilderImplementor}
 */
@SuppressWarnings("unused")
public abstract class AbstractDelegatingMetadataBuilderImplementor<T extends MetadataBuilderImplementor>  implements MetadataBuilderImplementor {

	private final MetadataBuilderImplementor delegate;

	/**
	 * Kept for compatibility reason but should be removed as soon as possible.
	 *
	 * @deprecated use {@link #delegate()} instead
	 */
	@Deprecated
	public MetadataBuilderImplementor getDelegate() {
		return delegate;
	}

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
	public MetadataBuilder applyIndexView(IndexView jandexView) {
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
	public MetadataBuilder enableNewIdentifierGeneratorSupport(boolean enable) {
		delegate.enableNewIdentifierGeneratorSupport( enable );
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
	public MetadataBuilder applyBasicType(BasicType type) {
		delegate.applyBasicType( type );
		return getThis();
	}

	@Override
	public MetadataBuilder applyBasicType(BasicType type, String... keys) {
		delegate.applyBasicType( type, keys );
		return getThis();
	}

	@Override
	public MetadataBuilder applyBasicType(UserType type, String... keys) {
		delegate.applyBasicType( type, keys );
		return getThis();
	}

	@Override
	public MetadataBuilder applyBasicType(CompositeUserType type, String... keys) {
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
	public MetadataBuilder applySourceProcessOrdering(MetadataSourceType... sourceTypes) {
		delegate.applySourceProcessOrdering( sourceTypes );
		return getThis();
	}

	@Override
	public MetadataBuilder applySqlFunction(String functionName, SQLFunction function) {
		delegate.applySqlFunction( functionName, function );
		return getThis();
	}

	@Override
	public MetadataBuilder applyAuxiliaryDatabaseObject(AuxiliaryDatabaseObject auxiliaryDatabaseObject) {
		delegate.applyAuxiliaryDatabaseObject( auxiliaryDatabaseObject );
		return getThis();
	}

	@Override
	public MetadataBuilder applyAttributeConverter(AttributeConverterDefinition definition) {
		delegate.applyAttributeConverter( definition );
		return getThis();
	}

	@Override
	public MetadataBuilder applyAttributeConverter(Class<? extends AttributeConverter> attributeConverterClass) {
		delegate.applyAttributeConverter( attributeConverterClass );
		return getThis();
	}

	@Override
	public MetadataBuilder applyAttributeConverter(Class<? extends AttributeConverter> attributeConverterClass, boolean autoApply) {
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
	public MetadataBuilder applyIdGenerationTypeInterpreter(IdGeneratorStrategyInterpreter interpreter) {
		delegate.applyIdGenerationTypeInterpreter( interpreter );
		return getThis();
	}

	@Override
	public <M extends MetadataBuilder> M unwrap(Class<M> type) {
		return delegate.unwrap( type );
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
