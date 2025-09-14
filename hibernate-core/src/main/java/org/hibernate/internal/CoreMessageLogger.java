/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.Properties;
import java.util.ServiceConfigurationError;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.cache.CacheException;
import org.hibernate.internal.log.SubSystemLogging;
import org.hibernate.type.SerializationException;

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
		description = "Miscellaneous Logging related to Hibernate ORM Core"
)
@MessageLogger(projectCode = "HHH")
@ValidIdRange(min=2,max = 20000)
@Internal
public interface CoreMessageLogger extends BasicLogger {

	String NAME = SubSystemLogging.BASE + ".core";

	Logger LOGGER = Logger.getLogger( NAME );
	CoreMessageLogger CORE_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), CoreMessageLogger.class, NAME );

	@LogMessage(level = WARN)
	@Message(value = "I/O reported cached file could not be found: [%s]: %s", id = 23)
	void cachedFileNotFound(String path, FileNotFoundException error);

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
	@Message(value = "Defining %s=true ignored in HEM", id = 59)
	void definingFlushBeforeCompletionIgnoredInHem(String flushBeforeCompletion);

	@LogMessage(level = WARN)
	@Message(value = "Duplicate generator name %s", id = 69)
	void duplicateGeneratorName(String name);

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

	@LogMessage(level = WARN)
	@Message(value = "HSQLDB supports only READ_UNCOMMITTED isolation", id = 118)
	void hsqldbSupportsOnlyReadCommittedIsolation();

	@LogMessage(level = WARN)
	@Message(value = "Ignoring unique constraints specified on table generator [%s]", id = 120)
	void ignoringTableGeneratorConstraints(String name);

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

	@LogMessage(level = WARN)
	@Message(value = "Overriding %s is dangerous, this might break the EJB3 specification implementation", id = 193)
	void overridingTransactionStrategyDangerous(String transactionStrategy);

	@LogMessage(level = DEBUG)
	@Message(value = "Package not found or no package-info.java: %s", id = 194)
	void packageNotFound(String packageName);

	@LogMessage(level = WARN)
	@Message(value = "LinkageError while attempting to load package: %s", id = 195)
	void linkageError(String packageName, @Cause LinkageError e);

	@LogMessage(level = INFO)
	@Message(value = "Processing PersistenceUnitInfo [name: %s]", id = 204)
	void processingPersistenceUnitInfoName(String persistenceUnitName);

	@LogMessage(level = INFO)
	@Message(value = "Loaded properties from resource hibernate.properties: %s", id = 205)
	void propertiesLoaded(Properties maskOut);

	@LogMessage(level = DEBUG)
	@Message(value = "'hibernate.properties' not found", id = 206)
	void propertiesNotFound();

	@LogMessage(level = INFO)
	@Message(value = "Reading mappings from cache file: %s", id = 219)
	void readingCachedMappings(File cachedFile);

	@LogMessage(level = INFO)
	@Message(value = "Reading mappings from file: %s", id = 220)
	void readingMappingsFromFile(String path);

	@LogMessage(level = WARN)
	@Message(value = "Recognized obsolete hibernate namespace %s. Use namespace %s instead. Refer to Hibernate 3.6 Migration Guide",
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

	@LogMessage(level = WARN)
	@Message(value = "Could not deserialize cache file [%s]: %s", id = 307)
	void unableToDeserializeCache(String path, SerializationException error);

	@LogMessage(level = INFO)
	@Message(value = "Could not find any META-INF/persistence.xml file in the classpath", id = 318)
	void unableToFindPersistenceXmlInClasspath();

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
	@Message(value = "I/O reported error writing cached file: %s: %s", id = 378)
	void unableToWriteCachedFile(String path, String message);

	@LogMessage(level = WARN)
	@Message(value = "The %s.%s.%s version of H2 implements temporary table creation such that it commits current transaction; multi-table, bulk HQL/JPQL will not work properly",
			id = 393)
	void unsupportedMultiTableBulkHqlJpaql(int majorVersion, int minorVersion, int buildId);

	@LogMessage(level = ERROR)
	@Message(value = "Don't use old DTDs, read the Hibernate 3.x Migration Guide", id = 404)
	void usingOldDtd();

	@LogMessage(level = INFO)
	@Message(value = "Hibernate ORM core version %s", id = 412)
	void version(String versionString);

	@LogMessage(level = WARN)
	@Message(value = "Warnings creating temp table: %s", id = 413)
	void warningsCreatingTempTable(SQLWarning warning);

	@LogMessage(level = WARN)
	@Message(value = "Write locks via update not supported for non-versioned entities [%s]", id = 416)
	void writeLocksNotSupported(String entityName);

	@LogMessage(level = WARN)
	@Message(
			id = 437,
			value = """
					Attempting to save one or more entities that have a non-nullable association with an unsaved transient entity.
					The unsaved transient entity must be saved in an operation prior to saving these dependent entities.
						Unsaved transient entity: %s
						Dependent entities: %s
						Non-nullable associations: %s"""
	)
	void cannotResolveNonNullableTransientDependencies(
			String transientEntityString,
			Set<String> dependentEntityStrings,
			Set<String> nonNullableAssociationPaths);

	@LogMessage(level = WARN)
	@Message(
			value = "Dialect [%s] limits the number of elements in an IN predicate to %s entries.  " +
					"However, the given parameter list [%s] contained %s entries, which will likely cause failures " +
					"to execute the query in the database",
			id = 443
	)
	void tooManyInExpressions(String dialectName, int limit, String paramName, int size);

	@LogMessage(level = WARN)
	@Message(
			value = "Encountered request for locking however dialect reports that database prefers locking be done in a " +
					"separate select (follow-on locking); results will be locked after initial query executes",
			id = 444
	)
	void usingFollowOnLocking();

	@LogMessage(level = WARN)
	@Message(
			id = 449,
			value = "@Convert annotation applied to Map attribute [%s] did not explicitly specify "
					+ "'attributeName=\"key\" or 'attributeName=\"value\"' as required by spec; "
					+ "attempting to infer whether converter applies to key or value"
	)
	void nonCompliantMapConversion(String collectionRole);

	@LogMessage(level = INFO)
	@Message(value = "Omitting cached file [%s] as the mapping file is newer", id = 473)
	void cachedFileObsolete(File cachedFile);

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

	@LogMessage(level = WARN)
	@Message(value = "A ManagedEntity was associated with a stale PersistenceContext. A ManagedEntity may only be associated with one PersistenceContext at a time; %s", id = 480)
	void stalePersistenceContextInEntityEntry(String msg);

	@LogMessage(level = ERROR)
	@Message(value = "Illegally attempted to associate a proxy for entity [%s] with id [%s] with two open sessions.", id = 485)
	void attemptToAssociateProxyWithTwoOpenSessions(String entityName, Object id);

	@LogMessage(level = WARN)
	@Message(value = "The query [%s] updates an immutable entity: %s",
			id = 487)
	void immutableEntityUpdateQuery(String sourceQuery, String querySpaces);

	@LogMessage(level = DEBUG)
	@Message(value = "The query [%s] updates an immutable entity: %s",
			id = 488)
	void immutableEntityUpdateQueryAllowed(String sourceQuery, String querySpaces);

	@LogMessage(level = INFO)
	@Message(value = "No JTA platform available (set 'hibernate.transaction.jta.platform' to enable JTA platform integration)", id = 489)
	void noJtaPlatform();

	@LogMessage(level = INFO)
	@Message(value = "Using JTA platform [%s]", id = 490)
	void usingJtaPlatform(String jtaPlatformClassName);

	@LogMessage(level = WARN)
	@Message(value = "'%1$s.%2$s' uses both @NotFound and FetchType.LAZY. @ManyToOne and " +
			"@OneToOne associations mapped with @NotFound are forced to EAGER fetching.", id = 491)
	void ignoreNotFoundWithFetchTypeLazy(String entity, String association);

	@LogMessage(level = WARN)
	@Message(value = "Attempt to merge an uninitialized collection with queued operations; queued operations will be ignored: %s", id = 494)
	void ignoreQueuedOperationsOnMerge(String collectionInfoString);

	@LogMessage(level = WARN)
	@Message(value = "The [%s] property of the [%s] entity was modified, but it won't be updated because the property is immutable.", id = 502)
	void ignoreImmutablePropertyModification(String propertyName, String entityName);

	@LogMessage(level = WARN)
	@Message(value = "Multiple configuration properties defined to create schema. Choose at most one among 'jakarta.persistence.create-database-schemas' or 'hibernate.hbm2ddl.create_namespaces'.", id = 504)
	void multipleSchemaCreationSettingsDefined();

	@LogMessage(level = WARN)
	@Message(value = "Multi-table insert is not available due to missing identity and window function support for: %s", id = 509)
	void multiTableInsertNotAvailable(String entityName);

	@LogMessage(level = WARN)
	@Message(value = "Association with '@Fetch(JOIN)' and 'fetch=FetchType.LAZY' found. This is interpreted as lazy: %s", id = 510)
	void fetchModeJoinWithLazyWarning(String role);

	@LogMessage(level = WARN)
	@Message(value = "The %2$s version for [%s] is no longer supported, hence certain features may not work properly. The minimum supported version is %3$s. Check the community dialects project for available legacy versions.", id = 511)
	void unsupportedDatabaseVersion(String databaseName, String actualVersion, String minimumVersion);

	@LogMessage(level = DEBUG)
	@Message(value = "Unable to create the ReflectionOptimizer for [%s]: %s",
			id = 513)
	void unableToGenerateReflectionOptimizer(String className, String cause);

	@LogMessage(level = WARN)
	@Message(value = "Failed to discover types for enhancement from class: %s",
			id = 516)
	void enhancementDiscoveryFailed(String className, @Cause Throwable cause);

	@LogMessage(level = ERROR)
	@Message(value = "Illegal argument on static metamodel field injection: %s#%s; expected type: %s; encountered type: %s", id = 15007)
	void illegalArgumentOnStaticMetamodelFieldInjection(
			String name,
			String name2,
			String name3,
			String name4);

	@LogMessage(level = WARN)
	@Message(value = "Unable to locate static metamodel field: %s#%s; this may or may not indicate a problem with the static metamodel", id = 15011)
	void unableToLocateStaticMetamodelField(
			String name,
			String name2);

	@LogMessage(level = DEBUG)
	@Message(
			id = 15015,
			value = "Encountered a MappedSuperclass [%s] not used in any entity hierarchy"
	)
	void unusedMappedSuperclass(String name);

	@LogMessage(level = WARN)
	@Message(
			id = 15018,
			value = "Encountered multiple persistence-unit stanzas defining same name [%s]; persistence-unit names must be unique"
	)
	void duplicatedPersistenceUnitName(String name);

	@LogMessage(level = WARN)
	@Message(
			id = 15019,
			value = "Invalid JSON column type [%s], was expecting [%s]; for efficiency schema should be migrate to JSON DDL type"
	)
	void invalidJSONColumnType(String actual, String expected);

	@LogMessage(level = TRACE)
	@Message(value = "Initializing service: %s", id = 500)
	void initializingService(String serviceRole);

	@LogMessage(level = INFO)
	@Message(value = "Error stopping service: %s", id = 369)
	void unableToStopService(String serviceRole, @Cause Exception e);

	@LogMessage(level = WARN)
	@Message(value = "Ignoring ServiceConfigurationError caught while instantiating service: %s", id = 505)
	void ignoringServiceConfigurationError(String serviceContract, @Cause ServiceConfigurationError error);

	@LogMessage(level = WARN)
	@Message(value = "Encountered request for service by non-primary service role [%s -> %s]", id = 450)
	void alternateServiceRole(String requestedRole, String targetRole);
}
