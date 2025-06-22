/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceConfigurationError;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.cache.CacheException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.service.Service;
import org.hibernate.type.SerializationException;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import jakarta.transaction.Synchronization;
import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.cfg.JdbcSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT;
import static org.hibernate.cfg.ValidationSettings.JAKARTA_VALIDATION_MODE;
import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * The jboss-logging {@link MessageLogger} for the hibernate-core module.  It reserves message ids ranging from
 * 00001 to 10000 inclusively.
 * <p>
 * New messages must be added after the last message defined to ensure message codes are unique.
 */
@MessageLogger(projectCode = "HHH")
@ValidIdRange(min=2,max = 20000)
@Internal
public interface CoreMessageLogger extends BasicLogger {

	@LogMessage(level = WARN)
	@Message(value = "Already session bound on call to bind(); make sure you clean up your sessions", id = 2)
	void alreadySessionBound();

	@LogMessage(level = WARN)
	@Message(value = "Configuration settings with for connection provider '%s' are set, but the connection provider is not on the classpath; these properties will be ignored",
			id = 22)
	void providerClassNotFound(String c3p0ProviderClassName);

	@LogMessage(level = WARN)
	@Message(value = "I/O reported cached file could not be found: [%s]: %s", id = 23)
	void cachedFileNotFound(String path, FileNotFoundException error);

	@LogMessage(level = INFO)
	@Message(value = "Second-level cache region factory [%s]", id = 25)
	void regionFactory(String name);

	@LogMessage(level = DEBUG)
	@Message(value = "Second-level cache disabled", id = 26)
	void noRegionFactory();

	@LogMessage(level = WARN)
	@Message(value = "Calling joinTransaction() on a non JTA EntityManager", id = 27)
	void callingJoinTransactionOnNonJtaEntityManager();

	@LogMessage(level = DEBUG)
	@Message(value = "Instantiating factory with settings: %s", id = 30)
	void instantiatingFactory(Map<String, Object> settings);

	@LogMessage(level = DEBUG)
	@Message(value = "Closing factory", id = 31)
	void closingFactory();

	@LogMessage(level = DEBUG)
	@Message(value = "Serializing factory: %s", id = 32)
	void serializingFactory(String uuid);

	@LogMessage(level = DEBUG)
	@Message(value = "Deserialized factory: %s", id = 33)
	void deserializedFactory(String uuid);

	@LogMessage(level = WARN)
	@Message(value = "Composite-id class does not override equals(): %s", id = 38)
	void compositeIdClassDoesNotOverrideEquals(String name);

	@LogMessage(level = WARN)
	@Message(value = "Composite-id class does not override hashCode(): %s", id = 39)
	void compositeIdClassDoesNotOverrideHashCode(String name);

	@LogMessage(level = WARN)
	@Message(value = "Ignoring bag join fetch [%s] due to prior collection join fetch", id = 51)
	void containsJoinFetchedCollection(String role);

	@LogMessage(level = WARN)
	@Message(value = "Defining %s=true ignored in HEM", id = 59)
	void definingFlushBeforeCompletionIgnoredInHem(String flushBeforeCompletion);

	@LogMessage(level = WARN)
	@Message(value = "DEPRECATED: use [%s] instead with custom [%s] implementation", id = 65)
	void deprecatedUuidGenerator(String name, String name2);

	@LogMessage(level = WARN)
	@Message(value = "Duplicate generator name %s", id = 69)
	void duplicateGeneratorName(String name);

	@LogMessage(level = INFO)
	@Message(value = "entity-listener duplication, first event definition will be used: %s", id = 73)
	void duplicateListener(String className);

	@LogMessage(level = WARN)
	@Message(value = "Found more than one <persistence-unit-metadata>, subsequent ignored", id = 74)
	void duplicateMetadata();

	@LogMessage(level = WARN)
	@Message(value = "Entity [%s] is abstract-class/interface explicitly mapped as non-abstract; be sure to supply entity-names",
			id = 84)
	void entityMappedAsNonAbstract(String name);

