/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.boot.spi;

import jakarta.persistence.FetchType;
import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.spi.ClassTransformer;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.discovery.archive.internal.StandardArchiveDescriptorFactory;
import org.hibernate.jpa.boot.discovery.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.jpa.boot.discovery.internal.StandardDiscovery;
import org.hibernate.jpa.boot.discovery.spi.Boundaries;
import org.hibernate.jpa.boot.discovery.spi.Discovery;
import org.hibernate.jpa.boot.discovery.spi.DiscoveryProvider;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import static org.hibernate.jpa.internal.JpaLogger.JPA_LOGGER;

/**
 * PersistenceUnitDescriptor wrapper around {@linkplain PersistenceConfiguration}
 *
 * @author Steve Ebersole
 */
public class PersistenceConfigurationDescriptor implements PersistenceUnitDescriptor {
	private final PersistenceConfiguration persistenceConfiguration;

	private final Properties properties;
	private final List<String> managedClassNames;
	private final List<String> allClassNames;

	public PersistenceConfigurationDescriptor(PersistenceConfiguration persistenceConfiguration) {
		this.persistenceConfiguration = persistenceConfiguration;
		this.properties = CollectionHelper.asProperties( persistenceConfiguration.properties() );
		this.managedClassNames = persistenceConfiguration.managedClasses().stream().map( Class::getName ).toList();

		this.allClassNames = findAllClasses( managedClassNames, persistenceConfiguration );
	}

	@Override
	public Properties getProperties() {
		return properties;
	}

	@Override
	public String getName() {
		return persistenceConfiguration.name();
	}

	@Override
	public String getProviderClassName() {
		return persistenceConfiguration.provider();
	}

	@Override
	public boolean isUseQuotedIdentifiers() {
		return properties.get( AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS ) == Boolean.TRUE;
	}

	@Override
	public boolean isExcludeUnlistedClasses() {
		// if we do not know the root url nor jar files we cannot do scanning
		return !(persistenceConfiguration instanceof HibernatePersistenceConfiguration configuration)
			|| configuration.rootUrl() == null && configuration.jarFileUrls().isEmpty();
	}

	@Override
	public FetchType getDefaultToOneFetchType() {
		return persistenceConfiguration.defaultToOneFetchType();
	}

	@Override
	public PersistenceUnitTransactionType getPersistenceUnitTransactionType() {
		return persistenceConfiguration.transactionType();
	}

	@Override
	public List<String> getManagedClassNames() {
		return managedClassNames;
	}

	@Override
	public List<String> getAllClassNames() {
		return allClassNames;
	}

	@Override
	public List<String> getMappingFileNames() {
		return persistenceConfiguration.mappingFiles();
	}

	@Override
	public Object getNonJtaDataSource() {
		return persistenceConfiguration.nonJtaDataSource();
	}

	@Override
	public Object getJtaDataSource() {
		return persistenceConfiguration.jtaDataSource();
	}

	@Override
	public ValidationMode getValidationMode() {
		return persistenceConfiguration.validationMode();
	}

	@Override
	public SharedCacheMode getSharedCacheMode() {
		return persistenceConfiguration.sharedCacheMode();
	}

