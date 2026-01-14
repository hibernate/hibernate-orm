/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.boot.internal;

import java.net.URL;
import java.util.List;
import java.util.Properties;

import jakarta.persistence.FetchType;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.spi.ClassTransformer;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.internal.enhance.EnhancingClassTransformerImpl;

import jakarta.persistence.PersistenceException;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.PersistenceUnitTransactionType;


import static org.hibernate.jpa.internal.JpaLogger.JPA_LOGGER;

/// Wraps a JPA {@linkplain PersistenceUnitInfo} as Hibernate's {@linkplain PersistenceUnitDescriptor}
///
/// @author Steve Ebersole
public class PersistenceUnitInfoDescriptor implements PersistenceUnitDescriptor {
	private final PersistenceUnitInfo persistenceUnitInfo;
	private final boolean disableClassTransformerRegistration;
	private ClassTransformer classTransformer;

	public PersistenceUnitInfoDescriptor(PersistenceUnitInfo persistenceUnitInfo) {
		this( persistenceUnitInfo, false );
	}

	public PersistenceUnitInfoDescriptor(PersistenceUnitInfo persistenceUnitInfo, boolean disableClassTransformerRegistration) {
		this.persistenceUnitInfo = persistenceUnitInfo;
		this.disableClassTransformerRegistration = disableClassTransformerRegistration;
	}

	@Override
	public URL getPersistenceUnitRootUrl() {
		return persistenceUnitInfo.getPersistenceUnitRootUrl();
	}

	@Override
	public String getName() {
		return persistenceUnitInfo.getPersistenceUnitName();
	}

	@Override
	public Object getNonJtaDataSource() {
		return persistenceUnitInfo.getNonJtaDataSource();
	}

	@Override
	public Object getJtaDataSource() {
		return persistenceUnitInfo.getJtaDataSource();
	}

	@Override
	public String getProviderClassName() {
		return persistenceUnitInfo.getPersistenceProviderClassName();
	}

	@Override
	public PersistenceUnitTransactionType getPersistenceUnitTransactionType() {
		return persistenceUnitInfo.getTransactionType();
	}

	@Override
	public boolean isUseQuotedIdentifiers() {
		return false;
	}

	@Override
	public Properties getProperties() {
		return persistenceUnitInfo.getProperties();
	}

	@Override
	public ClassLoader getClassLoader() {
		return persistenceUnitInfo.getClassLoader();
	}

	@Override
	public ClassLoader getTempClassLoader() {
		return persistenceUnitInfo.getNewTempClassLoader();
	}

	@Override
	public boolean isExcludeUnlistedClasses() {
		return persistenceUnitInfo.excludeUnlistedClasses();
	}

	@Override
	public FetchType getDefaultToOneFetchType() {
		return persistenceUnitInfo.getDefaultToOneFetchType();
	}

	@Override
	public ValidationMode getValidationMode() {
		return persistenceUnitInfo.getValidationMode();
	}

	@Override
	public SharedCacheMode getSharedCacheMode() {
		return persistenceUnitInfo.getSharedCacheMode();
	}

	@Override
	public List<String> getManagedClassNames() {
		return persistenceUnitInfo.getManagedClassNames();
	}

	@Override
	public List<String> getMappingFileNames() {
		return persistenceUnitInfo.getMappingFileNames();
	}

	@Override
	public List<URL> getJarFileUrls() {
		return persistenceUnitInfo.getJarFileUrls();
	}

	@Override
	public boolean isClassTransformerRegistrationDisabled() {
		return disableClassTransformerRegistration;
	}

	@Override
	public ClassTransformer pushClassTransformer(EnhancementContext enhancementContext) {
		if ( this.classTransformer != null ) {
			throw new PersistenceException( "Persistence unit ["
					+ persistenceUnitInfo.getPersistenceUnitName()
					+ "] can only have a single class transformer." );
		}
		// During testing, we will return a null temp class loader
		// in cases where we don't care about enhancement
		if ( persistenceUnitInfo.getNewTempClassLoader() != null ) {
			if ( JPA_LOGGER.isTraceEnabled() ) {
				JPA_LOGGER.pushingClassTransformers( getName(), String.valueOf( enhancementContext.getLoadingClassLoader() ) );
			}
			final EnhancingClassTransformerImpl classTransformer =
					new EnhancingClassTransformerImpl( enhancementContext );
			this.classTransformer = classTransformer;
			persistenceUnitInfo.addTransformer( classTransformer );
		}

		return classTransformer;
	}
}