	@LogMessage(level = ERROR)
	@Message(value = "Exception in interceptor afterTransactionCompletion()", id = 87)
	void exceptionInAfterTransactionCompletionInterceptor(@Cause Throwable e);

	@LogMessage(level = ERROR)
	@Message(value = "Exception in interceptor beforeTransactionCompletion()", id = 88)
	void exceptionInBeforeTransactionCompletionInterceptor(@Cause Throwable e);

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

	@LogMessage(level = INFO)
	@Message(value = "Forcing table use for sequence-style generator due to pooled optimizer selection where db does not support pooled sequences",
			id = 107)
	void forcingTableUse();

	@LogMessage(level = WARN)
	@Message(value = "GUID identifier generated: %s", id = 113)
	void guidGenerated(String result);

	@LogMessage(level = DEBUG)
	@Message(value = "Handling transient entity in delete processing", id = 114)
	void handlingTransientEntity();

	@LogMessage(level = WARN)
	@Message(value = "Config specified explicit optimizer of [%s], but [%s=%s]; using optimizer [%s] increment default of [%s].", id = 116)
	void honoringOptimizerSetting(
			String none,
			String incrementParam,
			int incrementSize,
			String positiveOrNegative,
			int defaultIncrementSize);

	@LogMessage(level = DEBUG)
	@Message(value = "HQL: %s, time: %sms, rows: %s", id = 117)
	void hql(String hql, Long valueOf, Long valueOf2);

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
	@Message(value = "Instantiating explicit connection provider: %s", id = 130)
	void instantiatingExplicitConnectionProvider(String providerClassName);

	@LogMessage(level = ERROR)
	@Message(value = "Array element type error\n%s", id = 132)
	void invalidArrayElementType(String message);

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

	@LogMessage(level = WARN)
	@Message(value = "No appropriate connection provider encountered, assuming application will be supplying connections",
			id = 181)
	void noAppropriateConnectionProvider();

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
	@Message(value = "Pooled optimizer source reported [%s] as the initial value; use of 1 or greater highly recommended",
			id = 201)
	void pooledOptimizerReportedInitialValue(IntegralDataTypeHolder value);

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

	@LogMessage(level = WARN)
	@Message(value = "Scoping types to session factory %s after already scoped %s", id = 233)
	void scopingTypesToSessionFactoryAfterAlreadyScoped(
			SessionFactoryImplementor factory,
			SessionFactoryImplementor factory2);

//	@LogMessage(level = WARN)
//	@Message(value = "SQL Error: %s, SQLState: %s", id = 247)
//	void sqlWarning(int errorCode, String sqlState);

	@LogMessage(level = INFO)
	@Message(value = "Start time: %s", id = 251)
	void startTime(long startTime);

	@LogMessage(level = INFO)
	@Message(value = "Synchronization [%s] was already registered", id = 259)
	void synchronizationAlreadyRegistered(Synchronization synchronization);

	@LogMessage(level = ERROR)
	@Message(value = "Exception calling user Synchronization [%s]: %s", id = 260)
	void synchronizationFailed(Synchronization synchronization, Throwable t);

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
	@Message(value = "Unable to apply constraints on DDL for %s", id = 274)
	void unableToApplyConstraints(String className, @Cause Exception e);

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

	@LogMessage(level = ERROR)
	@Message(value = "Unable to construct current session context [%s]", id = 302)
	void unableToConstructCurrentSessionContext(String impl, @Cause Throwable e);

	@LogMessage(level = WARN)
	@Message(value = "Unable to close temporary session used to load lazy collection associated to no session", id = 303)
	void unableToCloseTemporarySession();

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

	@LogMessage(level = WARN)
	@Message(value = "Unable to interpret specified optimizer [%s], falling back to noop", id = 321)
	void unableToLocateCustomOptimizerClass(String type);

	@LogMessage(level = WARN)
	@Message(value = "Unable to instantiate specified optimizer [%s], falling back to noop", id = 322)
	void unableToInstantiateOptimizer(String type);

