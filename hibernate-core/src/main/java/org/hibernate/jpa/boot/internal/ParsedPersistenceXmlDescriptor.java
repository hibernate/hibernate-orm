/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.boot.internal;

import jakarta.persistence.FetchType;
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import org.hibernate.boot.archive.internal.ArchiveHelper;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.spi.ClassTransformer;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.hibernate.jpa.internal.JpaLogger.JPA_LOGGER;

/// [PersistenceUnitDescriptor] implementation describing the information
/// gleaned from a `<persistence-unit/>` element in a `persistence.xml` when
/// Hibernate itself parses the `persistence.xml` file.
///
/// @author Steve Ebersole
public class ParsedPersistenceXmlDescriptor implements PersistenceUnitDescriptor {
	private final URL persistenceUnitRootUrl;

	private String name;
	private String providerClassName;

	private boolean excludeUnlistedClasses;
	private FetchType defaultToOneFetchType;
	private boolean useQuotedIdentifiers;
	private final List<String> classes = new ArrayList<>();
	private final List<String> mappingFiles = new ArrayList<>();
	private final List<URL> jarFileUrls = new ArrayList<>();

	private PersistenceUnitTransactionType transactionType;
	private Object nonJtaDataSource;
	private Object jtaDataSource;

	private ValidationMode validationMode;
	private SharedCacheMode sharedCacheMode;

	private final Properties properties = new Properties();

	public ParsedPersistenceXmlDescriptor(URL persistenceUnitRootUrl) {
		this.persistenceUnitRootUrl = persistenceUnitRootUrl;
	}

	@Override
	public URL getPersistenceUnitRootUrl() {
		return persistenceUnitRootUrl;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public FetchType getDefaultToOneFetchType() {
		return defaultToOneFetchType;
	}

	public void setDefaultToOneFetchType(FetchType defaultToOneFetchType) {
		this.defaultToOneFetchType = defaultToOneFetchType;
	}

	@Override
	public Object getNonJtaDataSource() {
		return nonJtaDataSource;
	}

	public void setNonJtaDataSource(Object nonJtaDataSource) {
		this.nonJtaDataSource = nonJtaDataSource;
	}

	@Override
	public Object getJtaDataSource() {
		return jtaDataSource;
	}

	public void setJtaDataSource(Object jtaDataSource) {
		this.jtaDataSource = jtaDataSource;
	}

	@Override
	public String getProviderClassName() {
		return providerClassName;
	}

	public void setProviderClassName(String providerClassName) {
		this.providerClassName = providerClassName;
	}

	@Override
	public PersistenceUnitTransactionType getPersistenceUnitTransactionType() {
		return transactionType;
	}

	public void setTransactionType(PersistenceUnitTransactionType transactionType) {
		this.transactionType = transactionType;
	}

	@Override
	public boolean isUseQuotedIdentifiers() {
		return useQuotedIdentifiers;
	}

	public void setUseQuotedIdentifiers(boolean useQuotedIdentifiers) {
		this.useQuotedIdentifiers = useQuotedIdentifiers;
	}

	@Override
	public Properties getProperties() {
		return properties;
	}

	@Override
	public boolean isExcludeUnlistedClasses() {
		return excludeUnlistedClasses;
	}

	public void setExcludeUnlistedClasses(boolean excludeUnlistedClasses) {
		this.excludeUnlistedClasses = excludeUnlistedClasses;
	}

	@Override
	public ValidationMode getValidationMode() {
		return validationMode;
	}

	public void setValidationMode(ValidationMode validationMode) {
		this.validationMode = validationMode;
	}

	public void setValidationMode(String validationMode) {
		setValidationMode( ValidationMode.valueOf( validationMode ) );
	}

	@Override
	public SharedCacheMode getSharedCacheMode() {
		return sharedCacheMode;
	}

	public void setSharedCacheMode(SharedCacheMode sharedCacheMode) {
		this.sharedCacheMode = sharedCacheMode;
	}

	public void setSharedCacheMode(String sharedCacheMode) {
		setSharedCacheMode( SharedCacheMode.valueOf( sharedCacheMode ) );
	}

	@Override
	public List<String> getManagedClassNames() {
		return classes;
	}

	@Override
	public List<String> getAllClassNames() {
		return classes;
	}

	public void addClasses(String... classes) {
		addClasses( Arrays.asList( classes ) );
	}

	public void addClasses(List<String> classes) {
		this.classes.addAll( classes );
	}

	@Override
	public List<String> getMappingFileNames() {
		return mappingFiles;
	}

	public void addMappingFiles(String... mappingFiles) {
		addMappingFiles( Arrays.asList( mappingFiles ) );
	}

	public void addMappingFiles(List<String> mappingFiles) {
		this.mappingFiles.addAll( mappingFiles );
	}

	@Override
	public List<URL> getJarFileUrls() {
		return jarFileUrls;
	}

	public void addJarFileUrl(URL jarFileUrl) {
		jarFileUrls.add( jarFileUrl );
	}

	public void addJarFileRefs(List<String> jarFiles) {
		try (URLClassLoader urlClassLoader = new URLClassLoader( new URL[] {persistenceUnitRootUrl},
				Thread.currentThread().getContextClassLoader() )) {
			jarFiles.forEach(
					jarFile -> addJarFileUrl( ArchiveHelper.resolveJarFileReference( jarFile, urlClassLoader ) ) );
		}
		catch (IOException ignore) {
		}
	}

	@Override
	public ClassLoader getClassLoader() {
		return null;
	}

	@Override
	public ClassLoader getTempClassLoader() {
		return null;
	}

	@Override
	public boolean isClassTransformerRegistrationDisabled() {
		return true;
	}

	@Override
	public ClassTransformer pushClassTransformer(EnhancementContext enhancementContext) {
		if ( JPA_LOGGER.isDebugEnabled() ) {
			JPA_LOGGER.pushingClassTransformerUnsupported( getName() );
		}
		return null;
	}
}
