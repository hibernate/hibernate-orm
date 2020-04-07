/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.Hashtable;
import java.util.Properties;
import java.util.ServiceConfigurationError;
import java.util.Set;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.cache.CacheException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;
import org.hibernate.engine.jndi.JndiException;
import org.hibernate.engine.jndi.JndiNameException;
import org.hibernate.engine.loading.internal.CollectionLoadContext;
import org.hibernate.engine.loading.internal.EntityLoadContext;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.type.BasicType;
import org.hibernate.type.SerializationException;
import org.hibernate.type.Type;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * The jboss-logging {@link MessageLogger} for the hibernate-core module.  It reserves message ids ranging from
 * 00001 to 10000 inclusively.
 * <p/>
 * New messages must be added after the last message defined to ensure message codes are unique.
 */
@MessageLogger(projectCode = "HHH")
@ValidIdRange( min = 1, max = 10000 )
public interface CoreMessageLogger extends BasicLogger {

	@LogMessage(level = WARN)
	@Message(value = "Already session bound on call to bind(); make sure you clean up your sessions!", id = 2)
	void alreadySessionBound();

	@LogMessage(level = INFO)
	@Message(value = "Autocommit mode: %s", id = 6)
	void autoCommitMode(boolean autocommit);

	@LogMessage(level = WARN)
	@Message(value = "JTASessionContext being used with JDBC transactions; auto-flush will not operate correctly with getCurrentSession()",
			id = 8)
	void autoFlushWillNotWork();

	@LogMessage(level = INFO)
	@Message(value = "On release of batch it still contained JDBC statements", id = 10)
	void batchContainedStatementsOnRelease();

	@LogMessage(level = INFO)
	@Message(value = "Bytecode provider name : %s", id = 21)
	void bytecodeProvider(String provider);

	@LogMessage(level = WARN)
	@Message(value = "c3p0 properties were encountered, but the %s provider class was not found on the classpath; these properties are going to be ignored.",
			id = 22)
	void c3p0ProviderClassNotFound(String c3p0ProviderClassName);

	@LogMessage(level = WARN)
	@Message(value = "I/O reported cached file could not be found : %s : %s", id = 23)
	void cachedFileNotFound(String path, FileNotFoundException error);

	@LogMessage(level = INFO)
	@Message(value = "Cache provider: %s", id = 24)
	void cacheProvider(String name);

	@LogMessage(level = WARN)
	@Message(value = "Calling joinTransaction() on a non JTA EntityManager", id = 27)
	void callingJoinTransactionOnNonJtaEntityManager();

	@LogMessage(level = DEBUG)
	@Message(value = "Closing", id = 31)
	void closing();

	@LogMessage(level = INFO)
	@Message(value = "Collections fetched (minimize this): %s", id = 32)
	void collectionsFetched(long collectionFetchCount);

	@LogMessage(level = INFO)
	@Message(value = "Collections loaded: %s", id = 33)
	void collectionsLoaded(long collectionLoadCount);

	@LogMessage(level = INFO)
	@Message(value = "Collections recreated: %s", id = 34)
	void collectionsRecreated(long collectionRecreateCount);

	@LogMessage(level = INFO)
	@Message(value = "Collections removed: %s", id = 35)
	void collectionsRemoved(long collectionRemoveCount);

	@LogMessage(level = INFO)
	@Message(value = "Collections updated: %s", id = 36)
	void collectionsUpdated(long collectionUpdateCount);

	@LogMessage(level = INFO)
	@Message(value = "Columns: %s", id = 37)
	void columns(Set keySet);

	@LogMessage(level = WARN)
	@Message(value = "Composite-id class does not override equals(): %s", id = 38)
	void compositeIdClassDoesNotOverrideEquals(String name);

	@LogMessage(level = WARN)
	@Message(value = "Composite-id class does not override hashCode(): %s", id = 39)
	void compositeIdClassDoesNotOverrideHashCode(String name);

	@LogMessage(level = INFO)
	@Message(value = "Configuration resource: %s", id = 40)
	void configurationResource(String resource);

	@LogMessage(level = INFO)
	@Message(value = "Configured SessionFactory: %s", id = 41)
	void configuredSessionFactory(String name);

	@LogMessage(level = INFO)
	@Message(value = "Configuring from file: %s", id = 42)
	void configuringFromFile(String file);

	@LogMessage(level = INFO)
	@Message(value = "Configuring from resource: %s", id = 43)
	void configuringFromResource(String resource);

	@LogMessage(level = INFO)
	@Message(value = "Configuring from URL: %s", id = 44)
	void configuringFromUrl(URL url);

	@LogMessage(level = INFO)
	@Message(value = "Configuring from XML document", id = 45)
	void configuringFromXmlDocument();

	@LogMessage(level = INFO)
	@Message(value = "Connections obtained: %s", id = 48)
	void connectionsObtained(long connectCount);

	@LogMessage(level = ERROR)
	@Message(value = "Container is providing a null PersistenceUnitRootUrl: discovery impossible", id = 50)
	void containerProvidingNullPersistenceUnitRootUrl();

	@LogMessage(level = WARN)
	@Message(value = "Ignoring bag join fetch [%s] due to prior collection join fetch", id = 51)
	void containsJoinFetchedCollection(String role);

	@LogMessage(level = INFO)
	@Message(value = "Creating subcontext: %s", id = 53)
	void creatingSubcontextInfo(String intermediateContextName);

	@LogMessage(level = WARN)
	@Message(value = "Defining %s=true ignored in HEM", id = 59)
	void definingFlushBeforeCompletionIgnoredInHem(String flushBeforeCompletion);

	@LogMessage(level = WARN)
	@Message(value = "@ForceDiscriminator is deprecated use @DiscriminatorOptions instead.", id = 62)
	void deprecatedForceDescriminatorAnnotation();

	@LogMessage(level = WARN)
	@Message(value = "The Oracle9Dialect dialect has been deprecated; use either Oracle9iDialect or Oracle10gDialect instead",
			id = 63)
	void deprecatedOracle9Dialect();

	@LogMessage(level = WARN)
	@Message(value = "The OracleDialect dialect has been deprecated; use Oracle8iDialect instead", id = 64)
	void deprecatedOracleDialect();

	@LogMessage(level = WARN)
	@Message(value = "DEPRECATED : use [%s] instead with custom [%s] implementation", id = 65)
	void deprecatedUuidGenerator(String name, String name2);

	@LogMessage(level = INFO)
	@Message(value = "Disallowing insert statement comment for select-identity due to Oracle driver bug", id = 67)
	void disallowingInsertStatementComment();

	@LogMessage(level = WARN)
	@Message(value = "Duplicate generator name %s", id = 69)
	void duplicateGeneratorName(String name);

	@LogMessage(level = WARN)
	@Message(value = "Duplicate generator table: %s", id = 70)
	void duplicateGeneratorTable(String name);

	@LogMessage(level = INFO)
	@Message(value = "Duplicate import: %s -> %s", id = 71)
	void duplicateImport(String entityName, String rename);

	@LogMessage(level = WARN)
	@Message(value = "Duplicate joins for class: %s", id = 72)
	void duplicateJoins(String entityName);

	@LogMessage(level = INFO)
	@Message(value = "entity-listener duplication, first event definition will be used: %s", id = 73)
	void duplicateListener(String className);

	@LogMessage(level = WARN)
	@Message(value = "Found more than one <persistence-unit-metadata>, subsequent ignored", id = 74)
	void duplicateMetadata();

	@LogMessage(level = INFO)
	@Message(value = "Entities deleted: %s", id = 76)
	void entitiesDeleted(long entityDeleteCount);

	@LogMessage(level = INFO)
	@Message(value = "Entities fetched (minimize this): %s", id = 77)
	void entitiesFetched(long entityFetchCount);

	@LogMessage(level = INFO)
	@Message(value = "Entities inserted: %s", id = 78)
	void entitiesInserted(long entityInsertCount);

	@LogMessage(level = INFO)
	@Message(value = "Entities loaded: %s", id = 79)
	void entitiesLoaded(long entityLoadCount);

	@LogMessage(level = INFO)
	@Message(value = "Entities updated: %s", id = 80)
	void entitiesUpdated(long entityUpdateCount);

	@LogMessage(level = WARN)
	@Message(value = "@org.hibernate.annotations.Entity used on a non root entity: ignored for %s", id = 81)
	void entityAnnotationOnNonRoot(String className);

	@LogMessage(level = WARN)
	@Message(value = "Entity Manager closed by someone else (%s must not be used)", id = 82)
	void entityManagerClosedBySomeoneElse(String autoCloseSession);

	@LogMessage(level = WARN)
	@Message(value = "Entity [%s] is abstract-class/interface explicitly mapped as non-abstract; be sure to supply entity-names",
			id = 84)
	void entityMappedAsNonAbstract(String name);

	@LogMessage(level = INFO)
	@Message(value = "%s %s found", id = 85)
	void exceptionHeaderFound(String exceptionHeader, String metaInfOrmXml);

