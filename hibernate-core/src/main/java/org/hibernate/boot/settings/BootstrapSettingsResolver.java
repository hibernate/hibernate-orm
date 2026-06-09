/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.settings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.boot.CacheRegionDefinition.CacheRegionType;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

import jakarta.persistence.FetchType;

import static org.hibernate.cfg.AvailableSettings.CLASS_CACHE_PREFIX;
import static org.hibernate.cfg.AvailableSettings.COLLECTION_CACHE_PREFIX;

/// Resolves the subset of bootstrap settings used while collecting,
/// categorizing, and binding boot-model sources.
///
/// The resolver is intentionally a normalization boundary.  Callers may arrive
/// with a plain configuration map, a Hibernate-specific
/// [HibernatePersistenceConfiguration], or a parsed [PersistenceUnitDescriptor]
/// plus integration settings.  Each form is collapsed into the same
/// [ResolvedBootstrapSettings] contract before source collection continues.
/// ORM's [Environment#getProperties()] supplies the baseline values, including
/// legacy `/hibernate.properties` handling and system properties; explicit
/// settings then override that baseline.
///
/// This class should own precedence and interpretation rules for the named
/// settings in [ResolvedBootstrapSettings].  Settings that are merely carried to
/// later bootstrap stages should remain in the raw configuration map and should
/// not grow a named accessor here unless they affect early boot-model behavior.
///
/// @since 9.0
/// @author Steve Ebersole
public class BootstrapSettingsResolver {
	/// Resolves settings from a raw configuration map.
	///
	/// This overload represents the native/default path.  It does not mark the
	/// result as JPA bootstrap, uses `FetchType.EAGER` as the default to-one fetch
	/// type, and resolves model-shaping settings from ORM's environment baseline
	/// overlaid with the supplied map.
	///
	/// @param configurationValues Raw configuration values
	///
	/// @return The resolved bootstrap settings
	public static ResolvedBootstrapSettings resolve(Map<?, ?> configurationValues) {
		return resolve( configurationValues, false, FetchType.EAGER );
	}

	/// Resolves settings from a raw configuration map with explicit entry-point
	/// defaults.
	///
	/// @param configurationValues Raw configuration values
	/// @param jpaBootstrap Whether this bootstrap originated from a Jakarta
	/// Persistence entry point
	/// @param defaultToOneFetchType Default to-one fetch type for mapping
	/// processing
	///
	/// @return The resolved bootstrap settings
	public static ResolvedBootstrapSettings resolve(
			Map<?, ?> configurationValues,
			boolean jpaBootstrap,
			FetchType defaultToOneFetchType) {
		final var resolvedConfigurationValues = copyEnvironmentProperties();
		overlay( configurationValues, resolvedConfigurationValues );
		return createResolvedSettings(
				resolvedConfigurationValues,
				jpaBootstrap,
				defaultToOneFetchType
		);
	}

	/// Resolves settings from Hibernate's programmatic JPA bootstrap
	/// configuration.
	///
	/// ORM's environment baseline is overlaid with the configuration object's raw
	/// properties.  Values exposed directly by the configuration object, such as
	/// default to-one fetch type, are used as the named setting sources.  The raw
	/// properties map is still carried forward in
	/// [ResolvedBootstrapSettings#configurationValues()].
	///
	/// @param persistenceConfiguration The programmatic persistence-unit
	/// configuration
	///
	/// @return The resolved bootstrap settings
	public static ResolvedBootstrapSettings resolve(HibernatePersistenceConfiguration persistenceConfiguration) {
		final var resolvedConfigurationValues = copyEnvironmentProperties();
		overlay( persistenceConfiguration.properties(), resolvedConfigurationValues );
		return createResolvedSettings(
				resolvedConfigurationValues,
				true,
				persistenceConfiguration.defaultToOneFetchType()
		);
	}

	/// Resolves settings from Hibernate's programmatic JPA bootstrap
	/// configuration and runtime integration settings.
	///
	/// ORM's environment baseline is overlaid with the configuration object's raw
	/// properties, and integration settings overlay them both.
	///
	/// @param persistenceConfiguration The programmatic persistence-unit
	/// configuration
	/// @param integrationSettings Runtime integration settings to overlay
	///
	/// @return The resolved bootstrap settings
	public static ResolvedBootstrapSettings resolve(
			HibernatePersistenceConfiguration persistenceConfiguration,
			Map<?, ?> integrationSettings) {
		final var resolvedConfigurationValues = copyEnvironmentProperties();
		overlay( persistenceConfiguration.properties(), resolvedConfigurationValues );
		overlay( integrationSettings, resolvedConfigurationValues );
		return createResolvedSettings(
				resolvedConfigurationValues,
				true,
				persistenceConfiguration.defaultToOneFetchType()
		);
	}

