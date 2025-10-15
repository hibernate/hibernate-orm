/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.spi;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.Internal;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.SchemaToolingSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.Action;
import org.hibernate.tool.schema.SourceType;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.ExceptionHandlerHaltImpl;
import org.hibernate.tool.schema.internal.ExceptionHandlerLoggedImpl;

import org.jboss.logging.Logger;

import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_AUTO;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_CHARSET_NAME;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_CREATE_SCRIPT_SOURCE;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_CREATE_SOURCE;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_DATABASE_ACTION;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_DROP_SCRIPT_SOURCE;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_DROP_SOURCE;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_HALT_ON_ERROR;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_SCRIPTS_ACTION;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_SCRIPTS_CREATE_APPEND;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_SCRIPTS_CREATE_TARGET;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_SCRIPTS_DROP_TARGET;
import static org.hibernate.cfg.SchemaToolingSettings.JAKARTA_HBM2DDL_CREATE_SCRIPT_SOURCE;
import static org.hibernate.cfg.SchemaToolingSettings.JAKARTA_HBM2DDL_CREATE_SOURCE;
import static org.hibernate.cfg.SchemaToolingSettings.JAKARTA_HBM2DDL_DATABASE_ACTION;
import static org.hibernate.cfg.SchemaToolingSettings.JAKARTA_HBM2DDL_DROP_SCRIPT_SOURCE;
import static org.hibernate.cfg.SchemaToolingSettings.JAKARTA_HBM2DDL_DROP_SOURCE;
import static org.hibernate.cfg.SchemaToolingSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION;
import static org.hibernate.cfg.SchemaToolingSettings.JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET;
import static org.hibernate.cfg.SchemaToolingSettings.JAKARTA_HBM2DDL_SCRIPTS_DROP_TARGET;
import static org.hibernate.engine.config.spi.StandardConverters.BOOLEAN;
import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;
import static org.hibernate.tool.schema.Action.interpretHbm2ddlSetting;
import static org.hibernate.tool.schema.Action.interpretJpaSetting;
import static org.hibernate.tool.schema.internal.Helper.interpretNamespaceHandling;
import static org.hibernate.tool.schema.internal.Helper.interpretScriptSourceSetting;
import static org.hibernate.tool.schema.internal.Helper.interpretScriptTargetSetting;

/**
 * Responsible for coordinating {@link SchemaManagementTool} execution
 * whether from {@value SchemaToolingSettings#HBM2DDL_AUTO}, JPA-standard
 * {@value SchemaToolingSettings#JAKARTA_HBM2DDL_DATABASE_ACTION}, or
 * {@link org.hibernate.relational.SchemaManager}.
 * <p>
 * The main entry point is {@link #process}.
 *
 * @author Steve Ebersole
 */
public class SchemaManagementToolCoordinator {
	private static final Logger LOG = Logger.getLogger( SchemaManagementToolCoordinator.class );

	public static void process(
			final Metadata metadata,
			final ServiceRegistry serviceRegistry,
			final Map<String,Object> configuration,
			DelayedDropRegistry delayedDropRegistry) {
		final Set<ActionGrouping> groupings = ActionGrouping.interpret( metadata, configuration );
		if ( groupings.isEmpty() ) {
			// no actions specified
			LOG.debug( "No schema management actions found" );
		}
		else {
			final var databaseActionMap = collectDatabaseActions( groupings );
			final var scriptActionMap = collectScriptActions( groupings );

			final var tool = serviceRegistry.getService( SchemaManagementTool.class );
			final var configService = serviceRegistry.requireService( ConfigurationService.class );

			final boolean haltOnError =
					configService.getSetting( HBM2DDL_HALT_ON_ERROR, BOOLEAN, false );

			final var executionOptions =
					buildExecutionOptions( configuration,
							haltOnError
									? ExceptionHandlerHaltImpl.INSTANCE
									: ExceptionHandlerLoggedImpl.INSTANCE );

			if ( scriptActionMap != null ) {
				scriptActionMap.forEach( (action, contributors) ->
						performScriptAction( action, metadata, tool, serviceRegistry, executionOptions, configService ) );
			}

			if ( databaseActionMap != null ) {
				databaseActionMap.forEach( (action, contributors) -> {
					performDatabaseAction(
							action,
							metadata,
							tool,
							serviceRegistry,
							executionOptions,
							exportable -> contributors.contains( exportable.getContributor() )
					);

					if ( action == Action.CREATE_DROP ) {
						delayedDropRegistry.registerOnCloseAction(
								tool.getSchemaDropper( configuration ).buildDelayedAction(
										metadata,
										executionOptions,
										exportable -> contributors.contains( exportable.getContributor() ),
										buildDatabaseTargetDescriptor( configuration,
												DropSettingSelector.INSTANCE, serviceRegistry )
								)
						);
					}
				} );
			}
		}
	}

