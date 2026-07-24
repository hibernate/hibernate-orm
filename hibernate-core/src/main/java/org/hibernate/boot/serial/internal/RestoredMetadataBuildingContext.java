/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.serial.internal;

import java.util.EnumSet;

import org.hibernate.boot.mapping.internal.context.MappingResolutionServices;
import org.hibernate.boot.mapping.internal.context.MappingResolutionServicesImpl;
import org.hibernate.boot.mapping.internal.context.RootMappingDefaults;
import org.hibernate.boot.mapping.internal.context.TypeDefinitionRegistryStandardImpl;
import org.hibernate.boot.mapping.internal.xml.PersistenceUnitMetadata;
import org.hibernate.boot.model.TypeDefinitionRegistry;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.boot.pipeline.internal.MappingResolutionOptions;
import org.hibernate.boot.pipeline.internal.TypeContributionCoordinator;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.EffectiveMappingDefaults;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;

import jakarta.persistence.AccessType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.CascadeType;
import org.hibernate.usertype.CompositeUserType;

/// Service-backed building context used exclusively while restoring derived mapping state.
///
/// @since 9.0
/// @author Steve Ebersole
public final class RestoredMetadataBuildingContext implements MetadataBuildingContext {
	private final BootstrapContext bootstrapContext;
	private final MappingResolutionOptions options;
	private final MappingResolutionServices services;
	private final TypeDefinitionRegistryStandardImpl typeDefinitionRegistry;
	private final EffectiveMappingDefaults effectiveDefaults;
	private final ObjectNameNormalizer objectNameNormalizer;

	public RestoredMetadataBuildingContext(
			BootstrapContext bootstrapContext,
			MappingResolutionOptions options,
			Iterable<org.hibernate.boot.model.TypeDefinition> typeDefinitions) {
		this.bootstrapContext = bootstrapContext;
		this.options = options;
		this.services = new MappingResolutionServicesImpl( bootstrapContext );
		bootstrapContext.getTypeConfiguration().scope( this );
		final TypeContributions restorationContributions = new TypeContributions() {
			@Override
			public org.hibernate.type.spi.TypeConfiguration getTypeConfiguration() {
				return bootstrapContext.getTypeConfiguration();
			}

			@Override
			public void contributeAttributeConverter(Class<? extends AttributeConverter<?, ?>> converterClass) {
				// Converter declarations are restored with their mapping values.
			}

			@Override
			public void contributeType(CompositeUserType<?> type) {
				// Composite user-type declarations are restored with their components.
			}
		};
		TypeContributionCoordinator.contribute(
				restorationContributions,
				java.util.List.of(),
				bootstrapContext.getServiceRegistry()
		);
		this.typeDefinitionRegistry = new TypeDefinitionRegistryStandardImpl();
		typeDefinitions.forEach( typeDefinitionRegistry::register );
		this.effectiveDefaults = new RootMappingDefaults( options.getMappingDefaults(), EmptyPersistenceUnitMetadata.INSTANCE );
		this.objectNameNormalizer = new ObjectNameNormalizer( this );
	}

	@Override
	public BootstrapContext getBootstrapContext() {
		return bootstrapContext;
	}

	@Override
	public MappingResolutionServices getServiceComponents() {
		return services;
	}

	@Override
	public MappingResolutionOptions getBuildingPlan() {
		return options;
	}

	@Override
	public EffectiveMappingDefaults getEffectiveDefaults() {
		return effectiveDefaults;
	}

	@Override
	public InFlightMetadataCollector getMetadataCollector() {
		throw new IllegalStateException( "Restored metadata has no in-flight metadata collector" );
	}

	@Override
	public ObjectNameNormalizer getObjectNameNormalizer() {
		return objectNameNormalizer;
	}

	@Override
	public TypeDefinitionRegistry getTypeDefinitionRegistry() {
		return typeDefinitionRegistry;
	}

	@Override
	public String getCurrentContributorName() {
		return "orm-metadata-archive";
	}

	private enum EmptyPersistenceUnitMetadata implements PersistenceUnitMetadata {
		INSTANCE;

		@Override public boolean areXmlMappingsComplete() { return false; }
		@Override public String getDefaultSchema() { return null; }
		@Override public String getDefaultCatalog() { return null; }
		@Override public AccessType getAccessType() { return null; }
		@Override public String getDefaultAccessStrategyName() { return null; }
		@Override public EnumSet<CascadeType> getDefaultCascadeTypes() { return EnumSet.noneOf( CascadeType.class ); }
		@Override public boolean useQuotedIdentifiers() { return false; }
	}
}