	@LogMessage(level = INFO)
	@Message(value = "%s No %s found", id = 86)
	void exceptionHeaderNotFound(String exceptionHeader, String metaInfOrmXml);

	@LogMessage(level = ERROR)
	@Message(value = "Exception in interceptor afterTransactionCompletion()", id = 87)
	void exceptionInAfterTransactionCompletionInterceptor(@Cause Throwable e);

	@LogMessage(level = ERROR)
	@Message(value = "Exception in interceptor beforeTransactionCompletion()", id = 88)
	void exceptionInBeforeTransactionCompletionInterceptor(@Cause Throwable e);

	@LogMessage(level = INFO)
	@Message(value = "Sub-resolver threw unexpected exception, continuing to next : %s", id = 89)
	void exceptionInSubResolver(String message);

	@LogMessage(level = ERROR)
	@Message(value = "Expected type: %s, actual value: %s", id = 91)
	void expectedType(String name,
			String string);

	@LogMessage(level = WARN)
	@Message(value = "An item was expired by the cache while it was locked (increase your cache timeout): %s", id = 92)
	void expired(Object key);

	@LogMessage(level = INFO)
	@Message(value = "Bound factory to JNDI name: %s", id = 94)
	void factoryBoundToJndiName(String name);

	@LogMessage(level = INFO)
	@Message(value = "A factory was renamed from [%s] to [%s] in JNDI", id = 96)
	void factoryJndiRename(String oldName, String newName);

	@LogMessage(level = INFO)
	@Message(value = "Unbound factory from JNDI name: %s", id = 97)
	void factoryUnboundFromJndiName(String name);

	@LogMessage(level = INFO)
	@Message(value = "A factory was unbound from name: %s", id = 98)
	void factoryUnboundFromName(String name);

	@LogMessage(level = ERROR)
	@Message(value = "an assertion failure occurred" + " (this may indicate a bug in Hibernate, but is more likely due"
			+ " to unsafe use of the session): %s", id = 99)
	void failed(Throwable throwable);

	@LogMessage(level = WARN)
	@Message(value = "Fail-safe cleanup (collections) : %s", id = 100)
	void failSafeCollectionsCleanup(CollectionLoadContext collectionLoadContext);

	@LogMessage(level = WARN)
	@Message(value = "Fail-safe cleanup (entities) : %s", id = 101)
	void failSafeEntitiesCleanup(EntityLoadContext entityLoadContext);

	@LogMessage(level = INFO)
	@Message(value = "Fetching database metadata", id = 102)
	void fetchingDatabaseMetadata();

	@LogMessage(level = WARN)
	@Message(value = "firstResult/maxResults specified with collection fetch; applying in memory!", id = 104)
	void firstOrMaxResultsSpecifiedWithCollectionFetch();

	@LogMessage(level = INFO)
	@Message(value = "Flushes: %s", id = 105)
	void flushes(long flushCount);

	@LogMessage(level = INFO)
	@Message(value = "Forcing container resource cleanup on transaction completion", id = 106)
	void forcingContainerResourceCleanup();

	@LogMessage(level = INFO)
	@Message(value = "Forcing table use for sequence-style generator due to pooled optimizer selection where db does not support pooled sequences",
			id = 107)
	void forcingTableUse();

	@LogMessage(level = INFO)
	@Message(value = "Foreign keys: %s", id = 108)
	void foreignKeys(Set keySet);

	@LogMessage(level = INFO)
	@Message(value = "Found mapping document in jar: %s", id = 109)
	void foundMappingDocument(String name);

	@LogMessage(level = WARN)
	@Message(value = "GUID identifier generated: %s", id = 113)
	void guidGenerated(String result);

	@LogMessage(level = INFO)
	@Message(value = "Handling transient entity in delete processing", id = 114)
	void handlingTransientEntity();

	@LogMessage(level = INFO)
	@Message(value = "Hibernate connection pool size: %s (min=%s)", id = 115)
	void hibernateConnectionPoolSize(int poolSize, int minSize);

	@LogMessage(level = WARN)
	@Message(value = "Config specified explicit optimizer of [%s], but [%s=%s]; using optimizer [%s] increment default of [%s].", id = 116)
	void honoringOptimizerSetting(
			String none,
			String incrementParam,
			int incrementSize,
			String positiveOrNegative,
			int defaultIncrementSize
	);

	@LogMessage(level = DEBUG)
	@Message(value = "HQL: %s, time: %sms, rows: %s", id = 117)
	void hql(
			String hql,
			Long valueOf,
			Long valueOf2);

	@LogMessage(level = WARN)
	@Message(value = "HSQLDB supports only READ_UNCOMMITTED isolation", id = 118)
	void hsqldbSupportsOnlyReadCommittedIsolation();

	@LogMessage(level = WARN)
	@Message(value = "On EntityLoadContext#clear, hydratingEntities contained [%s] entries", id = 119)
	void hydratingEntitiesCount(int size);

	@LogMessage(level = WARN)
	@Message(value = "Ignoring unique constraints specified on table generator [%s]", id = 120)
	void ignoringTableGeneratorConstraints(String name);

	@LogMessage(level = INFO)
	@Message(value = "Ignoring unrecognized query hint [%s]", id = 121)
	void ignoringUnrecognizedQueryHint(String hintName);

	@LogMessage(level = ERROR)
	@Message(value = "IllegalArgumentException in class: %s, getter method of property: %s", id = 122)
	void illegalPropertyGetterArgument(
			String name,
			String propertyName);

	@LogMessage(level = ERROR)
	@Message(value = "IllegalArgumentException in class: %s, setter method of property: %s", id = 123)
	void illegalPropertySetterArgument(
			String name,
			String propertyName);

	@LogMessage(level = WARN)
	@Message(value = "@Immutable used on a non root entity: ignored for %s", id = 124)
	void immutableAnnotationOnNonRoot(String className);

	@LogMessage(level = WARN)
	@Message(value = "Mapping metadata cache was not completely processed", id = 125)
	void incompleteMappingMetadataCacheProcessing();

	@LogMessage(level = INFO)
	@Message(value = "Indexes: %s", id = 126)
	void indexes(Set keySet);

	@LogMessage(level = DEBUG)
	@Message(value = "Could not bind JNDI listener", id = 127)
	void couldNotBindJndiListener();

	@LogMessage(level = INFO)
	@Message(value = "Instantiating explicit connection provider: %s", id = 130)
	void instantiatingExplicitConnectionProvider(String providerClassName);

	@LogMessage(level = ERROR)
	@Message(value = "Array element type error\n%s", id = 132)
	void invalidArrayElementType(String message);

	@LogMessage(level = WARN)
	@Message(value = "Discriminator column has to be defined in the root entity, it will be ignored in subclass: %s",
			id = 133)
	void invalidDiscriminatorAnnotation(String className);

	@LogMessage(level = ERROR)
	@Message(value = "Application attempted to edit read only item: %s", id = 134)
	void invalidEditOfReadOnlyItem(Object key);

	@LogMessage(level = ERROR)
	@Message(value = "Invalid JNDI name: %s", id = 135)
	void invalidJndiName(
			String name,
			@Cause JndiNameException e);

	@LogMessage(level = WARN)
	@Message(value = "Inapropriate use of @OnDelete on entity, annotation ignored: %s", id = 136)
	void invalidOnDeleteAnnotation(String entityName);

	@LogMessage(level = WARN)
	@Message(value = "Root entity should not hold a PrimaryKeyJoinColum(s), will be ignored: %s", id = 137)
	void invalidPrimaryKeyJoinColumnAnnotation(String className);

	@LogMessage(level = WARN)
	@Message(value = "Mixing inheritance strategy in a entity hierarchy is not allowed, ignoring sub strategy in: %s",
			id = 138)
	void invalidSubStrategy(String className);

	@LogMessage(level = WARN)
	@Message(value = "Illegal use of @Table in a subclass of a SINGLE_TABLE hierarchy: %s", id = 139)
	void invalidTableAnnotation(String className);

	@LogMessage(level = INFO)
	@Message(value = "JACC contextID: %s", id = 140)
	void jaccContextId(String contextId);

	@LogMessage(level = INFO)
	@Message(value = "java.sql.Types mapped the same code [%s] multiple times; was [%s]; now [%s]", id = 141)
	void JavaSqlTypesMappedSameCodeMultipleTimes(
			int code,
			String old,
			String name);

	@Message(value = "Bytecode enhancement failed: %s", id = 142)
	String bytecodeEnhancementFailed(String entityName);

	@Message(value = "Bytecode enhancement failed because no public, protected or package-private default constructor was found for entity: %s. Private constructors don't work with runtime proxies!", id = 143)
	String bytecodeEnhancementFailedBecauseOfDefaultConstructor(String entityName);

	@LogMessage(level = WARN)
	@Message(value = "%s = false breaks the EJB3 specification", id = 144)
	void jdbcAutoCommitFalseBreaksEjb3Spec(String autocommit);

	@Message(value = "JDBC rollback failed", id = 151)
	String jdbcRollbackFailed();

	@LogMessage(level = INFO)
	@Message(value = "JNDI InitialContext properties:%s", id = 154)
	void jndiInitialContextProperties(Hashtable hash);