	private static Map<Action, Set<String>> collectScriptActions(Set<ActionGrouping> groupings) {
		Map<Action,Set<String>> scriptActionMap = null;
		for ( var grouping : groupings ) {
			if ( grouping.scriptAction != Action.NONE ) {
				// for script action
				final Set<String> contributors;
				if ( scriptActionMap == null ) {
					scriptActionMap = new HashMap<>();
					contributors = new HashSet<>();
					scriptActionMap.put( grouping.scriptAction, contributors );
				}
				else {
					contributors = scriptActionMap.computeIfAbsent(
							grouping.scriptAction,
							action -> new HashSet<>()
					);
				}
				contributors.add( grouping.contributor );
			}
		}
		return scriptActionMap;
	}

	private static Map<Action, Set<String>> collectDatabaseActions(Set<ActionGrouping> groupings) {
		Map<Action,Set<String>> databaseActionMap = null;
		for ( var grouping : groupings ) {
			// for database action
			if ( grouping.databaseAction != Action.NONE ) {
				final Set<String> contributors;
				if ( databaseActionMap == null ) {
					databaseActionMap = new HashMap<>();
					contributors = new HashSet<>();
					databaseActionMap.put( grouping.databaseAction, contributors );
				}
				else {
					contributors = databaseActionMap.computeIfAbsent(
							grouping.databaseAction,
							action -> new HashSet<>()
					);
				}
				contributors.add( grouping.contributor );
			}
		}
		return databaseActionMap;
	}

	public static ExecutionOptions buildExecutionOptions(
			final Map<String,Object> configurationValues,
			final ExceptionHandler exceptionHandler) {
		return new ExecutionOptions() {
			@Override
			public boolean shouldManageNamespaces() {
				return interpretNamespaceHandling( configurationValues );
			}

			@Override
			public Map<String,Object> getConfigurationValues() {
				return configurationValues;
			}

			@Override
			public ExceptionHandler getExceptionHandler() {
				return exceptionHandler;
			}
		};
	}

	private static void performDatabaseAction(
			final Action action,
			Metadata metadata,
			SchemaManagementTool tool,
			ServiceRegistry serviceRegistry,
			final ExecutionOptions executionOptions,
			ContributableMatcher contributableInclusionFilter) {

		// IMPL NOTE: JPA binds source and target info

		final var configurationValues = executionOptions.getConfigurationValues();
		switch ( action ) {
			case CREATE_ONLY: {
				final var createDescriptor = buildDatabaseTargetDescriptor(
						configurationValues,
						CreateSettingSelector.INSTANCE,
						serviceRegistry
				);
				tool.getSchemaCreator( configurationValues ).doCreation(
						metadata,
						executionOptions,
						contributableInclusionFilter,
						createDescriptor,
						createDescriptor
				);
				break;
			}
			case CREATE:
			case CREATE_DROP: {
				final var dropDescriptor = buildDatabaseTargetDescriptor(
						configurationValues,
						DropSettingSelector.INSTANCE,
						serviceRegistry
				);
				tool.getSchemaDropper( configurationValues ).doDrop(
						metadata,
						executionOptions,
						contributableInclusionFilter,
						dropDescriptor,
						dropDescriptor
				);
				final var createDescriptor = buildDatabaseTargetDescriptor(
						configurationValues,
						CreateSettingSelector.INSTANCE,
						serviceRegistry
				);
				tool.getSchemaCreator( configurationValues ).doCreation(
						metadata,
						executionOptions,
						contributableInclusionFilter,
						createDescriptor,
						createDescriptor
				);
				break;
			}
			case DROP: {
				final var dropDescriptor = buildDatabaseTargetDescriptor(
						configurationValues,
						DropSettingSelector.INSTANCE,
						serviceRegistry
				);
				tool.getSchemaDropper( configurationValues ).doDrop(
						metadata,
						executionOptions,
						contributableInclusionFilter,
						dropDescriptor,
						dropDescriptor
				);
				break;
			}
			case UPDATE: {
				final var migrateDescriptor = buildDatabaseTargetDescriptor(
						configurationValues,
						MigrateSettingSelector.INSTANCE,
						serviceRegistry
				);
				tool.getSchemaMigrator( configurationValues ).doMigration(
						metadata,
						executionOptions,
						contributableInclusionFilter,
						migrateDescriptor
				);
				break;
			}
			case VALIDATE: {
				tool.getSchemaValidator( configurationValues ).doValidation(
						metadata,
						executionOptions,
						contributableInclusionFilter
				);
				break;
			}
			case SYNCHRONIZE: {
				tool.getSequenceSynchronizer( configurationValues ).doSynchronize(
						metadata,
						executionOptions,
						contributableInclusionFilter,
						buildDatabaseTargetDescriptor(
								configurationValues,
								CreateSettingSelector.INSTANCE,
								serviceRegistry
						)
				);
				break;
			}
			case TRUNCATE: {
				tool.getSchemaTruncator( configurationValues ).doTruncate(
						metadata,
						executionOptions,
						contributableInclusionFilter,
						buildDatabaseTargetDescriptor(
								configurationValues,
								CreateSettingSelector.INSTANCE,
								serviceRegistry
						)
				);
				break;
			}
			case POPULATE: {
				tool.getSchemaPopulator( configurationValues ).doPopulation(
						executionOptions,
						buildDatabaseTargetDescriptor(
								configurationValues,
								CreateSettingSelector.INSTANCE,
								serviceRegistry
						)
				);
				break;
			}
		}
	}

