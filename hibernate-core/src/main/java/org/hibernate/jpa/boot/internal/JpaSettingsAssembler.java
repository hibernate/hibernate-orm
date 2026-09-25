/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.boot.internal;

import jakarta.persistence.PersistenceException;
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.annotation.Nonnull;
import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.boot.CacheRegionDefinition.CacheRegionType;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.boot.internal.BootstrapSettingsNormalizer.ConnectionSetting;

import static jakarta.persistence.PersistenceUnitTransactionType.JTA;
import static org.hibernate.cfg.AvailableSettings.CFG_XML_FILE;
import static org.hibernate.cfg.AvailableSettings.CLASS_CACHE_PREFIX;
import static org.hibernate.cfg.AvailableSettings.COLLECTION_CACHE_PREFIX;
import static org.hibernate.cfg.AvailableSettings.DATASOURCE;
import static org.hibernate.cfg.AvailableSettings.DRIVER;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_JDBC_DRIVER;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_JDBC_PASSWORD;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_JDBC_URL;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_JDBC_USER;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_JTA_DATASOURCE;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_NON_JTA_DATASOURCE;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_SHARED_CACHE_MODE;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_TRANSACTION_TYPE;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_VALIDATION_MODE;
import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_DRIVER;
import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_PASSWORD;
import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_URL;
import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_USER;
import static org.hibernate.cfg.AvailableSettings.JPA_JTA_DATASOURCE;
import static org.hibernate.cfg.AvailableSettings.JPA_NON_JTA_DATASOURCE;
import static org.hibernate.cfg.AvailableSettings.JPA_SHARED_CACHE_MODE;
import static org.hibernate.cfg.AvailableSettings.JPA_TRANSACTION_TYPE;
import static org.hibernate.cfg.AvailableSettings.JPA_VALIDATION_MODE;
import static org.hibernate.cfg.AvailableSettings.PASS;
import static org.hibernate.cfg.AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY;
import static org.hibernate.cfg.AvailableSettings.URL;
import static org.hibernate.cfg.AvailableSettings.USER;
import static org.hibernate.cfg.PersistenceSettings.PERSISTENCE_UNIT_NAME;
import static org.hibernate.cfg.TransactionSettings.FLUSH_BEFORE_COMPLETION;
import static org.hibernate.internal.util.config.ConfigurationHelper.getBoolean;
import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;
import static org.hibernate.internal.util.NullnessHelper.coalesceSuppliedValues;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.config.ConfigurationHelper.getString;
import static org.hibernate.jpa.boot.spi.PersistenceConfigurationDescriptor.collectSchemaManagementActions;
import static org.hibernate.jpa.internal.JpaLogger.JPA_LOGGER;
import static org.hibernate.jpa.internal.util.PersistenceUnitTransactionTypeHelper.interpretTransactionType;

/// Assembles JPA settings while retaining each entry point's source precedence and defaults.
/// Shared connection-setting aliases are applied by [ConnectionSetting].
///
/// @since 8.0
/// @author Steve Ebersole
@SuppressWarnings("deprecation")
final class JpaSettingsAssembler {
	private final PersistenceUnitDescriptor persistenceUnit;
	private final DataSource dataSource;
	private final Supplier<String> exceptionHeader;

	JpaSettingsAssembler(PersistenceUnitDescriptor persistenceUnit, DataSource dataSource, Supplier<String> exceptionHeader) {
		this.persistenceUnit = persistenceUnit;
		this.dataSource = dataSource;
		this.exceptionHeader = exceptionHeader;
	}

	MergedSettings assemble(
			@Nonnull HibernatePersistenceConfiguration cfg,
			@Nonnull StandardServiceRegistryBuilder standardRegistryBuilder) {
		var mergedSettings = new MergedSettings();

		mergedSettings.getConfigurationValues().putAll( cfg.properties() );
		collectSchemaManagementActions( cfg, mergedSettings.getConfigurationValues()::putIfAbsent );
		mergedSettings.getConfigurationValues().put( PERSISTENCE_UNIT_NAME, cfg.name() );

		// see if the persistence.xml settings named a Hibernate config file
		final String cfgXmlResourceName = getCfgXmlResourceName( Collections.emptyMap(), mergedSettings );
		if ( isNotEmpty( cfgXmlResourceName ) ) {
			processHibernateConfigXmlResources( standardRegistryBuilder, mergedSettings, cfgXmlResourceName );
		}

		normalizeSettings( null, null, mergedSettings );

		processConfigurationValues( mergedSettings );
		ignoreFlushBeforeCompletion( mergedSettings );

		return mergedSettings;
	}


