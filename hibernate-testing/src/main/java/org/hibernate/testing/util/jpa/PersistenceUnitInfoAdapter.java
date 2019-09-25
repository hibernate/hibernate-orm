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
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.hibernate.jpa.HibernatePersistenceProvider;

public class PersistenceUnitInfoAdapter implements PersistenceUnitInfo {
	private final Properties properties;

	public PersistenceUnitInfoAdapter() {
		this( new Properties() );
	}

	public PersistenceUnitInfoAdapter(Properties properties) {
		this.properties = properties;
	}

	@Override
	public String getPersistenceUnitName() {
		return "pu";
	}

	@Override
	public String getPersistenceProviderClassName() {
		return HibernatePersistenceProvider.class.getName();
	}

	@Override
	public PersistenceUnitTransactionType getTransactionType() {
		return PersistenceUnitTransactionType.RESOURCE_LOCAL;
	}

	@Override
	public DataSource getJtaDataSource() {
		return null;
	}

	@Override
	public DataSource getNonJtaDataSource() {
		return null;
	}

	@Override
	public List<String> getMappingFileNames() {
		return Collections.emptyList();
	}

	@Override
	public List<URL> getJarFileUrls() {
		return Collections.emptyList();
	}

	@Override
	public URL getPersistenceUnitRootUrl() {
		return null;
	}

	@Override
	public List<String> getManagedClassNames() {
		return Collections.emptyList();
	}

	@Override
	public boolean excludeUnlistedClasses() {
		return false;
	}

	@Override
	public SharedCacheMode getSharedCacheMode() {
		return SharedCacheMode.NONE;
	}

	@Override
	public ValidationMode getValidationMode() {
		return ValidationMode.NONE;
	}

	@Override
	public Properties getProperties() {
		return properties;
	}

	@Override
	public String getPersistenceXMLSchemaVersion() {
		return "2.1";
	}

	@Override
	public ClassLoader getClassLoader() {
		return PersistenceUnitInfoAdapter.class.getClassLoader();
	}

	@Override
	public void addTransformer(ClassTransformer transformer) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ClassLoader getNewTempClassLoader() {
		return null;
	}
}
