/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.jpa;

import java.net.URL;
import java.util.List;
import java.util.Properties;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.hibernate.jpa.HibernatePersistenceProvider;

import static java.lang.System.identityHashCode;
import static java.util.Collections.emptyList;

/**
 * Implementation of {@link PersistenceUnitInfo} for testing use.
 * <p>
 * Expected usage is to override methods relevant to their specific tests.
 * <p>
 * See {@link PersistenceUnitInfoImpl} for a more bean-like implementation
 *
 * @author Steve Ebersole
 */
public class PersistenceUnitInfoAdapter implements PersistenceUnitInfo {
	private final String name = "persistenceUnitInfoAdapter@" + identityHashCode( this );
	private Properties properties;

	public String getPersistenceUnitName() {
		return name;
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

	@Override @SuppressWarnings("removal")
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
		return emptyList();
	}

	public List<URL> getJarFileUrls() {
		return emptyList();
	}

	public URL getPersistenceUnitRootUrl() {
		return null;
	}

	public List<String> getManagedClassNames() {
		return emptyList();
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
		if ( properties == null ) {
			properties = new Properties();
		}
		return properties;
	}

	public String getPersistenceXMLSchemaVersion() {
		return null;
	}

	public ClassLoader getClassLoader() {
		return Thread.currentThread().getContextClassLoader();
	}

	public void addTransformer(ClassTransformer transformer) {
	}

	public ClassLoader getNewTempClassLoader() {
		return null;
	}
}
