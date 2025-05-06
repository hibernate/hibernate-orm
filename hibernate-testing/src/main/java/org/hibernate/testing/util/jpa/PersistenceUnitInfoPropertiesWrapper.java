/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.util.jpa;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.hibernate.jpa.HibernatePersistenceProvider;

import static java.lang.Thread.currentThread;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class PersistenceUnitInfoPropertiesWrapper implements PersistenceUnitInfo {
	private final Properties properties;

	public PersistenceUnitInfoPropertiesWrapper() {
		properties = new Properties();
	}

	public PersistenceUnitInfoPropertiesWrapper(Properties properties) {
		this.properties = properties;
	}

	public String getPersistenceUnitName() {
		return "persistenceUnitAdapter";
	}

	public String getPersistenceProviderClassName() {
		return HibernatePersistenceProvider.class.getName();
	}

	@Override
	public String getScopeAnnotationName() {
		return null;
	}

	@Override
	public List<String> getQualifierAnnotationNames() {
		return List.of();
	}

	@SuppressWarnings("removal")
	public PersistenceUnitTransactionType getTransactionType() {
		return null;
	}

	public DataSource getJtaDataSource() {
		return null;
	}

	public DataSource getNonJtaDataSource() {
		return null;
	}

	public List<String> getMappingFileNames() {
		return Collections.emptyList();
	}

	public List<URL> getJarFileUrls() {
		return Collections.emptyList();
	}

	public URL getPersistenceUnitRootUrl() {
		return null;
	}

	public List<String> getManagedClassNames() {
		return Collections.emptyList();
	}

	public boolean excludeUnlistedClasses() {
		return false;
	}

	public SharedCacheMode getSharedCacheMode() {
		return null;
	}

	public ValidationMode getValidationMode() {
		return null;
	}

	public Properties getProperties() {
		return properties;
	}

	public String getPersistenceXMLSchemaVersion() {
		return null;
	}

	public ClassLoader getClassLoader() {
		return currentThread().getContextClassLoader();
	}

	public void addTransformer(ClassTransformer transformer) {
	}

	public ClassLoader getNewTempClassLoader() {
		return null;
	}
}