	MergedSettings assemble(
			PersistenceUnitDescriptor persistenceUnit,
			Map<String,Object> integrationSettings,
			StandardServiceRegistryBuilder registryBuilder,
			Consumer<MergedSettings> mergedSettingsBaseline) {
		final var mergedSettings = new MergedSettings();
		if ( mergedSettingsBaseline != null ) {
			mergedSettingsBaseline.accept( mergedSettings );
		}
		mergedSettings.processPersistenceUnitDescriptorProperties( persistenceUnit );

		// see if the persistence.xml settings named a Hibernate config file
		final String cfgXmlResourceName = getCfgXmlResourceName( integrationSettings, mergedSettings );
		if ( isNotEmpty( cfgXmlResourceName ) ) {
			processHibernateConfigXmlResources( registryBuilder, mergedSettings, cfgXmlResourceName );
		}

		normalizeSettings( persistenceUnit, integrationSettings, mergedSettings );

		processConfigurationValues( mergedSettings );
		ignoreFlushBeforeCompletion( mergedSettings );
		return mergedSettings;
	}

	private void handleCacheRegionDefinition(String valueString, String keyString, MergedSettings mergedSettings) {
		if ( keyString.startsWith( CLASS_CACHE_PREFIX ) ) {
			mergedSettings.addCacheRegionDefinition(
					parseCacheRegionDefinitionEntry(
							keyString.substring( CLASS_CACHE_PREFIX.length() + 1 ),
							valueString,
							CacheRegionType.ENTITY
					)
			);
		}
		else if ( keyString.startsWith( COLLECTION_CACHE_PREFIX ) ) {
			mergedSettings.addCacheRegionDefinition(
					parseCacheRegionDefinitionEntry(
							keyString.substring( COLLECTION_CACHE_PREFIX.length() + 1 ),
							valueString,
							CacheRegionType.COLLECTION
					)
			);
		}
	}

	private static String getCfgXmlResourceName(Map<String, Object> integrationSettings, MergedSettings mergedSettings) {
		final String cfgXmlResourceName = (String) mergedSettings.getConfigurationValues().remove( CFG_XML_FILE );
		return isEmpty( cfgXmlResourceName )
				// see if integration settings named a Hibernate config file
				? (String) integrationSettings.get( CFG_XML_FILE )
				: cfgXmlResourceName;
	}

	/// Handles normalizing the settings coming from multiple sources, applying proper precedences
	private void normalizeSettings(
			PersistenceUnitDescriptor persistenceUnit,
			Map<String, Object> integrationSettings,
			MergedSettings mergedSettings) {
		// make a copy so that we can remove things as we process them
		final Map<String, Object> integrationSettingsCopy = integrationSettings == null
				? null
				: new HashMap<>( integrationSettings );
		normalizeConnectionAccessUserAndPass( integrationSettingsCopy, mergedSettings );
		normalizeTransactionCoordinator( persistenceUnit, integrationSettingsCopy, mergedSettings );
		normalizeDataAccess( integrationSettingsCopy, mergedSettings, persistenceUnit );
		normalizeValidationMode( persistenceUnit, integrationSettingsCopy, mergedSettings );
		normalizeSharedCacheMode( persistenceUnit, integrationSettingsCopy, mergedSettings );

		if ( integrationSettingsCopy != null ) {
			// Apply all "integration overrides" as the last step.  By specification,
			// these should have precedence.
			// NOTE that this occurs after the specialized normalize calls above which
			//      remove any specially-handled settings.
			for ( var entry : integrationSettingsCopy.entrySet() ) {
				final String key = entry.getKey();
				if ( key != null ) {
					final Object value = entry.getValue();
					if ( value == null ) {
						mergedSettings.getConfigurationValues().remove( key );
					}
					else {
						mergedSettings.getConfigurationValues().put( key, value );
					}
				}
			}
		}
	}