	@LogMessage(level = WARN)
	@Message(value = "Unable to instantiate UUID generation strategy class: %s", id = 325)
	void unableToInstantiateUuidGenerationStrategy(Exception ignore);

	@LogMessage(level = INFO)
	@Message(value = "Error performing load command", id = 327)
	void unableToLoadCommand(@Cause HibernateException e);

	@LogMessage(level = ERROR)
	@Message(value = "Problem loading properties from hibernate.properties", id = 329)
	void unableToLoadProperties();

	@LogMessage(level = WARN)
	@Message(value = "Unable to locate requested UUID generation strategy class: %s", id = 334)
	void unableToLocateUuidGenerationStrategy(String strategyClassName);

	@LogMessage(level = WARN)
	@Message(value = "Unable to log SQLWarnings: %s", id = 335)
	void unableToLogSqlWarnings(SQLException sqle);

	@LogMessage(level = ERROR)
	@Message(value = "Unable to mark for rollback on PersistenceException: ", id = 337)
	void unableToMarkForRollbackOnPersistenceException(@Cause Exception e);

	@LogMessage(level = ERROR)
	@Message(value = "Unable to mark for rollback on TransientObjectException: ", id = 338)
	void unableToMarkForRollbackOnTransientObjectException(@Cause Exception e);

	@LogMessage(level = WARN)
	@Message(value = "Could not obtain connection metadata: %s", id = 339)
	void unableToObtainConnectionMetadata(SQLException error);

	@LogMessage(level = WARN)
	@Message(value = "Could not obtain connection to query JDBC database metadata", id = 342)
	void unableToObtainConnectionToQueryMetadata(@Cause Exception e);

	@LogMessage(level = ERROR)
	@Message(value = "Could not read or init a hi value", id = 351)
	void unableToReadOrInitHiValue(@Cause SQLException e);

	@LogMessage(level = ERROR)
	@Message(value = "Could not release a cache lock: %s", id = 353)
	void unableToReleaseCacheLock(CacheException ce);

	@LogMessage(level = INFO)
	@Message(value = "Unable to release isolated connection [%s]", id = 356)
	void unableToReleaseIsolatedConnection(Throwable ignore);

	@LogMessage(level = WARN)
	@Message(value = "Unable to release type info result set", id = 357)
	void unableToReleaseTypeInfoResultSet();

	@LogMessage(level = WARN)
	@Message(value = "Unable to erase previously added bag join fetch", id = 358)
	void unableToRemoveBagJoinFetch();

	@LogMessage(level = WARN)
	@Message(value = "Unable to retrieve type info result set: %s", id = 362)
	void unableToRetrieveTypeInfoResultSet(String string);

	@LogMessage(level = INFO)
	@Message(value = "Unable to rollback connection on exception [%s]", id = 363)
	void unableToRollbackConnection(Exception ignore);

	@LogMessage(level = INFO)
	@Message(value = "Unable to rollback isolated transaction on error [%s]: [%s]", id = 364)
	void unableToRollbackIsolatedTransaction(Exception e, Exception ignore);

	@LogMessage(level = ERROR)
	@Message(value = "Error running schema update", id = 366)
	void unableToRunSchemaUpdate(@Cause Exception e);

	@LogMessage(level = INFO)
	@Message(value = "Error stopping service [%s]", id = 369)
	void unableToStopService(Class<? extends Service> class1, @Cause Exception e);

	@LogMessage(level = ERROR)
	@Message(value = "Could not updateQuery hi value in: %s", id = 376)
	void unableToUpdateQueryHiValue(String tableName, @Cause SQLException e);

	@LogMessage(level = WARN)
	@Message(value = "I/O reported error writing cached file: %s: %s", id = 378)
	void unableToWriteCachedFile(String path, String message);

	@LogMessage(level = WARN)
	@Message(value = "ResultSet had no statement associated with it, but was not yet registered", id = 386)
	void unregisteredResultSetWithoutStatement();

