package org.hibernate.tool.schema.internal;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Locale;

import org.hibernate.Internal;
import org.hibernate.internal.log.SubSystemLogging;
import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.hibernate.tool.schema.spi.GenerationTarget;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Logging related to schema creation, migration, validation, and script execution.
 */
@MessageLogger(projectCode = "HHH")
@ValidIdRange(min = 90090001, max = 90091000)
@SubSystemLogging(
		name = SchemaManagementLogging.NAME,
		description = "Logging related to schema management"
)
@Internal
public interface SchemaManagementLogging extends BasicLogger {
	String NAME = SubSystemLogging.BASE + ".schema";

	SchemaManagementLogging SCHEMA_LOGGER = Logger.getMessageLogger(
			MethodHandles.lookup(), SchemaManagementLogging.class, NAME, Locale.ROOT
	);

	@LogMessage(level = INFO)
	@Message(value = "Running hbm2ddl schema export", id = 90090001)
	void runningHbm2ddlSchemaExport();

	@LogMessage(level = INFO)
	@Message(value = "Running hbm2ddl schema update", id = 90090002)
	void runningHbm2ddlSchemaUpdate();

	@LogMessage(level = INFO)
	@Message(value = "Running schema validator", id = 90090003)
	void runningSchemaValidator();

	@LogMessage(level = INFO)
	@Message(value = "Table not found: %s", id = 90090004)
	void tableNotFound(String name);

	@LogMessage(level = INFO)
	@Message(value = "More than one table found: %s", id = 90090005)
	void multipleTablesFound(String name);

	@LogMessage(level = ERROR)
	@Message(value = "Error creating schema ", id = 90090006)
	void unableToCreateSchema(@Cause Exception e);

	@LogMessage(level = ERROR)
	@Message(value = "Error running schema update", id = 90090007)
	void unableToRunSchemaUpdate(@Cause Exception e);

	@LogMessage(level = INFO)
	@Message(value = "Cannot locate column information using identifier [%s]; ignoring index [%s]", id = 90090008 )
	void logCannotLocateIndexColumnInformation(String columnIdentifierText, String indexIdentifierText);

	@LogMessage(level = DEBUG)
	@Message(value = "Executing script [%s]", id = 90090009)
	void executingScript(String scriptName);

	@LogMessage(level = DEBUG)
	@Message(value = "Starting delayed evictData of schema as part of SessionFactory shut-down'", id = 90090010)
	void startingDelayedSchemaDrop();

	@LogMessage(level = ERROR)
	@Message(value = "Unsuccessful: %s", id = 90090011)
	void unsuccessfulSchemaManagementCommand(String command);

	@LogMessage(level = DEBUG)
	@Message( value = "Error performing delayed DROP command [%s]", id = 90090012 )
	void unsuccessfulDelayedDropCommand(CommandAcceptanceException e);

	@LogMessage(level = WARN)
	@Message(value = """
			Multiple configuration properties defined to create schema.\
			Choose at most one among 'jakarta.persistence.create-database-schemas' or 'hibernate.hbm2ddl.create_namespaces'.""",
			id = 90090013)
	void multipleSchemaCreationSettingsDefined();

	@LogMessage(level = DEBUG)
	@Message( id = 90090014, value = "Problem releasing GenerationTarget [%s]" )
	void problemReleasingGenerationTarget(GenerationTarget target, @Cause Exception e);

	@LogMessage(level = TRACE)
	@Message(id = 90090015, value = "Attempting to resolve script source setting: %s")
	void attemptingToResolveScriptSourceSetting(String scriptSourceSettingString);

	@LogMessage(level = DEBUG)
	@Message(id = 90090016, value = "Attempting to create non-existent script target file: %s")
	void attemptingToCreateScriptTarget(String absolutePath);

	@LogMessage(level = DEBUG)
	@Message(id = 90090017, value = "Could not create non-existent script target file")
	void couldNotCreateScriptTarget(@Cause Exception e);

	@LogMessage(level = DEBUG)
	@Message(id = 90090018, value = "Attempting to resolve writer for URL: %s")
	void attemptingToCreateWriter(URL url);

	@LogMessage(level = TRACE)
	@Message(id = 90090019, value = "Interpreting UniqueConstraintSchemaUpdateStrategy from setting: %s")
	void interpretingUniqueConstraintSchemaUpdateStrategy(Object setting);

	@LogMessage(level = DEBUG)
	@Message(id = 90090020, value = "Unable to interpret given setting [%s] as UniqueConstraintSchemaUpdateStrategy")
	void unableToInterpretUniqueConstraintSchemaUpdateStrategy(Object setting);

	@LogMessage(level = DEBUG)
	@Message(id = 90090021, value = "Multiple schemas found with that name [%s.%s]")
	void multipleSchemasFound(String catalogName, String schemaName);

	@LogMessage(level = DEBUG)
	@Message(id = 90090022, value = "Problem releasing DatabaseInformation")
	void problemReleasingDatabaseInformation(@Cause Exception exception);

	@LogMessage(level = WARN)
	@Message(id = 90090023, value = "GenerationTarget encountered exception accepting command : %s")
	void generationTargetEncounteredException(String message, @Cause CommandAcceptanceException exception);

	@LogMessage(level = TRACE)
	@Message(id = 90090024, value = "Trying as URL...")
	void tryingScriptSourceAsUrl();

	@LogMessage(level = DEBUG)
	@Message(id = 90090025, value = "Unable to resolve indicated Dialect resolution info (%s, %s, %s)")
	void unableToResolveSchemaDialect(String databaseName, String majorVersion, String minorVersion);

	@LogMessage(level = WARN)
	@Message(id = 90090026, value = "Specified schema generation script file [%s] did not exist for reading")
	void schemaGenerationScriptFileNotFound(File file);

	@LogMessage(level = WARN)
	@Message(id = 90090027, value = "Unable to close file reader for generation script source")
	void unableToCloseSchemaScriptReader(@Cause IOException exception);

	@LogMessage(level = DEBUG)
	@Message(id = 90090028, value = "No schema management actions found")
	void noSchemaManagementActions();

	@LogMessage(level = DEBUG)
	@Message(id = 90090029, value = "No schema actions specified for contributor '%s'")
	void noSchemaActionsForContributor(String contributor);

	@LogMessage(level = DEBUG)
	@Message(id = 90090030, value = "Skipping SchemaExport as Action.NONE was passed")
	void skippingSchemaExportWithNoAction();

	@LogMessage(level = DEBUG)
	@Message(id = 90090031, value = "Skipping SchemaExport as no targets were specified")
	void skippingSchemaExportWithNoTargets();

	@LogMessage(level = DEBUG)
	@Message(id = 90090032, value = "Skipping SchemaUpdate as no targets were specified")
	void skippingSchemaUpdateWithNoTargets();
}