	private static SourceType sourceType(Map<?, ?> configuration, SettingSelector settingSelector, Object scriptSourceSetting) {
		return SourceType.interpret( settingSelector.getSourceTypeSetting( configuration ),
				scriptSourceSetting != null ? SourceType.SCRIPT : SourceType.METADATA );
	}

	private static JpaTargetAndSourceDescriptor buildDatabaseTargetDescriptor(
			Map<?,?> configuration,
			SettingSelector settingSelector,
			ServiceRegistry serviceRegistry) {

		final Object scriptSourceSetting = settingSelector.getScriptSourceSetting( configuration );
		final var sourceType = sourceType( configuration, settingSelector, scriptSourceSetting );
		final boolean includesScripts = sourceType != SourceType.METADATA;
		if ( includesScripts && scriptSourceSetting == null ) {
			throw new SchemaManagementException(
					"Schema generation configuration indicated to include CREATE scripts, but no script was specified"
			);
		}

		final var scriptSourceInput =
				includesScripts
						? interpretScriptSourceSetting( scriptSourceSetting,
								serviceRegistry.getService( ClassLoaderService.class ),
								(String) configuration.get( HBM2DDL_CHARSET_NAME ) )
						: null;

		return new JpaTargetAndSourceDescriptor() {
			@Override
			public EnumSet<TargetType> getTargetTypes() {
				return EnumSet.of( TargetType.DATABASE );
			}

			@Override
			public ScriptTargetOutput getScriptTargetOutput() {
				return null;
			}

			@Override
			public SourceType getSourceType() {
				return sourceType;
			}

			@Override
			public ScriptSourceInput getScriptSourceInput() {
				return scriptSourceInput;
			}
		};
	}