	// Keep this at DEBUG level, rather than warn.  Numerous connection pool implementations can return a
	// proxy/wrapper around the JDBC Statement, causing excessive logging here.  See HHH-8210.
	@LogMessage(level = DEBUG)
	@Message(value = "ResultSet's statement was not registered", id = 387)
	void unregisteredStatement();

	@LogMessage(level = WARN)
	@Message(value = "The %s.%s.%s version of H2 implements temporary table creation such that it commits current transaction; multi-table, bulk HQL/JPQL will not work properly",
			id = 393)
	void unsupportedMultiTableBulkHqlJpaql(int majorVersion, int minorVersion, int buildId);

	@LogMessage(level = INFO)
	@Message(value = "Explicit segment value for id generator [%s.%s] suggested; using default [%s]", id = 398)
	void usingDefaultIdGeneratorSegmentValue(String tableName, String segmentColumnName, String defaultToUse);

	@LogMessage(level = ERROR)
	@Message(value = "Don't use old DTDs, read the Hibernate 3.x Migration Guide", id = 404)
	void usingOldDtd();

	@LogMessage(level = WARN)
	@Message(value = "Using %s which does not generate IETF RFC 4122 compliant UUID values; consider using %s instead",
			id = 409)
	void usingUuidHexGenerator(String name, String name2);

	@LogMessage(level = INFO)
	@Message(value = "Hibernate ORM core version %s", id = 412)
	void version(String versionString);

	@LogMessage(level = WARN)
	@Message(value = "Warnings creating temp table: %s", id = 413)
	void warningsCreatingTempTable(SQLWarning warning);

	@LogMessage(level = WARN)
	@Message(value = "Write locks via update not supported for non-versioned entities [%s]", id = 416)
	void writeLocksNotSupported(String entityName);

