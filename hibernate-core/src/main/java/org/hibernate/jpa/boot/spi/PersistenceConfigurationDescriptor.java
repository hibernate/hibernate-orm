/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.boot.spi;

import java.net.URL;
import java.util.List;
import java.util.Properties;

import jakarta.persistence.FetchType;
import jakarta.persistence.SchemaManagementAction;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.spi.ClassTransformer;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.SchemaToolingSettings;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.HibernatePersistenceProvider;

import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.PersistenceUnitTransactionType;

import static org.hibernate.jpa.internal.JpaLogger.JPA_LOGGER;

/**
 * PersistenceUnitDescriptor wrapper around {@linkplain PersistenceConfiguration}
 *
 * @author Steve Ebersole
 */
public class PersistenceConfigurationDescriptor implements PersistenceUnitDescriptor {
	private final PersistenceConfiguration persistenceConfiguration;

	private final Properties properties;
	private final List<String> managedClassNames;

	public PersistenceConfigurationDescriptor(PersistenceConfiguration persistenceConfiguration) {
		this.persistenceConfiguration = persistenceConfiguration;
		managedClassNames = persistenceConfiguration.managedClasses().stream().map( Class::getName ).toList();
		properties = CollectionHelper.asProperties( persistenceConfiguration.properties() );
		final var databaseAction = persistenceConfiguration.schemaManagementDatabaseAction();
		if ( databaseAction != SchemaManagementAction.NONE ) {
			properties.put( SchemaToolingSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, databaseAction.name() );
		}
		final var scriptsAction = persistenceConfiguration.getSchemaManagementScriptsAction();
		if ( scriptsAction != SchemaManagementAction.NONE ) {
			properties.put( SchemaToolingSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION, scriptsAction.name() );
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
		return properties.get( AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS ) == Boolean.TRUE;
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
	public ValidationMode getValidationMode() {
		return persistenceConfiguration.validationMode();
	}

	@Override
	public SharedCacheMode getSharedCacheMode() {
		return persistenceConfiguration.sharedCacheMode();
	}

	@Override
	public List<String> getManagedClassNames() {
		return managedClassNames;
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
