/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.LoadState;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.ProviderUtil;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.UnloadedClass;
import org.hibernate.bytecode.enhance.spi.UnloadedField;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.jpa.boot.spi.PersistenceConfigurationDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceXmlParser;
import org.hibernate.jpa.internal.enhance.EnhancingClassTransformerImpl;
import org.hibernate.jpa.internal.util.PersistenceUtilHelper;
import org.hibernate.jpa.internal.util.PersistenceUtilHelper.MetadataCache;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static org.hibernate.cfg.BytecodeSettings.BYTECODE_PROVIDER_INSTANCE;
import static org.hibernate.cfg.BytecodeSettings.ENHANCER_ENABLE_ASSOCIATION_MANAGEMENT;
import static org.hibernate.cfg.BytecodeSettings.ENHANCER_ENABLE_DIRTY_TRACKING;
import static org.hibernate.cfg.BytecodeSettings.ENHANCER_ENABLE_LAZY_INITIALIZATION;
import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;
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
	private final Set<TransformerKey> unitsWithTransformer = new HashSet<>();

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

	private EntityManagerFactoryBuilder getEntityManagerFactoryBuilderOrNull(
			String persistenceUnitName,
			Map<?,?> properties,
			@Nullable ClassLoader providedClassLoader,
			@Nullable ClassLoaderService providedClassLoaderService) {
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

	@Override
	public boolean generateSchema(PersistenceConfiguration persistenceConfiguration) {
		JPA_LOGGER.startingGenerateSchema( persistenceConfiguration.name() );
		final var builder = getEntityManagerFactoryBuilder(persistenceConfiguration);
		builder.generateSchema();
		return true;
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
		return ProviderUtilImpl.INSTANCE;
	}

	private record TransformerKey(String puName, String loaderName) {}

	@Override
	public ClassTransformer getClassTransformer(PersistenceUnitInfo persistenceUnit, Map<?, ?> integrationSettings) {
		// todo (jpa4) : If this method is called, be sure to not push the transform via `PersistentUnitInfo#addTransformer`.
		//		See usage of `PersistenceUnitDescriptor#pushClassTransformer` in `EntityManagerFactoryBuilderImpl`.
		// 		Leverage `unitsWithTransformer` in `#createContainerEntityManagerFactory`
		var transformerKey = new TransformerKey( persistenceUnit.getPersistenceUnitName(), persistenceUnit.getClassLoader().getName() );
		if ( !unitsWithTransformer.add( transformerKey ) ) {
			if ( JPA_LOGGER.isTraceEnabled() ) {
				JPA_LOGGER.duplicatedRequestForClassTransformer( transformerKey.puName, transformerKey.loaderName );
			}
			return null;
		}

		if ( JPA_LOGGER.isTraceEnabled() ) {
			JPA_LOGGER.requestForClassTransformer( transformerKey.puName, transformerKey.loaderName );
		}

		//noinspection removal
		final boolean dirtyTrackingEnabled = resolveEnhancementProperty(
				ENHANCER_ENABLE_DIRTY_TRACKING,
				integrationSettings,
				persistenceUnit,
				true
		);
		//noinspection removal
		final boolean lazyInitializationEnabled = resolveEnhancementProperty(
				ENHANCER_ENABLE_LAZY_INITIALIZATION,
				integrationSettings,
				persistenceUnit,
				true
		);
		final boolean associationManagementEnabled = resolveEnhancementProperty(
				ENHANCER_ENABLE_ASSOCIATION_MANAGEMENT,
				integrationSettings,
				persistenceUnit,
				false
		);

		if ( !lazyInitializationEnabled ) {
			//noinspection removal
			DEPRECATION_LOGGER.deprecatedSettingForRemoval( ENHANCER_ENABLE_LAZY_INITIALIZATION, "true" );
		}
		if ( !dirtyTrackingEnabled ) {
			//noinspection removal
			DEPRECATION_LOGGER.deprecatedSettingForRemoval( ENHANCER_ENABLE_DIRTY_TRACKING, "true" );
		}

		if ( dirtyTrackingEnabled || lazyInitializationEnabled || associationManagementEnabled ) {
			final var classLoader = persistenceUnit.getNewTempClassLoader();
			if ( classLoader == null ) {
				throw new PersistenceException( String.format( Locale.ROOT,
						"[persistence unit: %s] Enhancement requires a temp class loader, but none was given",
						persistenceUnit.getPersistenceUnitName()
				) );
			}

			final var enhancementContext = createEnhancementContext(
					dirtyTrackingEnabled,
					lazyInitializationEnabled,
					associationManagementEnabled,
					integrationSettings,
					persistenceUnit
			);

			final EnhancingClassTransformerImpl classTransformer =
					new EnhancingClassTransformerImpl( enhancementContext );

			// NOTE : the ClassTransformer method is called discoverType, but in reality it
			// pre-enhances the classes...
			persistenceUnit.getAllManagedClassNames().forEach( (className) -> {
				classTransformer.discoverTypes( classLoader, className );
			} );

			return classTransformer;
		}

		return null;
	}

	private boolean resolveEnhancementProperty(
			String propertyName,
			Map<?, ?> integrationSettings,
			PersistenceUnitInfo persistenceUnitInfo,
			boolean defaultValue) {
		// prefer integration settings
		var integrationSetting = integrationSettings.get( propertyName );
		if ( integrationSetting != null ) {
			return Boolean.parseBoolean( integrationSetting.toString() );
		}

		// check the persistence unit config
		var unitSetting = persistenceUnitInfo.getProperties().get( propertyName );
		if ( unitSetting != null ) {
			return Boolean.parseBoolean( unitSetting.toString() );
		}

		return defaultValue;
	}

	protected EnhancementContext createEnhancementContext(
			final boolean dirtyTrackingEnabled,
			final boolean lazyInitializationEnabled,
			final boolean associationManagementEnabled,
			Map<?, ?> integrationSettings,
			PersistenceUnitInfo persistenceUnit) {
		var overriddenBytecodeProvider = getExplicitBytecodeProvider( integrationSettings, persistenceUnit );

		return new DefaultEnhancementContext() {
			@Override
			public boolean isEntityClass(UnloadedClass classDescriptor) {
				return persistenceUnit.getAllManagedClassNames().contains( classDescriptor.getName() )
					&& super.isEntityClass( classDescriptor );
			}

			@Override
			public boolean isCompositeClass(UnloadedClass classDescriptor) {
				return persistenceUnit.getAllManagedClassNames().contains( classDescriptor.getName() )
					&& super.isCompositeClass( classDescriptor );
			}

			@Override
			public boolean doBiDirectionalAssociationManagement(UnloadedField field) {
				return associationManagementEnabled;
			}

			@Override
			public boolean doDirtyCheckingInline(UnloadedClass classDescriptor) {
				return dirtyTrackingEnabled;
			}

			@Override
			public boolean hasLazyLoadableAttributes(UnloadedClass classDescriptor) {
				return lazyInitializationEnabled;
			}

			@Override
			public boolean isLazyLoadable(UnloadedField field) {
				return lazyInitializationEnabled;
			}

			@Override
			public boolean doExtendedEnhancement(UnloadedClass classDescriptor) {
				// doesn't make any sense to have extended enhancement enabled at runtime. we only enhance entities anyway.
				return false;
			}

			@Override
			public BytecodeProvider getBytecodeProvider() {
				return overriddenBytecodeProvider;
			}
		};
	}

	private BytecodeProvider getExplicitBytecodeProvider(
			Map<?, ?> integrationSettings,
			PersistenceUnitInfo persistenceUnit) {
		// again, prefer integration settings
		var setting = integrationSettings.get( BYTECODE_PROVIDER_INSTANCE );
		if ( setting == null ) {
			setting = persistenceUnit.getProperties().get( BYTECODE_PROVIDER_INSTANCE );
		}

		if ( setting != null && ! (setting instanceof BytecodeProvider) ) {
			throw new PersistenceException( String.format( Locale.ROOT,
					"Property %s was set to `%s`, which is not compatible with the expected type %s",
					BYTECODE_PROVIDER_INSTANCE,
					setting,
					BytecodeProvider.class.getName()
			) );
		}

		return (BytecodeProvider) setting;
	}


	private static class ProviderUtilImpl implements ProviderUtil {
		public static final ProviderUtilImpl INSTANCE = new ProviderUtilImpl();

		private final MetadataCache cache = new MetadataCache();

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
	}
}