	private static void normalizeSharedCacheMode(
			PersistenceUnitDescriptor persistenceUnit,
			Map<String, Object> integrationSettingsCopy,
			MergedSettings mergedSettings) {
		final var configurationSettings = mergedSettings.getConfigurationValues();

		if ( integrationSettingsCopy != null ) {
			final Object jakartaCacheMode = integrationSettingsCopy.remove( JAKARTA_SHARED_CACHE_MODE );
			if ( jakartaCacheMode != null ) {
				configurationSettings.put( JAKARTA_SHARED_CACHE_MODE, jakartaCacheMode );
				// EARLY EXIT!!!
				return;
			}

			final Object legacyCacheMode = integrationSettingsCopy.remove( JPA_SHARED_CACHE_MODE );
			if ( legacyCacheMode != null ) {
				DEPRECATION_LOGGER.deprecatedSetting( JPA_SHARED_CACHE_MODE, JAKARTA_SHARED_CACHE_MODE );
				configurationSettings.put( JAKARTA_SHARED_CACHE_MODE, legacyCacheMode );
				configurationSettings.put( JPA_SHARED_CACHE_MODE, legacyCacheMode );
				// EARLY EXIT!!!
				return;
			}
		}

		if ( persistenceUnit != null && persistenceUnit.getSharedCacheMode() != null ) {
			configurationSettings.put( JAKARTA_SHARED_CACHE_MODE, persistenceUnit.getSharedCacheMode() );
		}

		var legacyCacheMode = configurationSettings.get( JPA_SHARED_CACHE_MODE );
		if ( legacyCacheMode != null ) {
			DEPRECATION_LOGGER.deprecatedSetting( JPA_SHARED_CACHE_MODE, JAKARTA_SHARED_CACHE_MODE );
			configurationSettings.put( JAKARTA_SHARED_CACHE_MODE, legacyCacheMode );
		}
	}

	private static void normalizeValidationMode(
			PersistenceUnitDescriptor persistenceUnit,
			Map<String, Object> integrationSettingsCopy,
			MergedSettings mergedSettings) {
		final var configurationSettings = mergedSettings.getConfigurationValues();
		if ( integrationSettingsCopy != null ) {
			final Object jakartaValidationMode = integrationSettingsCopy.remove( JAKARTA_VALIDATION_MODE );
			if ( jakartaValidationMode != null ) {
				configurationSettings.put( JAKARTA_VALIDATION_MODE, jakartaValidationMode );
				// EARLY EXIT!!!
				return;
			}

			final Object legacyValidationMode = integrationSettingsCopy.remove( JPA_VALIDATION_MODE );
			if ( legacyValidationMode != null ) {
				DEPRECATION_LOGGER.deprecatedSetting( JPA_VALIDATION_MODE, JAKARTA_VALIDATION_MODE );
				configurationSettings.put( JPA_VALIDATION_MODE, legacyValidationMode );
				configurationSettings.put( JAKARTA_VALIDATION_MODE, legacyValidationMode );
				// EARLY EXIT!!!
				return;
			}
		}

		if ( persistenceUnit != null && persistenceUnit.getValidationMode() != null ) {
			configurationSettings.put( JAKARTA_VALIDATION_MODE, persistenceUnit.getValidationMode() );
			// EARLY EXIT!!!
			return;
		}

		var legacyValidationMode = configurationSettings.get( JPA_VALIDATION_MODE );
		if ( legacyValidationMode != null ) {
			DEPRECATION_LOGGER.deprecatedSetting( JPA_VALIDATION_MODE, JAKARTA_VALIDATION_MODE );
			configurationSettings.put( JAKARTA_VALIDATION_MODE, legacyValidationMode );
			configurationSettings.put( JPA_VALIDATION_MODE, legacyValidationMode );
		}
	}

