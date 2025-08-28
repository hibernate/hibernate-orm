/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.util.jpa;

import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;
import java.net.URL;
import java.util.List;
import java.util.Properties;

public class DelegatingPersistenceUnitInfo implements PersistenceUnitInfo {
	private final PersistenceUnitInfo delegate;

	public DelegatingPersistenceUnitInfo(PersistenceUnitInfo delegate) {
		this.delegate = delegate;
	}

	@Override
	public String getPersistenceUnitName() {
		return delegate.getPersistenceUnitName();
	}

	@Override
	public String getPersistenceProviderClassName() {
		return delegate.getPersistenceProviderClassName();
	}

	@Override
	public String getScopeAnnotationName() {
		return delegate.getScopeAnnotationName();
	}

	@Override
	public List<String> getQualifierAnnotationNames() {
		return delegate.getQualifierAnnotationNames();
	}

	@Override @SuppressWarnings("removal")
	public PersistenceUnitTransactionType getTransactionType() {
		return delegate.getTransactionType();
	}

	@Override
	public DataSource getJtaDataSource() {
		return delegate.getJtaDataSource();
	}

	@Override
	public DataSource getNonJtaDataSource() {
		return delegate.getNonJtaDataSource();
	}

	@Override
	public List<String> getMappingFileNames() {
		return delegate.getMappingFileNames();
	}

	@Override
	public List<URL> getJarFileUrls() {
		return delegate.getJarFileUrls();
	}

	@Override
	public URL getPersistenceUnitRootUrl() {
		return delegate.getPersistenceUnitRootUrl();
	}

	@Override
	public List<String> getManagedClassNames() {
		return delegate.getManagedClassNames();
	}

	@Override
	public boolean excludeUnlistedClasses() {
		return delegate.excludeUnlistedClasses();
	}

	@Override
	public SharedCacheMode getSharedCacheMode() {
		return delegate.getSharedCacheMode();
	}

	@Override
	public ValidationMode getValidationMode() {
		return delegate.getValidationMode();
	}

	@Override
	public Properties getProperties() {
		return delegate.getProperties();
	}

	@Override
	public String getPersistenceXMLSchemaVersion() {
		return delegate.getPersistenceXMLSchemaVersion();
	}

	@Override
	public ClassLoader getClassLoader() {
		return delegate.getClassLoader();
	}

	@Override
	public void addTransformer(ClassTransformer transformer) {
		delegate.addTransformer( transformer );
	}

	@Override
	public ClassLoader getNewTempClassLoader() {
		return delegate.getNewTempClassLoader();
	}
}
