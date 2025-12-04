/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.cache.CacheException;
import org.hibernate.engine.spi.CascadingAction;
import org.hibernate.engine.spi.CollectionEntry;
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

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Miscellaneous logging related to Hibernate ORM Core.
 */
@SubSystemLogging(
		name = SessionLogging.NAME,
		description = "Miscellaneous logging related to Hibernate ORM Core"
)
@MessageLogger(projectCode = "HHH")
@ValidIdRange(min = 1, max = 8000)
@Internal
public interface CoreMessageLogger extends BasicLogger {

	String NAME = SubSystemLogging.BASE + ".core";

	CoreMessageLogger CORE_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), CoreMessageLogger.class, NAME );

	@LogMessage(level = INFO)
	@Message(value = "Hibernate ORM core version %s", id = 1)
	void version(String versionString);

	@LogMessage(level = WARN)
	@Message(value = "Composite id class does not override equals(): %s", id = 38)
	void compositeIdClassDoesNotOverrideEquals(String name);

	@LogMessage(level = WARN)
	@Message(value = "Composite id class does not override hashCode(): %s", id = 39)
	void compositeIdClassDoesNotOverrideHashCode(String name);

	@LogMessage(level = WARN)
	@Message(value = "Ignoring bag join fetch [%s] due to prior collection join fetch", id = 51)
	void containsJoinFetchedCollection(String role);

	@LogMessage(level = WARN)
	@Message(value = "Entity [%s] is abstract-class/interface explicitly mapped as non-abstract; be sure to supply entity-names",
			id = 84)
	void entityMappedAsNonAbstract(String name);

	@LogMessage(level = INFO)
	@Message(value = "Sub-resolver threw unexpected exception, continuing to next: %s", id = 89)
	void exceptionInSubResolver(String message);

	@LogMessage(level = ERROR)
	@Message(value = "Expected type: %s, actual value: %s", id = 91)
	void expectedType(String name,
			@Nullable String string);

	@LogMessage(level = ERROR)
	@Message(value = "an assertion failure occurred" + " (this may indicate a bug in Hibernate, but is more likely due"
			+ " to unsafe use of the session): %s", id = 99)
	void failed(Throwable throwable);

	@LogMessage(level = ERROR)
	@Message(value = "IllegalArgumentException in class: %s, getter method of property: %s", id = 122)
	void illegalPropertyGetterArgument(String name, String propertyName);

	@LogMessage(level = ERROR)
	@Message(value = "IllegalArgumentException in class: %s, setter method of property: %s", id = 123)
	void illegalPropertySetterArgument(String name, String propertyName);

	@LogMessage(level = INFO)
	@Message(value = "java.sql.Types mapped the same code [%s] multiple times; was [%s]; now [%s]", id = 141)
	void JavaSqlTypesMappedSameCodeMultipleTimes(int code, String old, String name);

	@LogMessage(level = DEBUG)
	@Message(value = "Lazy property fetching available for: %s", id = 157)
	void lazyPropertyFetchingAvailable(String name);

	@LogMessage(level = WARN)
	@Message(value = "Function template anticipated %s arguments, but %s arguments encountered", id = 174)
	void missingArguments(
			int anticipatedNumberOfArguments,
			int numberOfArguments);

	@LogMessage(level = WARN)
	@Message(value = "Narrowing proxy to %s - this operation breaks ==", id = 179)
	void narrowingProxy(Class<?> concreteProxyClass);

	@LogMessage(level = INFO)
	@Message(value = "No default (no-argument) constructor for class [%s] (class must be instantiated by Interceptor)",
			id = 182)
	void noDefaultConstructor(String name);

	@LogMessage(level = INFO)
	@Message(value = "Loaded properties from resource hibernate.properties: %s", id = 205)
	void propertiesLoaded(Properties maskOut);

	@LogMessage(level = DEBUG)
	@Message(value = "'hibernate.properties' not found", id = 206)
	void propertiesNotFound();

	@LogMessage(level = WARN)
	@Message(
			value = """
					Recognized obsolete hibernate namespace %s.\
					Use namespace %s instead. Refer to Hibernate 3.6 Migration Guide""",
			id = 223)
	void recognizedObsoleteHibernateNamespace(String oldHibernateNamespace, String hibernateNamespace);

	@LogMessage(level = INFO)
	@Message(value = "Running hbm2ddl schema export", id = 227)
	void runningHbm2ddlSchemaExport();

	@LogMessage(level = INFO)
	@Message(value = "Running hbm2ddl schema update", id = 228)
	void runningHbm2ddlSchemaUpdate();

	@LogMessage(level = INFO)
	@Message(value = "Running schema validator", id = 229)
	void runningSchemaValidator();