	/// Because a DataSource can be secured (requiring Hibernate to pass the USER/PASSWORD when accessing the DataSource)
	/// we apply precedence to the USER and PASS separately
	private void normalizeConnectionAccessUserAndPass(
			Map<?, ?> integrationSettingsCopy,
			MergedSettings mergedSettings) {
		final Object effectiveUser = coalesceSuppliedValues(
				() -> {
					if ( integrationSettingsCopy != null ) {
						return coalesceSuppliedValues(
								() -> integrationSettingsCopy.remove( USER ),
								() -> integrationSettingsCopy.remove( JAKARTA_JDBC_USER ),
								() -> {
									final Object setting = integrationSettingsCopy.remove( JPA_JDBC_USER );
									if ( setting != null ) {
										DEPRECATION_LOGGER.deprecatedSetting( JPA_JDBC_USER, JAKARTA_JDBC_USER );
									}
									return setting;
								}
						);
					}
					else {
						return null;
					}
				},
				() -> {
					if ( persistenceUnit != null ) {
						return coalesceSuppliedValues(
								() -> extractPuProperty( persistenceUnit, USER ),
								() -> extractPuProperty( persistenceUnit, JAKARTA_JDBC_USER ),
								() -> {
									final Object setting = extractPuProperty( persistenceUnit, JPA_JDBC_USER );
									if ( setting != null ) {
										DEPRECATION_LOGGER.deprecatedSetting( JPA_JDBC_USER, JAKARTA_JDBC_USER );
									}
									return setting;
								}
						);
					}
					else {
						return null;
					}
				},
				() -> mergedSettings.getConfigurationValues().get( USER ),
				() -> mergedSettings.getConfigurationValues().get( JAKARTA_JDBC_USER ),
				() -> {
					final Object setting = mergedSettings.getConfigurationValues().get( JPA_JDBC_USER );
					if ( setting != null ) {
						DEPRECATION_LOGGER.deprecatedSetting( JPA_JDBC_USER, JAKARTA_JDBC_USER );
					}
					return setting;
				}
		);

		final Object effectivePass = coalesceSuppliedValues(
				() -> {
					if ( integrationSettingsCopy != null ) {
						return coalesceSuppliedValues(
								() -> integrationSettingsCopy.remove( PASS ),
								() -> integrationSettingsCopy.remove( JAKARTA_JDBC_PASSWORD ),
								() -> {
									final Object setting = integrationSettingsCopy.remove( JPA_JDBC_PASSWORD );
									if ( setting != null ) {
										DEPRECATION_LOGGER.deprecatedSetting( JPA_JDBC_PASSWORD, JAKARTA_JDBC_PASSWORD );
									}
									return setting;
								}
						);
					}
					else {
						return null;
					}
				},
				() -> {
					if ( persistenceUnit != null ) {
						return coalesceSuppliedValues(
								() -> extractPuProperty( persistenceUnit, PASS ),
								() -> extractPuProperty( persistenceUnit, JAKARTA_JDBC_PASSWORD ),
								() -> {
									{
										final Object setting = extractPuProperty( persistenceUnit, JPA_JDBC_PASSWORD );
										if ( setting != null ) {
											DEPRECATION_LOGGER.deprecatedSetting( JPA_JDBC_PASSWORD, JAKARTA_JDBC_PASSWORD );
										}
										return setting;
									}
								}
						);
					}
					else {
						return null;
					}
				},
				() -> mergedSettings.getConfigurationValues().get( PASS ),
				() -> mergedSettings.getConfigurationValues().get( JAKARTA_JDBC_PASSWORD ),
				() -> {
					final Object setting = mergedSettings.getConfigurationValues().get( JPA_JDBC_PASSWORD );
					if ( setting != null ) {
						DEPRECATION_LOGGER.deprecatedSetting( JPA_JDBC_PASSWORD, JAKARTA_JDBC_PASSWORD );
					}
					return setting;
				}
		);

		if ( effectiveUser != null || effectivePass != null ) {
			applyUserAndPass( effectiveUser, effectivePass, mergedSettings );
		}
	}

	private <T> T extractPuProperty(PersistenceUnitDescriptor persistenceUnit, String propertyName) {
		final var properties = persistenceUnit.getProperties();
		//noinspection unchecked
		return properties == null ? null : (T) properties.get( propertyName );
	}

	private void applyUserAndPass(Object effectiveUser, Object effectivePass, MergedSettings mergedSettings) {
		final var configuration = mergedSettings.getConfigurationValues();
		if ( effectiveUser != null ) {
			ConnectionSetting.USER.applyResolvedValue( configuration, effectiveUser );
		}
		if ( effectivePass != null ) {
			ConnectionSetting.PASSWORD.applyResolvedValue( configuration, effectivePass );
		}
	}

	private static final String IS_JTA_TXN_COORD = "local.setting.IS_JTA_TXN_COORD";

	private void normalizeTransactionCoordinator(
			PersistenceUnitDescriptor persistenceUnit,
			Map<?, ?> integrationSettingsCopy,
			MergedSettings mergedSettings) {
		final var txnType = determineTransactionType( persistenceUnit, integrationSettingsCopy, mergedSettings );
		final boolean definiteJtaCoordinator =
				mergedSettings.getConfigurationValues().containsKey( TRANSACTION_COORDINATOR_STRATEGY )
						? handeTransactionCoordinatorStrategy( mergedSettings )
						: handleTransactionType( mergedSettings, txnType );
		mergedSettings.getConfigurationValues().put( IS_JTA_TXN_COORD, definiteJtaCoordinator );
	}

	private static boolean handeTransactionCoordinatorStrategy(MergedSettings mergedSettings) {
		JPA_LOGGER.overridingTransactionStrategyDangerous( TRANSACTION_COORDINATOR_STRATEGY );
		// see if we can tell whether it is a JTA coordinator
		final Object strategy = mergedSettings.getConfigurationValues().get( TRANSACTION_COORDINATOR_STRATEGY );
		return strategy instanceof TransactionCoordinatorBuilder transactionCoordinatorBuilder
			&& transactionCoordinatorBuilder.isJta();
	}