	/// Resolves settings from a parsed persistence-unit descriptor and runtime
	/// integration settings.
	///
	/// ORM's environment baseline is overlaid with persistence-unit properties,
	/// and integration settings overlay them both.  This matches the JPA
	/// bootstrap shape where container or caller-provided integration values may
	/// override persistence-unit configuration.
	///
	/// @param persistenceUnitDescriptor The parsed persistence-unit descriptor
	/// @param integrationSettings Runtime integration settings to overlay
	///
	/// @return The resolved bootstrap settings
	public static ResolvedBootstrapSettings resolve(
			PersistenceUnitDescriptor persistenceUnitDescriptor,
			Map<?, ?> integrationSettings) {
		final var resolvedConfigurationValues = copyEnvironmentProperties();
		overlay( persistenceUnitDescriptor.getProperties(), resolvedConfigurationValues );
		overlay( integrationSettings, resolvedConfigurationValues );
		return createResolvedSettings(
				resolvedConfigurationValues,
				true,
				persistenceUnitDescriptor.getDefaultToOneFetchType()
		);
	}

	private static ResolvedBootstrapSettings createResolvedSettings(
			Map<String, Object> configurationValues,
			boolean jpaBootstrap,
			FetchType defaultToOneFetchType) {
		return new ResolvedBootstrapSettings(
				configurationValues,
				jpaBootstrap,
				new ResolvedMappingSettings(
						resolveXmlMappingEnabled( configurationValues ),
						resolveValidateXml( configurationValues ),
						defaultToOneFetchType,
						resolveCreateImplicitDiscriminatorsForJoinedInheritance( configurationValues ),
						resolveIgnoreExplicitDiscriminatorsForJoinedInheritance( configurationValues ),
						resolveCacheRegionDefinitions( configurationValues )
				)
		);
	}

	private static LinkedHashMap<String, Object> copyEnvironmentProperties() {
		return copyConfigurationValues( Environment.getProperties() );
	}

	private static LinkedHashMap<String, Object> copyConfigurationValues(Map<?, ?> configurationValues) {
		final var result = new LinkedHashMap<String, Object>();
		overlay( configurationValues, result );
		return result;
	}

	private static void overlay(Map<?, ?> source, Map<String, Object> target) {
		if ( source == null ) {
			return;
		}
		source.forEach( (key, value) -> {
			if ( key != null ) {
				target.put( key.toString(), value );
			}
		} );
	}

	private static boolean resolveXmlMappingEnabled(Map<String, Object> configurationValues) {
		final Object enabled = configurationValues.get( MappingSettings.XML_MAPPING_ENABLED );
		return enabled == null || parseBoolean( enabled );
	}

	private static boolean resolveValidateXml(Map<String, Object> configurationValues) {
		final Object enabled = configurationValues.get( MappingSettings.VALIDATE_XML );
		return enabled != null && parseBoolean( enabled );
	}

	private static boolean resolveCreateImplicitDiscriminatorsForJoinedInheritance(Map<String, Object> configurationValues) {
		final Object enabled = configurationValues.get( MappingSettings.IMPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS );
		return enabled != null && parseBoolean( enabled );
	}

	private static boolean resolveIgnoreExplicitDiscriminatorsForJoinedInheritance(Map<String, Object> configurationValues) {
		final Object enabled = configurationValues.get( MappingSettings.IGNORE_EXPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS );
		return enabled != null && parseBoolean( enabled );
	}

	private static boolean parseBoolean(Object value) {
		if ( value instanceof Boolean booleanValue ) {
			return booleanValue;
		}
		return Boolean.parseBoolean( value.toString() );
	}

	private static List<CacheRegionDefinition> resolveCacheRegionDefinitions(Map<String, Object> configurationValues) {
		final var cacheRegionDefinitions = new ArrayList<CacheRegionDefinition>();
		configurationValues.forEach( (key, value) -> {
			if ( value instanceof String valueString ) {
				if ( key.startsWith( CLASS_CACHE_PREFIX + "." ) ) {
					cacheRegionDefinitions.add( parseCacheRegionDefinitionEntry(
							key.substring( CLASS_CACHE_PREFIX.length() + 1 ),
							valueString,
							CacheRegionType.ENTITY
					) );
				}
				else if ( key.startsWith( COLLECTION_CACHE_PREFIX + "." ) ) {
					cacheRegionDefinitions.add( parseCacheRegionDefinitionEntry(
							key.substring( COLLECTION_CACHE_PREFIX.length() + 1 ),
							valueString,
							CacheRegionType.COLLECTION
					) );
				}
			}
		} );
		return cacheRegionDefinitions;
	}

	private static CacheRegionDefinition parseCacheRegionDefinitionEntry(
			String role,
			String value,
			CacheRegionType cacheType) {
		final var params = new StringTokenizer( value, ";, " );
		if ( !params.hasMoreTokens() ) {
			throw illegalCacheRegionDefinitionException( role, value, cacheType );
		}

		final String usage = params.nextToken();
		final String region = params.hasMoreTokens() ? params.nextToken() : null;
		final boolean lazyProperty = cacheType == CacheRegionType.ENTITY
				&& ( !params.hasMoreTokens() || "all".equalsIgnoreCase( params.nextToken() ) );
		return new CacheRegionDefinition( cacheType, role, usage, region, lazyProperty );
	}

	private static IllegalArgumentException illegalCacheRegionDefinitionException(
			String role,
			String value,
			CacheRegionType cacheType) {
		return new IllegalArgumentException(
				"Cache region configuration `%s.%s %s` not of form `usage[,region[,lazy]]`".formatted(
						cacheType == CacheRegionType.ENTITY ? CLASS_CACHE_PREFIX : COLLECTION_CACHE_PREFIX,
						role,
						value
				)
		);
	}
}
