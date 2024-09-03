/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.jpa;

import java.net.URL;
import java.util.ArrayList;
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

import static java.util.Collections.emptyList;

/**
 * Implementation of {@link PersistenceUnitInfo} for testing use.
 * <p>
 * This implementation provides a bean-like contract for providing PU information.
 * <p>
 * See {@link PersistenceUnitInfoAdapter} for an override-based solution
 *
 * @author Steve Ebersole
 */
public class PersistenceUnitInfoImpl implements PersistenceUnitInfo {
	private final String name;

	private final Properties properties = new Properties();

	private String scopeAnnotationName;
	private List<String> qualifierAnnotationNames = List.of();
	private SharedCacheMode cacheMode;
	private ValidationMode validationMode;
	@SuppressWarnings("removal")
	private PersistenceUnitTransactionType transactionType;

	private List<String> mappingFiles;
	private List<String> managedClassNames;
	private boolean excludeUnlistedClasses;

	public PersistenceUnitInfoImpl(String name) {
		this.name = name;
	}

	@Override
	public String getPersistenceUnitName() {
		return name;
	}

	@Override
	public String getScopeAnnotationName() {
		return scopeAnnotationName;
	}

	public void setScopeAnnotationName(String scopeAnnotationName) {
		this.scopeAnnotationName = scopeAnnotationName;
	}

	@Override
	public List<String> getQualifierAnnotationNames() {
		return qualifierAnnotationNames;
	}

	public void setQualifierAnnotationNames(List<String> qualifierAnnotationNames) {
		this.qualifierAnnotationNames = qualifierAnnotationNames;
	}

	@Override
	public Properties getProperties() {
		return properties;
	}

	@Override
	public String getPersistenceProviderClassName() {
		return HibernatePersistenceProvider.class.getName();
	}

	@Override @SuppressWarnings("removal")
	public PersistenceUnitTransactionType getTransactionType() {
		return transactionType;
	}

	public void setTransactionType(@SuppressWarnings("removal") PersistenceUnitTransactionType transactionType) {
		this.transactionType = transactionType;
	}

	@Override
	public SharedCacheMode getSharedCacheMode() {
		return cacheMode;
	}

	public void setCacheMode(SharedCacheMode cacheMode) {
		this.cacheMode = cacheMode;
	}

	@Override
	public ValidationMode getValidationMode() {
		return validationMode;
	}

	public void setValidationMode(ValidationMode validationMode) {
		this.validationMode = validationMode;
	}

	@Override
	public List<String> getMappingFileNames() {
		return mappingFiles == null ? emptyList() : mappingFiles;
	}

	public void applyMappingFiles(String... mappingFiles) {
		if ( this.mappingFiles == null ) {
			this.mappingFiles = new ArrayList<>();
		}
		Collections.addAll( this.mappingFiles, mappingFiles );
	}

	@Override
	public List<String> getManagedClassNames() {
		return managedClassNames == null ? emptyList() : managedClassNames;
	}

	public void applyManagedClassNames(String... managedClassNames) {
		if ( this.managedClassNames == null ) {
			this.managedClassNames = new ArrayList<>();
		}
		Collections.addAll( this.managedClassNames, managedClassNames );
	}

	@Override
	public boolean excludeUnlistedClasses() {
		return excludeUnlistedClasses;
	}

	public void setExcludeUnlistedClasses(boolean excludeUnlistedClasses) {
		this.excludeUnlistedClasses = excludeUnlistedClasses;
	}

	@Override
	public String getPersistenceXMLSchemaVersion() {
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
	public URL getPersistenceUnitRootUrl() {
		return null;
	}

	@Override
	public ClassLoader getClassLoader() {
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