	private static boolean handleTransactionType(MergedSettings mergedSettings, PersistenceUnitTransactionType txnType) {
		final boolean isJtaTransactionType = txnType == JTA;
		mergedSettings.getConfigurationValues()
				.put( TRANSACTION_COORDINATOR_STRATEGY, isJtaTransactionType
						? JtaTransactionCoordinatorBuilderImpl.class
						: JdbcResourceLocalTransactionCoordinatorBuilderImpl.class );
		return isJtaTransactionType;
	}

	private static PersistenceUnitTransactionType determineTransactionType(
			PersistenceUnitDescriptor persistenceUnit,
			Map<?, ?> integrationSettingsCopy,
			MergedSettings mergedSettings) {
		final var txnType = configuredTransactionType( persistenceUnit, integrationSettingsCopy, mergedSettings );
		if ( txnType == null ) {
			// is it more appropriate to have this be based on bootstrap entry point (EE vs SE)?
			JPA_LOGGER.fallingBackToResourceLocal();
			return PersistenceUnitTransactionType.RESOURCE_LOCAL;
		}
		else {
			return txnType;
		}
	}

	private static PersistenceUnitTransactionType configuredTransactionType(
			PersistenceUnitDescriptor persistenceUnit,
			Map<?, ?> integrationSettingsCopy,
			MergedSettings mergedSettings) {
		if ( integrationSettingsCopy != null ) {
			Object intgTxnType = integrationSettingsCopy.remove( JAKARTA_TRANSACTION_TYPE );
			if ( intgTxnType == null ) {
				intgTxnType = integrationSettingsCopy.remove( JPA_TRANSACTION_TYPE );
				if ( intgTxnType != null ) {
					DEPRECATION_LOGGER.deprecatedSetting( JPA_TRANSACTION_TYPE, JAKARTA_TRANSACTION_TYPE );
				}
			}
			if ( intgTxnType != null ) {
				return interpretTransactionType( intgTxnType );
			}
		}

		if ( persistenceUnit != null ) {
			final var persistenceUnitTransactionType = persistenceUnit.getPersistenceUnitTransactionType();
			if ( persistenceUnitTransactionType != null ) {
				return persistenceUnitTransactionType;
			}
		}

		Object puPropTxnType = mergedSettings.getConfigurationValues().get( JAKARTA_TRANSACTION_TYPE );
		if ( puPropTxnType == null ) {
			puPropTxnType = mergedSettings.getConfigurationValues().get( JPA_TRANSACTION_TYPE );
			if ( puPropTxnType != null ) {
				DEPRECATION_LOGGER.deprecatedSetting( JPA_TRANSACTION_TYPE, JAKARTA_TRANSACTION_TYPE );
			}
		}
		return puPropTxnType == null ? null : interpretTransactionType( puPropTxnType );
	}

