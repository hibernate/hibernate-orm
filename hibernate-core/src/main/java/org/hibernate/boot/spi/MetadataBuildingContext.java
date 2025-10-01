/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.spi;

import org.hibernate.Incubating;
import org.hibernate.boot.model.TypeDefinitionRegistry;
import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;

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

	@Incubating
	default int getPreferredSqlTypeCodeForBoolean() {
		return ConfigurationHelper.getPreferredSqlTypeCodeForBoolean( getBootstrapContext().getServiceRegistry() );
	}

	@Incubating
	default int getPreferredSqlTypeCodeForDuration() {
		return ConfigurationHelper.getPreferredSqlTypeCodeForDuration( getBootstrapContext().getServiceRegistry() );
	}

	@Incubating
	default int getPreferredSqlTypeCodeForUuid() {
		return ConfigurationHelper.getPreferredSqlTypeCodeForUuid( getBootstrapContext().getServiceRegistry() );
	}

	@Incubating
	default int getPreferredSqlTypeCodeForInstant() {
		return ConfigurationHelper.getPreferredSqlTypeCodeForInstant( getBootstrapContext().getServiceRegistry() );
	}

	@Incubating
	default int getPreferredSqlTypeCodeForArray() {
		return ConfigurationHelper.getPreferredSqlTypeCodeForArray( getBootstrapContext().getServiceRegistry() );
	}

	@Incubating
	default boolean isPreferJavaTimeJdbcTypesEnabled() {
		return isPreferJavaTimeJdbcTypesEnabled( getBootstrapContext().getServiceRegistry() );
	}

	@Incubating
	default boolean isPreferNativeEnumTypesEnabled() {
		return isPreferNativeEnumTypesEnabled( getBootstrapContext().getServiceRegistry() );
	}

	@Incubating
	default boolean isPreferLocaleLanguageTagEnabled() {
		return isPreferLocaleLanguageTagEnabled( getBootstrapContext().getServiceRegistry() );
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
		return ConfigurationHelper.getBoolean(
				MappingSettings.JAVA_TIME_USE_DIRECT_JDBC,
				configurationService.getSettings(),
				// todo : true would be better eventually so maybe just rip off that band aid
				false
		);
	}

	static boolean isPreferNativeEnumTypesEnabled(ConfigurationService configurationService) {
		return ConfigurationHelper.getBoolean(
				MappingSettings.PREFER_NATIVE_ENUM_TYPES,
				configurationService.getSettings(),
				// todo: switch to true with HHH-17905
				false
		);
	}

	static boolean isPreferLocaleLanguageTagEnabled(ConfigurationService configurationService) {
		return ConfigurationHelper.getBoolean(
				MappingSettings.PREFER_LOCALE_LANGUAGE_TAG,
				configurationService.getSettings(),
				false
		);
	}

	TypeDefinitionRegistry getTypeDefinitionRegistry();

	/**
	 * The name of the contributor whose mappings we are currently processing
	 */
	String getCurrentContributorName();
}