//	@LogMessage(level = WARN)
//	@Message(value = "Scoping types to session factory %s after already scoped %s", id = 233)
//	void scopingTypesToSessionFactoryAfterAlreadyScoped(
//			SessionFactoryImplementor factory,
//			SessionFactoryImplementor factory2);

//	@LogMessage(level = WARN)
//	@Message(value = "SQL Error: %s, SQLState: %s", id = 247)
//	void sqlWarning(int errorCode, String sqlState);

	@LogMessage(level = INFO)
	@Message(value = "Start time: %s", id = 251)
	void startTime(long startTime);

	@LogMessage(level = INFO)
	@Message(value = "Table not found: %s", id = 262)
	void tableNotFound(String name);

	@LogMessage(level = INFO)
	@Message(value = "More than one table found: %s", id = 263)
	void multipleTablesFound(String name);

	@LogMessage(level = INFO)
	@Message(value = "Transactions: %s", id = 266)
	void transactions(long transactionCount);

	@LogMessage(level = WARN)
	@Message(value = "Type [%s] defined no registration keys; ignoring", id = 269)
	void typeDefinedNoRegistrationKeys(Object type);

	@LogMessage(level = WARN)
	@Message(value = "Error accessing type info result set: %s", id = 273)
	void unableToAccessTypeInfoResultSet(String string);

	@LogMessage(level = WARN)
	@Message(value = "Unable to cleanup temporary id table after use [%s]", id = 283)
	void unableToCleanupTemporaryIdTable(Throwable t);

	@LogMessage(level = INFO)
	@Message(value = "Error closing InitialContext [%s]", id = 285)
	void unableToCloseInitialContext(String string);

	@LogMessage(level = WARN)
	@Message(value = "Could not close input stream", id = 287)
	void unableToCloseInputStream(@Cause IOException e);

	@LogMessage(level = WARN)
	@Message(value = "IOException occurred closing output stream", id = 292)
	void unableToCloseOutputStream(@Cause IOException e);

	@LogMessage(level = WARN)
	@Message(value = "IOException occurred closing stream", id = 296)
	void unableToCloseStream(@Cause IOException e);

	@LogMessage(level = ERROR)
	@Message(value = "Could not close stream on hibernate.properties: %s", id = 297)
	void unableToCloseStreamError(IOException error);

	@LogMessage(level = WARN)
	@Message(value = "Could not copy system properties, system properties will be ignored", id = 304)
	void unableToCopySystemProperties();

	@LogMessage(level = WARN)
	@Message(value = "Could not create proxy factory for:%s", id = 305)
	void unableToCreateProxyFactory(String entityName, @Cause HibernateException e);

	@LogMessage(level = ERROR)
	@Message(value = "Error creating schema ", id = 306)
	void unableToCreateSchema(@Cause Exception e);

	@LogMessage(level = ERROR)
	@Message(value = "Problem loading properties from hibernate.properties", id = 329)
	void unableToLoadProperties();

	@LogMessage(level = WARN)
	@Message(value = "Unable to log SQLWarnings: %s", id = 335)
	void unableToLogSqlWarnings(SQLException sqle);

	@LogMessage(level = ERROR)
	@Message(value = "Unable to mark for rollback on PersistenceException: ", id = 337)
	void unableToMarkForRollbackOnPersistenceException(@Cause Exception e);

	@LogMessage(level = ERROR)
	@Message(value = "Unable to mark for rollback on TransientObjectException: ", id = 338)
	void unableToMarkForRollbackOnTransientObjectException(@Cause Exception e);

	@LogMessage(level = ERROR)
	@Message(value = "Could not release a cache lock: %s", id = 353)
	void unableToReleaseCacheLock(CacheException ce);

	@LogMessage(level = WARN)
	@Message(value = "Unable to release type info result set", id = 357)
	void unableToReleaseTypeInfoResultSet();

	@LogMessage(level = WARN)
	@Message(value = "Unable to erase previously added bag join fetch", id = 358)
	void unableToRemoveBagJoinFetch();

	@LogMessage(level = WARN)
	@Message(value = "Unable to retrieve type info result set: %s", id = 362)
	void unableToRetrieveTypeInfoResultSet(String string);

	@LogMessage(level = ERROR)
	@Message(value = "Error running schema update", id = 366)
	void unableToRunSchemaUpdate(@Cause Exception e);

	@LogMessage(level = WARN)
	@Message(
			value = """
					The %s.%s.%s version of H2 implements temporary table creation such that it commits current transaction;\
					multi-table, bulk HQL/JPQL will not work properly""",
			id = 393)
	void unsupportedMultiTableBulkHqlJpaql(int majorVersion, int minorVersion, int buildId);

	@LogMessage(level = ERROR)
	@Message(value = "Don't use old DTDs, read the Hibernate 3.x Migration Guide", id = 404)
	void usingOldDtd();

	@LogMessage(level = WARN)
	@Message(value = "Warnings creating temp table: %s", id = 413)
	void warningsCreatingTempTable(SQLWarning warning);

	@LogMessage(level = WARN)
	@Message(
			value = """
					Dialect [%s] limits the number of elements in an IN predicate to %s entries.  \
					However, the given parameter list [%s] contained %s entries, which will likely cause failures \
					to execute the query in the database""",
			id = 443
	)
	void tooManyInExpressions(String dialectName, int limit, String paramName, int size);

	@LogMessage(level = WARN)
	@Message(
			value = """
					Encountered request for locking however dialect reports that database prefers locking be done in a \
					separate select (follow-on locking); results will be locked after initial query executes""",
			id = 444
	)
	void usingFollowOnLocking();

	@LogMessage(level = INFO)
	@Message(value = "Cannot locate column information using identifier [%s]; ignoring index [%s]", id = 475 )
	void logCannotLocateIndexColumnInformation(String columnIdentifierText, String indexIdentifierText);

	@LogMessage(level = DEBUG)
	@Message(value = "Executing script [%s]", id = 476)
	void executingScript(String scriptName);

	@LogMessage(level = DEBUG)
	@Message(value = "Starting delayed evictData of schema as part of SessionFactory shut-down'", id = 477)
	void startingDelayedSchemaDrop();

	@LogMessage(level = ERROR)
	@Message(value = "Unsuccessful: %s", id = 478)
	void unsuccessfulSchemaManagementCommand(String command);

	@LogMessage(level = DEBUG)
	@Message( value = "Error performing delayed DROP command [%s]", id = 479 )
	void unsuccessfulDelayedDropCommand(CommandAcceptanceException e);

	@LogMessage(level = WARN)
	@Message(
			value = """
					A ManagedEntity was associated with a stale PersistenceContext.\
					A ManagedEntity may only be associated with one PersistenceContext at a time; %s""",
			id = 480)
	void stalePersistenceContextInEntityEntry(String msg);

	@LogMessage(level = ERROR)
	@Message(value = "Illegally attempted to associate a proxy for entity [%s] with id [%s] with two open sessions.", id = 485)
	void attemptToAssociateProxyWithTwoOpenSessions(String entityName, Object id);

	@LogMessage(level = WARN)
	@Message(value = "The query [%s] updates an immutable entity: %s",
			id = 487)
	void immutableEntityUpdateQuery(@Nullable String sourceQuery, String querySpaces);

	@LogMessage(level = DEBUG)
	@Message(value = "The query [%s] updates an immutable entity: %s",
			id = 488)
	void immutableEntityUpdateQueryAllowed(@Nullable String sourceQuery, String querySpaces);

	@LogMessage(level = INFO)
	@Message(value = "No JTA platform available (set 'hibernate.transaction.jta.platform' to enable JTA platform integration)", id = 489)
	void noJtaPlatform();

	@LogMessage(level = INFO)
	@Message(value = "Using JTA platform [%s]", id = 490)
	void usingJtaPlatform(String jtaPlatformClassName);

	@LogMessage(level = WARN)
	@Message(value = "Attempt to merge an uninitialized collection with queued operations; queued operations will be ignored: %s", id = 494)
	void ignoreQueuedOperationsOnMerge(String collectionInfoString);

	@LogMessage(level = WARN)
	@Message(value = "The [%s] property of the [%s] entity was modified, but it won't be updated because the property is immutable.", id = 502)
	void ignoreImmutablePropertyModification(String propertyName, String entityName);

	@LogMessage(level = WARN)
	@Message(value = """
			Multiple configuration properties defined to create schema.\
			Choose at most one among 'jakarta.persistence.create-database-schemas' or 'hibernate.hbm2ddl.create_namespaces'.""",
			id = 504)
	void multipleSchemaCreationSettingsDefined();

	@LogMessage(level = WARN)
	@Message(value = "Multi-table insert is not available due to missing identity and window function support for: %s", id = 509)
	void multiTableInsertNotAvailable(String entityName);

	@LogMessage(level = WARN)
	@Message(value = "Association with '@Fetch(JOIN)' and 'fetch=FetchType.LAZY' found. This is interpreted as lazy: %s", id = 510)
	void fetchModeJoinWithLazyWarning(String role);

	@LogMessage(level = WARN)
	@Message(
			value = """
					The %2$s version for [%s] is no longer supported, hence certain features may not work properly.\
					The minimum supported version is %3$s. Check the community dialects project for available legacy versions.""",
			id = 511)
	void unsupportedDatabaseVersion(String databaseName, String actualVersion, String minimumVersion);

	@LogMessage(level = DEBUG)
	@Message(value = "Unable to create the ReflectionOptimizer for [%s]: %s",
			id = 513)
	void unableToGenerateReflectionOptimizer(String className, String cause);

	@LogMessage(level = DEBUG)
	@Message(
			id = 517,
			value = "Encountered a MappedSuperclass [%s] not used in any entity hierarchy"
	)
	void unusedMappedSuperclass(String name);

	@LogMessage(level = WARN)
	@Message(
			id = 519,
			value = "Invalid JSON column type [%s], was expecting [%s]; for efficiency schema should be migrate to JSON DDL type"
	)
	void invalidJSONColumnType(String actual, String expected);

	@LogMessage(level = ERROR)
	@Message(
			id = 5001,
			value = "Illegal argument on static metamodel field injection: %s#%s; expected type: %s; encountered type: %s"
	)
	void illegalArgumentOnStaticMetamodelFieldInjection(
			String name,
			String name2,
			String name3,
			String name4);

	@LogMessage(level = WARN)
	@Message(
			id = 5002,
			value = "Unable to locate static metamodel field: %s#%s; this may or may not indicate a problem with the static metamodel"
	)
	void unableToLocateStaticMetamodelField(
			String name,
			String name2);

	@LogMessage(level = DEBUG)
	@Message( id = 6001, value = "Error creating temp table" )
	void errorCreatingTempTable(@Cause Exception e);

	@LogMessage(level = DEBUG)
	@Message( id = 6002, value = "Unable to create temporary table [%s]: '%s' failed" )
	void unableToCreateTempTable(String qualifiedTableName, String creationCommand, @Cause SQLException e);

	@LogMessage(level = DEBUG)
	@Message( id = 6003, value = "Error dropping temp table" )
	void errorDroppingTempTable(@Cause Exception e);

	@LogMessage(level = DEBUG)
	@Message( id = 6004, value = "Unable to drop temporary table [%s]: '%s' failed" )
	void unableToDropTempTable(String qualifiedTableName, String creationCommand, @Cause SQLException e);

	@LogMessage(level = TRACE)
	@Message( id = 6005, value = "Cascading %s to child entity '%s'" )
	void cascading(CascadingAction<?> delete, String childEntityName);

	@LogMessage(level = TRACE)
	@Message( id = 6006, value = "Cascading %s to collection '%s'" )
	void cascadingCollection(CascadingAction<?> delete, String collectionRole);

	@LogMessage(level = TRACE)
	@Message( id = 6007, value = "Done cascading %s to collection '%s'" )
	void doneCascadingCollection(CascadingAction<?> delete, String collectionRole);

	@LogMessage(level = TRACE)
	@Message( id = 6008, value = "Processing cascade %s for entity '%s'" )
	void processingCascade(CascadingAction<?> action, String entityName);

	@LogMessage(level = TRACE)
	@Message( id = 6009, value = "Processing cascade %s for entity '%s'" )
	void doneProcessingCascade(CascadingAction<?> action, String entityName);

	@LogMessage(level = TRACE)
	@Message( id = 6011, value = "Deleting orphaned child entity instance of type '%s'" )
	void deletingOrphanOfType(String entityName);

	@LogMessage(level = TRACE)
	@Message( id = 6012, value = "Deleting orphaned child entity instance: %s" )
	void deletingOrphan(String info);

	@LogMessage(level = TRACE)
	@Message( id = 6013, value = "Deleting orphans for collection '%s'" )
	void deletingOrphans(String role);

	@LogMessage(level = TRACE)
	@Message( id = 6014, value = "Done deleting orphans for collection '%s'" )
	void doneDeletingOrphans(String role);

	@LogMessage(level = TRACE)
	@Message( id = 6021, value = "Collection dirty: %s" )
	void collectionDirty(String info);

	@LogMessage(level = TRACE)
	@Message( id = 6022, value = "Reset storedSnapshot to %s for %s" )
	void resetStoredSnapshot(Serializable storedSnapshot, CollectionEntry collectionEntry);

	@LogMessage(level = TRACE)
	@Message( id = 6041, value = "Building session factory using provided StandardServiceRegistry" )
	void buildingFactoryWithProvidedRegistry();

	@LogMessage(level = TRACE)
	@Message( id = 6042, value = "Building session factory using internal StandardServiceRegistryBuilder" )
	void buildingFactoryWithInternalRegistryBuilder();

	@LogMessage(level = TRACE)
	@Message( id = 6043, value = "Found collection with unloaded owner: %s" )
	void collectionWithUnloadedOwner(String info);

	@LogMessage(level = TRACE)
	@Message( id = 6044, value = "Forcing collection initialization" )
	void forcingCollectionInitialization();

	@LogMessage(level = TRACE)
	@Message( id = 6045, value = "Collection dereferenced: %s" )
	void collectionDereferenced(String info);

	@LogMessage(level = TRACE)
	@Message( id = 6046, value = "Skipping uninitialized bytecode-lazy collection: %s" )
	void skippingUninitializedBytecodeLazyCollection(String info);

	@LogMessage(level = TRACE)
	@Message( id = 6047, value = "Collection found: %s, was: %s (initialized)" )
	void collectionFoundInitialized(String is, String was);

	@LogMessage(level = TRACE)
	@Message( id = 6048, value = "Collection found: %s, was: %s (uninitialized)" )
	void collectionFoundUninitialized(String is, String was);

	@LogMessage(level = TRACE)
	@Message( id = 6049, value = "Created collection wrapper for: %s" )
	void createdCollectionWrapper(String s);

	@LogMessage(level = TRACE)
	@Message( id = 6051, value = "Starting serialization of [%s] EntityEntry entries" )
	void startingEntityEntrySerialization(int count);

	@LogMessage(level = TRACE)
	@Message( id = 6052, value = "Starting deserialization of [%s] EntityEntry entries" )
	void startingEntityEntryDeserialization(int count);

	@LogMessage(level = ERROR)
	@Message( id = 6053, value = "Unable to deserialize [%s]" )
	void unableToDeserialize(String entityEntryClassName);

	@LogMessage(level = TRACE)
	@Message( id = 6061, value = "Extracted generated values for entity %s - %s" )
	void extractedGeneratedValues(String info, String results);

	@LogMessage(level = WARN)
	@Message( id = 6062, value = "Could not resolve type name [%s] as Java type" )
	void couldNotResolveTypeName(String typeName, @Cause ClassLoadingException exception);

	@LogMessage(level = DEBUG)
	@Message( id = 6063, value = "Problem releasing GenerationTarget [%s]" )
	void problemReleasingGenerationTarget(GenerationTarget target, @Cause Exception e);

	@LogMessage(level = WARN)
	@Message( id = 6064, value = "Unable to close temp session" )
	void unableToCLoseTempSession();

	// AbstractEntityPersister

	@LogMessage(level = TRACE)
	@Message( id = 6565, value = "Initializing lazy properties from datastore (triggered for '%s')" )
	void initializingLazyPropertiesFromDatastore(String fieldName);

	@LogMessage(level = TRACE)
	@Message( id = 6566, value = "Initializing lazy properties from second-level cache" )
	void initializingLazyPropertiesFromSecondLevelCache();

	@LogMessage(level = TRACE)
	@Message( id = 6567, value = "Done initializing lazy properties" )
	void doneInitializingLazyProperties();

	@LogMessage(level = TRACE)
	@Message( id = 6568, value = "Resolving unique key [%s] to identifier for entity [%s]" )
	void resolvingUniqueKeyToIdentifier(Object key, String entityName);

	@LogMessage(level = TRACE)
	@Message( id = 6569, value = "Reading entity version: %s" )
	void readingEntityVersion(String info);

	@LogMessage(level = TRACE)
	@Message( id = 6570, value = "Fetching entity: %s" )
	void fetchingEntity(String info);

	@LogMessage(level = TRACE)
	@Message( id = 6571, value = "%s is dirty" )
	void propertyIsDirty(String qualifiedProperty);

	@LogMessage(level = TRACE)
	@Message( id = 6572, value = "Forcing version increment [%s]" )
	void forcingVersionIncrement(String info);

	@LogMessage(level = TRACE)
	@Message( id = 6573, value = "Getting current natural-id snapshot state for `%s#%s" )
	void gettingCurrentNaturalIdSnapshot(String entityName, Object id);

	@LogMessage(level = TRACE)
	@Message( id = 6574, value = "Initializing lazy properties of: %s, field access: %s" )
	void initializingLazyPropertiesOf(String info, String fieldName);

	// TransactionImpl

	@LogMessage(level = DEBUG)
	@Message(id = 6581, value = "TransactionImpl created on closed Session/EntityManager")
	void transactionCreatedOnClosedSession();

	@LogMessage(level = DEBUG)
	@Message(id = 6582, value = "TransactionImpl created in JPA compliant mode")
	void transactionCreatedInJpaCompliantMode();

	@LogMessage(level = DEBUG)
	@Message(id = 6583, value = "Beginning transaction")
	void beginningTransaction();

	@LogMessage(level = DEBUG)
	@Message(id = 6584, value = "Committing transaction")
	void committingTransaction();

	@LogMessage(level = DEBUG)
	@Message(id = 6585, value = "Rolling back transaction")
	void rollingBackTransaction();

	@LogMessage(level = DEBUG)
	@Message(id = 6586, value = "rollback() called with inactive transaction")
	void rollbackCalledOnInactiveTransaction();

	@LogMessage(level = DEBUG)
	@Message(id = 6587, value = "setRollbackOnly() called with inactive transaction")
	void setRollbackOnlyCalledOnInactiveTransaction();

	// session builders

	@LogMessage(level = DEBUG)
	@Message(id = 6588, value = "Opening session [tenant=%s]")
	void openingSession(Object tenantIdentifier);

	@LogMessage(level = DEBUG)
	@Message(id = 6589, value = "Opening stateless session [tenant=%s]")
	void openingStatelessSession(Object tenantIdentifier);

	@LogMessage(level = TRACE)
	@Message(id = 6591, value = "Identifier unsaved-value strategy %s")
	void idUnsavedValueStrategy(String strategy);

	@LogMessage(level = TRACE)
	@Message(id = 6592, value = "Identifier unsaved-value [%s]")
	void idUnsavedValue(@Nullable Object value);

	@LogMessage(level = TRACE)
	@Message(id = 6593, value = "Version unsaved-value strategy %s")
	void versionUnsavedValueStrategy(String strategy);

	@LogMessage(level = TRACE)
	@Message(id = 6594, value = "Version unsaved-value [%s]")
	void versionUnsavedValue(@Nullable Object value);

	@LogMessage(level = TRACE)
	@Message(id = 601, value = "Attempting to resolve script source setting: %s")
	void attemptingToResolveScriptSourceSetting(String scriptSourceSettingString);

	@LogMessage(level = DEBUG)
	@Message(id = 602, value = "Attempting to create non-existent script target file: %s")
	void attemptingToCreateScriptTarget(String absolutePath);

	@LogMessage(level = DEBUG)
	@Message(id = 603, value = "Could not create non-existent script target file")
	void couldNotCreateScriptTarget(@Cause Exception e);

	@LogMessage(level = DEBUG)
	@Message(id = 604, value = "Attempting to resolve writer for URL: %s")
	void attemptingToCreateWriter(URL url);
}
