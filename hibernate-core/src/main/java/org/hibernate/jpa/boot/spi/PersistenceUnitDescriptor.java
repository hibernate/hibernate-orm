/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.boot.spi;

import java.net.URL;
import java.util.List;
import java.util.Properties;

import jakarta.persistence.FetchType;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.PersistenceUnitTransactionType;

import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.spi.ClassTransformer;

/// Abstraction for dealing with `<persistence-unit/>` information
/// specified in the `persistence.xml` file.  This information can
/// come from either:
///
///   - from the Jakarta EE container as an instance of
///     [jakarta.persistence.spi.PersistenceUnitInfo]
///   - in an SE environment, parsed by Hibernate itself
///
/// @see jakarta.persistence.spi.PersistenceUnitInfo
/// @see org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl.JaxbPersistenceUnitImpl
///
/// @author Steve Ebersole
public interface PersistenceUnitDescriptor {
	/// The root url for the persistence unit.
	///
	/// @implNote When Hibernate performs scanning, this URL is used as the base for scanning.
	URL getPersistenceUnitRootUrl();

	/// The persistence unit name.
	///
	/// @see org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl.JaxbPersistenceUnitImpl#getName
	String getName();

	/// The explicitly specified provider class name, or `null` if not specified.
	///
	/// @see jakarta.persistence.spi.PersistenceUnitInfo#getPersistenceProviderClassName
	/// @see org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl.JaxbPersistenceUnitImpl#getProvider
	String getProviderClassName();

	/// Whether the use of identifier quoting is in effect for this whole persistence unit.
	boolean isUseQuotedIdentifiers();

	/// Whether scanning for classes should be performed.  If not, the list of classes available is limited to:
	///   - classes listed in [#getManagedClassNames()]
	///   - classes named in all [#getMappingFileNames]
	///   - classes discovered in [#getJarFileUrls]
	///
	/// @see jakarta.persistence.spi.PersistenceUnitInfo#excludeUnlistedClasses
	/// @see org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl.JaxbPersistenceUnitImpl#isExcludeUnlistedClasses
	boolean isExcludeUnlistedClasses();

	/// @see jakarta.persistence.spi.PersistenceUnitInfo#getDefaultToOneFetchType()
	/// @see org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl.JaxbPersistenceUnitImpl#getDefaultToOneFetchType
	///
	/// @since 8.0
	FetchType getDefaultToOneFetchType();

	/// @see jakarta.persistence.spi.PersistenceUnitInfo#getTransactionType()
	/// @see org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl.JaxbPersistenceUnitImpl#getTransactionType
	PersistenceUnitTransactionType getPersistenceUnitTransactionType();

	/// @see jakarta.persistence.spi.PersistenceUnitInfo#getValidationMode
	/// @see org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl.JaxbPersistenceUnitImpl#getValidationMode
	ValidationMode getValidationMode();

	/// @see jakarta.persistence.spi.PersistenceUnitInfo#getSharedCacheMode
	/// @see org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl.JaxbPersistenceUnitImpl#getSharedCacheMode
	SharedCacheMode getSharedCacheMode();

	/// @see jakarta.persistence.spi.PersistenceUnitInfo#getManagedClassNames
	/// @see org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl.JaxbPersistenceUnitImpl#getClasses
	List<String> getManagedClassNames();

	/// @see jakarta.persistence.spi.PersistenceUnitInfo#getMappingFileNames
	/// @see org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl.JaxbPersistenceUnitImpl#getMappingFiles
	List<String> getMappingFileNames();

	/// @see jakarta.persistence.spi.PersistenceUnitInfo#getJarFileUrls
	/// @see org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl.JaxbPersistenceUnitImpl#getJarFiles
	List<URL> getJarFileUrls();

	/// @see jakarta.persistence.spi.PersistenceUnitInfo#getNonJtaDataSource
	/// @see org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl.JaxbPersistenceUnitImpl#getNonJtaDataSource
	Object getNonJtaDataSource();

	/// @see jakarta.persistence.spi.PersistenceUnitInfo#getJtaDataSource
	/// @see org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl.JaxbPersistenceUnitImpl#getJtaDataSource
	Object getJtaDataSource();

	/// @see jakarta.persistence.spi.PersistenceUnitInfo#getProperties
	/// @see org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl.JaxbPersistenceUnitImpl#getPropertyContainer
	Properties getProperties();

	/// @see jakarta.persistence.spi.PersistenceUnitInfo#getClassLoader
	ClassLoader getClassLoader();

	/// @see jakarta.persistence.spi.PersistenceUnitInfo#getNewTempClassLoader
	ClassLoader getTempClassLoader();

	boolean isClassTransformerRegistrationDisabled();

	ClassTransformer pushClassTransformer(EnhancementContext enhancementContext);
}
