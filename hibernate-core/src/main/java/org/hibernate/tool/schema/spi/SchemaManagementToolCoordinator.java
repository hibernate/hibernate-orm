/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.spi;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.hibernate.Internal;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.Action;
import org.hibernate.tool.schema.SourceType;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.ExceptionHandlerHaltImpl;
import org.hibernate.tool.schema.internal.ExceptionHandlerLoggedImpl;
import org.hibernate.tool.schema.internal.Helper;

import org.jboss.logging.Logger;

import static org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO;
import static org.hibernate.cfg.AvailableSettings.HBM2DDL_CREATE_SCRIPT_SOURCE;
import static org.hibernate.cfg.AvailableSettings.HBM2DDL_CREATE_SOURCE;
import static org.hibernate.cfg.AvailableSettings.HBM2DDL_DATABASE_ACTION;
import static org.hibernate.cfg.AvailableSettings.HBM2DDL_DROP_SCRIPT_SOURCE;
import static org.hibernate.cfg.AvailableSettings.HBM2DDL_DROP_SOURCE;
import static org.hibernate.cfg.AvailableSettings.HBM2DDL_SCRIPTS_ACTION;
import static org.hibernate.cfg.AvailableSettings.HBM2DDL_SCRIPTS_CREATE_TARGET;
import static org.hibernate.cfg.AvailableSettings.HBM2DDL_SCRIPTS_DROP_TARGET;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_HBM2DDL_CREATE_SCRIPT_SOURCE;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_HBM2DDL_CREATE_SOURCE;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_HBM2DDL_DROP_SCRIPT_SOURCE;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_HBM2DDL_DROP_SOURCE;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_DROP_TARGET;
import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;

/**
 * Responsible for coordinating {@link SchemaManagementTool} execution
 * whether from {@value AvailableSettings#HBM2DDL_AUTO}, JPA-standard
 * {@value AvailableSettings#JAKARTA_HBM2DDL_DATABASE_ACTION}, or
 * {@link org.hibernate.relational.SchemaManager}.
 * <p>
 * The main entry point is {@link #process}.
 *
 * @author Steve Ebersole
 */
public class SchemaManagementToolCoordinator {
	private static final Logger log = Logger.getLogger( SchemaManagementToolCoordinator.class );

