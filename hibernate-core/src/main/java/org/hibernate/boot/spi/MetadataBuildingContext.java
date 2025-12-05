/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.spi;

import org.hibernate.Incubating;
import org.hibernate.boot.model.TypeDefinitionRegistry;
import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;

import static org.hibernate.cfg.MappingSettings.JAVA_TIME_USE_DIRECT_JDBC;
import static org.hibernate.cfg.MappingSettings.PREFER_LOCALE_LANGUAGE_TAG;
import static org.hibernate.cfg.MappingSettings.PREFER_NATIVE_ENUM_TYPES;
import static org.hibernate.internal.util.config.ConfigurationHelper.getBoolean;

/**
 * Describes the context in which the process of building {@link org.hibernate.boot.Metadata}
 * from {@link org.hibernate.boot.MetadataSources} occurs.
 * <p>
 * {@link MetadataBuildingContext}s are hierarchical: global, persistence unit, document, mapping.
 *
 * @author Steve Ebersole
 *
 * @since 5.0
 */
public interface MetadataBuildingContext {
	BootstrapContext getBootstrapContext();

	/**
	 * Access to the options specified by the {@link org.hibernate.boot.MetadataBuilder}
	 *
	 * @return The options
	 */
	MetadataBuildingOptions getBuildingOptions();

	/**
	 * Access to mapping defaults in effect for this context
	 *
	 * @return The mapping defaults.
	 */
	EffectiveMappingDefaults getEffectiveDefaults();

	/**
	 * Access to the collector of metadata as we build it.
	 *
	 * @return The metadata collector.
	 */
	InFlightMetadataCollector getMetadataCollector();

	/**
	 * Not sure how I feel about this exposed here
	 *
	 * @return The ObjectNameNormalizer
	 */
	ObjectNameNormalizer getObjectNameNormalizer();

	private StandardServiceRegistry getRegistry() {
		return getBootstrapContext().getServiceRegistry();
	}

	@Incubating
	default int getPreferredSqlTypeCodeForBoolean() {
		return ConfigurationHelper.getPreferredSqlTypeCodeForBoolean( getRegistry() );
	}

	@Incubating
	default int getPreferredSqlTypeCodeForDuration() {
		return ConfigurationHelper.getPreferredSqlTypeCodeForDuration( getRegistry() );
	}

	@Incubating
	default int getPreferredSqlTypeCodeForUuid() {
		return ConfigurationHelper.getPreferredSqlTypeCodeForUuid( getRegistry() );
	}

	@Incubating
	default int getPreferredSqlTypeCodeForInstant() {
		return ConfigurationHelper.getPreferredSqlTypeCodeForInstant( getRegistry() );
	}

	@Incubating
	default int getPreferredSqlTypeCodeForArray() {
		return ConfigurationHelper.getPreferredSqlTypeCodeForArray( getRegistry() );
	}

	@Incubating
	default boolean isPreferJavaTimeJdbcTypesEnabled() {
		return isPreferJavaTimeJdbcTypesEnabled( getRegistry() );
	}

	@Incubating
	default boolean isPreferNativeEnumTypesEnabled() {
		return isPreferNativeEnumTypesEnabled( getRegistry() );
	}

	@Incubating
	default boolean isPreferLocaleLanguageTagEnabled() {
		return isPreferLocaleLanguageTagEnabled( getRegistry() );
	}

	static boolean isPreferJavaTimeJdbcTypesEnabled(ServiceRegistry serviceRegistry) {
		return isPreferJavaTimeJdbcTypesEnabled( serviceRegistry.requireService( ConfigurationService.class ) );
	}

	static boolean isPreferNativeEnumTypesEnabled(ServiceRegistry serviceRegistry) {
		return isPreferNativeEnumTypesEnabled( serviceRegistry.requireService( ConfigurationService.class ) );
	}

	static boolean isPreferLocaleLanguageTagEnabled(ServiceRegistry serviceRegistry) {
		return isPreferLocaleLanguageTagEnabled( serviceRegistry.requireService( ConfigurationService.class ) );
	}

	static boolean isPreferJavaTimeJdbcTypesEnabled(ConfigurationService configurationService) {
		return getBoolean( JAVA_TIME_USE_DIRECT_JDBC, configurationService.getSettings() );
	}

	static boolean isPreferNativeEnumTypesEnabled(ConfigurationService configurationService) {
		//TODO: HHH-17905 proposes to switch this default to true
		return getBoolean( PREFER_NATIVE_ENUM_TYPES, configurationService.getSettings() );
	}

	static boolean isPreferLocaleLanguageTagEnabled(ConfigurationService configurationService) {
		return getBoolean( PREFER_LOCALE_LANGUAGE_TAG, configurationService.getSettings() );
	}

	TypeDefinitionRegistry getTypeDefinitionRegistry();

	/**
	 * The name of the contributor whose mappings we are currently processing
	 */
	String getCurrentContributorName();
}
