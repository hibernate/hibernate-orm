/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.spi;

import java.util.EnumSet;
import java.util.Map;

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

/**
 * Responsible for coordinating SchemaManagementTool execution(s) for auto-tooling whether
 * from JPA or hbm2ddl.auto.
 * <p/>
 * The main entry point is {@link #process}
 *
 * @author Steve Ebersole
 */
public class SchemaManagementToolCoordinator {
	private static final Logger log = Logger.getLogger( SchemaManagementToolCoordinator.class );

	public static void process(
			final Metadata metadata,
			final ServiceRegistry serviceRegistry,
			final Map configurationValues,
			DelayedDropRegistry delayedDropRegistry) {
		final ActionGrouping actions = ActionGrouping.interpret( configurationValues );

		if ( actions.getDatabaseAction() == Action.NONE && actions.getScriptAction() == Action.NONE ) {
			// no actions specified
			log.debug( "No actions specified; doing nothing" );
			return;
		}

		final SchemaManagementTool tool = serviceRegistry.getService( SchemaManagementTool.class );
		final ConfigurationService configService = serviceRegistry.getService( ConfigurationService.class );

		boolean haltOnError = configService.getSetting( AvailableSettings.HBM2DDL_HALT_ON_ERROR, StandardConverters.BOOLEAN, false);

		final ExecutionOptions executionOptions = buildExecutionOptions(
				configurationValues,
				haltOnError ? ExceptionHandlerHaltImpl.INSTANCE :
						ExceptionHandlerLoggedImpl.INSTANCE
		);

		performScriptAction( actions.getScriptAction(), metadata, tool, serviceRegistry, executionOptions );
		performDatabaseAction( actions.getDatabaseAction(), metadata, tool, serviceRegistry, executionOptions );

		if ( actions.getDatabaseAction() == Action.CREATE_DROP ) {
			//noinspection unchecked
			delayedDropRegistry.registerOnCloseAction(
					tool.getSchemaDropper( configurationValues ).buildDelayedAction(
							metadata,
							executionOptions,
							buildDatabaseTargetDescriptor(
									configurationValues,
									DropSettingSelector.INSTANCE,
									serviceRegistry
							)
					)
			);
		}
	}

	public static ExecutionOptions buildExecutionOptions(
			final Map configurationValues,
			final ExceptionHandler exceptionHandler) {
		return new ExecutionOptions() {
			@Override
			public boolean shouldManageNamespaces() {
				return Helper.interpretNamespaceHandling( configurationValues );
			}

			@Override
			public Map getConfigurationValues() {
				return configurationValues;
			}

			@Override
			public ExceptionHandler getExceptionHandler() {
				return exceptionHandler;
			}
		};
	}

