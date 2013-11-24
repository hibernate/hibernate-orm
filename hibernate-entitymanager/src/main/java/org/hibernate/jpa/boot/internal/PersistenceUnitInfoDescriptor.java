/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.boot.internal;

import java.net.URL;
import java.util.List;
import java.util.Properties;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.internal.instrument.InterceptFieldClassFileTransformer;

/**
 * @author Steve Ebersole
 */
public class PersistenceUnitInfoDescriptor implements PersistenceUnitDescriptor {
	private final PersistenceUnitInfo persistenceUnitInfo;

	public PersistenceUnitInfoDescriptor(PersistenceUnitInfo persistenceUnitInfo) {
		this.persistenceUnitInfo = persistenceUnitInfo;
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
	public PersistenceUnitTransactionType getTransactionType() {
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
	public boolean isExcludeUnlistedClasses() {
		return persistenceUnitInfo.excludeUnlistedClasses();
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
	public void pushClassTransformer(List<String> entityClassNames) {
		persistenceUnitInfo.addTransformer( new InterceptFieldClassFileTransformer( entityClassNames ) );
	}
}