	@LogMessage(level = ERROR)
	@Message(value = "JNDI name %s does not handle a session factory reference", id = 155)
	void jndiNameDoesNotHandleSessionFactoryReference(
			String sfJNDIName,
			@Cause ClassCastException e);

	@LogMessage(level = INFO)
	@Message(value = "Lazy property fetching available for: %s", id = 157)
	void lazyPropertyFetchingAvailable(String name);

	@LogMessage(level = WARN)
	@Message(value = "In CollectionLoadContext#endLoadingCollections, localLoadingCollectionKeys contained [%s], but no LoadingCollectionEntry was found in loadContexts",
			id = 159)
	void loadingCollectionKeyNotFound(CollectionKey collectionKey);

	@LogMessage(level = WARN)
	@Message(value = "On CollectionLoadContext#cleanup, localLoadingCollectionKeys contained [%s] entries", id = 160)
	void localLoadingCollectionKeysCount(int size);

	@LogMessage(level = INFO)
	@Message(value = "Logging statistics....", id = 161)
	void loggingStatistics();

	@LogMessage(level = DEBUG)
	@Message(value = "*** Logical connection closed ***", id = 162)
	void logicalConnectionClosed();

	@LogMessage(level = DEBUG)
	@Message(value = "Logical connection releasing its physical connection", id = 163)
	void logicalConnectionReleasingPhysicalConnection();

	@LogMessage(level = INFO)
	@Message(value = "Max query time: %sms", id = 173)
	void maxQueryTime(long queryExecutionMaxTime);

	@LogMessage(level = WARN)
	@Message(value = "Function template anticipated %s arguments, but %s arguments encountered", id = 174)
	void missingArguments(
			int anticipatedNumberOfArguments,
			int numberOfArguments);

	@LogMessage(level = WARN)
	@Message(value = "Class annotated @org.hibernate.annotations.Entity but not javax.persistence.Entity (most likely a user error): %s",
			id = 175)
	void missingEntityAnnotation(String className);


	@LogMessage(level = ERROR)
	@Message(value = "Error in named query: %s", id = 177)
	void namedQueryError(
			String queryName,
			@Cause HibernateException e);

	@LogMessage(level = WARN)
	@Message(value = "Naming exception occurred accessing factory: %s", id = 178)
	void namingExceptionAccessingFactory(NamingException exception);

	@LogMessage(level = WARN)
	@Message(value = "Narrowing proxy to %s - this operation breaks ==", id = 179)
	void narrowingProxy(Class concreteProxyClass);

	@LogMessage(level = WARN)
	@Message(value = "FirstResult/maxResults specified on polymorphic query; applying in memory!", id = 180)
	void needsLimit();

	@LogMessage(level = WARN)
	@Message(value = "No appropriate connection provider encountered, assuming application will be supplying connections",
			id = 181)
	void noAppropriateConnectionProvider();

	@LogMessage(level = INFO)
	@Message(value = "No default (no-argument) constructor for class: %s (class must be instantiated by Interceptor)",
			id = 182)
	void noDefaultConstructor(String name);

	@LogMessage(level = WARN)
	@Message(value = "no persistent classes found for query class: %s", id = 183)
	void noPersistentClassesFound(String query);

	@LogMessage(level = ERROR)
	@Message(value = "No session factory with JNDI name %s", id = 184)
	void noSessionFactoryWithJndiName(
			String sfJNDIName,
			@Cause NameNotFoundException e);

	@LogMessage(level = INFO)
	@Message(value = "Optimistic lock failures: %s", id = 187)
	void optimisticLockFailures(long optimisticFailureCount);

	@LogMessage(level = WARN)
	@Message(value = "@OrderBy not allowed for an indexed collection, annotation ignored.", id = 189)
	void orderByAnnotationIndexedCollection();

	@LogMessage(level = WARN)
	@Message(value = "Overriding %s is dangerous, this might break the EJB3 specification implementation", id = 193)
	void overridingTransactionStrategyDangerous(String transactionStrategy);

	@LogMessage(level = DEBUG)
	@Message(value = "Package not found or wo package-info.java: %s", id = 194)
	void packageNotFound(String packageName);

//	@LogMessage(level = WARN)
//	@Message(value = "Parameter position [%s] occurred as both JPA and Hibernate positional parameter", id = 195)
//	void parameterPositionOccurredAsBothJpaAndHibernatePositionalParameter(Integer position);

	@LogMessage(level = ERROR)
	@Message(value = "Error parsing XML (%s) : %s", id = 196)
	void parsingXmlError(
			int lineNumber,
			String message);

	@LogMessage(level = ERROR)
	@Message(value = "Error parsing XML: %s(%s) %s", id = 197)
	void parsingXmlErrorForFile(
			String file,
			int lineNumber,
			String message);

	@LogMessage(level = ERROR)
	@Message(value = "Warning parsing XML (%s) : %s", id = 198)
	void parsingXmlWarning(
			int lineNumber,
			String message);

	@LogMessage(level = WARN)
	@Message(value = "Warning parsing XML: %s(%s) %s", id = 199)
	void parsingXmlWarningForFile(
			String file,
			int lineNumber,
			String message);

	@LogMessage(level = WARN)
	@Message(value = "Persistence provider caller does not implement the EJB3 spec correctly."
			+ "PersistenceUnitInfo.getNewTempClassLoader() is null.", id = 200)
	void persistenceProviderCallerDoesNotImplementEjb3SpecCorrectly();

	@LogMessage(level = INFO)
	@Message(value = "Pooled optimizer source reported [%s] as the initial value; use of 1 or greater highly recommended",
			id = 201)
	void pooledOptimizerReportedInitialValue(IntegralDataTypeHolder value);

	@LogMessage(level = ERROR)
	@Message(value = "PreparedStatement was already in the batch, [%s].", id = 202)
	void preparedStatementAlreadyInBatch(String sql);

	@LogMessage(level = WARN)
	@Message(value = "processEqualityExpression() : No expression to process!", id = 203)
	void processEqualityExpression();

	@LogMessage(level = INFO)
	@Message(value = "Processing PersistenceUnitInfo [name: %s]", id = 204)
	void processingPersistenceUnitInfoName(String persistenceUnitName);

	@LogMessage(level = INFO)
	@Message(value = "Loaded properties from resource hibernate.properties: %s", id = 205)
	void propertiesLoaded(Properties maskOut);

	@LogMessage(level = DEBUG)
	@Message(value = "hibernate.properties not found", id = 206)
	void propertiesNotFound();

	@LogMessage(level = WARN)
	@Message(value = "Property %s not found in class but described in <mapping-file/> (possible typo error)", id = 207)
	void propertyNotFound(String property);

//	@LogMessage(level = WARN)
//	@Message(value = "%s has been deprecated in favor of %s; that provider will be used instead.", id = 208)
//	void providerClassDeprecated(
//			String providerClassName,
//			String actualProviderClassName);

	@LogMessage(level = WARN)
	@Message(value = "proxool properties were encountered, but the %s provider class was not found on the classpath; these properties are going to be ignored.",
			id = 209)
	void proxoolProviderClassNotFound(String proxoolProviderClassName);

	@LogMessage(level = INFO)
	@Message(value = "Queries executed to database: %s", id = 210)
	void queriesExecuted(long queryExecutionCount);

	@LogMessage(level = INFO)
	@Message(value = "Query cache hits: %s", id = 213)
	void queryCacheHits(long queryCacheHitCount);

	@LogMessage(level = INFO)
	@Message(value = "Query cache misses: %s", id = 214)
	void queryCacheMisses(long queryCacheMissCount);

	@LogMessage(level = INFO)
	@Message(value = "Query cache puts: %s", id = 215)
	void queryCachePuts(long queryCachePutCount);

	@LogMessage(level = INFO)
	@Message(value = "RDMSOS2200Dialect version: 1.0", id = 218)
	void rdmsOs2200Dialect();

	@LogMessage(level = INFO)
	@Message(value = "Reading mappings from cache file: %s", id = 219)
	void readingCachedMappings(File cachedFile);

	@LogMessage(level = INFO)
	@Message(value = "Reading mappings from file: %s", id = 220)
	void readingMappingsFromFile(String path);

	@LogMessage(level = INFO)
	@Message(value = "Reading mappings from resource: %s", id = 221)
	void readingMappingsFromResource(String resourceName);

	@LogMessage(level = WARN)
	@Message(value = "read-only cache configured for mutable collection [%s]", id = 222)
	void readOnlyCacheConfiguredForMutableCollection(String name);

	@LogMessage(level = WARN)
	@Message(value = "Recognized obsolete hibernate namespace %s. Use namespace %s instead. Refer to Hibernate 3.6 Migration Guide!",
			id = 223)
	void recognizedObsoleteHibernateNamespace(
			String oldHibernateNamespace,
			String hibernateNamespace);

	@LogMessage(level = WARN)
	@Message(value = "Property [%s] has been renamed to [%s]; update your properties appropriately", id = 225)
	void renamedProperty(
			Object propertyName,
			Object newPropertyName);