	public static void process(
			final Metadata metadata,
			final ServiceRegistry serviceRegistry,
			final Map<String,Object> configurationValues,
			DelayedDropRegistry delayedDropRegistry) {
		final Set<ActionGrouping> groupings = ActionGrouping.interpret( metadata, configurationValues );

		if ( groupings.isEmpty() ) {
			// no actions specified
			log.debug( "No actions found; doing nothing" );
			return;
		}

		Map<Action,Set<String>> databaseActionMap = null;
		Map<Action,Set<String>> scriptActionMap = null;

		for ( ActionGrouping grouping : groupings ) {
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

			// for script action
			if ( grouping.scriptAction != Action.NONE ) {
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


		final SchemaManagementTool tool = serviceRegistry.getService( SchemaManagementTool.class );
		final ConfigurationService configService = serviceRegistry.requireService( ConfigurationService.class );

		final boolean haltOnError = configService.getSetting(
				AvailableSettings.HBM2DDL_HALT_ON_ERROR,
				StandardConverters.BOOLEAN,
				false
		);

		final ExecutionOptions executionOptions =
				buildExecutionOptions( configurationValues,
						haltOnError
								? ExceptionHandlerHaltImpl.INSTANCE
								: ExceptionHandlerLoggedImpl.INSTANCE );

		if ( scriptActionMap != null ) {
			scriptActionMap.forEach(
					(action, contributors) -> {
						performScriptAction( action, metadata, tool, serviceRegistry, executionOptions, configService );
					}
			);
		}

		if ( databaseActionMap != null ) {
			databaseActionMap.forEach(
					(action, contributors) -> {

						performDatabaseAction(
								action,
								metadata,
								tool,
								serviceRegistry,
								executionOptions,
								(exportable) -> contributors.contains( exportable.getContributor() )
						);

						if ( action == Action.CREATE_DROP ) {
							delayedDropRegistry.registerOnCloseAction(
									tool.getSchemaDropper( configurationValues ).buildDelayedAction(
											metadata,
											executionOptions,
											(exportable) -> contributors.contains( exportable.getContributor() ),
											buildDatabaseTargetDescriptor(
													configurationValues,
													DropSettingSelector.INSTANCE,
													serviceRegistry
											)
									)
							);
						}
					}
			);
		}
	}

	public static ExecutionOptions buildExecutionOptions(
			final Map<String,Object> configurationValues,
			final ExceptionHandler exceptionHandler) {
		return new ExecutionOptions() {
			@Override
			public boolean shouldManageNamespaces() {
				return Helper.interpretNamespaceHandling( configurationValues );
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

		// IMPL NOTE : JPA binds source and target info

		switch ( action ) {
			case CREATE_ONLY: {
				//
				final JpaTargetAndSourceDescriptor createDescriptor = buildDatabaseTargetDescriptor(
						executionOptions.getConfigurationValues(),
						CreateSettingSelector.INSTANCE,
						serviceRegistry
				);
				tool.getSchemaCreator( executionOptions.getConfigurationValues() ).doCreation(
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
				final JpaTargetAndSourceDescriptor dropDescriptor = buildDatabaseTargetDescriptor(
						executionOptions.getConfigurationValues(),
						DropSettingSelector.INSTANCE,
						serviceRegistry
				);
				tool.getSchemaDropper( executionOptions.getConfigurationValues() ).doDrop(
						metadata,
						executionOptions,
						contributableInclusionFilter,
						dropDescriptor,
						dropDescriptor
				);
				final JpaTargetAndSourceDescriptor createDescriptor = buildDatabaseTargetDescriptor(
						executionOptions.getConfigurationValues(),
						CreateSettingSelector.INSTANCE,
						serviceRegistry
				);
				tool.getSchemaCreator( executionOptions.getConfigurationValues() ).doCreation(
						metadata,
						executionOptions,
						contributableInclusionFilter,
						createDescriptor,
						createDescriptor
				);
				break;
			}
			case DROP: {
				final JpaTargetAndSourceDescriptor dropDescriptor = buildDatabaseTargetDescriptor(
						executionOptions.getConfigurationValues(),
						DropSettingSelector.INSTANCE,
						serviceRegistry
				);
				tool.getSchemaDropper( executionOptions.getConfigurationValues() ).doDrop(
						metadata,
						executionOptions,
						contributableInclusionFilter,
						dropDescriptor,
						dropDescriptor
				);
				break;
			}
			case UPDATE: {
				final JpaTargetAndSourceDescriptor migrateDescriptor = buildDatabaseTargetDescriptor(
						executionOptions.getConfigurationValues(),
						MigrateSettingSelector.INSTANCE,
						serviceRegistry
				);
				tool.getSchemaMigrator( executionOptions.getConfigurationValues() ).doMigration(
						metadata,
						executionOptions,
						contributableInclusionFilter,
						migrateDescriptor
				);
				break;
			}
			case VALIDATE: {
				tool.getSchemaValidator( executionOptions.getConfigurationValues() ).doValidation(
						metadata,
						executionOptions,
						contributableInclusionFilter
				);
				break;
			}
			case TRUNCATE: {
				tool.getSchemaTruncator( executionOptions.getConfigurationValues() ).doTruncate(
						metadata,
						executionOptions,
						contributableInclusionFilter,
						buildDatabaseTargetDescriptor(
								executionOptions.getConfigurationValues(),
								CreateSettingSelector.INSTANCE,
								serviceRegistry
						)
				);
				break;
			}
			case POPULATE: {
				tool.getSchemaPopulator( executionOptions.getConfigurationValues() ).doPopulation(
						executionOptions,
						buildDatabaseTargetDescriptor(
								executionOptions.getConfigurationValues(),
								CreateSettingSelector.INSTANCE,
								serviceRegistry
						)
				);
				break;
			}
		}
	}

	private static JpaTargetAndSourceDescriptor buildDatabaseTargetDescriptor(
			Map<?,?> configurationValues,
			SettingSelector settingSelector,
			ServiceRegistry serviceRegistry) {
		final Object scriptSourceSetting = settingSelector.getScriptSourceSetting( configurationValues );
		final SourceType sourceType = SourceType.interpret(
				settingSelector.getSourceTypeSetting( configurationValues ),
				scriptSourceSetting != null ? SourceType.SCRIPT : SourceType.METADATA
		);

		final boolean includesScripts = sourceType != SourceType.METADATA;
		if ( includesScripts && scriptSourceSetting == null ) {
			throw new SchemaManagementException(
					"Schema generation configuration indicated to include CREATE scripts, but no script was specified"
			);
		}

		final ScriptSourceInput scriptSourceInput = includesScripts
				? Helper.interpretScriptSourceSetting(
						scriptSourceSetting,
						serviceRegistry.getService( ClassLoaderService.class ),
						(String) configurationValues.get( AvailableSettings.HBM2DDL_CHARSET_NAME )
				)
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
		switch ( scriptAction ) {
			case CREATE_ONLY: {
				final JpaTargetAndSourceDescriptor createDescriptor = buildScriptTargetDescriptor(
						executionOptions.getConfigurationValues(),
						CreateSettingSelector.INSTANCE,
						serviceRegistry,
						configurationService
				);
				tool.getSchemaCreator( executionOptions.getConfigurationValues() ).doCreation(
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
				final JpaTargetAndSourceDescriptor dropDescriptor = buildScriptTargetDescriptor(
						executionOptions.getConfigurationValues(),
						DropSettingSelector.INSTANCE,
						serviceRegistry,
						configurationService
				);
				tool.getSchemaDropper( executionOptions.getConfigurationValues() ).doDrop(
						metadata,
						executionOptions,
						(contributed) -> true,
						dropDescriptor,
						dropDescriptor
				);
				final JpaTargetAndSourceDescriptor createDescriptor = buildScriptTargetDescriptor(
						executionOptions.getConfigurationValues(),
						CreateSettingSelector.INSTANCE,
						serviceRegistry,
						configurationService
				);
				tool.getSchemaCreator( executionOptions.getConfigurationValues() ).doCreation(
						metadata,
						executionOptions,
						(contributed) -> true,
						createDescriptor,
						createDescriptor
				);
				break;
			}
			case DROP: {
				final JpaTargetAndSourceDescriptor dropDescriptor = buildScriptTargetDescriptor(
						executionOptions.getConfigurationValues(),
						DropSettingSelector.INSTANCE,
						serviceRegistry,
						configurationService
				);
				tool.getSchemaDropper( executionOptions.getConfigurationValues() ).doDrop(
						metadata,
						executionOptions,
						(contributed) -> true,
						dropDescriptor,
						dropDescriptor
				);
				break;
			}
			case UPDATE: {
				final JpaTargetAndSourceDescriptor migrateDescriptor = buildScriptTargetDescriptor(
						executionOptions.getConfigurationValues(),
						MigrateSettingSelector.INSTANCE,
						serviceRegistry,
						configurationService
				);
				tool.getSchemaMigrator( executionOptions.getConfigurationValues() ).doMigration(
						metadata,
						executionOptions,
						(contributed) -> true,
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
			Map<?,?> configurationValues,
			SettingSelector settingSelector,
			ServiceRegistry serviceRegistry,
			ConfigurationService configurationService) {
		final Object scriptSourceSetting = settingSelector.getScriptSourceSetting( configurationValues );
		final SourceType sourceType = SourceType.interpret(
				settingSelector.getSourceTypeSetting( configurationValues ),
				scriptSourceSetting != null ? SourceType.SCRIPT : SourceType.METADATA
		);

		final boolean includesScripts = sourceType != SourceType.METADATA;
		if ( includesScripts && scriptSourceSetting == null ) {
			throw new SchemaManagementException(
					"Schema generation configuration indicated to include CREATE scripts, but no script was specified"
			);
		}

		String charsetName = (String) configurationValues.get( AvailableSettings.HBM2DDL_CHARSET_NAME );

		final ScriptSourceInput scriptSourceInput = includesScripts
				? Helper.interpretScriptSourceSetting( scriptSourceSetting, serviceRegistry.getService( ClassLoaderService.class ), charsetName )
				: null;


		boolean append = configurationService.getSetting( AvailableSettings.HBM2DDL_SCRIPTS_CREATE_APPEND, StandardConverters.BOOLEAN, true );
		final ScriptTargetOutput scriptTargetOutput = Helper.interpretScriptTargetSetting(
				settingSelector.getScriptTargetSetting( configurationValues ),
				serviceRegistry.getService( ClassLoaderService.class ),
				charsetName,
				append
		);

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

	private static Object getConfigurationValue(final Map<?,?> configurationValues, final String referenceKey, final String legacyKey) {
		Object setting = configurationValues.get( referenceKey );
		if ( setting == null ) {
			setting = configurationValues.get( legacyKey );
			if ( setting != null ) {
				DEPRECATION_LOGGER.deprecatedSetting( referenceKey, legacyKey );
			}
		}
		return setting;
	}

	private static class CreateSettingSelector implements SettingSelector {
		/**
		 * Singleton access
		 */
		public static final CreateSettingSelector INSTANCE = new CreateSettingSelector();

		@Override
		public Object getSourceTypeSetting(Map<?,?> configurationValues) {
			return getConfigurationValue( configurationValues, JAKARTA_HBM2DDL_CREATE_SOURCE, HBM2DDL_CREATE_SOURCE );
		}

		@Override
		public Object getScriptSourceSetting(Map<?,?> configurationValues) {
			return getConfigurationValue( configurationValues, JAKARTA_HBM2DDL_CREATE_SCRIPT_SOURCE, HBM2DDL_CREATE_SCRIPT_SOURCE );
		}

		@Override
		public Object getScriptTargetSetting(Map<?,?> configurationValues) {
			return getConfigurationValue( configurationValues, JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET, HBM2DDL_SCRIPTS_CREATE_TARGET );
		}
	}

	private static class DropSettingSelector implements SettingSelector {
		/**
		 * Singleton access
		 */
		public static final DropSettingSelector INSTANCE = new DropSettingSelector();

		@Override
		public Object getSourceTypeSetting(Map<?,?> configurationValues) {
			return getConfigurationValue( configurationValues, JAKARTA_HBM2DDL_DROP_SOURCE, HBM2DDL_DROP_SOURCE );
		}

		@Override
		public Object getScriptSourceSetting(Map<?,?> configurationValues) {
			return getConfigurationValue( configurationValues, JAKARTA_HBM2DDL_DROP_SCRIPT_SOURCE, HBM2DDL_DROP_SCRIPT_SOURCE );
		}

		@Override
		public Object getScriptTargetSetting(Map<?,?> configurationValues) {
			return getConfigurationValue( configurationValues, JAKARTA_HBM2DDL_SCRIPTS_DROP_TARGET, HBM2DDL_SCRIPTS_DROP_TARGET );
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
			return getConfigurationValue( configurationValues, JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET, HBM2DDL_SCRIPTS_CREATE_TARGET );
		}
	}

	/**
	 * For JPA-style schema-gen, database and script target handing are configured
	 * individually - this tuple allows interpreting the action for both targets
	 * simultaneously
	 */
	public static class ActionGrouping {
		private final String contributor;
		private final Action databaseAction;
		private final Action scriptAction;

		public ActionGrouping(String contributor, Action databaseAction, Action scriptAction) {
			this.contributor = contributor;
			this.databaseAction = databaseAction;
			this.scriptAction = scriptAction;
		}

		public String getContributor() {
			return contributor;
		}

		public Action getDatabaseAction() {
			return databaseAction;
		}

		public Action getScriptAction() {
			return scriptAction;
		}

		/**
		 * For test use.  See {@link #interpret(Metadata, Map)} for the "real" impl
		 */
		@Internal
		public static ActionGrouping interpret(Map<?,?> configurationValues) {
			// default to the JPA settings
			Action databaseAction = determineJpaDbActionSetting( configurationValues );
			Action scriptAction = determineJpaScriptActionSetting( configurationValues );

			// if no JPA settings were specified, look at the legacy HBM2DDL_AUTO setting...
			if ( databaseAction == null && scriptAction == null ) {
				final Action autoAction = determineAutoSettingImpliedAction( configurationValues, null, null );
				if ( autoAction != null ) {
					databaseAction = autoAction;
				}
			}

			if ( databaseAction == null ) {
				databaseAction = Action.NONE;
			}
			if ( scriptAction == null ) {
				scriptAction = Action.NONE;
			}

			return new ActionGrouping( "orm", databaseAction, scriptAction );
		}

		private static Action determineJpaDbActionSetting(Map<?,?> configurationValues) {
			return determineJpaDbActionSetting( configurationValues, null, null );
		}

		/**
		 * Exposed for tests
		 */
		@Internal
		public static Action determineJpaDbActionSetting(
				Map<?,?> configurationValues,
				String contributor,
				Action defaultValue) {
			final String jakartaSettingName = contributor == null
					? JAKARTA_HBM2DDL_DATABASE_ACTION
					: JAKARTA_HBM2DDL_DATABASE_ACTION + "." + contributor;
			final String javaxSettingName = contributor == null
					? HBM2DDL_DATABASE_ACTION
					: HBM2DDL_DATABASE_ACTION + "." + contributor;

			Object databaseActionSetting = configurationValues.get( jakartaSettingName );
			if ( databaseActionSetting == null ) {
				databaseActionSetting = configurationValues.get( javaxSettingName );
				if ( databaseActionSetting != null ) {
					DEPRECATION_LOGGER.deprecatedSetting( HBM2DDL_DATABASE_ACTION, JAKARTA_HBM2DDL_DATABASE_ACTION );
				}
			}

			return databaseActionSetting == null ? defaultValue : Action.interpretJpaSetting( databaseActionSetting );
		}

		private static Action determineJpaScriptActionSetting(Map<?,?> configurationValues) {
			return determineJpaScriptActionSetting( configurationValues, null, null );
		}

		/**
		 * Exposed for tests
		 */
		@Internal
		public static Action determineJpaScriptActionSetting(
				Map<?,?> configurationValues,
				String contributor,
				Action defaultValue) {
			final String jakartaSettingName = contributor == null
					? JAKARTA_HBM2DDL_SCRIPTS_ACTION
					: JAKARTA_HBM2DDL_SCRIPTS_ACTION + "." + contributor;
			final String javaxSettingName = contributor == null
					? HBM2DDL_SCRIPTS_ACTION
					: HBM2DDL_SCRIPTS_ACTION + "." + contributor;

			Object scriptsActionSetting = configurationValues.get( jakartaSettingName );
			if ( scriptsActionSetting == null ) {
				scriptsActionSetting = configurationValues.get( javaxSettingName );
				if ( scriptsActionSetting != null ) {
					DEPRECATION_LOGGER.deprecatedSetting( HBM2DDL_SCRIPTS_ACTION, JAKARTA_HBM2DDL_SCRIPTS_ACTION );
				}
			}

			return scriptsActionSetting == null ? defaultValue : Action.interpretJpaSetting( scriptsActionSetting );
		}

		public static Action determineAutoSettingImpliedAction(Map<?,?> settings, String contributor, Action defaultValue) {
			final String settingName = contributor == null
					? HBM2DDL_AUTO
					: HBM2DDL_AUTO + "." + contributor;

			final Object scriptsActionSetting = settings.get( settingName );
			if ( scriptsActionSetting == null ) {
				return defaultValue;
			}

			return Action.interpretHbm2ddlSetting( scriptsActionSetting );
		}

		public static Set<ActionGrouping> interpret(Set<String> contributors, Map<?,?> configurationValues) {
			// these represent the base (non-contributor-specific) values
			final Action rootDatabaseAction = determineJpaDbActionSetting( configurationValues, null, null );
			final Action rootScriptAction = determineJpaScriptActionSetting( configurationValues, null, null );

			final Action rootAutoAction = determineAutoSettingImpliedAction( configurationValues, null, null );

			final Set<ActionGrouping> groupings = new HashSet<>( contributors.size() );

			// for each contributor, look for specific tooling config values
			for ( String contributor : contributors ) {
				Action databaseActionToUse = determineJpaDbActionSetting( configurationValues, contributor, rootDatabaseAction );
				Action scriptActionToUse = determineJpaScriptActionSetting( configurationValues, contributor, rootScriptAction );

				if ( databaseActionToUse == null && scriptActionToUse == null ) {
					// no JPA (jakarta nor javax) settings were specified, use the legacy Hibernate
					// `hbm2ddl.auto` setting to possibly set the database-action
					final Action contributorAutoSetting = determineAutoSettingImpliedAction( configurationValues, contributor, rootAutoAction );
					if ( contributorAutoSetting != null ) {
						databaseActionToUse = contributorAutoSetting;
					}
				}

				if ( databaseActionToUse == null ) {
					databaseActionToUse = Action.NONE;
				}
				if ( scriptActionToUse == null ) {
					scriptActionToUse = Action.NONE;
				}

				if ( databaseActionToUse == Action.NONE &&  scriptActionToUse == Action.NONE ) {
					log.debugf( "No schema actions specified for contributor `%s`; doing nothing", contributor );
					continue;
				}

				groupings.add( new ActionGrouping( contributor, databaseActionToUse, scriptActionToUse ) );
			}

			return groupings;
		}

		public static Set<ActionGrouping> interpret(Metadata metadata, Map<?,?> configurationValues) {
			return interpret( metadata.getContributors(), configurationValues );
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			ActionGrouping that = (ActionGrouping) o;
			return contributor.equals( that.contributor ) &&
					databaseAction == that.databaseAction &&
					scriptAction == that.scriptAction;
		}

		@Override
		public int hashCode() {
			return Objects.hash( contributor );
		}
	}
}
