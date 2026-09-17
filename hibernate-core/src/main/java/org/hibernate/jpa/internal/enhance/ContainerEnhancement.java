/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.internal.enhance;

import jakarta.persistence.PersistenceException;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;
import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.UnloadedClass;
import org.hibernate.bytecode.enhance.spi.UnloadedField;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.jpa.internal.TransformerTracker;
import org.hibernate.jpa.internal.TransformerTracker.TransformerKey;

import java.util.Locale;
import java.util.Map;

import static org.hibernate.cfg.BytecodeSettings.BYTECODE_PROVIDER_INSTANCE;
import static org.hibernate.cfg.BytecodeSettings.ENHANCER_ENABLE_ASSOCIATION_MANAGEMENT;
import static org.hibernate.cfg.BytecodeSettings.ENHANCER_ENABLE_DIRTY_TRACKING;
import static org.hibernate.cfg.BytecodeSettings.ENHANCER_ENABLE_LAZY_INITIALIZATION;
import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;
import static org.hibernate.jpa.internal.JpaLogger.JPA_LOGGER;

/// Shared transformer creation and discovery for Jakarta Persistence container entry points.
///
/// @author Steve Ebersole
public final class ContainerEnhancement {
	private ContainerEnhancement() {
	}

	public static ClassTransformer createTransformer(PersistenceUnitInfo persistenceUnit, Map<?, ?> integrationSettings, ContextFactory contextFactory) {
		final var candidates = org.hibernate.boot.model.process.internal.EnhancementCandidates.forContainer( persistenceUnit );
		var transformerKey = TransformerKey.from( persistenceUnit );
		if ( !TransformerTracker.canSupplyTransformer( transformerKey ) ) {
			if ( JPA_LOGGER.isTraceEnabled() ) {
				JPA_LOGGER.duplicatedRequestForClassTransformer( transformerKey.puName(), transformerKey.loaderName() );
			}
			return null;
		}

		if ( JPA_LOGGER.isTraceEnabled() ) {
			JPA_LOGGER.requestForClassTransformer( transformerKey.puName(), transformerKey.loaderName() );
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

			final var enhancementContext = contextFactory.create(
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
			candidates.forEach( (className) -> {
				classTransformer.discoverTypes( classLoader, className );
			} );

			return classTransformer;
		}

		return null;
	}

	private static boolean resolveEnhancementProperty(
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

	public static EnhancementContext createEnhancementContext(
			final boolean dirtyTrackingEnabled,
			final boolean lazyInitializationEnabled,
			final boolean associationManagementEnabled,
			Map<?, ?> integrationSettings,
			PersistenceUnitInfo persistenceUnit) {
		var overriddenBytecodeProvider = getExplicitBytecodeProvider( integrationSettings, persistenceUnit );

		return new DefaultEnhancementContext() {
			@Override
			public boolean isEntityClass(UnloadedClass classDescriptor) {
				return persistenceUnit.getAllClassNames().contains( classDescriptor.getName() )
					&& super.isEntityClass( classDescriptor );
			}

			@Override
			public boolean isCompositeClass(UnloadedClass classDescriptor) {
				return persistenceUnit.getAllClassNames().contains( classDescriptor.getName() )
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

	private static BytecodeProvider getExplicitBytecodeProvider(
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

	public static ClassTransformer createTransformer(PersistenceUnitInfo unit, Map<?, ?> settings) {
		return createTransformer( unit, settings, ContainerEnhancement::createEnhancementContext );
	}

	@FunctionalInterface
	public interface ContextFactory {
		EnhancementContext create(boolean dirtyTracking, boolean lazyInitialization, boolean associationManagement,
				Map<?, ?> settings, PersistenceUnitInfo unit);
	}
}