	@LogMessage(level = INFO)
	@Message(value = "Required a different provider: %s", id = 226)
	void requiredDifferentProvider(String provider);

	@LogMessage(level = INFO)
	@Message(value = "Running hbm2ddl schema export", id = 227)
	void runningHbm2ddlSchemaExport();

	@LogMessage(level = INFO)
	@Message(value = "Running hbm2ddl schema update", id = 228)
	void runningHbm2ddlSchemaUpdate();

	@LogMessage(level = INFO)
	@Message(value = "Running schema validator", id = 229)
	void runningSchemaValidator();

	@LogMessage(level = INFO)
	@Message(value = "Schema export complete", id = 230)
	void schemaExportComplete();

	@LogMessage(level = ERROR)
	@Message(value = "Schema export unsuccessful", id = 231)
	void schemaExportUnsuccessful(@Cause Exception e);

	@LogMessage(level = INFO)
	@Message(value = "Schema update complete", id = 232)
	void schemaUpdateComplete();

	@LogMessage(level = WARN)
	@Message(value = "Scoping types to session factory %s after already scoped %s", id = 233)
	void scopingTypesToSessionFactoryAfterAlreadyScoped(
			SessionFactoryImplementor factory,
			SessionFactoryImplementor factory2);

	@LogMessage(level = INFO)
	@Message(value = "Searching for mapping documents in jar: %s", id = 235)
	void searchingForMappingDocuments(String name);

	@LogMessage(level = INFO)
	@Message(value = "Second level cache hits: %s", id = 237)
	void secondLevelCacheHits(long secondLevelCacheHitCount);

	@LogMessage(level = INFO)
	@Message(value = "Second level cache misses: %s", id = 238)
	void secondLevelCacheMisses(long secondLevelCacheMissCount);

	@LogMessage(level = INFO)
	@Message(value = "Second level cache puts: %s", id = 239)
	void secondLevelCachePuts(long secondLevelCachePutCount);

	@LogMessage(level = INFO)
	@Message(value = "Service properties: %s", id = 240)
	void serviceProperties(Properties properties);

	@LogMessage(level = INFO)
	@Message(value = "Sessions closed: %s", id = 241)
	void sessionsClosed(long sessionCloseCount);

	@LogMessage(level = INFO)
	@Message(value = "Sessions opened: %s", id = 242)
	void sessionsOpened(long sessionOpenCount);

	@LogMessage(level = WARN)
	@Message(value = "@Sort not allowed for an indexed collection, annotation ignored.", id = 244)
	void sortAnnotationIndexedCollection();

	@LogMessage(level = WARN)
	@Message(value = "Manipulation query [%s] resulted in [%s] split queries", id = 245)
	void splitQueries(
			String sourceQuery,
			int length);

//	@LogMessage(level = ERROR)
//	@Message(value = "SQLException escaped proxy", id = 246)
//	void sqlExceptionEscapedProxy(@Cause SQLException e);

	@LogMessage(level = WARN)
	@Message(value = "SQL Error: %s, SQLState: %s", id = 247)
	void sqlWarning(
			int errorCode,
			String sqlState);

	@LogMessage(level = INFO)
	@Message(value = "Starting query cache at region: %s", id = 248)
	void startingQueryCache(String region);

	@LogMessage(level = INFO)
	@Message(value = "Starting service at JNDI name: %s", id = 249)
	void startingServiceAtJndiName(String boundName);

	@LogMessage(level = INFO)
	@Message(value = "Starting update timestamps cache at region: %s", id = 250)
	void startingUpdateTimestampsCache(String region);

	@LogMessage(level = INFO)
	@Message(value = "Start time: %s", id = 251)
	void startTime(long startTime);

	@LogMessage(level = INFO)
	@Message(value = "Statements closed: %s", id = 252)
	void statementsClosed(long closeStatementCount);

	@LogMessage(level = INFO)
	@Message(value = "Statements prepared: %s", id = 253)
	void statementsPrepared(long prepareStatementCount);

	@LogMessage(level = INFO)
	@Message(value = "Stopping service", id = 255)
	void stoppingService();

	@LogMessage(level = INFO)
	@Message(value = "sub-resolver threw unexpected exception, continuing to next : %s", id = 257)
	void subResolverException(String message);

	@LogMessage(level = INFO)
	@Message(value = "Successful transactions: %s", id = 258)
	void successfulTransactions(long committedTransactionCount);

	@LogMessage(level = INFO)
	@Message(value = "Synchronization [%s] was already registered", id = 259)
	void synchronizationAlreadyRegistered(Synchronization synchronization);

	@LogMessage(level = ERROR)
	@Message(value = "Exception calling user Synchronization [%s] : %s", id = 260)
	void synchronizationFailed(
			Synchronization synchronization,
			Throwable t);

	@LogMessage(level = INFO)
	@Message(value = "Table found: %s", id = 261)
	void tableFound(String string);

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
	@Message(value = "Transaction started on non-root session", id = 267)
	void transactionStartedOnNonRootSession();

	@LogMessage(level = INFO)
	@Message(value = "Transaction strategy: %s", id = 268)
	void transactionStrategy(String strategyClassName);

	@LogMessage(level = WARN)
	@Message(value = "Type [%s] defined no registration keys; ignoring", id = 269)
	void typeDefinedNoRegistrationKeys(BasicType type);

	@LogMessage(level = DEBUG)
	@Message(value = "Type registration [%s] overrides previous : %s", id = 270)
	void typeRegistrationOverridesPrevious(
			String key,
			Type old);

	@LogMessage(level = WARN)
	@Message(value = "Naming exception occurred accessing Ejb3Configuration", id = 271)
	void unableToAccessEjb3Configuration(@Cause NamingException e);

	@LogMessage(level = ERROR)
	@Message(value = "Error while accessing session factory with JNDI name %s", id = 272)
	void unableToAccessSessionFactory(
			String sfJNDIName,
			@Cause NamingException e);

	@LogMessage(level = WARN)
	@Message(value = "Error accessing type info result set : %s", id = 273)
	void unableToAccessTypeInfoResultSet(String string);

	@LogMessage(level = WARN)
	@Message(value = "Unable to apply constraints on DDL for %s", id = 274)
	void unableToApplyConstraints(
			String className,
			@Cause Exception e);

	@LogMessage(level = WARN)
	@Message(value = "Could not bind Ejb3Configuration to JNDI", id = 276)
	void unableToBindEjb3ConfigurationToJndi(@Cause JndiException e);

	@LogMessage(level = WARN)
	@Message(value = "Could not bind factory to JNDI", id = 277)
	void unableToBindFactoryToJndi(@Cause JndiException e);

	@LogMessage(level = INFO)
	@Message(value = "Could not bind value '%s' to parameter: %s; %s", id = 278)
	void unableToBindValueToParameter(
			String nullSafeToString,
			int index,
			String message);

	@LogMessage(level = ERROR)
	@Message(value = "Unable to build enhancement metamodel for %s", id = 279)
	void unableToBuildEnhancementMetamodel(String className);

	@LogMessage(level = INFO)
	@Message(value = "Could not build SessionFactory using the MBean classpath - will try again using client classpath: %s",
			id = 280)
	void unableToBuildSessionFactoryUsingMBeanClasspath(String message);

	@LogMessage(level = WARN)
	@Message(value = "Unable to clean up callable statement", id = 281)
	void unableToCleanUpCallableStatement(@Cause SQLException e);

	@LogMessage(level = WARN)
	@Message(value = "Unable to clean up prepared statement", id = 282)
	void unableToCleanUpPreparedStatement(@Cause SQLException e);

	@LogMessage(level = WARN)
	@Message(value = "Unable to cleanup temporary id table after use [%s]", id = 283)
	void unableToCleanupTemporaryIdTable(Throwable t);

	@LogMessage(level = ERROR)
	@Message(value = "Error closing connection", id = 284)
	void unableToCloseConnection(@Cause Exception e);

	@LogMessage(level = INFO)
	@Message(value = "Error closing InitialContext [%s]", id = 285)
	void unableToCloseInitialContext(String string);

	@LogMessage(level = ERROR)
	@Message(value = "Error closing input files: %s", id = 286)
	void unableToCloseInputFiles(
			String name,
			@Cause IOException e);

	@LogMessage(level = WARN)
	@Message(value = "Could not close input stream", id = 287)
	void unableToCloseInputStream(@Cause IOException e);

	@LogMessage(level = WARN)
	@Message(value = "Could not close input stream for %s", id = 288)
	void unableToCloseInputStreamForResource(
			String resourceName,
			@Cause IOException e);

	@LogMessage(level = INFO)
	@Message(value = "Unable to close iterator", id = 289)
	void unableToCloseIterator(@Cause SQLException e);

	@LogMessage(level = ERROR)
	@Message(value = "Could not close jar: %s", id = 290)
	void unableToCloseJar(String message);

	@LogMessage(level = ERROR)
	@Message(value = "Error closing output file: %s", id = 291)
	void unableToCloseOutputFile(
			String outputFile,
			@Cause IOException e);