	private void normalizeDataAccess(
			Map<?, ?> integrationSettingsCopy,
			MergedSettings mergedSettings,
			PersistenceUnitDescriptor persistenceUnit) {
		if ( dataSource != null ) {
			// we don't explicitly know if it's JTA
			applyDataSource( dataSource, null, integrationSettingsCopy, mergedSettings );
			// EARLY EXIT!!
			return;
		}

		final var configuration = mergedSettings.getConfigurationValues();

		if ( integrationSettingsCopy != null ) {
			if ( integrationSettingsCopy.containsKey( DATASOURCE ) ) {
				final Object dataSourceRef = integrationSettingsCopy.remove( DATASOURCE );
				if ( dataSourceRef != null ) {
					applyDataSource( dataSourceRef, null, integrationSettingsCopy, mergedSettings );
					// EARLY EXIT!!
					return;
				}
			}

			if ( integrationSettingsCopy.containsKey( JAKARTA_JTA_DATASOURCE ) ) {
				final Object dataSourceRef = integrationSettingsCopy.remove( JAKARTA_JTA_DATASOURCE );
				if ( dataSourceRef != null ) {
					applyDataSource( dataSourceRef, true, integrationSettingsCopy, mergedSettings );
					// EARLY EXIT!!
					return;
				}
			}

			if ( integrationSettingsCopy.containsKey( JPA_JTA_DATASOURCE ) ) {
				DEPRECATION_LOGGER.deprecatedSetting( JPA_JTA_DATASOURCE, JAKARTA_JTA_DATASOURCE );
				final Object dataSourceRef = integrationSettingsCopy.remove( JPA_JTA_DATASOURCE );
				if ( dataSourceRef != null ) {
					applyDataSource( dataSourceRef, true, integrationSettingsCopy, mergedSettings );
					// EARLY EXIT!!
					return;
				}
			}

			if ( integrationSettingsCopy.containsKey( JAKARTA_NON_JTA_DATASOURCE ) ) {
				final Object dataSourceRef = integrationSettingsCopy.remove( JAKARTA_NON_JTA_DATASOURCE );
				applyDataSource( dataSourceRef, false, integrationSettingsCopy, mergedSettings );
				// EARLY EXIT!!
				return;
			}

			if ( integrationSettingsCopy.containsKey( JPA_NON_JTA_DATASOURCE ) ) {
				DEPRECATION_LOGGER.deprecatedSetting( JPA_NON_JTA_DATASOURCE, JAKARTA_NON_JTA_DATASOURCE );
				final Object dataSourceRef = integrationSettingsCopy.remove( JPA_NON_JTA_DATASOURCE );
				applyDataSource( dataSourceRef, false, integrationSettingsCopy, mergedSettings );
				// EARLY EXIT!!
				return;
			}

			if ( integrationSettingsCopy.containsKey( URL ) ) {
				// hibernate-specific settings have precedence over the JPA ones
				final Object integrationJdbcUrl = integrationSettingsCopy.get( URL );
				if ( integrationJdbcUrl != null ) {
					applyJdbcSettings(
							integrationJdbcUrl,
							coalesceSuppliedValues(
									() -> getString( DRIVER, integrationSettingsCopy ),
									() -> getString( JAKARTA_JDBC_DRIVER, integrationSettingsCopy ),
									() -> {
										final String driver = getString( JPA_JDBC_DRIVER, integrationSettingsCopy );
										if ( driver != null ) {
											DEPRECATION_LOGGER.deprecatedSetting( JPA_JDBC_DRIVER,
													JAKARTA_JDBC_DRIVER );
										}
										return driver;
									},
									() -> getString( DRIVER, configuration ),
									() -> getString( JAKARTA_JDBC_DRIVER, configuration ),
									() -> {
										final String driver = getString( JPA_JDBC_DRIVER, configuration );
										if ( driver != null ) {
											DEPRECATION_LOGGER.deprecatedSetting( JPA_JDBC_DRIVER,
													JAKARTA_JDBC_DRIVER );
										}
										return driver;
									}
							),
							integrationSettingsCopy,
							mergedSettings
					);
					// EARLY EXIT!!
					return;
				}
			}

			if ( integrationSettingsCopy.containsKey( JAKARTA_JDBC_URL ) ) {
				final Object integrationJdbcUrl = integrationSettingsCopy.get( JAKARTA_JDBC_URL );
				if ( integrationJdbcUrl != null ) {
					applyJdbcSettings(
							integrationJdbcUrl,
							coalesceSuppliedValues(
									() -> getString( JAKARTA_JDBC_DRIVER, integrationSettingsCopy ),
									() -> getString( JAKARTA_JDBC_DRIVER, configuration )
							),
							integrationSettingsCopy,
							mergedSettings
					);
					// EARLY EXIT!!
					return;
				}
			}

			if ( integrationSettingsCopy.containsKey( JPA_JDBC_URL ) ) {
				DEPRECATION_LOGGER.deprecatedSetting( JPA_JDBC_URL, JAKARTA_JDBC_URL );
				final Object integrationJdbcUrl = integrationSettingsCopy.get( JPA_JDBC_URL );
				if ( integrationJdbcUrl != null ) {
					applyJdbcSettings(
							integrationJdbcUrl,
							coalesceSuppliedValues(
									() -> {
										final String driver = getString( JPA_JDBC_DRIVER, integrationSettingsCopy );
										if ( driver != null ) {
											DEPRECATION_LOGGER.deprecatedSetting( JPA_JDBC_DRIVER,
													JAKARTA_JDBC_DRIVER );
										}
										return driver;
									},
									() -> {
										final String driver = getString( JPA_JDBC_DRIVER, configuration );
										if ( driver != null ) {
											DEPRECATION_LOGGER.deprecatedSetting( JPA_JDBC_DRIVER,
													JAKARTA_JDBC_DRIVER );
										}
										return driver;
									}
							),
							integrationSettingsCopy,
							mergedSettings
					);
					// EARLY EXIT!!
					return;
				}
			}
		}

		if ( persistenceUnit != null ) {
			final Object jtaDataSource = persistenceUnit.getJtaDataSource();
			if ( jtaDataSource != null ) {
				applyDataSource( jtaDataSource, true, integrationSettingsCopy, mergedSettings );
				// EARLY EXIT!!
				return;
			}

			final Object nonJtaDataSource = persistenceUnit.getNonJtaDataSource();
			if ( nonJtaDataSource != null ) {
				applyDataSource( nonJtaDataSource, false, integrationSettingsCopy, mergedSettings );
				// EARLY EXIT!!
				return;
			}
		}

		if ( configuration.containsKey( URL ) ) {
			final Object url = configuration.get( URL );
			if ( url != null && !( url instanceof String stringUrl && isEmpty( stringUrl ) ) ) {
				applyJdbcSettings(
						url,
						getString( DRIVER, configuration ),
						integrationSettingsCopy,
						mergedSettings
				);
				// EARLY EXIT!!
				return;
			}
		}

		if ( configuration.containsKey( JAKARTA_JDBC_URL ) ) {
			final Object url = configuration.get( JAKARTA_JDBC_URL );
			if ( url != null && !( url instanceof String stringUrl && isEmpty( stringUrl ) ) ) {
				applyJdbcSettings(
						url,
						getString( JAKARTA_JDBC_DRIVER, configuration ),
						integrationSettingsCopy,
						mergedSettings
				);
				// EARLY EXIT!!
				return;
			}
		}

		if ( configuration.containsKey( JPA_JDBC_URL ) ) {
			DEPRECATION_LOGGER.deprecatedSetting( JPA_JDBC_URL, JAKARTA_JDBC_URL );
			final Object url = configuration.get( JPA_JDBC_URL );
			if ( url != null && !( url instanceof String stringUrl && isEmpty( stringUrl ) ) ) {
				final String driver = getString( JPA_JDBC_DRIVER, configuration );
				if ( driver != null ) {
					DEPRECATION_LOGGER.deprecatedSetting( JPA_JDBC_DRIVER, JAKARTA_JDBC_DRIVER );
				}
				applyJdbcSettings( url, driver, integrationSettingsCopy, mergedSettings );
			}
		}
	}

