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
package org.hibernate.ejb.boot.spi;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitTransactionType;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * @author Steve Ebersole
 */
public interface PersistenceUnitDescriptor {
	URL getPersistenceUnitRootUrl();

	String getName();

	Object getNonJtaDataSource();

	Object getJtaDataSource();

	String getProviderClassName();

	PersistenceUnitTransactionType getTransactionType();

	boolean isUseQuotedIdentifiers();

	Properties getProperties();

	boolean isExcludeUnlistedClasses();

	ValidationMode getValidationMode();

	SharedCacheMode getSharedCacheMode();

	List<String> getManagedClassNames();

	List<String> getMappingFileNames();

	List<URL> getJarFileUrls();

	public void pushClassTransformer(List<String> entityClassNames);
}