	@LogMessage(level = WARN)
	@Message(value = "IOException occurred closing output stream", id = 292)
	void unableToCloseOutputStream(@Cause IOException e);

	@LogMessage(level = ERROR)
	@Message(value = "Could not close session", id = 294)
	void unableToCloseSession(@Cause HibernateException e);

	@LogMessage(level = ERROR)
	@Message(value = "Could not close session during rollback", id = 295)
	void unableToCloseSessionDuringRollback(@Cause Exception e);

	@LogMessage(level = WARN)
	@Message(value = "IOException occurred closing stream", id = 296)
	void unableToCloseStream(@Cause IOException e);

	@LogMessage(level = ERROR)
	@Message(value = "Could not close stream on hibernate.properties: %s", id = 297)
	void unableToCloseStreamError(IOException error);

	@Message(value = "JTA commit failed", id = 298)
	String unableToCommitJta();

	@LogMessage(level = ERROR)
	@Message(value = "Could not complete schema update", id = 299)
	void unableToCompleteSchemaUpdate(@Cause Exception e);

	@LogMessage(level = ERROR)
	@Message(value = "Could not complete schema validation", id = 300)
	void unableToCompleteSchemaValidation(@Cause SQLException e);

	@LogMessage(level = WARN)
	@Message(value = "Unable to configure SQLExceptionConverter : %s", id = 301)
	void unableToConfigureSqlExceptionConverter(HibernateException e);

	@LogMessage(level = ERROR)
	@Message(value = "Unable to construct current session context [%s]", id = 302)
	void unableToConstructCurrentSessionContext(
			String impl,
			@Cause Throwable e);

	@LogMessage(level = WARN)
	@Message(value = "Unable to construct instance of specified SQLExceptionConverter : %s", id = 303)
	void unableToConstructSqlExceptionConverter(Throwable t);

	@LogMessage(level = WARN)
	@Message(value = "Could not copy system properties, system properties will be ignored", id = 304)
	void unableToCopySystemProperties();

	@LogMessage(level = WARN)
	@Message(value = "Could not create proxy factory for:%s", id = 305)
	void unableToCreateProxyFactory(
			String entityName,
			@Cause HibernateException e);

	@LogMessage(level = ERROR)
	@Message(value = "Error creating schema ", id = 306)
	void unableToCreateSchema(@Cause Exception e);

	@LogMessage(level = WARN)
	@Message(value = "Could not deserialize cache file: %s : %s", id = 307)
	void unableToDeserializeCache(
			String path,
			SerializationException error);

	@LogMessage(level = WARN)
	@Message(value = "Unable to destroy cache: %s", id = 308)
	void unableToDestroyCache(String message);

	@LogMessage(level = WARN)
	@Message(value = "Unable to destroy query cache: %s: %s", id = 309)
	void unableToDestroyQueryCache(
			String region,
			String message);

	@LogMessage(level = WARN)
	@Message(value = "Unable to destroy update timestamps cache: %s: %s", id = 310)
	void unableToDestroyUpdateTimestampsCache(
			String region,
			String message);

	@LogMessage(level = INFO)
	@Message(value = "Unable to determine lock mode value : %s -> %s", id = 311)
	void unableToDetermineLockModeValue(
			String hintName,
			Object value);

	@Message(value = "Could not determine transaction status", id = 312)
	String unableToDetermineTransactionStatus();

	@Message(value = "Could not determine transaction status after commit", id = 313)
	String unableToDetermineTransactionStatusAfterCommit();

	@LogMessage(level = WARN)
	@Message(value = "Unable to evictData temporary id table after use [%s]", id = 314)
	void unableToDropTemporaryIdTable(String message);

	@LogMessage(level = ERROR)
	@Message(value = "Exception executing batch [%s], SQL: %s", id = 315)
	void unableToExecuteBatch(Exception e, String sql );

	@LogMessage(level = WARN)
	@Message(value = "Error executing resolver [%s] : %s", id = 316)
	void unableToExecuteResolver(
			DialectResolver abstractDialectResolver,
			String message);

	@LogMessage(level = INFO)
	@Message(value = "Could not find any META-INF/persistence.xml file in the classpath", id = 318)
	void unableToFindPersistenceXmlInClasspath();

	@LogMessage(level = ERROR)
	@Message(value = "Could not get database metadata", id = 319)
	void unableToGetDatabaseMetadata(@Cause SQLException e);

	@LogMessage(level = WARN)
	@Message(value = "Unable to instantiate configured schema name resolver [%s] %s", id = 320)
	void unableToInstantiateConfiguredSchemaNameResolver(
			String resolverClassName,
			String message);

	@LogMessage(level = WARN)
	@Message(value = "Unable to interpret specified optimizer [%s], falling back to noop", id = 321)
	void unableToLocateCustomOptimizerClass(String type);

	@LogMessage(level = WARN)
	@Message(value = "Unable to instantiate specified optimizer [%s], falling back to noop", id = 322)
	void unableToInstantiateOptimizer(String type);

	@LogMessage(level = WARN)
	@Message(value = "Unable to instantiate UUID generation strategy class : %s", id = 325)
	void unableToInstantiateUuidGenerationStrategy(Exception ignore);

	@LogMessage(level = WARN)
	@Message(value = "Cannot join transaction: do not override %s", id = 326)
	void unableToJoinTransaction(String transactionStrategy);

	@LogMessage(level = INFO)
	@Message(value = "Error performing load command", id = 327)
	void unableToLoadCommand(@Cause HibernateException e);

	@LogMessage(level = WARN)
	@Message(value = "Unable to load/access derby driver class sysinfo to check versions : %s", id = 328)
	void unableToLoadDerbyDriver(String message);

	@LogMessage(level = ERROR)
	@Message(value = "Problem loading properties from hibernate.properties", id = 329)
	void unableToLoadProperties();

	@Message(value = "Unable to locate config file: %s", id = 330)
	String unableToLocateConfigFile(String path);

	@LogMessage(level = WARN)
	@Message(value = "Unable to locate configured schema name resolver class [%s] %s", id = 331)
	void unableToLocateConfiguredSchemaNameResolver(
			String resolverClassName,
			String message);

	@LogMessage(level = WARN)
	@Message(value = "Unable to locate MBeanServer on JMX service shutdown", id = 332)
	void unableToLocateMBeanServer();

	@LogMessage(level = WARN)
	@Message(value = "Unable to locate requested UUID generation strategy class : %s", id = 334)
	void unableToLocateUuidGenerationStrategy(String strategyClassName);

	@LogMessage(level = WARN)
	@Message(value = "Unable to log SQLWarnings : %s", id = 335)
	void unableToLogSqlWarnings(SQLException sqle);

	@LogMessage(level = WARN)
	@Message(value = "Could not log warnings", id = 336)
	void unableToLogWarnings(@Cause SQLException e);

	@LogMessage(level = ERROR)
	@Message(value = "Unable to mark for rollback on PersistenceException: ", id = 337)
	void unableToMarkForRollbackOnPersistenceException(@Cause Exception e);

	@LogMessage(level = ERROR)
	@Message(value = "Unable to mark for rollback on TransientObjectException: ", id = 338)
	void unableToMarkForRollbackOnTransientObjectException(@Cause Exception e);

	@LogMessage(level = WARN)
	@Message(value = "Could not obtain connection metadata: %s", id = 339)
	void unableToObjectConnectionMetadata(SQLException error);

	@LogMessage(level = WARN)
	@Message(value = "Could not obtain connection to query metadata: %s", id = 340)
	void unableToObjectConnectionToQueryMetadata(SQLException error);

	@LogMessage(level = WARN)
	@Message(value = "Could not obtain connection metadata : %s", id = 341)
	void unableToObtainConnectionMetadata(String message);

	@LogMessage(level = WARN)
	@Message(value = "Could not obtain connection to query metadata : %s", id = 342)
	void unableToObtainConnectionToQueryMetadata(String message);

	@LogMessage(level = ERROR)
	@Message(value = "Could not obtain initial context", id = 343)
	void unableToObtainInitialContext(@Cause NamingException e);

	@LogMessage(level = ERROR)
	@Message(value = "Could not parse the package-level metadata [%s]", id = 344)
	void unableToParseMetadata(String packageName);

	@Message(value = "JDBC commit failed", id = 345)
	String unableToPerformJdbcCommit();

	@LogMessage(level = ERROR)
	@Message(value = "Error during managed flush [%s]", id = 346)
	void unableToPerformManagedFlush(String message);

	@Message(value = "Unable to query java.sql.DatabaseMetaData", id = 347)
	String unableToQueryDatabaseMetadata();

	@LogMessage(level = ERROR)
	@Message(value = "Unable to read class: %s", id = 348)
	void unableToReadClass(String message);

	@LogMessage(level = INFO)
	@Message(value = "Could not read column value from result set: %s; %s", id = 349)
	void unableToReadColumnValueFromResultSet(
			String name,
			String message);

	@Message(value = "Could not read a hi value - you need to populate the table: %s", id = 350)
	String unableToReadHiValue(String tableName);

