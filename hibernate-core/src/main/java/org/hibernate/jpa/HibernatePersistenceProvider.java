/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa;

import java.util.Collection;
import java.util.HashMap;
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
import org.hibernate.jpa.boot.spi.PersistenceConfigurationDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceXmlParser;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.internal.util.PersistenceUtilHelper;
import org.hibernate.jpa.internal.util.PersistenceUtilHelper.MetadataCache;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static org.hibernate.jpa.boot.spi.ProviderChecker.isProvider;
import static org.hibernate.jpa.internal.JpaLogger.JPA_LOGGER;

/**
 * The best-ever implementation of a JPA {@link PersistenceProvider}.
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public class HibernatePersistenceProvider implements PersistenceProvider {

	private final MetadataCache cache = new MetadataCache();

	/**
	 * {@inheritDoc}
	 *
	 * @implSpec The values passed in the {@code map} override values found
	 *           in {@code persistence.xml} according to the JPA specification.
	 */
	@Override
	public EntityManagerFactory createEntityManagerFactory(String persistenceUnitName, Map<?,?> map) {
		JPA_LOGGER.startingCreateEntityManagerFactory( persistenceUnitName );
		final var builder = getEntityManagerFactoryBuilderOrNull( persistenceUnitName, map );
		if ( builder == null ) {
			JPA_LOGGER.couldNotObtainEmfBuilder("null");
			return null;
		}
		else {
			return builder.build();
		}
	}

	protected EntityManagerFactoryBuilder getEntityManagerFactoryBuilderOrNull(
			String persistenceUnitName, Map<?,?> properties) {
		return getEntityManagerFactoryBuilderOrNull(
				persistenceUnitName,
				properties,
				null,
				null
		);
	}

	protected EntityManagerFactoryBuilder getEntityManagerFactoryBuilderOrNull(
			String persistenceUnitName, Map<?,?> properties,
			ClassLoader providedClassLoader) {
		return getEntityManagerFactoryBuilderOrNull(
				persistenceUnitName,
				properties,
				providedClassLoader,
				null
		);
	}

	protected EntityManagerFactoryBuilder getEntityManagerFactoryBuilderOrNull(
			String persistenceUnitName, Map<?,?> properties,
			ClassLoaderService providedClassLoaderService) {
		return getEntityManagerFactoryBuilderOrNull(
				persistenceUnitName,
				properties,
				null,
				providedClassLoaderService
		);
	}

	private EntityManagerFactoryBuilder getEntityManagerFactoryBuilderOrNull(
			String persistenceUnitName, Map<?,?> properties,
			ClassLoader providedClassLoader, ClassLoaderService providedClassLoaderService) {
		JPA_LOGGER.attemptingToObtainEmfBuilder( persistenceUnitName );
		final var integration = wrap( properties );
		final var units = locatePersistenceUnits( integration, providedClassLoader, providedClassLoaderService );
		JPA_LOGGER.locatedAndParsedPersistenceUnits( units.size() );
		if ( persistenceUnitName == null && units.size() > 1 ) {
			// no persistence-unit name was specified, and we found multiple persistence-units
			throw new PersistenceException( "No name provided and multiple persistence units found" );
		}

		for ( var persistenceUnit : units ) {
			if ( JPA_LOGGER.isTraceEnabled() ) {
				JPA_LOGGER.checkingPersistenceUnitName(
						persistenceUnit.getName(),
						persistenceUnit.getProviderClassName(),
						persistenceUnitName
				);
			}

			final boolean matches =
					persistenceUnitName == null
					|| persistenceUnit.getName().equals( persistenceUnitName );
			if ( matches ) {
				// See if we (Hibernate) are the persistence provider
				if ( isProvider( persistenceUnit, properties ) ) {
					return providedClassLoaderService == null
							? getEntityManagerFactoryBuilder( persistenceUnit, integration, providedClassLoader )
							: getEntityManagerFactoryBuilder( persistenceUnit, integration, providedClassLoaderService );
				}
				else {
					JPA_LOGGER.excludingDueToProviderMismatch();
				}

			}
			else {
				JPA_LOGGER.excludingDueToNameMismatch();
			}
		}

		JPA_LOGGER.foundNoMatchingPersistenceUnits();
		return null;
	}

	protected static Map<?,?> wrap(Map<?,?> properties) {
		return properties == null ? emptyMap() : unmodifiableMap( properties );
	}

	// Check before changing: may be overridden in Quarkus
	protected Collection<PersistenceUnitDescriptor> locatePersistenceUnits(
			Map<?, ?> integration,
			ClassLoader providedClassLoader,
			ClassLoaderService providedClassLoaderService) {
		try {
			final var parser =
					PersistenceXmlParser.create( integration, providedClassLoader, providedClassLoaderService );
			final var xmlUrls =
					parser.getClassLoaderService()
							.locateResources( "META-INF/persistence.xml" );
			if ( xmlUrls.isEmpty() ) {
				JPA_LOGGER.unableToFindPersistenceXmlInClasspath();
				return List.of();
			}
			else {
				return parser.parse( xmlUrls ).values();
			}
		}
		catch (Exception e) {
			JPA_LOGGER.unableToLocatePersistenceUnits( e );
			throw new PersistenceException( "Unable to locate persistence units", e );
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @implSpec The values passed in the {@code map} override values found
	 *           in {@link PersistenceUnitInfo#getProperties()} according to
	 *           the JPA specification.
	 */
	@Override
	public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map<?,?> map) {
		JPA_LOGGER.startingCreateContainerEntityManagerFactory( info.getPersistenceUnitName() );
		return getEntityManagerFactoryBuilder( info, map ).build();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @implSpec The values passed in the {@code map} override values found
	 *           in {@link PersistenceUnitInfo#getProperties()} according to
	 *           the JPA specification.
	 */
	@Override
	public void generateSchema(PersistenceUnitInfo info, Map<?,?> map) {
		JPA_LOGGER.startingGenerateSchemaForPuiName( info.getPersistenceUnitName() );
		getEntityManagerFactoryBuilder( info, map ).generateSchema();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @implSpec The values passed in the {@code map} override values found
	 *           in {@code persistence.xml} according to the JPA specification.
	 */
	@Override
	public boolean generateSchema(String persistenceUnitName, Map<?,?> map) {
		JPA_LOGGER.startingGenerateSchema( persistenceUnitName );
		final var builder = getEntityManagerFactoryBuilderOrNull( persistenceUnitName, map );
		if ( builder == null ) {
			JPA_LOGGER.couldNotObtainEmfBuilder("false");
			return false;
		}
		else {
			builder.generateSchema();
			return true;
		}
	}

	private static Map<String, Object> settingsMap(Map<?, ?> integration) {
		final Map<String, Object> result = new HashMap<>();
		integration.forEach( (key, value) -> {
			// ignore non-string keys
			if (key instanceof String string) {
				result.put( string, value );
			}
		} );
		return result;
	}

	protected EntityManagerFactoryBuilder getEntityManagerFactoryBuilder(
			PersistenceUnitInfo info, Map<?,?> integration) {
		return Bootstrap.getEntityManagerFactoryBuilder( info, settingsMap( integration ) );
	}

	protected EntityManagerFactoryBuilder getEntityManagerFactoryBuilder(
			PersistenceUnitDescriptor persistenceUnitDescriptor,
			Map<?,?> integration,
			ClassLoader providedClassLoader) {
		return Bootstrap.getEntityManagerFactoryBuilder( persistenceUnitDescriptor,
				settingsMap( integration ), providedClassLoader );
	}

	protected EntityManagerFactoryBuilder getEntityManagerFactoryBuilder(
			PersistenceUnitDescriptor persistenceUnitDescriptor,
			Map<?,?> integration,
			ClassLoaderService providedClassLoaderService) {
		return Bootstrap.getEntityManagerFactoryBuilder( persistenceUnitDescriptor,
				settingsMap( integration ), providedClassLoaderService );
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
		return getEntityManagerFactoryBuilder( configuration ).build();
	}

	private EntityManagerFactoryBuilder getEntityManagerFactoryBuilder(PersistenceConfiguration configuration) {
		return getEntityManagerFactoryBuilder(
				new PersistenceConfigurationDescriptor( configuration ),
				emptyMap(),
				HibernatePersistenceProvider.class.getClassLoader()
		);
	}

	@Override
	public ProviderUtil getProviderUtil() {
		return providerUtil;
	}

}