	@Override
	public ClassLoader getClassLoader() {
		return HibernatePersistenceProvider.class.getClassLoader();
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

	@Override
	public URL getPersistenceUnitRootUrl() {
		return persistenceConfiguration instanceof HibernatePersistenceConfiguration configuration
				? configuration.rootUrl()
				: null;
	}

	@Override
	public List<URL> getJarFileUrls() {
		return persistenceConfiguration instanceof HibernatePersistenceConfiguration configuration
				? configuration.jarFileUrls()
				: null;
	}

	private static List<String> findAllClasses(
			List<String> managedClassNames,
			PersistenceConfiguration persistenceConfiguration) {
		// todo (jpa4) : determine a way to incorporate BootstrapContext into this process

		final var allClasses = new HashSet<>( managedClassNames );

		final ClassLoaderService classLoaderService;
		final Boundaries boundaries;
		if ( persistenceConfiguration instanceof HibernatePersistenceConfiguration hibernatePersistenceConfiguration ) {
			final List<URL> urls = new ArrayList<>();
			if ( hibernatePersistenceConfiguration.rootUrl()  != null ) {
				urls.add( hibernatePersistenceConfiguration.rootUrl() );
			}
			if ( CollectionHelper.isNotEmpty( hibernatePersistenceConfiguration.jarFileUrls() ) ) {
				urls.addAll( hibernatePersistenceConfiguration.jarFileUrls() );
			}
			final ClassLoader loader = new URLClassLoader(
					urls.toArray( new URL[0] ),
					Thread.currentThread().getContextClassLoader()
			);

			classLoaderService = new ClassLoaderServiceImpl( loader );
			boundaries = new ScanningBoundaries( hibernatePersistenceConfiguration );
		}
		else {
			classLoaderService = new ClassLoaderServiceImpl( Thread.currentThread().getContextClassLoader() );
			boundaries = new ScanningBoundaries( persistenceConfiguration );
		}

		final Discovery discovery;
		final Collection<DiscoveryProvider> discoveryProviders = classLoaderService.loadJavaServices( DiscoveryProvider.class );
		if ( CollectionHelper.isNotEmpty( discoveryProviders ) ) {
			discovery = discoveryProviders.iterator().next().builderScanner(
					determineArchiveDescriptorFactory( persistenceConfiguration, classLoaderService ),
					classLoaderService,
					persistenceConfiguration.properties()
			);
		}
		else {
			discovery = new StandardDiscovery( determineArchiveDescriptorFactory( persistenceConfiguration, classLoaderService ), classLoaderService );
		}

		discovery.discoverClassNames( boundaries, allClasses::add );
		return new ArrayList<>( allClasses );
	}

	private static ArchiveDescriptorFactory determineArchiveDescriptorFactory(PersistenceConfiguration persistenceConfiguration, ClassLoaderService classLoaderService) {
		final Object setting = persistenceConfiguration.properties().get( AvailableSettings.SCANNER_ARCHIVE_INTERPRETER );
		if ( setting instanceof ArchiveDescriptorFactory ref ) {
			return ref;
		}
		else if ( setting instanceof Class implClass ) {
			try {
				return (ArchiveDescriptorFactory) implClass.getDeclaredConstructor().newInstance();
			}
			catch (Exception e) {
				throw new HibernateException( "Unable to instantiate configured ArchiveDescriptorFactory - " + implClass.getName(), e );
			}
		}
		else if ( setting != null ) {
			var implClassName = setting.toString();
			var implClass = classLoaderService.classForName( implClassName );
			try {
				return (ArchiveDescriptorFactory) implClass.getDeclaredConstructor().newInstance();
			}
			catch (Exception e) {
				throw new HibernateException( "Unable to instantiate configured ArchiveDescriptorFactory - " + implClass.getName(), e );
			}
		}
		return new StandardArchiveDescriptorFactory();
	}

	private static class ScanningBoundaries implements Boundaries {
		private final List<URL> urls;
		private final List<String> mappingFiles;

		public ScanningBoundaries(HibernatePersistenceConfiguration persistenceConfiguration) {
			this.urls = collect( persistenceConfiguration.rootUrl(), persistenceConfiguration.jarFileUrls() );
			this.mappingFiles = persistenceConfiguration.mappingFiles();
		}

		private List<URL> collect(URL rootUrl, List<URL> nonRootUrls) {
			final List<URL> result = new ArrayList<>();
			if ( rootUrl != null ) {
				result.add( rootUrl );
			}
			if ( nonRootUrls != null ) {
				result.addAll( nonRootUrls );
			}
			return result;
		}

		public ScanningBoundaries(PersistenceConfiguration persistenceConfiguration) {
			this.urls = null;
			this.mappingFiles = persistenceConfiguration.mappingFiles();
		}

		@Override
		public List<URL> getUrls() {
			return urls;
		}

		@Override
		public List<String> getMappingFiles() {
			return mappingFiles;
		}
	}
}