	@LogMessage(level = ERROR)
	@Message(value = "Could not read or init a hi value", id = 351)
	void unableToReadOrInitHiValue(@Cause SQLException e);

	@LogMessage(level = ERROR)
	@Message(value = "Unable to release batch statement...", id = 352)
	void unableToReleaseBatchStatement();

	@LogMessage(level = ERROR)
	@Message(value = "Could not release a cache lock : %s", id = 353)
	void unableToReleaseCacheLock(CacheException ce);

	@LogMessage(level = INFO)
	@Message(value = "Unable to release initial context: %s", id = 354)
	void unableToReleaseContext(String message);

	@LogMessage(level = WARN)
	@Message(value = "Unable to release created MBeanServer : %s", id = 355)
	void unableToReleaseCreatedMBeanServer(String string);

	@LogMessage(level = INFO)
	@Message(value = "Unable to release isolated connection [%s]", id = 356)
	void unableToReleaseIsolatedConnection(Throwable ignore);

	@LogMessage(level = WARN)
	@Message(value = "Unable to release type info result set", id = 357)
	void unableToReleaseTypeInfoResultSet();

	@LogMessage(level = WARN)
	@Message(value = "Unable to erase previously added bag join fetch", id = 358)
	void unableToRemoveBagJoinFetch();

	@LogMessage(level = INFO)
	@Message(value = "Could not resolve aggregate function [%s]; using standard definition", id = 359)
	void unableToResolveAggregateFunction(String name);

	@LogMessage(level = INFO)
	@Message(value = "Unable to resolve mapping file [%s]", id = 360)
	void unableToResolveMappingFile(String xmlFile);

	@LogMessage(level = INFO)
	@Message(value = "Unable to retrieve cache from JNDI [%s]: %s", id = 361)
	void unableToRetrieveCache(
			String namespace,
			String message);

	@LogMessage(level = WARN)
	@Message(value = "Unable to retrieve type info result set : %s", id = 362)
	void unableToRetrieveTypeInfoResultSet(String string);

	@LogMessage(level = INFO)
	@Message(value = "Unable to rollback connection on exception [%s]", id = 363)
	void unableToRollbackConnection(Exception ignore);

	@LogMessage(level = INFO)
	@Message(value = "Unable to rollback isolated transaction on error [%s] : [%s]", id = 364)
	void unableToRollbackIsolatedTransaction(
			Exception e,
			Exception ignore);

	@Message(value = "JTA rollback failed", id = 365)
	String unableToRollbackJta();

	@LogMessage(level = ERROR)
	@Message(value = "Error running schema update", id = 366)
	void unableToRunSchemaUpdate(@Cause Exception e);

	@LogMessage(level = ERROR)
	@Message(value = "Could not set transaction to rollback only", id = 367)
	void unableToSetTransactionToRollbackOnly(@Cause SystemException e);

	@LogMessage(level = WARN)
	@Message(value = "Exception while stopping service", id = 368)
	void unableToStopHibernateService(@Cause Exception e);

	@LogMessage(level = INFO)
	@Message(value = "Error stopping service [%s]", id = 369)
	void unableToStopService(
			Class class1,
			@Cause Exception e);

	@LogMessage(level = WARN)
	@Message(value = "Exception switching from method: [%s] to a method using the column index. Reverting to using: [%<s]",
			id = 370)
	void unableToSwitchToMethodUsingColumnIndex(Method method);

	@LogMessage(level = ERROR)
	@Message(value = "Could not synchronize database state with session: %s", id = 371)
	void unableToSynchronizeDatabaseStateWithSession(HibernateException he);

	@LogMessage(level = ERROR)
	@Message(value = "Could not toggle autocommit", id = 372)
	void unableToToggleAutoCommit(@Cause Exception e);

	@LogMessage(level = ERROR)
	@Message(value = "Unable to transform class: %s", id = 373)
	void unableToTransformClass(String message);

	@LogMessage(level = WARN)
	@Message(value = "Could not unbind factory from JNDI", id = 374)
	void unableToUnbindFactoryFromJndi(@Cause JndiException e);

	@Message(value = "Could not update hi value in: %s", id = 375)
	String unableToUpdateHiValue(String tableName);

	@LogMessage(level = ERROR)
	@Message(value = "Could not updateQuery hi value in: %s", id = 376)
	void unableToUpdateQueryHiValue(
			String tableName,
			@Cause SQLException e);

	@LogMessage(level = INFO)
	@Message(value = "Error wrapping result set", id = 377)
	void unableToWrapResultSet(@Cause SQLException e);

	@LogMessage(level = WARN)
	@Message(value = "I/O reported error writing cached file : %s: %s", id = 378)
	void unableToWriteCachedFile(
			String path,
			String message);

	@LogMessage(level = WARN)
	@Message(value = "Unexpected literal token type [%s] passed for numeric processing", id = 380)
	void unexpectedLiteralTokenType(int type);

	@LogMessage(level = WARN)
	@Message(value = "JDBC driver did not return the expected number of row counts", id = 381)
	void unexpectedRowCounts();

	@LogMessage(level = WARN)
	@Message(value = "unrecognized bytecode provider [%s], using [%s] by default", id = 382)
	void unknownBytecodeProvider(String providerName, String defaultProvider);

	@LogMessage(level = WARN)
	@Message(value = "Unknown Ingres major version [%s]; using Ingres 9.2 dialect", id = 383)
	void unknownIngresVersion(int databaseMajorVersion);

	@LogMessage(level = WARN)
	@Message(value = "Unknown Oracle major version [%s]", id = 384)
	void unknownOracleVersion(int databaseMajorVersion);

	@LogMessage(level = WARN)
	@Message(value = "Unknown Microsoft SQL Server major version [%s] using [%s] dialect", id = 385)
	void unknownSqlServerVersion(int databaseMajorVersion, Class<? extends Dialect> dialectClass);

	@LogMessage(level = WARN)
	@Message(value = "ResultSet had no statement associated with it, but was not yet registered", id = 386)
	void unregisteredResultSetWithoutStatement();

	// Keep this at DEBUG level, rather than warn.  Numerous connection pool implementations can return a
	// proxy/wrapper around the JDBC Statement, causing excessive logging here.  See HHH-8210.
	@LogMessage(level = DEBUG)
	@Message(value = "ResultSet's statement was not registered", id = 387)
	void unregisteredStatement();

	@LogMessage(level = ERROR)
	@Message(value = "Unsuccessful: %s", id = 388)
	void unsuccessful(String sql);

	/**
	 * @deprecated Use {@link #unsuccessfulSchemaManagementCommand} instead
	 */
	@LogMessage(level = ERROR)
	@Message(value = "Unsuccessful: %s", id = 389)
	@Deprecated
	void unsuccessfulCreate(String string);

	@LogMessage(level = WARN)
	@Message(value = "Overriding release mode as connection provider does not support 'after_statement'", id = 390)
	void unsupportedAfterStatement();

	@LogMessage(level = WARN)
	@Message(value = "Ingres 10 is not yet fully supported; using Ingres 9.3 dialect", id = 391)
	void unsupportedIngresVersion();

	@LogMessage(level = WARN)
	@Message(value = "Hibernate does not support SequenceGenerator.initialValue() unless '%s' set", id = 392)
	void unsupportedInitialValue(String propertyName);

	@LogMessage(level = WARN)
	@Message(value = "The %s.%s.%s version of H2 implements temporary table creation such that it commits current transaction; multi-table, bulk hql/jpaql will not work properly",
			id = 393)
	void unsupportedMultiTableBulkHqlJpaql(
			int majorVersion,
			int minorVersion,
			int buildId);

	@LogMessage(level = WARN)
	@Message(value = "Oracle 11g is not yet fully supported; using Oracle 10g dialect", id = 394)
	void unsupportedOracleVersion();

	@LogMessage(level = INFO)
	@Message(value = "Updating schema", id = 396)
	void updatingSchema();

	@LogMessage(level = INFO)
	@Message(value = "Explicit segment value for id generator [%s.%s] suggested; using default [%s]", id = 398)
	void usingDefaultIdGeneratorSegmentValue(
			String tableName,
			String segmentColumnName,
			String defaultToUse);

	@LogMessage(level = INFO)
	@Message(value = "Using default transaction strategy (direct JDBC transactions)", id = 399)
	void usingDefaultTransactionStrategy();

	@LogMessage(level = INFO)
	@Message(value = "Using dialect: %s", id = 400)
	void usingDialect(Dialect dialect);

	@LogMessage(level = ERROR)
	@Message(value = "Don't use old DTDs, read the Hibernate 3.x Migration Guide!", id = 404)
	void usingOldDtd();

	@LogMessage(level = INFO)
	@Message(value = "Using bytecode reflection optimizer", id = 406)
	void usingReflectionOptimizer();

	@LogMessage(level = INFO)
	@Message(value = "Using java.io streams to persist binary types", id = 407)
	void usingStreams();

	@LogMessage(level = WARN)
	@Message(value = "Using %s which does not generate IETF RFC 4122 compliant UUID values; consider using %s instead",
			id = 409)
	void usingUuidHexGenerator(
			String name,
			String name2);