	private static void performScriptAction(
			Action scriptAction,
			Metadata metadata,
			SchemaManagementTool tool,
			ServiceRegistry serviceRegistry,
			ExecutionOptions executionOptions,
			ConfigurationService configurationService) {

		final var configurationValues = executionOptions.getConfigurationValues();
		switch ( scriptAction ) {
			case CREATE_ONLY: {
				final var createDescriptor = buildScriptTargetDescriptor(
						configurationValues,
						CreateSettingSelector.INSTANCE,
						serviceRegistry,
						configurationService
				);
				tool.getSchemaCreator( configurationValues ).doCreation(
						metadata,
						executionOptions,
						(contributed) -> true,
						createDescriptor,
						createDescriptor
				);
				break;
			}
			case CREATE:
			case CREATE_DROP: {
				final var dropDescriptor = buildScriptTargetDescriptor(
						configurationValues,
						DropSettingSelector.INSTANCE,
						serviceRegistry,
						configurationService
				);
				tool.getSchemaDropper( configurationValues ).doDrop(
						metadata,
						executionOptions,
						contributed -> true,
						dropDescriptor,
						dropDescriptor
				);
				final var createDescriptor = buildScriptTargetDescriptor(
						configurationValues,
						CreateSettingSelector.INSTANCE,
						serviceRegistry,
						configurationService
				);
				tool.getSchemaCreator( configurationValues ).doCreation(
						metadata,
						executionOptions,
						contributed -> true,
						createDescriptor,
						createDescriptor
				);
				break;
			}
			case DROP: {
				final var dropDescriptor = buildScriptTargetDescriptor(
						configurationValues,
						DropSettingSelector.INSTANCE,
						serviceRegistry,
						configurationService
				);
				tool.getSchemaDropper( configurationValues ).doDrop(
						metadata,
						executionOptions,
						contributed -> true,
						dropDescriptor,
						dropDescriptor
				);
				break;
			}
			case UPDATE: {
				final var migrateDescriptor = buildScriptTargetDescriptor(
						configurationValues,
						MigrateSettingSelector.INSTANCE,
						serviceRegistry,
						configurationService
				);
				tool.getSchemaMigrator( configurationValues ).doMigration(
						metadata,
						executionOptions,
						contributed -> true,
						migrateDescriptor
				);
				break;
			}
			case VALIDATE: {
				throw new SchemaManagementException( "VALIDATE is not valid SchemaManagementTool action for script output" );
			}
		}
	}

	private static JpaTargetAndSourceDescriptor buildScriptTargetDescriptor(
			Map<?,?> configuration,
			SettingSelector settingSelector,
			ServiceRegistry serviceRegistry,
			ConfigurationService configurationService) {

		final Object scriptTargetSetting = settingSelector.getScriptTargetSetting( configuration );
		final Object scriptSourceSetting = settingSelector.getScriptSourceSetting( configuration );

		final var sourceType = sourceType( configuration, settingSelector, scriptSourceSetting );
		final boolean includesScripts = sourceType != SourceType.METADATA;
		if ( includesScripts && scriptSourceSetting == null ) {
			throw new SchemaManagementException(
					"Schema generation configuration indicated to include CREATE scripts, but no script was specified"
			);
		}

		final String charsetName = (String) configuration.get( HBM2DDL_CHARSET_NAME );
		final var classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		final var scriptSourceInput =
				includesScripts
						? interpretScriptSourceSetting( scriptSourceSetting, classLoaderService, charsetName )
						: null;
		final var scriptTargetOutput =
				interpretScriptTargetSetting( scriptTargetSetting, classLoaderService, charsetName,
						configurationService.getSetting( HBM2DDL_SCRIPTS_CREATE_APPEND, BOOLEAN, true ) );

		return new JpaTargetAndSourceDescriptor() {
			@Override
			public EnumSet<TargetType> getTargetTypes() {
				return EnumSet.of( TargetType.SCRIPT );
			}

			@Override
			public ScriptTargetOutput getScriptTargetOutput() {
				return scriptTargetOutput;
			}

			@Override
			public SourceType getSourceType() {
				return sourceType;
			}

			@Override
			public ScriptSourceInput getScriptSourceInput() {
				return scriptSourceInput;
			}
		};
	}

	private interface SettingSelector {
		Object getSourceTypeSetting(Map<?,?> configurationValues);
		Object getScriptSourceSetting(Map<?,?> configurationValues);
		Object getScriptTargetSetting(Map<?,?> configurationValues);
	}

	private static Object settingValue(Map<?,?> configuration, String referenceKey, String legacyKey) {
		final Object setting = configuration.get( referenceKey );
		if ( setting != null ) {
			return setting;
		}
		else {
			final Object legacySetting = configuration.get( legacyKey );
			if ( legacySetting != null ) {
				DEPRECATION_LOGGER.deprecatedSetting( referenceKey, legacyKey );
			}
			return legacySetting;
		}
	}

	private static Object actionSettingValue(
			Map<?, ?> configuration, String contributor,
			String jakartaSettingName, String javaxSettingName) {
		final Object actionSetting =
				configuration.get( qualify( contributor, jakartaSettingName ) );
		if ( actionSetting != null ) {
			return actionSetting;
		}
		else {
			final Object deprecatedActionSetting =
					configuration.get( qualify( contributor, javaxSettingName ) );
			if ( deprecatedActionSetting != null ) {
				DEPRECATION_LOGGER.deprecatedSetting( javaxSettingName, jakartaSettingName );
			}
			return deprecatedActionSetting;
		}
	}

