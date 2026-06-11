/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.boot.CacheRegionDefinition.CacheRegionType;
import org.hibernate.cfg.MappingSettings;

import jakarta.persistence.FetchType;

import static org.hibernate.cfg.AvailableSettings.CLASS_CACHE_PREFIX;
import static org.hibernate.cfg.AvailableSettings.COLLECTION_CACHE_PREFIX;

/// Resolves mapping/model-build settings from normalized bootstrap
/// configuration.
///
/// @since 9.0
/// @author Steve Ebersole
public class MappingSettingsResolver {
	/// Resolve mapping/model-build settings from an already-normalized bootstrap
	/// settings bucket.
	///
	/// @param bootstrapSettings Resolved bootstrap settings carrying normalized
	/// configuration values
	/// @param defaultToOneFetchType Entry-point default for to-one associations
	///
	/// @return Resolved mapping settings
	public static ResolvedMappingSettings resolve(
			ResolvedBootstrapSettings bootstrapSettings,
			FetchType defaultToOneFetchType) {
		return resolve( bootstrapSettings.configurationValues(), defaultToOneFetchType );
	}

	/// Resolve mapping/model-build settings from normalized configuration values.
	///
	/// @param configurationValues Normalized configuration values
	/// @param defaultToOneFetchType Entry-point default for to-one associations
	///
	/// @return Resolved mapping settings
	public static ResolvedMappingSettings resolve(
			Map<String, Object> configurationValues,
			FetchType defaultToOneFetchType) {
		return new ResolvedMappingSettings(
				resolveXmlMappingEnabled( configurationValues ),
				resolveValidateXml( configurationValues ),
				defaultToOneFetchType,
				resolveCreateImplicitDiscriminatorsForJoinedInheritance( configurationValues ),
				resolveIgnoreExplicitDiscriminatorsForJoinedInheritance( configurationValues ),
				resolveCacheRegionDefinitions( configurationValues )
		);
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