	@LogMessage(level = DEBUG)
	@Message(value = "Closing unreleased batch", id = 420)
	void closingUnreleasedBatch();

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
			value = "Alias-specific lock modes requested, which is not currently supported with follow-on locking; " +
					"all acquired locks will be [%s]",
			id = 445
	)
	void aliasSpecificLockingWithFollowOnLocking(LockMode lockMode);

	@LogMessage(level = WARN)
	@Message(
			value = "Explicit use of UPGRADE_SKIPLOCKED in lock() calls is not recommended; use normal UPGRADE locking instead",
			id = 447
	)
	void explicitSkipLockedLockCombo();

	@LogMessage(level = INFO)
	@Message(value = "'" + JAKARTA_VALIDATION_MODE + "' named multiple values: %s", id = 448)
	void multipleValidationModes(String modes);

	@LogMessage(level = WARN)
	@Message(
			id = 449,
			value = "@Convert annotation applied to Map attribute [%s] did not explicitly specify 'attributeName' " +
					"using 'key'/'value' as required by spec; attempting to DoTheRightThing"
	)
	void nonCompliantMapConversion(String collectionRole);

	@LogMessage(level = WARN)
	@Message(
			id = 450,
			value = "Encountered request for Service by non-primary service role [%s -> %s]; please update usage"
	)
	void alternateServiceRole(String requestedRole, String targetRole);

	@LogMessage(level = WARN)
	@Message(
			id = 451,
			value = "Transaction afterCompletion called by a background thread; " +
					"delaying afterCompletion processing until the original thread can handle it. [status=%s]"
	)
	void rollbackFromBackgroundThread(int status);

	// 458-466 reserved for use by main branch (ORM 5.0.0)

	@LogMessage(level = DEBUG)
	@Message(value = "Creating pooled optimizer (lo) with [incrementSize=%s; returnClass=%s]", id = 467)
	void creatingPooledLoOptimizer(int incrementSize, String name);

	@LogMessage(level = WARN)
	@Message(value = "An unexpected session is defined for a collection, but the collection is not connected to that session. A persistent collection may only be associated with one session at a time. Overwriting session. %s", id = 470)
	void logUnexpectedSessionInCollectionNotConnected(String msg);

	@LogMessage(level = WARN)
	@Message(value = "Cannot unset session in a collection because an unexpected session is defined. A persistent collection may only be associated with one session at a time. %s", id = 471 )
	void logCannotUnsetUnexpectedSessionInCollection(String msg);

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
	@Message(value = "Attaching an uninitialized collection with queued operations to a session: %s", id = 495)
	void queuedOperationWhenAttachToSession(String collectionInfoString);

	@LogMessage(level = INFO)
	@Message(value = "Detaching an uninitialized collection with queued operations from a session: %s", id = 496)
	void queuedOperationWhenDetachFromSession(String collectionInfoString);

	@LogMessage(level = WARN)
	@Message(value = "The increment size of the [%s] sequence is set to [%d] in the entity mapping while the associated database sequence increment size is [%d]. The database sequence increment size will take precedence to avoid identifier allocation conflicts.", id = 497)
	void sequenceIncrementSizeMismatch(String sequenceName, int incrementSize, int databaseIncrementSize);

	@LogMessage(level = DEBUG)
	@Message(value = "Detaching an uninitialized collection with queued operations from a session due to rollback: %s", id = 498)
	void queuedOperationWhenDetachFromSessionOnRollback(String collectionInfoString);

	@LogMessage(level = WARN)
	@Message(value = "The [%s] property of the [%s] entity was modified, but it won't be updated because the property is immutable.", id = 502)
	void ignoreImmutablePropertyModification(String propertyName, String entityName);

	@LogMessage(level = WARN)
	@Message(value = "Multiple configuration properties defined to create schema. Choose at most one among 'jakarta.persistence.create-database-schemas' or 'hibernate.hbm2ddl.create_namespaces'.", id = 504)
	void multipleSchemaCreationSettingsDefined();

	@LogMessage(level = WARN)
	@Message(value = "Ignoring ServiceConfigurationError caught while trying to instantiate service '%s'.", id = 505)
	void ignoringServiceConfigurationError(Class<?> serviceContract, @Cause ServiceConfigurationError error);

	@LogMessage(level = WARN)
	@Message(value = "Detaching an uninitialized collection with enabled filters from a session: %s", id = 506)
	void enabledFiltersWhenDetachFromSession(String collectionInfoString);

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

	@LogMessage(level = DEBUG)
	@Message(value = "JDBCException was thrown for a transaction marked for rollback. " +
			" This is probably due to an operation failing fast due to the transaction being marked for rollback.",
			id = 520)
	void jdbcExceptionThrownWithTransactionRolledBack(@Cause JDBCException e);

	@LogMessage(level = DEBUG)
	@Message(value = "Flushing and evicting managed instance of type [%s] before removing detached instance with same id",
			id = 530)
	void flushAndEvictOnRemove(String entityName);

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
	@Message(value = "Returning null (as required by JPA spec) rather than throwing EntityNotFoundException " +
			"since the entity of type '%s' with id [%s] does not exist", id = 15013)
	void ignoringEntityNotFound(String entityName, String identifier);

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

	@LogMessage(level = DEBUG)
	@Message(
			id = 455,
			value =
					"'" + CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT + "' " +
					"""
					was enabled. This setting should only be enabled when JDBC Connections obtained by Hibernate \
					from the ConnectionProvider have auto-commit disabled. Enabling this setting when connections \
					have auto-commit enabled leads to execution of SQL operations outside of any JDBC transaction.\
					"""
	)
	void connectionProviderDisablesAutoCommitEnabled();

	@LogMessage(level = TRACE)
	@Message(value = "Closing logical connection", id = 456)
	void closingLogicalConnection();

	@LogMessage(level = TRACE)
	@Message(value = "Logical connection closed", id = 457)
	void logicalConnectionClosed();

	@LogMessage(level = DEBUG)
	@Message(
			id = 401,
			value = """
					Logging session metrics:
						%s ns acquiring %s JDBC connections
						%s ns releasing %s JDBC connections
						%s ns preparing %s JDBC statements
						%s ns executing %s JDBC statements
						%s ns executing %s JDBC batches
						%s ns performing %s second-level cache puts
						%s ns performing %s second-level cache hits
						%s ns performing %s second-level cache misses
						%s ns executing %s flushes (flushing a total of %s entities and %s collections)
						%s ns executing %s pre-partial-flushes
						%s ns executing %s partial-flushes (flushing a total of %s entities and %s collections)
					"""
	)
	void sessionMetrics(
			long jdbcConnectionAcquisitionTime,
			int jdbcConnectionAcquisitionCount,
			long jdbcConnectionReleaseTime,
			int jdbcConnectionReleaseCount,
			long jdbcPrepareStatementTime,
			int jdbcPrepareStatementCount,
			long jdbcExecuteStatementTime,
			int jdbcExecuteStatementCount,
			long jdbcExecuteBatchTime,
			int jdbcExecuteBatchCount,
			long cachePutTime,
			int cachePutCount,
			long cacheHitTime,
			int cacheHitCount,
			long cacheMissTime,
			int cacheMissCount,
			long flushTime,
			int flushCount,
			long flushEntityCount,
			long flushCollectionCount,
			long prePartialFlushTime,
			int prePartialFlushCount,
			long partialFlushTime,
			int partialFlushCount,
			long partialFlushEntityCount,
			long partialFlushCollectionCount);

	@LogMessage(level = INFO)
	@Message(
			id = 400,
			value = """
					Logging statistics:
						Start time: %s
						Sessions opened (closed): %s (%s)
						Transactions started (successful): %s (%s)
						Optimistic lock failures: %s
						Flushes: %s
						Connections obtained: %s
						Statements prepared (closed): %s (%s)
						Second-level cache puts: %s
						Second-level cache hits (misses): %s (%s)
						Entities loaded: %s
						Entities fetched: %s (minimize this)
						Entities updated, upserted, inserted, deleted: %s, %s, %s, %s
						Collections loaded: %s
						Collections fetched: %s (minimize this)
						Collections updated, removed, recreated: %s, %s, %s
						Natural id queries executed on database: %s
						Natural id cache puts: %s
						Natural id cache hits (misses): %s (%s)
						Max natural id query execution time: %s ms
						Queries executed on database: %s
						Query cache puts: %s
						Query cache hits (misses): %s (%s)
						Max query execution time: %s ms
						Update timestamps cache puts: %s
						Update timestamps cache hits (misses): %s (%s)
						Query plan cache hits (misses): %s (%s)
					"""
	)
	void logStatistics(
			long startTime,
			long sessionOpenCount,
			long sessionCloseCount,
			long transactionCount,
			long committedTransactionCount,
			long optimisticFailureCount,
			long flushCount,
			long connectCount,
			long prepareStatementCount,
			long closeStatementCount,
			long secondLevelCachePutCount,
			long secondLevelCacheHitCount,
			long secondLevelCacheMissCount,
			long entityLoadCount,
			long entityFetchCount,
			long entityUpdateCount,
			long entityUpsertCount,
			long entityInsertCount,
			long entityDeleteCount,
			long collectionLoadCount,
			long collectionFetchCount,
			long collectionUpdateCount,
			long collectionRemoveCount,
			long collectionRecreateCount,
			long naturalIdQueryExecutionCount,
			long naturalIdCachePutCount,
			long naturalIdCacheHitCount,
			long naturalIdCacheMissCount,
			long naturalIdQueryExecutionMaxTime,
			long queryExecutionCount,
			long queryCachePutCount,
			long queryCacheHitCount,
			long queryCacheMissCount,
			long queryExecutionMaxTime,
			long updateTimestampsCachePutCount,
			long updateTimestampsCacheHitCount,
			long updateTimestampsCacheMissCount,
			long queryPlanCacheHitCount,
			long queryPlanCacheMissCount);
}
