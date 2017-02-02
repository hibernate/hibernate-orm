/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

/**
 * @author Steve Ebersole
 */
public class PersistenceUnitDescriptorAdapter implements PersistenceUnitDescriptor {
	private final String name = "persistenceUnitDescriptorAdapter@" + System.identityHashCode( this );
	private Properties properties;

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isUseQuotedIdentifiers() {
		return false;
	}

	@Override
	public String getProviderClassName() {
		return HibernatePersistenceProvider.class.getName();
	}

	@Override
	public PersistenceUnitTransactionType getTransactionType() {
		return null;
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
	public boolean isExcludeUnlistedClasses() {
		return false;
	}

	@Override
	public SharedCacheMode getSharedCacheMode() {
		return null;
	}

	@Override
	public ValidationMode getValidationMode() {
		return null;
	}

	@Override
	public Properties getProperties() {
		if ( properties == null ) {
			properties = new Properties();
		}
		return properties;
	}

	@Override
	public ClassLoader getClassLoader() {
		return Thread.currentThread().getContextClassLoader();
	}

	@Override
	public ClassLoader getTempClassLoader() {
		return null;
	}

	@Override
	public void pushClassTransformer(EnhancementContext enhancementContext) {
	}
}
