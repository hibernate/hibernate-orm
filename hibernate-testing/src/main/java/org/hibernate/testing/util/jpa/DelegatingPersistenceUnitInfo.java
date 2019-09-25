/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.util.jpa;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
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
		return null;
	}
}