	@SuppressWarnings("unchecked")
	private static void performDatabaseAction(
			final Action action,
			Metadata metadata,
			SchemaManagementTool tool,
			ServiceRegistry serviceRegistry,
			final ExecutionOptions executionOptions) {

		// IMPL NOTE : JPA binds source and target info..

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
						migrateDescriptor
				);
				break;
			}
			case VALIDATE: {
				tool.getSchemaValidator( executionOptions.getConfigurationValues() ).doValidation(
						metadata,
						executionOptions
				);
				break;
			}
		}
	}

	private static JpaTargetAndSourceDescriptor buildDatabaseTargetDescriptor(
			Map configurationValues,
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

		final ScriptSourceInput scriptSourceInput = includesScripts ?
				Helper.interpretScriptSourceSetting(
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

	@SuppressWarnings("unchecked")
	private static void performScriptAction(
			Action scriptAction,
			Metadata metadata,
			SchemaManagementTool tool,
			ServiceRegistry serviceRegistry,
			ExecutionOptions executionOptions) {
		switch ( scriptAction ) {
			case CREATE_ONLY: {
				final JpaTargetAndSourceDescriptor createDescriptor = buildScriptTargetDescriptor(
						executionOptions.getConfigurationValues(),
						CreateSettingSelector.INSTANCE,
						serviceRegistry
				);
				tool.getSchemaCreator( executionOptions.getConfigurationValues() ).doCreation(
						metadata,
						executionOptions,
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
						serviceRegistry
				);
				tool.getSchemaDropper( executionOptions.getConfigurationValues() ).doDrop(
						metadata,
						executionOptions,
						dropDescriptor,
						dropDescriptor
				);
				final JpaTargetAndSourceDescriptor createDescriptor = buildScriptTargetDescriptor(
						executionOptions.getConfigurationValues(),
						CreateSettingSelector.INSTANCE,
						serviceRegistry
				);
				tool.getSchemaCreator( executionOptions.getConfigurationValues() ).doCreation(
						metadata,
						executionOptions,
						createDescriptor,
						createDescriptor
				);
				break;
			}
			case DROP: {
				final JpaTargetAndSourceDescriptor dropDescriptor = buildScriptTargetDescriptor(
						executionOptions.getConfigurationValues(),
						DropSettingSelector.INSTANCE,
						serviceRegistry
				);
				tool.getSchemaDropper( executionOptions.getConfigurationValues() ).doDrop(
						metadata,
						executionOptions,
						dropDescriptor,
						dropDescriptor
				);
				break;
			}
			case UPDATE: {
				final JpaTargetAndSourceDescriptor migrateDescriptor = buildScriptTargetDescriptor(
						executionOptions.getConfigurationValues(),
						MigrateSettingSelector.INSTANCE,
						serviceRegistry
				);
				tool.getSchemaMigrator( executionOptions.getConfigurationValues() ).doMigration(
						metadata,
						executionOptions,
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
			Map configurationValues,
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

		String charsetName = (String) configurationValues.get( AvailableSettings.HBM2DDL_CHARSET_NAME );

		final ScriptSourceInput scriptSourceInput = includesScripts
				? Helper.interpretScriptSourceSetting( scriptSourceSetting, serviceRegistry.getService( ClassLoaderService.class ), charsetName )
				: null;

		final ScriptTargetOutput scriptTargetOutput = Helper.interpretScriptTargetSetting(
				settingSelector.getScriptTargetSetting( configurationValues ),
				serviceRegistry.getService( ClassLoaderService.class ),
				charsetName
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
		Object getSourceTypeSetting(Map configurationValues);
		Object getScriptSourceSetting(Map configurationValues);
		Object getScriptTargetSetting(Map configurationValues);
	}

	private static class CreateSettingSelector implements SettingSelector {
		/**
		 * Singleton access
		 */
		public static final CreateSettingSelector INSTANCE = new CreateSettingSelector();

		@Override
		public Object getSourceTypeSetting(Map configurationValues) {
			return configurationValues.get( HBM2DDL_CREATE_SOURCE );
		}

		@Override
		public Object getScriptSourceSetting(Map configurationValues) {
			return configurationValues.get( HBM2DDL_CREATE_SCRIPT_SOURCE );
		}

		@Override
		public Object getScriptTargetSetting(Map configurationValues) {
			return configurationValues.get( HBM2DDL_SCRIPTS_CREATE_TARGET );
		}
	}

	private static class DropSettingSelector implements SettingSelector {
		/**
		 * Singleton access
		 */
		public static final DropSettingSelector INSTANCE = new DropSettingSelector();

		@Override
		public Object getSourceTypeSetting(Map configurationValues) {
			return configurationValues.get( HBM2DDL_DROP_SOURCE );
		}

		@Override
		public Object getScriptSourceSetting(Map configurationValues) {
			return configurationValues.get( HBM2DDL_DROP_SCRIPT_SOURCE );
		}

		@Override
		public Object getScriptTargetSetting(Map configurationValues) {
			return configurationValues.get( HBM2DDL_SCRIPTS_DROP_TARGET );
		}
	}

	private static class MigrateSettingSelector implements SettingSelector {
		/**
		 * Singleton access
		 */
		public static final MigrateSettingSelector INSTANCE = new MigrateSettingSelector();

		// todo : should this define new migrattor-specific settings?
		// for now we reuse the CREATE settings where applicable

		@Override
		public Object getSourceTypeSetting(Map configurationValues) {
			// for now, don't allow script source
			return SourceType.METADATA;
		}

		@Override
		public Object getScriptSourceSetting(Map configurationValues) {
			// for now, don't allow script source
			return null;
		}

		@Override
		public Object getScriptTargetSetting(Map configurationValues) {
			// for now, reuse the CREATE script target setting
			return configurationValues.get( HBM2DDL_SCRIPTS_CREATE_TARGET );
		}
	}

	/**
	 * For JPA-style schema-gen, database and script target handing are configured
	 * individually - this tuple allows interpreting the the action for both targets
	 * simultaneously
	 */
	public static class ActionGrouping {
		private final Action databaseAction;
		private final Action scriptAction;

		public ActionGrouping(Action databaseAction, Action scriptAction) {
			this.databaseAction = databaseAction;
			this.scriptAction = scriptAction;
		}

		public Action getDatabaseAction() {
			return databaseAction;
		}

		public Action getScriptAction() {
			return scriptAction;
		}

		public static ActionGrouping interpret(Map configurationValues) {
			// interpret the JPA settings first
			Action databaseAction = Action.interpretJpaSetting( configurationValues.get( HBM2DDL_DATABASE_ACTION ) );
			Action scriptAction = Action.interpretJpaSetting( configurationValues.get( HBM2DDL_SCRIPTS_ACTION ) );

			// if no JPA settings were specified, look at the legacy HBM2DDL_AUTO setting...
			if ( databaseAction == Action.NONE && scriptAction == Action.NONE ) {
				final Action hbm2ddlAutoAction = Action.interpretHbm2ddlSetting( configurationValues.get( HBM2DDL_AUTO ) );
				if ( hbm2ddlAutoAction != Action.NONE ) {
					databaseAction = hbm2ddlAutoAction;
				}
			}

			return new ActionGrouping( databaseAction, scriptAction );
		}
	}
}
