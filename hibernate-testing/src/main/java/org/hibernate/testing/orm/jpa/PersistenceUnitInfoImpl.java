/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.jpa;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.FetchType;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.hibernate.jpa.HibernatePersistenceProvider;

import static java.util.Collections.addAll;
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
	@Nonnull
	private final String name;
	@Nonnull
	private final Properties properties = new Properties();
	@Nullable
	private String scopeAnnotationName;
	@Nullable
	private List<String> qualifierAnnotationNames;
	@Nonnull
	private SharedCacheMode cacheMode = SharedCacheMode.ENABLE_SELECTIVE;
	@Nonnull
	private ValidationMode validationMode = ValidationMode.AUTO;
	@Nonnull
	private FetchType defaultToOneFetchType = FetchType.EAGER;
	@Nonnull
	private PersistenceUnitTransactionType transactionType =
			PersistenceUnitTransactionType.RESOURCE_LOCAL;

	@Nullable
	private List<String> mappingFiles;
	@Nullable
	private List<String> managedClassNames;
	private boolean excludeUnlistedClasses;
	private ClassLoader classLoader;

	public PersistenceUnitInfoImpl(@Nonnull String name) {
		this.name = name;
	}

	@Override
	@Nonnull
	public String getPersistenceUnitName() {
		return name;
	}

	@Override
	@Nullable
	public String getScopeAnnotationName() {
		return scopeAnnotationName;
	}

	public void setScopeAnnotationName(@Nullable String scopeAnnotationName) {
		this.scopeAnnotationName = scopeAnnotationName;
	}

	@Override
	@Nullable
	public List<String> getQualifierAnnotationNames() {
		return qualifierAnnotationNames;
	}

	public void setQualifierAnnotationNames(@Nullable List<String> qualifierAnnotationNames) {
		this.qualifierAnnotationNames = qualifierAnnotationNames;
	}

	@Override
	@Nonnull
	public Properties getProperties() {
		return properties;
	}

	@Override
	@Nullable
	public String getPersistenceProviderClassName() {
		return HibernatePersistenceProvider.class.getName();
	}

	@Override
	@Nonnull
	public PersistenceUnitTransactionType getTransactionType() {
		return transactionType;
	}

	public void setTransactionType(@Nonnull PersistenceUnitTransactionType transactionType) {
		this.transactionType = transactionType;
	}

	@Override
	@Nonnull
	public SharedCacheMode getSharedCacheMode() {
		return cacheMode;
	}

	public void setCacheMode(@Nonnull SharedCacheMode cacheMode) {
		this.cacheMode = cacheMode;
	}

	@Override
	@Nonnull
	public ValidationMode getValidationMode() {
		return validationMode;
	}

	@Override
	@Nonnull
	public FetchType getDefaultToOneFetchType() {
		return defaultToOneFetchType;
	}

	public void setDefaultToOneFetchType(@Nonnull FetchType defaultToOneFetchType) {
		this.defaultToOneFetchType = defaultToOneFetchType;
	}

	public void setValidationMode(@Nonnull ValidationMode validationMode) {
		this.validationMode = validationMode;
	}

	@Override
	@Nonnull
	public List<String> getMappingFileNames() {
		return mappingFiles == null ? emptyList() : mappingFiles;
	}

	public void applyMappingFiles(String... mappingFiles) {
		if ( this.mappingFiles == null ) {
			this.mappingFiles = new ArrayList<>();
		}
		addAll( this.mappingFiles, mappingFiles );
	}

	@Override
	@Nonnull
	public List<String> getManagedClassNames() {
		return managedClassNames == null ? emptyList() : managedClassNames;
	}

	@Override
	@Nonnull
	public List<String> getAllClassNames() {
		return getManagedClassNames();
	}

	public void applyManagedClassNames(@Nonnull String... managedClassNames) {
		if ( this.managedClassNames == null ) {
			this.managedClassNames = new ArrayList<>();
		}
		addAll( this.managedClassNames, managedClassNames );
	}

	@Override
	public boolean excludeUnlistedClasses() {
		return excludeUnlistedClasses;
	}

	public void setExcludeUnlistedClasses(boolean excludeUnlistedClasses) {
		this.excludeUnlistedClasses = excludeUnlistedClasses;
	}

	@Override
	@Nonnull
	public ClassLoader getClassLoader() {
		return classLoader;
	}

	public void setClassLoader(@Nonnull ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	@Nonnull
	public String getPersistenceXMLSchemaVersion() {
		return null;
	}

	@Override
	@Nullable
	public DataSource getJtaDataSource() {
		return null;
	}

	@Override
	@Nullable
	public DataSource getNonJtaDataSource() {
		return null;
	}

	@Override
	@Nonnull
	public List<URL> getJarFileUrls() {
		return null;
	}

	@Override
	@Nonnull
	public URL getPersistenceUnitRootUrl() {
		return null;
	}

	@Override
	public void addTransformer(@Nonnull ClassTransformer transformer) {

	}

	@Override
	@Nonnull
	public ClassLoader getNewTempClassLoader() {
		return null;
	}
}