	@LogMessage(level = INFO)
	@Message(value = "Hibernate Validator not found: ignoring", id = 410)
	void validatorNotFound();

	@LogMessage(level = INFO)
	@Message(value = "Hibernate ORM core version %s", id = 412)
	void version(String versionString);

	@LogMessage(level = WARN)
	@Message(value = "Warnings creating temp table : %s", id = 413)
	void warningsCreatingTempTable(SQLWarning warning);

	@LogMessage(level = INFO)
	@Message(value = "Property hibernate.search.autoregister_listeners is set to false. No attempt will be made to register Hibernate Search event listeners.",
			id = 414)
	void willNotRegisterListeners();

	@LogMessage(level = WARN)
	@Message(value = "Write locks via update not supported for non-versioned entities [%s]", id = 416)
	void writeLocksNotSupported(String entityName);

	@LogMessage(level = INFO)
	@Message(value = "Writing generated schema to file: %s", id = 417)
	void writingGeneratedSchemaToFile(String outputFile);

	@LogMessage(level = INFO)
	@Message(value = "Adding override for %s: %s", id = 418)
	void addingOverrideFor(
			String name,
			String name2);

	@LogMessage(level = WARN)
	@Message(value = "Resolved SqlTypeDescriptor is for a different SQL code. %s has sqlCode=%s; type override %s has sqlCode=%s",
			id = 419)
	void resolvedSqlTypeDescriptorForDifferentSqlCode(
			String name,
			String valueOf,
			String name2,
			String valueOf2);

	@LogMessage(level = DEBUG)
	@Message(value = "Closing un-released batch", id = 420)
	void closingUnreleasedBatch();

	@LogMessage(level = DEBUG)
	@Message(value = "Disabling contextual LOB creation as %s is true", id = 421)
	void disablingContextualLOBCreation(String nonContextualLobCreation);

	@LogMessage(level = DEBUG)
	@Message(value = "Disabling contextual LOB creation as connection was null", id = 422)
	void disablingContextualLOBCreationSinceConnectionNull();

	@LogMessage(level = DEBUG)
	@Message(value = "Disabling contextual LOB creation as JDBC driver reported JDBC version [%s] less than 4",
			id = 423)
	void disablingContextualLOBCreationSinceOldJdbcVersion(int jdbcMajorVersion);

	@LogMessage(level = DEBUG)
	@Message(value = "Disabling contextual LOB creation as createClob() method threw error : %s", id = 424)
	void disablingContextualLOBCreationSinceCreateClobFailed(Throwable t);

	@LogMessage(level = INFO)
	@Message(value = "Could not close session; swallowing exception[%s] as transaction completed", id = 425)
	void unableToCloseSessionButSwallowingError(HibernateException e);

	@LogMessage(level = WARN)
	@Message(value = "You should set hibernate.transaction.jta.platform if cache is enabled", id = 426)
	void setManagerLookupClass();

//	@LogMessage(level = WARN)
//	@Message(value = "Using deprecated %s strategy [%s], use newer %s strategy instead [%s]", id = 427)
//	void deprecatedTransactionManagerStrategy(String name,
//											  String transactionManagerStrategy,
//											  String name2,
//											  String jtaPlatform);

	@LogMessage(level = INFO)
	@Message(value = "Encountered legacy TransactionManagerLookup specified; convert to newer %s contract specified via %s setting",
			id = 428)
	void legacyTransactionManagerStrategy(
			String name,
			String jtaPlatform);

	@LogMessage(level = WARN)
	@Message(value = "Setting entity-identifier value binding where one already existed : %s.", id = 429)
	void entityIdentifierValueBindingExists(String name);

	@LogMessage(level = WARN)
	@Message(value = "The DerbyDialect dialect has been deprecated; use one of the version-specific dialects instead",
			id = 430)
	void deprecatedDerbyDialect();

	@LogMessage(level = WARN)
	@Message(value = "Unable to determine H2 database version, certain features may not work", id = 431)
	void undeterminedH2Version();

	@LogMessage(level = WARN)
	@Message(value = "There were not column names specified for index %s on table %s", id = 432)
	void noColumnsSpecifiedForIndex(String indexName, String tableName);

	@LogMessage(level = INFO)
	@Message(value = "update timestamps cache puts: %s", id = 433)
	void timestampCachePuts(long updateTimestampsCachePutCount);

	@LogMessage(level = INFO)
	@Message(value = "update timestamps cache hits: %s", id = 434)
	void timestampCacheHits(long updateTimestampsCachePutCount);

	@LogMessage(level = INFO)
	@Message(value = "update timestamps cache misses: %s", id = 435)
	void timestampCacheMisses(long updateTimestampsCachePutCount);

	@LogMessage(level = WARN)
	@Message(value = "Entity manager factory name (%s) is already registered.  If entity manager will be clustered " +
			"or passivated, specify a unique value for property '%s'", id = 436)
	void entityManagerFactoryAlreadyRegistered(String emfName, String propertyName);

	@LogMessage(level = WARN)
	@Message(value = "Attempting to save one or more entities that have a non-nullable association with an unsaved transient entity. The unsaved transient entity must be saved in an operation prior to saving these dependent entities.\n" +
			"\tUnsaved transient entity: (%s)\n\tDependent entities: (%s)\n\tNon-nullable association(s): (%s)", id = 437)
	void cannotResolveNonNullableTransientDependencies(
			String transientEntityString,
			Set<String> dependentEntityStrings,
			Set<String> nonNullableAssociationPaths);

	@LogMessage(level = INFO)
	@Message(value = "NaturalId cache puts: %s", id = 438)
	void naturalIdCachePuts(long naturalIdCachePutCount);

	@LogMessage(level = INFO)
	@Message(value = "NaturalId cache hits: %s", id = 439)
	void naturalIdCacheHits(long naturalIdCacheHitCount);

	@LogMessage(level = INFO)
	@Message(value = "NaturalId cache misses: %s", id = 440)
	void naturalIdCacheMisses(long naturalIdCacheMissCount);

	@LogMessage(level = INFO)
	@Message(value = "Max NaturalId query time: %sms", id = 441)
	void naturalIdMaxQueryTime(long naturalIdQueryExecutionMaxTime);

	@LogMessage(level = INFO)
	@Message(value = "NaturalId queries executed to database: %s", id = 442)
	void naturalIdQueriesExecuted(long naturalIdQueriesExecutionCount);

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

	/**
	 * @see org.hibernate.internal.log.DeprecationLogger#logDeprecationOfEmbedXmlSupport()
	 */
	@LogMessage(level = WARN)
	@Message(
			value = "embed-xml attributes were intended to be used for DOM4J entity mode. Since that entity mode has been " +
					"removed, embed-xml attributes are no longer supported and should be removed from mappings.",
			id = 446
	)
	void embedXmlAttributesNoLongerSupported();

	@LogMessage(level = WARN)
	@Message(
			value = "Explicit use of UPGRADE_SKIPLOCKED in lock() calls is not recommended; use normal UPGRADE locking instead",
			id = 447
	)
	void explicitSkipLockedLockCombo();

	@LogMessage(level = INFO)
	@Message(value = "'javax.persistence.validation.mode' named multiple values : %s", id = 448)
	void multipleValidationModes(String modes);

