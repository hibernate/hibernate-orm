/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.boot.spi;

import java.util.LinkedHashSet;
import java.util.Set;
import org.hibernate.boot.model.process.internal.ManagedResourceValidation;

import jakarta.persistence.FetchType;
import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.SchemaManagementAction;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.annotation.Nonnull;
import org.hibernate.Internal;
import org.hibernate.Remove;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.scan.spi.ScanningResult;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.spi.ClassTransformer;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.tool.schema.Action;

import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.function.BiConsumer;

import static org.hibernate.cfg.MappingSettings.GLOBALLY_QUOTED_IDENTIFIERS;
import static org.hibernate.cfg.SchemaToolingSettings.JAKARTA_HBM2DDL_DATABASE_ACTION;
import static org.hibernate.cfg.SchemaToolingSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION;

import static org.hibernate.internal.util.collections.CollectionHelper.asProperties;
import static org.hibernate.jpa.internal.JpaLogger.JPA_LOGGER;

/**
 * PersistenceUnitDescriptor wrapper around {@linkplain PersistenceConfiguration}
 *
 * @author Steve Ebersole
 */
@Remove
public class PersistenceConfigurationDescriptor implements PersistenceUnitDescriptor {
	private final PersistenceConfiguration persistenceConfiguration;

	private final Properties properties;
	private final List<String> managedClassNames;
	private final List<Class<?>> managedClasses;
	private final ScanningResult discovery;
	public PersistenceConfigurationDescriptor(
			@Nonnull HibernatePersistenceConfiguration configuration,
			@Nonnull StandardServiceRegistry registry) {
		this( configuration );
	}

	public PersistenceConfigurationDescriptor(@Nonnull PersistenceConfiguration configuration) {
		this( configuration, ScanningResult.NONE );
	}

	public PersistenceConfigurationDescriptor(PersistenceConfiguration configuration, ScanningResult discovery) {
		this.discovery = discovery;
		this.persistenceConfiguration = configuration;
		this.properties = persistenceConfigurationProperties( configuration );
		this.managedClasses = List.copyOf( configuration.managedClasses() );
		this.managedClassNames = managedClasses.stream().map( Class::getName ).distinct().toList();
		managedClassNames.forEach( name -> ManagedResourceValidation.validateClassName(
				name, "managedClass({class})", "managedPackageDescriptor(\"{package}\")", "managedModuleDescriptor()" ) );
	}

	public List<Class<?>> getManagedClasses() {
		return managedClasses;
	}

	@Override
	public List<String> getManagedPackageDescriptors() {
		return List.copyOf( new LinkedHashSet<>( persistenceConfiguration.managedPackageDescriptors() ) );
	}

	@Override
	public List<String> getManagedModuleDescriptors() {
		return List.copyOf( new LinkedHashSet<>( persistenceConfiguration.managedModuleDescriptors() ) );
	}

	@Override
	public List<String> getAllPackageDescriptors() {
		return complete( getManagedPackageDescriptors(), discovery.discoveredPackages() );
	}

	@Override
	public List<String> getAllModuleDescriptors() {
		return complete( getManagedModuleDescriptors(), discovery.discoveredModules() );
	}

	private static List<String> complete(List<String> explicit, Set<String> discovered) {
		final var names = new LinkedHashSet<>( explicit );
		names.addAll( discovered );
		return List.copyOf( names );
	}

	private static Properties persistenceConfigurationProperties(PersistenceConfiguration persistenceConfiguration) {
		final var properties = asProperties( persistenceConfiguration.properties() );
		collectSchemaManagementActions( persistenceConfiguration, (name, value) -> {
			if ( !properties.containsKey( name ) ) {
				properties.put( name, value );
			}
		} );
		return properties;
	}

	@Internal
	public static void collectSchemaManagementActions(
			PersistenceConfiguration persistenceConfiguration,
			BiConsumer<String, Object> collector) {
		collectSchemaManagementAction(
				persistenceConfiguration.schemaManagementDatabaseAction(),
				JAKARTA_HBM2DDL_DATABASE_ACTION,
				collector
		);
		collectSchemaManagementAction(
				persistenceConfiguration.schemaManagementScriptsAction(),
				JAKARTA_HBM2DDL_SCRIPTS_ACTION,
				collector
		);
	}

	private static void collectSchemaManagementAction(
			SchemaManagementAction action,
			String settingName,
			BiConsumer<String, Object> collector) {
		if ( action != null && action != SchemaManagementAction.NONE ) {
			collector.accept( settingName, Action.interpretJpaSetting( action ) );
		}
	}

	@Override
	public Properties getProperties() {
		return properties;
	}

	@Override
	public String getName() {
		return persistenceConfiguration.name();
	}

	@Override
	public String getProviderClassName() {
		return persistenceConfiguration.provider();
	}

	@Override
	public boolean isUseQuotedIdentifiers() {
		return properties.get( GLOBALLY_QUOTED_IDENTIFIERS ) == Boolean.TRUE;
	}

	@Override
	public boolean isExcludeUnlistedClasses() {
		// if we do not know the root url nor jar files we cannot do scanning
		return !(persistenceConfiguration instanceof HibernatePersistenceConfiguration configuration)
			|| configuration.rootUrl() == null && configuration.jarFileUrls().isEmpty();
	}

	@Override
	public FetchType getDefaultToOneFetchType() {
		return persistenceConfiguration.defaultToOneFetchType();
	}

	@Override
	public PersistenceUnitTransactionType getPersistenceUnitTransactionType() {
		return persistenceConfiguration.transactionType();
	}

	@Override
	public List<String> getManagedClassNames() {
		return managedClassNames;
	}

	@Override
	public List<String> getAllClassNames() {
		return complete( managedClassNames, discovery.discoveredClasses() );
	}

	@Override
	public List<String> getMappingFileNames() {
		return persistenceConfiguration.mappingFiles();
	}

	@Override
	public Object getNonJtaDataSource() {
		return persistenceConfiguration.nonJtaDataSource();
	}

	@Override
	public Object getJtaDataSource() {
		return persistenceConfiguration.jtaDataSource();
	}

	@Override
	public ValidationMode getValidationMode() {
		return persistenceConfiguration.validationMode();
	}

	@Override
	public SharedCacheMode getSharedCacheMode() {
		return persistenceConfiguration.sharedCacheMode();
	}

	@Override
	public ClassLoader getClassLoader() {
		return HibernatePersistenceProvider.class.getClassLoader();
	}

	@Override
	public ClassLoader getTempClassLoader() {
		return null;
	}

	@Override
	public boolean isClassTransformerRegistrationDisabled() {
		return true;
	}

	@Override
	public ClassTransformer pushClassTransformer(EnhancementContext enhancementContext) {
		if ( JPA_LOGGER.isDebugEnabled() ) {
			JPA_LOGGER.pushingClassTransformerUnsupported( getName() );
		}
		return null;
	}

	@Override
	public URL getPersistenceUnitRootUrl() {
		return persistenceConfiguration instanceof HibernatePersistenceConfiguration configuration
				? configuration.rootUrl()
				: null;
	}

	@Override
	public List<URL> getJarFileUrls() {
		return persistenceConfiguration instanceof HibernatePersistenceConfiguration configuration
				? configuration.jarFileUrls()
				: null;
	}

}
