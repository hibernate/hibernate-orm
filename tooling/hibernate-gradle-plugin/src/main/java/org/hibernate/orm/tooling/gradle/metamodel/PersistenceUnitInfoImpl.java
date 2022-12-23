/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.tooling.gradle.metamodel;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.sql.DataSource;

import org.hibernate.jpa.HibernatePersistenceProvider;

import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;

/**
 * @author Steve Ebersole
 */
class PersistenceUnitInfoImpl implements PersistenceUnitInfo {
	private final URL unitRoot;
	private final Properties properties;
	private final ClassLoader classLoader;
	private final List<String> managedClassNames = new ArrayList<>();
	private final List<String> mappingFileNames = new ArrayList<>();

	public PersistenceUnitInfoImpl(URL unitRoot, Properties properties, ClassLoader classLoader) {
		this.unitRoot = unitRoot;
		this.properties = properties;
		this.classLoader = classLoader;
	}

	@Override
	public String getPersistenceUnitName() {
		return "jpa-static-metamodel-gen";
	}

	@Override
	public URL getPersistenceUnitRootUrl() {
		return unitRoot;
	}

	@Override
	public Properties getProperties() {
		return properties;
	}

	@Override
	public ClassLoader getClassLoader() {
		return classLoader;
	}

	@Override
	public List<String> getManagedClassNames() {
		return managedClassNames;
	}

	public void addManagedClassName(String className) {
		getManagedClassNames().add( className );
	}

	@Override
	public List<String> getMappingFileNames() {
		return mappingFileNames;
	}

	public void addMappingFile(String fileName) {
		getMappingFileNames().add( fileName );
	}

	@Override
	public String getPersistenceProviderClassName() {
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
	public List<URL> getJarFileUrls() {
		return null;
	}

	@Override
	public boolean excludeUnlistedClasses() {
		return true;
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
	public String getPersistenceXMLSchemaVersion() {
		return null;
	}

	@Override
	public void addTransformer(ClassTransformer transformer) {

	}

	@Override
	public ClassLoader getNewTempClassLoader() {
		return null;
	}
}
