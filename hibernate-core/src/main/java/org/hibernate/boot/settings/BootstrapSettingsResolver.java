/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.function.Supplier;

import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.boot.CacheRegionDefinition.CacheRegionType;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

import jakarta.persistence.FetchType;

import static org.hibernate.cfg.AvailableSettings.CLASS_CACHE_PREFIX;
import static org.hibernate.cfg.AvailableSettings.COLLECTION_CACHE_PREFIX;
import static org.hibernate.cfg.CacheSettings.JAKARTA_SHARED_CACHE_MODE;
import static org.hibernate.cfg.CacheSettings.JPA_SHARED_CACHE_MODE;
import static org.hibernate.cfg.JdbcSettings.DATASOURCE;
import static org.hibernate.cfg.JdbcSettings.DRIVER;
import static org.hibernate.cfg.JdbcSettings.JAKARTA_JDBC_DRIVER;
import static org.hibernate.cfg.JdbcSettings.JAKARTA_JDBC_PASSWORD;
import static org.hibernate.cfg.JdbcSettings.JAKARTA_JDBC_URL;
import static org.hibernate.cfg.JdbcSettings.JAKARTA_JDBC_USER;
import static org.hibernate.cfg.JdbcSettings.JAKARTA_JTA_DATASOURCE;
import static org.hibernate.cfg.JdbcSettings.JAKARTA_NON_JTA_DATASOURCE;
import static org.hibernate.cfg.JdbcSettings.JPA_JDBC_DRIVER;
import static org.hibernate.cfg.JdbcSettings.JPA_JDBC_PASSWORD;
import static org.hibernate.cfg.JdbcSettings.JPA_JDBC_URL;
import static org.hibernate.cfg.JdbcSettings.JPA_JDBC_USER;
import static org.hibernate.cfg.JdbcSettings.JPA_JTA_DATASOURCE;
import static org.hibernate.cfg.JdbcSettings.JPA_NON_JTA_DATASOURCE;
import static org.hibernate.cfg.JdbcSettings.PASS;
import static org.hibernate.cfg.JdbcSettings.URL;
import static org.hibernate.cfg.JdbcSettings.USER;
import static org.hibernate.cfg.PersistenceSettings.JPA_TRANSACTION_TYPE;
import static org.hibernate.cfg.PersistenceSettings.JAKARTA_TRANSACTION_TYPE;
import static org.hibernate.cfg.PersistenceSettings.PERSISTENCE_UNIT_NAME;
import static org.hibernate.cfg.ValidationSettings.JPA_VALIDATION_MODE;
import static org.hibernate.cfg.ValidationSettings.JAKARTA_VALIDATION_MODE;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;

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
		putIfNonNull( resolvedConfigurationValues, PERSISTENCE_UNIT_NAME, persistenceUnitDescriptor.getName() );
		normalizeDescriptorSettings(
				resolvedConfigurationValues,
				persistenceUnitDescriptor,
				integrationSettings
		);
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

	private static void normalizeDescriptorSettings(
			Map<String, Object> configurationValues,
			PersistenceUnitDescriptor persistenceUnitDescriptor,
			Map<?, ?> integrationSettings) {
		final var integrationSettingsCopy = integrationSettings == null
				? new HashMap<>()
				: new HashMap<>( integrationSettings );

		normalizeConnectionAccessUserAndPass( configurationValues, persistenceUnitDescriptor, integrationSettingsCopy );
		normalizeTransactionType( configurationValues, persistenceUnitDescriptor, integrationSettingsCopy );
		normalizeDataAccess( configurationValues, persistenceUnitDescriptor, integrationSettingsCopy );
		normalizeValidationMode( configurationValues, persistenceUnitDescriptor, integrationSettingsCopy );
		normalizeSharedCacheMode( configurationValues, persistenceUnitDescriptor, integrationSettingsCopy );
		overlay( integrationSettingsCopy, configurationValues );
	}

	private static void normalizeConnectionAccessUserAndPass(
			Map<String, Object> configurationValues,
			PersistenceUnitDescriptor persistenceUnitDescriptor,
			Map<?, ?> integrationSettingsCopy) {
		final var user = coalesce(
				() -> remove( integrationSettingsCopy, USER ),
				() -> remove( integrationSettingsCopy, JAKARTA_JDBC_USER ),
				() -> remove( integrationSettingsCopy, JPA_JDBC_USER ),
				() -> get( persistenceUnitDescriptor.getProperties(), USER ),
				() -> get( persistenceUnitDescriptor.getProperties(), JAKARTA_JDBC_USER ),
				() -> get( persistenceUnitDescriptor.getProperties(), JPA_JDBC_USER ),
				() -> configurationValues.get( USER ),
				() -> configurationValues.get( JAKARTA_JDBC_USER ),
				() -> configurationValues.get( JPA_JDBC_USER )
		);
		final var pass = coalesce(
				() -> remove( integrationSettingsCopy, PASS ),
				() -> remove( integrationSettingsCopy, JAKARTA_JDBC_PASSWORD ),
				() -> remove( integrationSettingsCopy, JPA_JDBC_PASSWORD ),
				() -> get( persistenceUnitDescriptor.getProperties(), PASS ),
				() -> get( persistenceUnitDescriptor.getProperties(), JAKARTA_JDBC_PASSWORD ),
				() -> get( persistenceUnitDescriptor.getProperties(), JPA_JDBC_PASSWORD ),
				() -> configurationValues.get( PASS ),
				() -> configurationValues.get( JAKARTA_JDBC_PASSWORD ),
				() -> configurationValues.get( JPA_JDBC_PASSWORD )
		);
		if ( user != null ) {
			configurationValues.put( USER, user );
			configurationValues.put( JAKARTA_JDBC_USER, user );
			configurationValues.put( JPA_JDBC_USER, user );
		}
		if ( pass != null ) {
			configurationValues.put( PASS, pass );
			configurationValues.put( JAKARTA_JDBC_PASSWORD, pass );
			configurationValues.put( JPA_JDBC_PASSWORD, pass );
		}
	}

	private static void normalizeTransactionType(
			Map<String, Object> configurationValues,
			PersistenceUnitDescriptor persistenceUnitDescriptor,
			Map<?, ?> integrationSettingsCopy) {
		final var transactionType = coalesce(
				() -> remove( integrationSettingsCopy, JAKARTA_TRANSACTION_TYPE ),
				() -> remove( integrationSettingsCopy, JPA_TRANSACTION_TYPE ),
				persistenceUnitDescriptor::getPersistenceUnitTransactionType,
				() -> configurationValues.get( JAKARTA_TRANSACTION_TYPE ),
				() -> configurationValues.get( JPA_TRANSACTION_TYPE )
		);
		if ( transactionType != null ) {
			configurationValues.put( JAKARTA_TRANSACTION_TYPE, transactionType );
		}
	}

	private static void normalizeDataAccess(
			Map<String, Object> configurationValues,
			PersistenceUnitDescriptor persistenceUnitDescriptor,
			Map<?, ?> integrationSettingsCopy) {
		if ( hasValue( integrationSettingsCopy, DATASOURCE ) ) {
			applyDataSource( configurationValues, integrationSettingsCopy, remove( integrationSettingsCopy, DATASOURCE ), null );
			return;
		}
		if ( hasValue( integrationSettingsCopy, JAKARTA_JTA_DATASOURCE ) ) {
			applyDataSource( configurationValues, integrationSettingsCopy, remove( integrationSettingsCopy, JAKARTA_JTA_DATASOURCE ), true );
			return;
		}
		if ( hasValue( integrationSettingsCopy, JPA_JTA_DATASOURCE ) ) {
			applyDataSource( configurationValues, integrationSettingsCopy, remove( integrationSettingsCopy, JPA_JTA_DATASOURCE ), true );
			return;
		}
		if ( integrationSettingsCopy.containsKey( JAKARTA_NON_JTA_DATASOURCE ) ) {
			applyDataSource( configurationValues, integrationSettingsCopy, remove( integrationSettingsCopy, JAKARTA_NON_JTA_DATASOURCE ), false );
			return;
		}
		if ( integrationSettingsCopy.containsKey( JPA_NON_JTA_DATASOURCE ) ) {
			applyDataSource( configurationValues, integrationSettingsCopy, remove( integrationSettingsCopy, JPA_NON_JTA_DATASOURCE ), false );
			return;
		}
		if ( hasValue( integrationSettingsCopy, URL ) ) {
			applyJdbcSettings(
					configurationValues,
					integrationSettingsCopy,
					integrationSettingsCopy.get( URL ),
					stringValue( coalesce(
							() -> integrationSettingsCopy.get( DRIVER ),
							() -> integrationSettingsCopy.get( JAKARTA_JDBC_DRIVER ),
							() -> integrationSettingsCopy.get( JPA_JDBC_DRIVER ),
							() -> configurationValues.get( DRIVER ),
							() -> configurationValues.get( JAKARTA_JDBC_DRIVER ),
							() -> configurationValues.get( JPA_JDBC_DRIVER )
					) )
			);
			return;
		}
		if ( hasValue( integrationSettingsCopy, JAKARTA_JDBC_URL ) ) {
			applyJdbcSettings(
					configurationValues,
					integrationSettingsCopy,
					integrationSettingsCopy.get( JAKARTA_JDBC_URL ),
					stringValue( coalesce(
							() -> integrationSettingsCopy.get( JAKARTA_JDBC_DRIVER ),
							() -> configurationValues.get( JAKARTA_JDBC_DRIVER )
					) )
			);
			return;
		}
		if ( hasValue( integrationSettingsCopy, JPA_JDBC_URL ) ) {
			applyJdbcSettings(
					configurationValues,
					integrationSettingsCopy,
					integrationSettingsCopy.get( JPA_JDBC_URL ),
					stringValue( coalesce(
							() -> integrationSettingsCopy.get( JPA_JDBC_DRIVER ),
							() -> configurationValues.get( JPA_JDBC_DRIVER )
					) )
			);
			return;
		}

		if ( persistenceUnitDescriptor.getJtaDataSource() != null ) {
			applyDataSource( configurationValues, integrationSettingsCopy, persistenceUnitDescriptor.getJtaDataSource(), true );
			return;
		}
		if ( persistenceUnitDescriptor.getNonJtaDataSource() != null ) {
			applyDataSource( configurationValues, integrationSettingsCopy, persistenceUnitDescriptor.getNonJtaDataSource(), false );
			return;
		}
		if ( hasValue( configurationValues, URL ) ) {
			applyJdbcSettings( configurationValues, integrationSettingsCopy, configurationValues.get( URL ), stringValue( configurationValues.get( DRIVER ) ) );
			return;
		}
		if ( hasValue( configurationValues, JAKARTA_JDBC_URL ) ) {
			applyJdbcSettings( configurationValues, integrationSettingsCopy, configurationValues.get( JAKARTA_JDBC_URL ), stringValue( configurationValues.get( JAKARTA_JDBC_DRIVER ) ) );
			return;
		}
		if ( hasValue( configurationValues, JPA_JDBC_URL ) ) {
			applyJdbcSettings( configurationValues, integrationSettingsCopy, configurationValues.get( JPA_JDBC_URL ), stringValue( configurationValues.get( JPA_JDBC_DRIVER ) ) );
		}
	}

	private static void applyDataSource(
			Map<String, Object> configurationValues,
			Map<?, ?> integrationSettingsCopy,
			Object dataSource,
			Boolean jta) {
		if ( dataSource == null ) {
			return;
		}
		if ( Boolean.TRUE.equals( jta ) ) {
			configurationValues.put( JAKARTA_JTA_DATASOURCE, dataSource );
			configurationValues.put( JPA_JTA_DATASOURCE, dataSource );
			removeKeys( configurationValues, JAKARTA_NON_JTA_DATASOURCE, JPA_NON_JTA_DATASOURCE );
		}
		else {
			configurationValues.put( JAKARTA_NON_JTA_DATASOURCE, dataSource );
			configurationValues.put( JPA_NON_JTA_DATASOURCE, dataSource );
			removeKeys( configurationValues, JAKARTA_JTA_DATASOURCE, JPA_JTA_DATASOURCE );
		}
		configurationValues.put( DATASOURCE, dataSource );
		removeKeys(
				configurationValues,
				DRIVER,
				JAKARTA_JDBC_DRIVER,
				JPA_JDBC_DRIVER,
				URL,
				JAKARTA_JDBC_URL,
				JPA_JDBC_URL
		);
		removeKeys(
				integrationSettingsCopy,
				DATASOURCE,
				JAKARTA_JTA_DATASOURCE,
				JPA_JTA_DATASOURCE,
				JAKARTA_NON_JTA_DATASOURCE,
				JPA_NON_JTA_DATASOURCE,
				DRIVER,
				JAKARTA_JDBC_DRIVER,
				JPA_JDBC_DRIVER,
				URL,
				JAKARTA_JDBC_URL,
				JPA_JDBC_URL
		);
	}

	private static void applyJdbcSettings(
			Map<String, Object> configurationValues,
			Map<?, ?> integrationSettingsCopy,
			Object url,
			String driver) {
		configurationValues.put( URL, url );
		configurationValues.put( JAKARTA_JDBC_URL, url );
		configurationValues.put( JPA_JDBC_URL, url );
		if ( isNotEmpty( driver ) ) {
			configurationValues.put( DRIVER, driver );
			configurationValues.put( JAKARTA_JDBC_DRIVER, driver );
			configurationValues.put( JPA_JDBC_DRIVER, driver );
		}
		else {
			removeKeys( configurationValues, DRIVER, JAKARTA_JDBC_DRIVER, JPA_JDBC_DRIVER );
		}
		removeKeys(
				configurationValues,
				DATASOURCE,
				JAKARTA_JTA_DATASOURCE,
				JPA_JTA_DATASOURCE,
				JAKARTA_NON_JTA_DATASOURCE,
				JPA_NON_JTA_DATASOURCE
		);
		removeKeys(
				integrationSettingsCopy,
				DRIVER,
				JAKARTA_JDBC_DRIVER,
				JPA_JDBC_DRIVER,
				URL,
				JAKARTA_JDBC_URL,
				JPA_JDBC_URL,
				USER,
				JAKARTA_JDBC_USER,
				JPA_JDBC_USER,
				PASS,
				JAKARTA_JDBC_PASSWORD,
				JPA_JDBC_PASSWORD,
				DATASOURCE,
				JAKARTA_JTA_DATASOURCE,
				JPA_JTA_DATASOURCE,
				JAKARTA_NON_JTA_DATASOURCE,
				JPA_NON_JTA_DATASOURCE
		);
	}

	private static void normalizeValidationMode(
			Map<String, Object> configurationValues,
			PersistenceUnitDescriptor persistenceUnitDescriptor,
			Map<?, ?> integrationSettingsCopy) {
		final var validationMode = coalesce(
				() -> remove( integrationSettingsCopy, JAKARTA_VALIDATION_MODE ),
				() -> remove( integrationSettingsCopy, JPA_VALIDATION_MODE ),
				persistenceUnitDescriptor::getValidationMode,
				() -> configurationValues.get( JAKARTA_VALIDATION_MODE ),
				() -> configurationValues.get( JPA_VALIDATION_MODE )
		);
		if ( validationMode != null ) {
			configurationValues.put( JAKARTA_VALIDATION_MODE, validationMode );
		}
	}

	private static void normalizeSharedCacheMode(
			Map<String, Object> configurationValues,
			PersistenceUnitDescriptor persistenceUnitDescriptor,
			Map<?, ?> integrationSettingsCopy) {
		final var sharedCacheMode = coalesce(
				() -> remove( integrationSettingsCopy, JAKARTA_SHARED_CACHE_MODE ),
				() -> remove( integrationSettingsCopy, JPA_SHARED_CACHE_MODE ),
				persistenceUnitDescriptor::getSharedCacheMode,
				() -> configurationValues.get( JAKARTA_SHARED_CACHE_MODE ),
				() -> configurationValues.get( JPA_SHARED_CACHE_MODE )
		);
		if ( sharedCacheMode != null ) {
			configurationValues.put( JAKARTA_SHARED_CACHE_MODE, sharedCacheMode );
		}
	}

	private static boolean hasValue(Map<?, ?> values, String key) {
		final var value = values.get( key );
		return value != null && !( value instanceof String stringValue && isEmpty( stringValue ) );
	}

	private static Object get(Map<?, ?> values, String key) {
		return values == null ? null : values.get( key );
	}

	private static Object remove(Map<?, ?> values, String key) {
		return values == null ? null : values.remove( key );
	}

	private static void removeKeys(Map<?, ?> values, String... keys) {
		if ( values == null ) {
			return;
		}
		for ( String key : keys ) {
			values.remove( key );
		}
	}

	@SafeVarargs
	private static <T> T coalesce(Supplier<T>... suppliers) {
		for ( Supplier<T> supplier : suppliers ) {
			final var value = supplier.get();
			if ( value != null ) {
				return value;
			}
		}
		return null;
	}

	private static String stringValue(Object value) {
		return value == null ? null : value.toString();
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

	private static void putIfNonNull(Map<String, Object> target, String key, Object value) {
		if ( value != null ) {
			target.putIfAbsent( key, value );
		}
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