	private static String qualify(String contributor, String settingName) {
		return contributor == null ? settingName : settingName + '.' + contributor;
	}

	private static class CreateSettingSelector implements SettingSelector {
		/**
		 * Singleton access
		 */
		public static final CreateSettingSelector INSTANCE = new CreateSettingSelector();

		@Override
		public Object getSourceTypeSetting(Map<?,?> configurationValues) {
			return settingValue(
					configurationValues,
					JAKARTA_HBM2DDL_CREATE_SOURCE,
					HBM2DDL_CREATE_SOURCE
			);
		}

		@Override
		public Object getScriptSourceSetting(Map<?,?> configurationValues) {
			return settingValue(
					configurationValues,
					JAKARTA_HBM2DDL_CREATE_SCRIPT_SOURCE,
					HBM2DDL_CREATE_SCRIPT_SOURCE
			);
		}

		@Override
		public Object getScriptTargetSetting(Map<?,?> configurationValues) {
			return settingValue(
					configurationValues,
					JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET,
					HBM2DDL_SCRIPTS_CREATE_TARGET
			);
		}
	}

	private static class DropSettingSelector implements SettingSelector {
		/**
		 * Singleton access
		 */
		public static final DropSettingSelector INSTANCE = new DropSettingSelector();

		@Override
		public Object getSourceTypeSetting(Map<?,?> configurationValues) {
			return settingValue(
					configurationValues,
					JAKARTA_HBM2DDL_DROP_SOURCE,
					HBM2DDL_DROP_SOURCE
			);
		}

		@Override
		public Object getScriptSourceSetting(Map<?,?> configurationValues) {
			return settingValue(
					configurationValues,
					JAKARTA_HBM2DDL_DROP_SCRIPT_SOURCE,
					HBM2DDL_DROP_SCRIPT_SOURCE
			);
		}

		@Override
		public Object getScriptTargetSetting(Map<?,?> configurationValues) {
			return settingValue(
					configurationValues,
					JAKARTA_HBM2DDL_SCRIPTS_DROP_TARGET,
					HBM2DDL_SCRIPTS_DROP_TARGET
			);
		}
	}

	private static class MigrateSettingSelector implements SettingSelector {
		/**
		 * Singleton access
		 */
		public static final MigrateSettingSelector INSTANCE = new MigrateSettingSelector();

		// todo : should this define new migrator-specific settings?
		// for now we reuse the CREATE settings where applicable

		@Override
		public Object getSourceTypeSetting(Map<?,?> configurationValues) {
			// for now, don't allow script source
			return SourceType.METADATA;
		}

		@Override
		public Object getScriptSourceSetting(Map<?,?> configurationValues) {
			// for now, don't allow script source
			return null;
		}

		@Override
		public Object getScriptTargetSetting(Map<?,?> configurationValues) {
			// for now, reuse the CREATE script target setting
			return settingValue(
					configurationValues,
					JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET,
					HBM2DDL_SCRIPTS_CREATE_TARGET
			);
		}
	}

	/**
	 * For JPA-style schema-gen, database and script target handing are configured
	 * individually - this tuple allows interpreting the action for both targets
	 * simultaneously
	 */
	public record ActionGrouping(String contributor, Action databaseAction, Action scriptAction) {

		/**
		 * For test use.  See {@link #interpret(Metadata, Map)} for the "real" impl
		 */
		@Internal
		public static ActionGrouping interpret(Map<?, ?> configurationValues) {
			// default to the JPA settings
			final Action databaseAction = determineJpaDbActionSetting( configurationValues );
			final Action scriptAction = determineJpaScriptActionSetting( configurationValues );
			if ( databaseAction != null || scriptAction != null ) {
				return new ActionGrouping( "orm",
						databaseAction == null ? Action.NONE : databaseAction,
						scriptAction == null ? Action.NONE : scriptAction );
			}
			else {
				// if no JPA settings were specified, look at the legacy HBM2DDL_AUTO setting
				final Action autoAction =
						determineAutoSettingImpliedAction( configurationValues, null, null );
				return new ActionGrouping( "orm",
						autoAction == null ? Action.NONE : autoAction,
						Action.NONE);
			}
		}

		private static Action determineJpaDbActionSetting(Map<?, ?> configurationValues) {
			return determineJpaDbActionSetting( configurationValues, null, null );
		}