	private void applyDataSource(
			Object dataSourceRef,
			Boolean useJtaDataSource,
			Map<?, ?> integrationSettingsCopy,
			MergedSettings mergedSettings) {

		// `IS_JTA_TXN_COORD` is a value set during `#normalizeTransactionCoordinator` to indicate whether
		// the execution environment "is JTA" as best as it can tell..
		//
		// we use this value when JTA was not explicitly specified in regards to the DataSource
		final boolean isJtaTransactionCoordinator =
				(Boolean) mergedSettings.getConfigurationValues().remove( IS_JTA_TXN_COORD );
		final boolean isJta = useJtaDataSource == null ? isJtaTransactionCoordinator : useJtaDataSource;

		// add to EMF properties (questionable - see HHH-13432)
		final String inverseEmfKey;
		final String jakartaInverseEmfKey;
		if ( isJta ) {
			inverseEmfKey = JPA_NON_JTA_DATASOURCE;
			jakartaInverseEmfKey = JAKARTA_NON_JTA_DATASOURCE;
		}
		else {
			inverseEmfKey = JPA_JTA_DATASOURCE;
			jakartaInverseEmfKey = JAKARTA_JTA_DATASOURCE;
		}
		( isJta ? ConnectionSetting.JTA_DATASOURCE : ConnectionSetting.NON_JTA_DATASOURCE )
				.applyPersistenceAliases( mergedSettings.getConfigurationValues(), dataSourceRef );

		// clear any settings logically overridden by this datasource
		cleanUpConfigKeys(
				integrationSettingsCopy,
				mergedSettings,
				inverseEmfKey,
				jakartaInverseEmfKey,
				JPA_JDBC_DRIVER,
				JAKARTA_JDBC_DRIVER,
				DRIVER,
				JPA_JDBC_URL,
				JAKARTA_JDBC_URL,
				URL
		);


		// clean-up the entries in the "integration overrides" so they do not get get picked
		// up in the general "integration overrides" handling
		cleanUpConfigKeys(
				integrationSettingsCopy,
				DATASOURCE,
				JPA_JTA_DATASOURCE,
				JAKARTA_JTA_DATASOURCE,
				JPA_NON_JTA_DATASOURCE,
				JAKARTA_NON_JTA_DATASOURCE
		);

		// add under Hibernate's DATASOURCE setting where the ConnectionProvider will find it
		mergedSettings.getConfigurationValues().put( DATASOURCE, dataSourceRef );
	}

	private void cleanUpConfigKeys(Map<?, ?> integrationSettingsCopy, MergedSettings mergedSettings, String... keys) {
		for ( String key : keys ) {
			if ( integrationSettingsCopy != null ) {
				final Object removedSetting = integrationSettingsCopy.remove( key );
				if ( removedSetting != null ) {
					JPA_LOGGER.removedIntegrationOverride( key );
				}
			}

			final Object removedMergedSetting = mergedSettings.getConfigurationValues().remove( key );
			if ( removedMergedSetting != null ) {
				JPA_LOGGER.removedMergedSetting( key );
			}
		}
	}