	@LogMessage(level = WARN)
	@Message(
			id = 449,
			value = "@Convert annotation applied to Map attribute [%s] did not explicitly specify attributeName " +
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

	@LogMessage(level = WARN)
	@Message(value = "Exception while loading a class or resource found during scanning", id = 452)
	void unableToLoadScannedClassOrResource(@Cause Exception e);

	@LogMessage(level = WARN)
	@Message(value = "Exception while discovering OSGi service implementations : %s", id = 453)
	void unableToDiscoverOsgiService(String service, @Cause Exception e);

	/**
	 * @deprecated Use {@link org.hibernate.internal.log.DeprecationLogger#deprecatedManyToManyOuterJoin} instead
	 */
	@Deprecated
	@LogMessage(level = WARN)
	@Message(value = "The outer-join attribute on <many-to-many> has been deprecated. Instead of outer-join=\"false\", use lazy=\"extra\" with <map>, <set>, <bag>, <idbag>, or <list>, which will only initialize entities (not as a proxy) as needed.", id = 454)
	void deprecatedManyToManyOuterJoin();

	/**
	 * @deprecated Use {@link org.hibernate.internal.log.DeprecationLogger#deprecatedManyToManyFetch} instead
	 */
	@Deprecated
	@LogMessage(level = WARN)
	@Message(value = "The fetch attribute on <many-to-many> has been deprecated. Instead of fetch=\"select\", use lazy=\"extra\" with <map>, <set>, <bag>, <idbag>, or <list>, which will only initialize entities (not as a proxy) as needed.", id = 455)
	void deprecatedManyToManyFetch();

	@LogMessage(level = WARN)
	@Message(value = "Named parameters are used for a callable statement, but database metadata indicates named parameters are not supported.", id = 456)
	void unsupportedNamedParameters();

	@LogMessage(level = WARN)
	@Message(
			id = 457,
			value = "Joined inheritance hierarchy [%1$s] defined explicit @DiscriminatorColumn.  Legacy Hibernate behavior " +
					"was to ignore the @DiscriminatorColumn.  However, as part of issue HHH-6911 we now apply the " +
					"explicit @DiscriminatorColumn.  If you would prefer the legacy behavior, enable the `%2$s` setting " +
					"(%2$s=true)"
	)
	void applyingExplicitDiscriminatorColumnForJoined(String className, String overrideSetting);

	// 458-466 reserved for use by master (ORM 5.0.0)

	@LogMessage(level = DEBUG)
	@Message(value = "Creating pooled optimizer (lo) with [incrementSize=%s; returnClass=%s]", id = 467)
	void creatingPooledLoOptimizer(int incrementSize, String name);

	@LogMessage(level = WARN)
	@Message(value = "Unable to interpret type [%s] as an AttributeConverter due to an exception : %s", id = 468)
	void logBadHbmAttributeConverterType(String type, String exceptionMessage);

	@Message(value = "The ClassLoaderService can not be reused. This instance was stopped already.", id = 469)
	HibernateException usingStoppedClassLoaderService();

	@LogMessage(level = WARN)
	@Message(value = "An unexpected session is defined for a collection, but the collection is not connected to that session. A persistent collection may only be associated with one session at a time. Overwriting session. %s", id = 470)
	void logUnexpectedSessionInCollectionNotConnected(String msg);

	@LogMessage(level = WARN)
	@Message(value = "Cannot unset session in a collection because an unexpected session is defined. A persistent collection may only be associated with one session at a time. %s", id = 471 )
	void logCannotUnsetUnexpectedSessionInCollection(String msg);

	@LogMessage(level = WARN)
	@Message(value = "Hikari properties were encountered, but the Hikari ConnectionProvider was not found on the classpath; these properties are going to be ignored.",
			id = 472)
	void hikariProviderClassNotFound();

	@LogMessage(level = INFO)
	@Message(value = "Omitting cached file [%s] as the mapping file is newer", id = 473)
	void cachedFileObsolete(File cachedFile);

	@Message(
			value = "Ambiguous persistent property methods detected on %s; mark one as @Transient : [%s] and [%s]",
			id = 474
	)
	String ambiguousPropertyMethods(String entityName, String oneMethodSig, String secondMethodSig);

	@LogMessage(level = INFO)
	@Message(value = "Cannot locate column information using identifier [%s]; ignoring index [%s]", id = 475 )
	void logCannotLocateIndexColumnInformation(String columnIdentifierText, String indexIdentifierText);

	@LogMessage(level = INFO)
	@Message(value = "Executing import script '%s'", id = 476)
	void executingImportScript(String scriptName);

	@LogMessage(level = INFO)
	@Message(value = "Starting delayed evictData of schema as part of SessionFactory shut-down'", id = 477)
	void startingDelayedSchemaDrop();

	@LogMessage(level = ERROR)
	@Message(value = "Unsuccessful: %s", id = 478)
	void unsuccessfulSchemaManagementCommand(String command);

	@Message(
			value = "Collection [%s] was not processed by flush()."
			+ " This is likely due to unsafe use of the session (e.g. used in multiple threads concurrently, updates during entity lifecycle hooks).",
			id = 479
	)
	String collectionNotProcessedByFlush(String role);

	@LogMessage(level = WARN)
	@Message(value = "A ManagedEntity was associated with a stale PersistenceContext. A ManagedEntity may only be associated with one PersistenceContext at a time; %s", id = 480)
	void stalePersistenceContextInEntityEntry(String msg);

	@LogMessage(level = WARN)
	@Message(
			id = 481,
			value = "Encountered Java type [%s] for which we could not locate a JavaTypeDescriptor and " +
					"which does not appear to implement equals and/or hashCode.  This can lead to " +
					"significant performance problems when performing equality/dirty checking involving " +
					"this Java type.  Consider registering a custom JavaTypeDescriptor or at least " +
					"implementing equals/hashCode."
	)
	void unknownJavaTypeNoEqualsHashCode(Class javaType);

	@LogMessage(level = WARN)
	@Message(value = "@org.hibernate.annotations.Cache used on a non-root entity: ignored for [%s]. Please see the Hibernate documentation for proper usage.", id = 482)
	void cacheOrCacheableAnnotationOnNonRoot(String className);

	@LogMessage(level = WARN)
	@Message(
			id = 483,
			value = "An experimental feature has been enabled (" +
					AvailableSettings.CREATE_EMPTY_COMPOSITES_ENABLED +
					"=true) that instantiates empty composite/embedded " +
					"objects when all of its attribute values are null. This feature has known issues and " +
					"should not be used in production until it is stabilized. See Hibernate Jira " +
					"issue HHH-11936 for details."
	)
	void emptyCompositesEnabled();

	@LogMessage(level = WARN)
	@Message(value = "Vibur properties were encountered, but the Vibur ConnectionProvider was not found on the classpath; these properties are going to be ignored.",
			id = 484)
	void viburProviderClassNotFound();

	@LogMessage(level = ERROR)
	@Message(value = "Illegally attempted to associate a proxy for entity [%s] with id [%s] with two open sessions.", id = 485)
	void attemptToAssociateProxyWithTwoOpenSessions(
			String entityName,
			Object id
	);

	@LogMessage(level = WARN)
	@Message(value = "Agroal properties were encountered, but the Agroal ConnectionProvider was not found on the classpath; these properties are going to be ignored.",
			id = 486)
	void agroalProviderClassNotFound();

	@LogMessage(level = WARN)
	@Message(value = "The query: [%s] attempts to update an immutable entity: %s",
			id = 487)
	void immutableEntityUpdateQuery(String sourceQuery, String querySpaces);

	@Message(value = "Bytecode enhancement failed for class: %1$s. It might be due to the Java module system preventing Hibernate ORM from defining an enhanced class "
			+ "in the same package as class %1$s. In this case, the class should be opened and exported to Hibernate ORM.", id = 488)
	String bytecodeEnhancementFailedUnableToGetPrivateLookupFor(String className);

	@LogMessage(level = WARN)
	@Message(value = "Setting " + AvailableSettings.NATIVE_EXCEPTION_HANDLING_51_COMPLIANCE + "=true is not valid with JPA bootstrapping; setting will be ignored.", id = 489 )
	void nativeExceptionHandling51ComplianceJpaBootstrapping();

	@LogMessage(level = INFO)
	@Message(value = "Using JtaPlatform implementation: [%s]", id = 490)
	void usingJtaPlatform(String jtaPlatformClassName);

	@LogMessage(level = WARN)
	@Message(value = "The [%2$s] association in the [%1$s] entity uses both @NotFound(action = NotFoundAction.IGNORE) and FetchType.LAZY. " +
			"The NotFoundAction.IGNORE @ManyToOne and @OneToOne associations are always fetched eagerly.", id = 491)
	void ignoreNotFoundWithFetchTypeLazy(String entity, String association);

	@LogMessage(level = INFO)
	@Message(value = "Query plan cache hits: %s", id = 492)
	void queryPlanCacheHits(long queryPlanCacheHitCount);

	@LogMessage(level = INFO)
	@Message(value = "Query plan cache misses: %s", id = 493)
	void queryPlanCacheMisses(long queryPlanCacheMissCount);

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
	@Message(value = "Using @AttributeOverride or @AttributeOverrides in conjunction with entity inheritance is not supported: %s. The overriding definitions are ignored.", id = 499)
	void unsupportedAttributeOverrideWithEntityInheritance(String entityName);

	/* 6.0 message loggers
	 @LogMessage(level = WARN)
	 @Message(value = "The bytecode provider class [%s] could not be loaded", id = 500)
	 void bytecodeProviderClassNotFound(String className);

	 @LogMessage(level = WARN)
	 @Message(value = "The bytecode provider class [%s] does not implement BytecodeProvider", id = 501)
	 void bytecodeProviderInvalidClass(String className);
	 */

	@LogMessage(level = WARN)
	@Message(value = "The [%s] property of the [%s] entity was modified, but it won't be updated because the property is immutable.", id = 502)
	void ignoreImmutablePropertyModification(String propertyName, String entityName);

	@LogMessage(level = WARN)
	@Message(value = "A class should not be annotated with both @Inheritance and @MappedSuperclass. @Inheritance will be ignored for: %s.", id = 503)
	void unsupportedMappedSuperclassWithEntityInheritance(String entityName);

	@LogMessage(level = WARN)
	@Message(value = "Multiple configuration properties defined to create schema. Choose at most one among 'javax.persistence.create-database-schemas', 'hibernate.hbm2ddl.create_namespaces', 'hibernate.hbm2dll.create_namespaces' (this last being deprecated).", id = 504)
	void multipleSchemaCreationSettingsDefined();

	@LogMessage(level = WARN)
	@Message(value = "Ignoring ServiceConfigurationError caught while trying to instantiate service '%s'.", id = 505)
	void ignoringServiceConfigurationError(Class<?> serviceContract, @Cause ServiceConfigurationError error);

}