		/**
		 * Exposed for tests
		 */
		@Internal
		public static Action determineJpaDbActionSetting(
				Map<?, ?> configuration,
				String contributor,
				Action defaultValue) {
			final Object databaseActionSetting =
					actionSettingValue( configuration, contributor,
							JAKARTA_HBM2DDL_DATABASE_ACTION, HBM2DDL_DATABASE_ACTION );
			return databaseActionSetting == null
					? defaultValue
					: interpretJpaSetting( databaseActionSetting );
		}

		private static Action determineJpaScriptActionSetting(Map<?, ?> configurationValues) {
			return determineJpaScriptActionSetting( configurationValues, null, null );
		}

		/**
		 * Exposed for tests
		 */
		@Internal
		public static Action determineJpaScriptActionSetting(
				Map<?, ?> configuration,
				String contributor,
				Action defaultValue) {
			final Object scriptsActionSetting =
					actionSettingValue( configuration, contributor,
							JAKARTA_HBM2DDL_SCRIPTS_ACTION, HBM2DDL_SCRIPTS_ACTION );
			return scriptsActionSetting == null ? defaultValue : interpretJpaSetting( scriptsActionSetting );
		}

		private static Action determineAutoSettingImpliedAction(Map<?, ?> settings, String contributor, Action defaultValue) {
			final Object actionSetting = settings.get( qualify( contributor, HBM2DDL_AUTO ) );
			return actionSetting == null ? defaultValue : interpretHbm2ddlSetting( actionSetting );
		}

		public static Set<ActionGrouping> interpret(Set<String> contributors, Map<?, ?> configuration) {
			// these represent the base (non-contributor-specific) values
			final Action rootDatabaseAction =
					determineJpaDbActionSetting( configuration, null, null );
			final Action rootScriptAction =
					determineJpaScriptActionSetting( configuration, null, null );
			final Action rootAutoAction =
					determineAutoSettingImpliedAction( configuration, null, null );
			final Set<ActionGrouping> groupings = new HashSet<>( contributors.size() );
			// for each contributor, look for specific tooling config values
			for ( String contributor : contributors ) {
				final Action scriptActionToUse =
						scriptActionToUse( configuration, contributor, rootScriptAction );
				final Action databaseActionToUse =
						databaseActionToUse( configuration, contributor, rootDatabaseAction, scriptActionToUse, rootAutoAction );
				if ( databaseActionToUse == Action.NONE && scriptActionToUse == Action.NONE ) {
					LOG.debugf( "No schema actions specified for contributor '%s'", contributor );
				}
				else {
					groupings.add( new ActionGrouping( contributor, databaseActionToUse, scriptActionToUse ) );
				}
			}
			return groupings;
		}

		private static Action scriptActionToUse(Map<?, ?> configurationValues, String contributor, Action rootScriptAction) {
			final Action scriptActionToUse =
					determineJpaScriptActionSetting( configurationValues, contributor, rootScriptAction );
			return scriptActionToUse == null ? Action.NONE : scriptActionToUse;
		}

		private static Action databaseActionToUse(
				Map<?, ?> configuration, String contributor,
				Action rootDatabaseAction, Action scriptActionToUse, Action rootAutoAction) {
			final Action databaseActionToUse =
					determineJpaDbActionSetting( configuration, contributor, rootDatabaseAction );
			if ( databaseActionToUse == null && scriptActionToUse == Action.NONE ) {
				// no JPA (jakarta nor javax) settings were specified, try the legacy setting 'hbm2ddl.auto'
				final Action contributorAutoSetting =
						determineAutoSettingImpliedAction( configuration, contributor, rootAutoAction );
				return contributorAutoSetting == null ? Action.NONE : contributorAutoSetting;
			}
			else {
				return databaseActionToUse == null ? Action.NONE : databaseActionToUse;
			}
		}

		public static Set<ActionGrouping> interpret(Metadata metadata, Map<?, ?> configuration) {
			return interpret( metadata.getContributors(), configuration );
		}

		@Deprecated(since = "7.2", forRemoval = true)
		public String getContributor() {
			return contributor;
		}

		@Deprecated(since = "7.2", forRemoval = true)
		public Action getDatabaseAction() {
			return databaseAction;
		}

		@Deprecated(since = "7.2", forRemoval = true)
		public Action getScriptAction() {
			return scriptAction;
		}
	}
}