	private void cleanUpConfigKeys(Map<?, ?> settings, String... keys) {
		for ( String key : keys ) {
			settings.remove( key );
		}
	}

	private void applyJdbcSettings(
			Object url,
			String driver,
			Map<?, ?> integrationSettingsCopy,
			MergedSettings mergedSettings) {
		ConnectionSetting.URL.applyResolvedValue( mergedSettings.getConfigurationValues(), url );

		if ( driver != null ) {
			ConnectionSetting.DRIVER.applyResolvedValue( mergedSettings.getConfigurationValues(), driver );
		}
		else {
			ConnectionSetting.DRIVER.remove( mergedSettings.getConfigurationValues() );
		}

		// clean up the integration-map values
		if ( integrationSettingsCopy != null ) {
			cleanUpConfigKeys(
					integrationSettingsCopy,
					DRIVER,
					JPA_JDBC_DRIVER,
					JAKARTA_JDBC_DRIVER,
					URL,
					JPA_JDBC_URL,
					JAKARTA_JDBC_URL,
					USER,
					JPA_JDBC_USER,
					JAKARTA_JDBC_USER,
					PASS,
					JPA_JDBC_PASSWORD,
					JAKARTA_JDBC_PASSWORD
			);
		}

		cleanUpConfigKeys(
				integrationSettingsCopy,
				mergedSettings,
				DATASOURCE,
				JPA_JTA_DATASOURCE,
				JAKARTA_JTA_DATASOURCE,
				JPA_NON_JTA_DATASOURCE,
				JAKARTA_NON_JTA_DATASOURCE
		);
	}

	private void processHibernateConfigXmlResources(
			StandardServiceRegistryBuilder serviceRegistryBuilder,
			MergedSettings mergedSettings,
			String cfgXmlResourceName) {
		final var loadedConfig =
				serviceRegistryBuilder.getConfigLoader()
						.loadConfigXmlResource( cfgXmlResourceName );
		mergedSettings.processHibernateConfigXmlResources( loadedConfig );
		serviceRegistryBuilder.getAggregatedCfgXml().merge( loadedConfig );
	}

	private CacheRegionDefinition parseCacheRegionDefinitionEntry(
			String role, String value, CacheRegionType cacheType) {
		final var params = new StringTokenizer( value, ";, " );
		if ( !params.hasMoreTokens() ) {
			throw illegalCacheRegionDefinitionException( role, value, cacheType );
		}
		else {
			final String usage = params.nextToken();
			final String region = params.hasMoreTokens() ? params.nextToken() : null;
			final boolean lazyProperty =
					cacheType == CacheRegionType.ENTITY
							&& ( !params.hasMoreTokens() || "all".equalsIgnoreCase( params.nextToken() ) );
			return new CacheRegionDefinition( cacheType, role, usage, region, lazyProperty );
		}
	}

	private PersistenceException illegalCacheRegionDefinitionException(
			String role, String value, CacheRegionType cacheType) {
		final StringBuilder message =
				new StringBuilder( "Cache region configuration '" )
						.append( cacheType == CacheRegionType.ENTITY ? CLASS_CACHE_PREFIX : COLLECTION_CACHE_PREFIX )
						.append( '.' )
						.append(role)
						.append( ' ' )
						.append(value)
						.append( "' not of form 'usage[,region[,lazy]]' " )
						.append( exceptionHeader.get() );
		return new PersistenceException( message.toString() );
	}


	private static void ignoreFlushBeforeCompletion(MergedSettings mergedSettings) {
		// flush before completion validation
		final var config = mergedSettings.getConfigurationValues();
		if ( getBoolean( FLUSH_BEFORE_COMPLETION, config ) ) {
			JPA_LOGGER.definingFlushBeforeCompletionIgnoredInHem( FLUSH_BEFORE_COMPLETION );
			config.put( FLUSH_BEFORE_COMPLETION, String.valueOf(false) );
		}
	}


	private void processConfigurationValues(MergedSettings mergedSettings) {
		// here we are going to iterate the merged config settings looking for:
		//		1) additional JACC permissions
		//		2) additional cache region declarations
		//
		// we will also clean up any references with null entries
		final var iterator = mergedSettings.getConfigurationValues().entrySet().iterator();
		while ( iterator.hasNext() ) {
			final var entry = iterator.next();
			final Object value = entry.getValue();
			if ( value == null ) {
				// remove entries with null values
				iterator.remove();
				break; //TODO: this looks wrong!
			}
			else if ( value instanceof String valueString ) {
				handleCacheRegionDefinition( valueString, entry.getKey(), mergedSettings );
			}
		}

	}
}
