/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.jpa.boot.spi;

import java.net.URL;
import java.util.List;
import java.util.Properties;

import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.spi.ClassTransformer;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jpa.HibernatePersistenceProvider;

import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.PersistenceUnitTransactionType;
import org.hibernate.jpa.internal.util.PersistenceUnitTransactionTypeHelper;

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
		this.properties = CollectionHelper.asProperties( persistenceConfiguration.properties() );
		this.managedClassNames = persistenceConfiguration.managedClasses().stream().map( Class::getName ).toList();
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
		// because we do not know the root url nor jar files we cannot do scanning
		return true;
	}

	@Override @SuppressWarnings("removal")
	public jakarta.persistence.spi.PersistenceUnitTransactionType getTransactionType() {
		return PersistenceUnitTransactionTypeHelper.toDeprecatedForm( getPersistenceUnitTransactionType() );
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
	public void pushClassTransformer(EnhancementContext enhancementContext) {

	}

	@Override
	public ClassTransformer getClassTransformer() {
		return null;
	}

	@Override
	public URL getPersistenceUnitRootUrl() {
		return null;
	}

	@Override
	public List<URL> getJarFileUrls() {
		return null;
	}
}
