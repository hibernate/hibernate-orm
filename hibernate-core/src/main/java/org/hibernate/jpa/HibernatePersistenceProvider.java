/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.spi.LoadState;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.ProviderUtil;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jpa.boot.spi.PersistenceConfigurationDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceXmlParser;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.boot.spi.ProviderChecker;
import org.hibernate.jpa.internal.util.PersistenceUtilHelper;

/**
 * The best-ever implementation of a JPA {@link PersistenceProvider}.
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public class HibernatePersistenceProvider implements PersistenceProvider {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( HibernatePersistenceProvider.class );

	private final PersistenceUtilHelper.MetadataCache cache = new PersistenceUtilHelper.MetadataCache();

	/**
	 * {@inheritDoc}
	 *
	 * @implSpec Per the specification, the values passed as {@code properties} override values found in {@code persistence.xml}
	 */
	@Override
	public EntityManagerFactory createEntityManagerFactory(String persistenceUnitName, Map properties) {
		log.tracef( "Starting createEntityManagerFactory for persistenceUnitName %s", persistenceUnitName );
		final EntityManagerFactoryBuilder builder = getEntityManagerFactoryBuilderOrNull( persistenceUnitName, properties );
		if ( builder == null ) {
			log.trace( "Could not obtain matching EntityManagerFactoryBuilder, returning null" );
			return null;
		}
		return builder.build();
	}

	protected EntityManagerFactoryBuilder getEntityManagerFactoryBuilderOrNull(String persistenceUnitName, Map<?,?> properties) {
		return getEntityManagerFactoryBuilderOrNull( persistenceUnitName, properties, null, null );
	}

	protected EntityManagerFactoryBuilder getEntityManagerFactoryBuilderOrNull(String persistenceUnitName, Map<?,?> properties,
			ClassLoader providedClassLoader) {
		return getEntityManagerFactoryBuilderOrNull( persistenceUnitName, properties, providedClassLoader, null );
	}

	protected EntityManagerFactoryBuilder getEntityManagerFactoryBuilderOrNull(String persistenceUnitName, Map<?,?> properties,
			ClassLoaderService providedClassLoaderService) {
		return getEntityManagerFactoryBuilderOrNull( persistenceUnitName, properties, null, providedClassLoaderService );
	}

	private EntityManagerFactoryBuilder getEntityManagerFactoryBuilderOrNull(String persistenceUnitName, Map<?,?> properties,
			ClassLoader providedClassLoader, ClassLoaderService providedClassLoaderService) {
		log.tracef( "Attempting to obtain correct EntityManagerFactoryBuilder for persistenceUnitName : %s", persistenceUnitName );

		final Map<?,?> integration = wrap( properties );
		final Collection<PersistenceUnitDescriptor> units = locatePersistenceUnits( integration, providedClassLoader, providedClassLoaderService );

		log.tracef( "Located and parsed %s persistence units; checking each", units.size() );

		if ( persistenceUnitName == null && units.size() > 1 ) {
			// no persistence-unit name to look for was given and we found multiple persistence-units
			throw new PersistenceException( "No name provided and multiple persistence units found" );
		}

		for ( PersistenceUnitDescriptor persistenceUnit : units ) {
			if ( log.isTraceEnabled() ) {
				log.tracef(
						"Checking persistence-unit [name=%s, explicit-provider=%s] against incoming persistence unit name [%s]",
						persistenceUnit.getName(),
						persistenceUnit.getProviderClassName(),
						persistenceUnitName
				);
			}

			final boolean matches = persistenceUnitName == null || persistenceUnit.getName().equals( persistenceUnitName );
			if ( !matches ) {
				log.tracef( "Excluding from consideration due to name mismatch" );
				continue;
			}

			// See if we (Hibernate) are the persistence provider
			if ( ! ProviderChecker.isProvider( persistenceUnit, properties ) ) {
				log.tracef( "Excluding from consideration due to provider mismatch" );
				continue;
			}

			if ( providedClassLoaderService != null ) {
				return getEntityManagerFactoryBuilder( persistenceUnit, integration, providedClassLoaderService );
			}
			else {
				return getEntityManagerFactoryBuilder( persistenceUnit, integration, providedClassLoader );
			}
		}

		log.debug( "Found no matching persistence units" );
		return null;
	}

	protected static Map<?,?> wrap(Map<?,?> properties) {
		return properties == null ? Collections.emptyMap() : Collections.unmodifiableMap( properties );
	}

	// Check before changing: may be overridden in Quarkus
	protected Collection<PersistenceUnitDescriptor> locatePersistenceUnits(Map<?, ?> integration, ClassLoader providedClassLoader,
			ClassLoaderService providedClassLoaderService) {
		final Collection<PersistenceUnitDescriptor> units;
		try {
			var parser = PersistenceXmlParser.create( integration, providedClassLoader, providedClassLoaderService );
			final List<URL> xmlUrls = parser.getClassLoaderService().locateResources( "META-INF/persistence.xml" );
			if ( xmlUrls.isEmpty() ) {
				log.unableToFindPersistenceXmlInClasspath();
				units = List.of();
			}
			else {
				units = parser.parse( xmlUrls ).values();
			}
		}
		catch (Exception e) {
			log.debug( "Unable to locate persistence units", e );
			throw new PersistenceException( "Unable to locate persistence units", e );
		}
		return units;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Note: per-spec, the values passed as {@code properties} override values found in {@link PersistenceUnitInfo}
	 */
	@Override
	public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map properties) {
		if ( log.isTraceEnabled() ) {
			log.tracef( "Starting createContainerEntityManagerFactory : %s", info.getPersistenceUnitName() );
		}

		return getEntityManagerFactoryBuilder( info, properties ).build();
	}

	@Override
	public void generateSchema(PersistenceUnitInfo info, Map map) {
		if ( log.isTraceEnabled() ) {
			log.tracef( "Starting generateSchema : PUI.name=%s", info.getPersistenceUnitName() );
		}

		final EntityManagerFactoryBuilder builder = getEntityManagerFactoryBuilder( info, map );
		builder.generateSchema();
	}

	@Override
	public boolean generateSchema(String persistenceUnitName, Map map) {
		log.tracef( "Starting generateSchema for persistenceUnitName %s", persistenceUnitName );

		final EntityManagerFactoryBuilder builder = getEntityManagerFactoryBuilderOrNull( persistenceUnitName, map );
		if ( builder == null ) {
			log.trace( "Could not obtain matching EntityManagerFactoryBuilder, returning false" );
			return false;
		}
		builder.generateSchema();
		return true;
	}

	protected EntityManagerFactoryBuilder getEntityManagerFactoryBuilder(PersistenceUnitInfo info, Map<?,?> integration) {
		return Bootstrap.getEntityManagerFactoryBuilder( info, integration );
	}

	protected EntityManagerFactoryBuilder getEntityManagerFactoryBuilder(PersistenceUnitDescriptor persistenceUnitDescriptor,
			Map<?,?> integration, ClassLoader providedClassLoader) {
		return Bootstrap.getEntityManagerFactoryBuilder( persistenceUnitDescriptor, integration, providedClassLoader );
	}

	protected EntityManagerFactoryBuilder getEntityManagerFactoryBuilder(PersistenceUnitDescriptor persistenceUnitDescriptor,
			Map<?,?> integration, ClassLoaderService providedClassLoaderService) {
		return Bootstrap.getEntityManagerFactoryBuilder( persistenceUnitDescriptor, integration, providedClassLoaderService );
	}

	private final ProviderUtil providerUtil = new ProviderUtil() {
		@Override
		public LoadState isLoadedWithoutReference(Object proxy, String property) {
			return PersistenceUtilHelper.isLoadedWithoutReference( proxy, property, cache );
		}
		@Override
		public LoadState isLoadedWithReference(Object proxy, String property) {
			return PersistenceUtilHelper.isLoadedWithReference( proxy, property, cache );
		}
		@Override
		public LoadState isLoaded(Object object) {
			return PersistenceUtilHelper.getLoadState( object );
		}
	};

	@Override
	public EntityManagerFactory createEntityManagerFactory(PersistenceConfiguration configuration) {
		final EntityManagerFactoryBuilder builder = getEntityManagerFactoryBuilder(
				new PersistenceConfigurationDescriptor( configuration ),
				Collections.emptyMap(),
				HibernatePersistenceProvider.class.getClassLoader()
		);
		return builder.build();
	}

	@Override
	public ProviderUtil getProviderUtil() {
		return providerUtil;
	}

}
