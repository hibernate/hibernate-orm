/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import org.hibernate.boot.cfgxml.spi.CfgXmlAccessService;
import org.hibernate.boot.cfgxml.spi.LoadedConfig;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableStrategy;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import java.util.HashMap;
import java.util.Map;

import static org.hibernate.cfg.PersistenceSettings.PERSISTENCE_UNIT_NAME;
import static org.hibernate.cfg.PersistenceSettings.SESSION_FACTORY_JNDI_NAME;
import static org.hibernate.cfg.PersistenceSettings.SESSION_FACTORY_NAME;
import static org.hibernate.cfg.ValidationSettings.JAKARTA_VALIDATION_FACTORY;
import static org.hibernate.cfg.ValidationSettings.JPA_VALIDATION_FACTORY;
import static org.hibernate.engine.config.spi.StandardConverters.STRING;
import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;

/**
 * Helper methods used to set up a {@link SessionFactoryImpl}.
 */
class SessionFactorySettings {

	static Map<String, Object> getSettings(
			SessionFactoryOptions options, SessionFactoryServiceRegistry serviceRegistry) {
		final Map<String, Object> settings =
				serviceRegistry.requireService( ConfigurationService.class )
						.getSettings();
		final Map<String,Object> result = new HashMap<>( settings );
		if ( !settings.containsKey( JPA_VALIDATION_FACTORY )
				&& !settings.containsKey( JAKARTA_VALIDATION_FACTORY ) ) {
			final Object reference = options.getValidatorFactoryReference();
			if ( reference != null ) {
				result.put( JPA_VALIDATION_FACTORY, reference );
				result.put( JAKARTA_VALIDATION_FACTORY, reference );
			}
		}
		return result;
	}

	static String getSessionFactoryName(
			SessionFactoryOptions options, SessionFactoryServiceRegistry serviceRegistry) {
		final String sessionFactoryName = options.getSessionFactoryName();
		if ( sessionFactoryName != null ) {
			return sessionFactoryName;
		}

		final LoadedConfig loadedConfig =
				serviceRegistry.requireService( CfgXmlAccessService.class )
						.getAggregatedConfig();
		if ( loadedConfig != null ) {
			final String nameFromAggregation = loadedConfig.getSessionFactoryName();
			if ( nameFromAggregation != null ) {
				return nameFromAggregation;
			}
		}

		final ConfigurationService configurationService =
				serviceRegistry.requireService( ConfigurationService.class );
		return configurationService.getSetting( PERSISTENCE_UNIT_NAME, STRING );
	}

	static void maskOutSensitiveInformation(Map<String, Object> props) {
		maskOutIfSet( props, AvailableSettings.JPA_JDBC_USER );
		maskOutIfSet( props, AvailableSettings.JPA_JDBC_PASSWORD );
		maskOutIfSet( props, AvailableSettings.JAKARTA_JDBC_USER );
		maskOutIfSet( props, AvailableSettings.JAKARTA_JDBC_PASSWORD );
		maskOutIfSet( props, AvailableSettings.USER );
		maskOutIfSet( props, AvailableSettings.PASS );
	}

	private static void maskOutIfSet(Map<String, Object> props, String setting) {
		if ( props.containsKey( setting ) ) {
			props.put( setting, "****" );
		}
	}

	static String determineJndiName(
			String name,
			SessionFactoryOptions options,
			SessionFactoryServiceRegistry serviceRegistry) {
		final ConfigurationService configService =
				serviceRegistry.requireService( ConfigurationService.class );
		final String explicitJndiName = configService.getSetting( SESSION_FACTORY_JNDI_NAME, STRING );
		if ( isNotEmpty( explicitJndiName ) ) {
			return explicitJndiName;
		}
		// do not use name for JNDI if explicitly asked not to
		else if ( options.isSessionFactoryNameAlsoJndiName() == Boolean.FALSE ) {
			return null;
		}
		else {
			final String expliciSessionFactoryname = configService.getSetting( SESSION_FACTORY_NAME, STRING );
			if ( isNotEmpty( expliciSessionFactoryname ) ) {
				return expliciSessionFactoryname;
			}
			final String unitName = configService.getSetting( PERSISTENCE_UNIT_NAME, STRING );
			// if name comes from JPA persistence-unit name
			return ! isNotEmpty( unitName ) ? name : null;
		}
	}

	static void deprecationCheck(Map<String, Object> settings) {
		for ( String setting:settings.keySet() ) {
			switch ( setting ) {
				case "hibernate.hql.bulk_id_strategy.global_temporary.create_tables":
					DEPRECATION_LOGGER.deprecatedSetting( "hibernate.hql.bulk_id_strategy.global_temporary.create_tables", GlobalTemporaryTableStrategy.CREATE_ID_TABLES );
				case "hibernate.hql.bulk_id_strategy.global_temporary.drop_tables":
					DEPRECATION_LOGGER.deprecatedSetting( "hibernate.hql.bulk_id_strategy.global_temporary.drop_tables", GlobalTemporaryTableStrategy.DROP_ID_TABLES );
				case "hibernate.hql.bulk_id_strategy.persistent.create_tables":
					DEPRECATION_LOGGER.deprecatedSetting( "hibernate.hql.bulk_id_strategy.persistent.create_tables", PersistentTableStrategy.CREATE_ID_TABLES );
				case "hibernate.hql.bulk_id_strategy.persistent.drop_tables":
					DEPRECATION_LOGGER.deprecatedSetting( "hibernate.hql.bulk_id_strategy.persistent.drop_tables", PersistentTableStrategy.DROP_ID_TABLES );
				case "hibernate.hql.bulk_id_strategy.persistent.schema":
					DEPRECATION_LOGGER.deprecatedSetting( "hibernate.hql.bulk_id_strategy.persistent.schema", PersistentTableStrategy.SCHEMA );
				case "hibernate.hql.bulk_id_strategy.persistent.catalog":
					DEPRECATION_LOGGER.deprecatedSetting( "hibernate.hql.bulk_id_strategy.persistent.catalog", PersistentTableStrategy.CATALOG );
				case "hibernate.hql.bulk_id_strategy.local_temporary.drop_tables":
					DEPRECATION_LOGGER.deprecatedSetting( "hibernate.hql.bulk_id_strategy.local_temporary.drop_tables", LocalTemporaryTableStrategy.DROP_ID_TABLES );
			}
		}
	}
}
