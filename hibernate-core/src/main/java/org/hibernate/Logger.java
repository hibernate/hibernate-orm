/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.hibernate;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.naming.InvalidNameException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import org.hibernate.cache.CacheException;
import org.hibernate.cfg.AccessType;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.CollectionKey;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.loading.CollectionLoadContext;
import org.hibernate.engine.loading.EntityLoadContext;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.service.jdbc.dialect.internal.AbstractDialectResolver;
import org.hibernate.type.BasicType;
import org.hibernate.type.SerializationException;
import org.hibernate.type.Type;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * Defines internationalized messages for hibernate-core. New messages must be added after the last message defined to ensure
 * message codes are unique.
 */
// TODO: @cause, errorv because of var args and reordering of parameters, combine loggers in hibernate-core,
// formattable, message codes, register project code, category=class, rename Log
@MessageLogger( projectCode = "HHH" )
public interface Logger extends BasicLogger {

    @LogMessage( level = INFO )
    @Message( "Adding secondary table to entity %s -> %s" )
    void addingSecondaryTableToEntity( String entity,
                                       String table );

    @LogMessage( level = WARN )
    @Message( value = "Already session bound on call to bind(); make sure you clean up your sessions!" )
    void alreadySessionBound();

    @LogMessage( level = WARN )
    @Message( value = "Placing @Access(AccessType.%s) on a field does not have any effect." )
    void annotationHasNoEffect( AccessType type );

    @LogMessage( level = WARN )
    @Message( value = "Attempt to map column [%s] to no target column after explicit target column(s) named for FK [name=%s]" )
    void attemptToMapColumnToNoTargetColumn( String loggableString,
                                             String name );

    @LogMessage( level = WARN )
    @Message( value = "Attribute \"order-by\" ignored in JDK1.3 or less" )
    void attributeIgnored();

    @LogMessage( level = INFO )
    @Message( value = "Autocommit mode: %s" )
    void autoCommitMode( boolean autocommit );

    @LogMessage( level = INFO )
    @Message( value = "Automatic flush during beforeCompletion(): %s" )
    void autoFlush( String enabledDisabled );

    @LogMessage( level = WARN )
    @Message( value = "JTASessionContext being used with JDBCTransactionFactory; auto-flush will not operate correctly with getCurrentSession()" )
    void autoFlushWillNotWork();

    @LogMessage( level = INFO )
    @Message( value = "Automatic session close at end of transaction: %s" )
    void autoSessionClose( String enabledDisabled );

    @LogMessage( level = INFO )
    @Message( value = "On release of batch it still contained JDBC statements" )
    void batchContainedStatementsOnRelease();

    @LogMessage( level = INFO )
    @Message( value = "Batcher factory: %s" )
    void batcherFactory( String batcherClass );

    @LogMessage( level = INFO )
    @Message( value = "Bind entity %s on table %s" )
    void bindEntityOnTable( String entity,
                            String table );

    @LogMessage( level = INFO )
    @Message( value = "Binding Any Meta definition: %s" )
    void bindingAnyMetaDefinition( String name );

    @LogMessage( level = INFO )
    @Message( value = "Binding entity from annotated class: %s" )
    void bindingEntityFromClass( String className );

    @LogMessage( level = INFO )
    @Message( value = "Binding filter definition: %s" )
    void bindingFilterDefinition( String name );

    @LogMessage( level = INFO )
    @Message( value = "Binding named native query: %s => %s" )
    void bindingNamedNativeQuery( String name,
                                  String query );

    @LogMessage( level = INFO )
    @Message( value = "Binding named query: %s => %s" )
    void bindingNamedQuery( String name,
                            String query );

    @LogMessage( level = INFO )
    @Message( value = "Binding result set mapping: %s" )
    void bindingResultSetMapping( String mapping );

    @LogMessage( level = INFO )
    @Message( value = "Binding type definition: %s" )
    void bindingTypeDefinition( String name );

    @LogMessage( level = INFO )
    @Message( value = "Bound Ejb3Configuration to JNDI name: %s" )
    void boundEjb3ConfigurationToJndiName( String name );

    @LogMessage( level = INFO )
    @Message( value = "Building session factory" )
    void buildingSessionFactory();

    @LogMessage( level = INFO )
    @Message( value = "Bytecode provider name : %s" )
    void bytecodeProvider( String provider );

    @LogMessage( level = WARN )
    @Message( value = "c3p0 properties were encountered, but the %s provider class was not found on the classpath; these properties are going to be ignored." )
    void c3p0ProviderClassNotFound( String c3p0ProviderClassName );

    @LogMessage( level = WARN )
    @Message( value = "I/O reported cached file could not be found : %s : %s" )
    void cachedFileNotFound( String path,
                             FileNotFoundException error );

    @LogMessage( level = INFO )
    @Message( value = "Cache provider: %s" )
    void cacheProvider( String name );

    @LogMessage( level = INFO )
    @Message( value = "Cache region factory : %s" )
    void cacheRegionFactory( String regionFactoryClassName );

    @LogMessage( level = INFO )
    @Message( value = "Cache region prefix: %s" )
    void cacheRegionPrefix( String prefix );

    @LogMessage( level = WARN )
    @Message( value = "Calling joinTransaction() on a non JTA EntityManager" )
    void callingJoinTransactionOnNonJtaEntityManager();

    @LogMessage( level = INFO )
    @Message( value = "Check Nullability in Core (should be disabled when Bean Validation is on): %s" )
    void checkNullability( String enabledDisabled );

    @LogMessage( level = INFO )
    @Message( value = "Cleaning up connection pool [%s]" )
    void cleaningUpConnectionPool( String url );

    @LogMessage( level = INFO )
    @Message( value = "Closing" )
    void closing();

    @LogMessage( level = INFO )
    @Message( value = "Collections fetched (minimize this): %ld" )
    void collectionsFetched( long collectionFetchCount );

    @LogMessage( level = INFO )
    @Message( value = "Collections loaded: %ld" )
    void collectionsLoaded( long collectionLoadCount );

    @LogMessage( level = INFO )
    @Message( value = "Collections recreated: %ld" )
    void collectionsRecreated( long collectionRecreateCount );

    @LogMessage( level = INFO )
    @Message( value = "Collections removed: %ld" )
    void collectionsRemoved( long collectionRemoveCount );

    @LogMessage( level = INFO )
    @Message( value = "Collections updated: %ld" )
    void collectionsUpdated( long collectionUpdateCount );

    @LogMessage( level = INFO )
    @Message( value = "Columns: %s" )
    void columns( Set keySet );

    @LogMessage( level = INFO )
    @Message( value = "Configuration resource: %s" )
    void configurationResource( String resource );

    @LogMessage( level = INFO )
    @Message( value = "Configured SessionFactory: %s" )
    void configuredSessionFactory( String name );

    @LogMessage( level = INFO )
    @Message( value = "Configuring from file: %s" )
    void configuringFromFile( String file );

    @LogMessage( level = INFO )
    @Message( value = "Configuring from resource: %s" )
    void configuringFromResource( String resource );

    @LogMessage( level = INFO )
    @Message( value = "Configuring from URL: %s" )
    void configuringFromUrl( URL url );

    @LogMessage( level = INFO )
    @Message( value = "Configuring from XML document" )
    void configuringFromXmlDocument();

    @LogMessage( level = INFO )
    @Message( value = "Connection properties: %s" )
    void connectionProperties( Properties connectionProps );

    @LogMessage( level = INFO )
    @Message( value = "Connection release mode: %s" )
    void connectionReleaseMode( String releaseModeName );

    @LogMessage( level = INFO )
    @Message( value = "Connections obtained: %ld" )
    void connectionsObtained( long connectCount );

    @LogMessage( level = INFO )
    @Message( value = "%s did not provide constructor accepting java.util.Properties; attempting no-arg constructor." )
    void constructorWithPropertiesNotFound( String regionFactoryClassName );

    @LogMessage( level = ERROR )
    @Message( value = "Container is providing a null PersistenceUnitRootUrl: discovery impossible" )
    void containerProvidingNullPersistenceUnitRootUrl();

    @LogMessage( level = WARN )
    @Message( value = "Ignoring bag join fetch [%s] due to prior collection join fetch" )
    void containsJoinFetchedCollection( String role );

    @Message( value = "Could not close connection" )
    Object couldNotCloseConnection();

    @LogMessage( level = INFO )
    @Message( value = "Creating subcontext: %s" )
    void creatingSubcontextInfo( String intermediateContextName );

    @LogMessage( level = INFO )
    @Message( value = "Database ->\n" + "       name : %s\n" + "    version : %s\n" + "      major : %s\n" + "      minor : %s" )
    void database( String databaseProductName,
                   String databaseProductVersion,
                   int databaseMajorVersion,
                   int databaseMinorVersion );

    @LogMessage( level = INFO )
    @Message( value = "Default batch fetch size: %s" )
    void defaultBatchFetchSize( int batchFetchSize );

    @LogMessage( level = INFO )
    @Message( value = "Default catalog: %s" )
    void defaultCatalog( String defaultCatalog );

    @LogMessage( level = INFO )
    @Message( value = "Default entity-mode: %s" )
    void defaultEntityMode( EntityMode defaultEntityMode );

    @LogMessage( level = INFO )
    @Message( value = "Default schema: %s" )
    void defaultSchema( String defaultSchema );

    @LogMessage( level = WARN )
    @Message( value = "Defining %s=true ignored in HEM" )
    void definingFlushBeforeCompletionIgnoredInHem( String flushBeforeCompletion );

    @LogMessage( level = INFO )
    @Message( value = "Deleted entity synthetic identifier rollback: %s" )
    void deletedEntitySyntheticIdentifierRollback( String enabledDisabled );

    @LogMessage( level = WARN )
    @Message( value = "Per HHH-5451 support for cglib as a bytecode provider has been deprecated." )
    void deprecated();

    @LogMessage( level = WARN )
    @Message( value = "@ForceDiscriminator is deprecated use @DiscriminatorOptions instead." )
    void deprecatedForceDescriminatorAnnotation();

    @LogMessage( level = WARN )
    @Message( value = "The Oracle9Dialect dialect has been deprecated; use either Oracle9iDialect or Oracle10gDialect instead" )
    void deprecatedOracle9Dialect();

    @LogMessage( level = WARN )
    @Message( value = "The OracleDialect dialect has been deprecated; use Oracle8iDialect instead" )
    void deprecatedOracleDialect();

    @LogMessage( level = WARN )
    @Message( value = "DEPRECATED : use {} instead with custom {} implementation" )
    void deprecatedUuidGenerator( String name,
                                  String name2 );

    @LogMessage( level = WARN )
    @Message( value = "Dialect resolver class not found: %s" )
    void dialectResolverNotFound( String resolverName );

    @LogMessage( level = INFO )
    @Message( value = "Disallowing insert statement comment for select-identity due to Oracle driver bug" )
    void disallowingInsertStatementComment();

    @LogMessage( level = INFO )
    @Message( value = "Driver ->\n" + "       name : %s\n" + "    version : %s\n" + "      major : %s\n" + "      minor : %s" )
    void driver( String driverProductName,
                 String driverProductVersion,
                 int driverMajorVersion,
                 int driverMinorVersion );

    @LogMessage( level = WARN )
    @Message( value = "Duplicate generator name %s" )
    void duplicateGeneratorName( String name );

    @LogMessage( level = WARN )
    @Message( value = "Duplicate generator table: %s" )
    void duplicateGeneratorTable( String name );

    @LogMessage( level = INFO )
    @Message( value = "Duplicate import: %s -> %s" )
    void duplicateImport( String entityName,
                          String rename );

    @LogMessage( level = WARN )
    @Message( value = "Duplicate joins for class: %s" )
    void duplicateJoins( String entityName );

    @LogMessage( level = INFO )
    @Message( value = "entity-listener duplication, first event definition will be used: %s" )
    void duplicateListener( String className );

    @LogMessage( level = WARN )
    @Message( value = "Found more than one <persistence-unit-metadata>, subsequent ignored" )
    void duplicateMetadata();

    @LogMessage( level = INFO )
    @Message( value = "Echoing all SQL to stdout" )
    void echoingSql();

    @LogMessage( level = INFO )
    @Message( value = "Ejb3Configuration name: %s" )
    void ejb3ConfigurationName( String name );

    @LogMessage( level = INFO )
    @Message( value = "An Ejb3Configuration was renamed from name: %s" )
    void ejb3ConfigurationRenamedFromName( String name );

    @LogMessage( level = INFO )
    @Message( value = "An Ejb3Configuration was unbound from name: %s" )
    void ejb3ConfigurationUnboundFromName( String name );

    @LogMessage( level = INFO )
    @Message( value = "Entities deleted: %ld" )
    void entitiesDeleted( long entityDeleteCount );

    @LogMessage( level = INFO )
    @Message( value = "Entities fetched (minimize this): %ld" )
    void entitiesFetched( long entityFetchCount );

    @LogMessage( level = INFO )
    @Message( value = "Entities inserted: %ld" )
    void entitiesInserted( long entityInsertCount );

    @LogMessage( level = INFO )
    @Message( value = "Entities loaded: %ld" )
    void entitiesLoaded( long entityLoadCount );

    @LogMessage( level = INFO )
    @Message( value = "Entities updated: %ld" )
    void entitiesUpdated( long entityUpdateCount );

    @LogMessage( level = WARN )
    @Message( value = "@org.hibernate.annotations.Entity used on a non root entity: ignored for %s" )
    void entityAnnotationOnNonRoot( String className );

    @LogMessage( level = WARN )
    @Message( value = "Entity Manager closed by someone else (%s must not be used)" )
    void entityManagerClosedBySomeoneElse( String autoCloseSession );

    @LogMessage( level = INFO )
    @Message( value = "Hibernate EntityManager %s" )
    void entityManagerVersion( String versionString );

    @LogMessage( level = WARN )
    @Message( value = "Entity [%s] is abstract-class/interface explicitly mapped as non-abstract; be sure to supply entity-names" )
    void entityMappedAsNonAbstract( String name );

    @LogMessage( level = INFO )
    @Message( value = "%s %s found" )
    void exceptionHeaderFound( String exceptionHeader,
                               String metaInfOrmXml );

    @LogMessage( level = INFO )
    @Message( value = "%s No %s found" )
    void exceptionHeaderNotFound( String exceptionHeader,
                                  String metaInfOrmXml );

    @LogMessage( level = ERROR )
    @Message( value = "Exception in interceptor afterTransactionCompletion()" )
    void exceptionInAfterTransactionCompletionInterceptor( @Cause Throwable e );

    @LogMessage( level = ERROR )
    @Message( value = "Exception in interceptor beforeTransactionCompletion()" )
    void exceptionInBeforeTransactionCompletionInterceptor( @Cause Throwable e );

    @LogMessage( level = INFO )
    @Message( value = "Sub-resolver threw unexpected exception, continuing to next : %s" )
    void exceptionInSubResolver( String message );

    @LogMessage( level = INFO )
    @Message( value = "Executing import script: %s" )
    void executingImportScript( String name );

    @LogMessage( level = ERROR )
    @Message( value = "Expected type: %s, actual value: %s" )
    void expectedType( String name,
                       String string );

    @LogMessage( level = WARN )
    @Message( value = "An item was expired by the cache while it was locked (increase your cache timeout): %s" )
    void expired( Object key );

    @LogMessage( level = WARN )
    @Message( value = "Exploded jar file does not exist (ignored): %s" )
    void explodedJarDoesNotExist( URL jarUrl );

    @LogMessage( level = WARN )
    @Message( value = "Exploded jar file not a directory (ignored): %s" )
    void explodedJarNotDirectory( URL jarUrl );

    @LogMessage( level = INFO )
    @Message( value = "Exporting generated schema to database" )
    void exportingGeneratedSchemaToDatabase();

    @LogMessage( level = INFO )
    @Message( value = "Bound factory to JNDI name: %s" )
    void factoryBoundToJndiName( String name );

    @LogMessage( level = INFO )
    @Message( value = "Factory name: %s" )
    void factoryName( String name );

    @LogMessage( level = INFO )
    @Message( value = "A factory was renamed from name: %s" )
    void factoryRenamedFromName( String name );

    @LogMessage( level = INFO )
    @Message( value = "Unbound factory from JNDI name: %s" )
    void factoryUnboundFromJndiName( String name );

    @LogMessage( level = INFO )
    @Message( value = "A factory was unbound from name: %s" )
    void factoryUnboundFromName( String name );

    @LogMessage( level = ERROR )
    @Message( value = "an assertion failure occured" + " (this may indicate a bug in Hibernate, but is more likely due"
                      + " to unsafe use of the session): %s" )
    void failed( Throwable throwable );

    @LogMessage( level = WARN )
    @Message( value = "Fail-safe cleanup (collections) : %s" )
    void failSafeCollectionsCleanup( CollectionLoadContext collectionLoadContext );

    @LogMessage( level = WARN )
    @Message( value = "Fail-safe cleanup (entities) : %s" )
    void failSafeEntitiesCleanup( EntityLoadContext entityLoadContext );

    @LogMessage( level = INFO )
    @Message( value = "Fetching database metadata" )
    void fetchingDatabaseMetadata();

    @LogMessage( level = WARN )
    @Message( value = "@Filter not allowed on subclasses (ignored): %s" )
    void filterAnnotationOnSubclass( String className );

    @LogMessage( level = WARN )
    @Message( value = "firstResult/maxResults specified with collection fetch; applying in memory!" )
    void firstOrMaxResultsSpecifiedWithCollectionFetch();

    @LogMessage( level = INFO )
    @Message( value = "Flushes: %ld" )
    void flushes( long flushCount );

    @LogMessage( level = INFO )
    @Message( value = "Forcing container resource cleanup on transaction completion" )
    void forcingContainerResourceCleanup();

    @LogMessage( level = INFO )
    @Message( value = "Forcing table use for sequence-style generator due to pooled optimizer selection where db does not support pooled sequences" )
    void forcingTableUse();

    @LogMessage( level = INFO )
    @Message( value = "Foreign keys: %s" )
    void foreignKeys( Set keySet );

    @LogMessage( level = INFO )
    @Message( value = "Found mapping document in jar: %s" )
    void foundMappingDocument( String name );

    @LogMessage( level = INFO )
    @Message( value = "JVM does not support Statement.getGeneratedKeys()" )
    void generatedKeysNotSupported();

    @LogMessage( level = INFO )
    @Message( value = "Generate SQL with comments: %s" )
    void generateSqlWithComments( String enabledDisabled );

    @LogMessage( level = ERROR )
    @Message( value = "Getters of lazy classes cannot be final: %s.%s" )
    void gettersOfLazyClassesCannotBeFinal( String entityName,
                                            String name );

    @LogMessage( level = WARN )
    @Message( value = "GUID identifier generated: %s" )
    void guidGenerated( String result );

    @LogMessage( level = INFO )
    @Message( value = "Handling transient entity in delete processing" )
    void handlingTransientEntity();

    @LogMessage( level = INFO )
    @Message( value = "Hibernate connection pool size: %d" )
    void hibernateConnectionPoolSize( int poolSize );

    @LogMessage( level = WARN )
    @Message( value = "Config specified explicit optimizer of [%s], but [%s=%d; honoring optimizer setting" )
    void honoringOptimizerSetting( String none,
                                   String incrementParam,
                                   int incrementSize );

    @LogMessage( level = INFO )
    @Message( value = "HQL: %s, time: %sms, rows: %s" )
    void hql( String hql,
              Long valueOf,
              Long valueOf2 );

    @LogMessage( level = WARN )
    @Message( value = "HSQLDB supports only READ_UNCOMMITTED isolation" )
    void hsqldbSupportsOnlyReadCommittedIsolation();

    @LogMessage( level = WARN )
    @Message( value = "On EntityLoadContext#clear, hydratingEntities contained [%d] entries" )
    void hydratingEntitiesCount( int size );

    @LogMessage( level = WARN )
    @Message( value = "Ignoring unique constraints specified on table generator [%s]" )
    void ignoringTableGeneratorConstraints( String name );

    @LogMessage( level = INFO )
    @Message( value = "Ignoring unrecognized query hint [%s]" )
    void ignoringUnrecognizedQueryHint( String hintName );

    @LogMessage( level = ERROR )
    @Message( value = "Illegal argument on static metamodel field injection : %s#%s; expected type :  %s; encountered type : %s" )
    void illegalArgumentOnStaticMetamodelFieldInjection( String name,
                                                         String name2,
                                                         String name3,
                                                         String name4 );

    @LogMessage( level = ERROR )
    @Message( value = "IllegalArgumentException in class: %s, getter method of property: %s" )
    void illegalPropertyGetterArgument( String name,
                                        String propertyName );

    @LogMessage( level = ERROR )
    @Message( value = "IllegalArgumentException in class: %s, setter method of property: %s" )
    void illegalPropertySetterArgument( String name,
                                        String propertyName );

    @LogMessage( level = WARN )
    @Message( value = "@Immutable used on a non root entity: ignored for %s" )
    void immutableAnnotationOnNonRoot( String className );

    @LogMessage( level = WARN )
    @Message( value = "Mapping metadata cache was not completely processed" )
    void incompleteMappingMetadataCacheProcessing();

    @LogMessage( level = INFO )
    @Message( value = "Indexes: %s" )
    void indexes( Set keySet );

    @LogMessage( level = WARN )
    @Message( value = "InitialContext did not implement EventContext" )
    void initialContextDidNotImplementEventContext();

    @LogMessage( level = WARN )
    @Message( value = "InitialContext did not implement EventContext" )
    void initialContextDoesNotImplementEventContext();

    @LogMessage( level = INFO )
    @Message( value = "Instantiated TransactionManagerLookup" )
    void instantiatedTransactionManagerLookup();

    @LogMessage( level = INFO )
    @Message( value = "Instantiating explicit connection provider: %s" )
    void instantiatingExplicitConnectinProvider( String providerClassName );

    @LogMessage( level = INFO )
    @Message( value = "Instantiating TransactionManagerLookup: %s" )
    void instantiatingTransactionManagerLookup( String tmLookupClass );

    @LogMessage( level = ERROR )
    @Message( value = "Array element type error\n%s" )
    void invalidArrayElementType( String message );

    @LogMessage( level = WARN )
    @Message( value = "Discriminator column has to be defined in the root entity, it will be ignored in subclass: %s" )
    void invalidDescriminatorAnnotation( String className );

    @LogMessage( level = ERROR )
    @Message( value = "Application attempted to edit read only item: %s" )
    void invalidEditOfReadOnlyItem( Object key );

    @LogMessage( level = ERROR )
    @Message( value = "Invalid JNDI name: %s" )
    void invalidJndiName( String name,
                          @Cause InvalidNameException e );

    @LogMessage( level = WARN )
    @Message( value = "Inapropriate use of @OnDelete on entity, annotation ignored: %s" )
    void invalidOnDeleteAnnotation( String entityName );

    @LogMessage( level = WARN )
    @Message( value = "Root entity should not hold an PrimaryKeyJoinColum(s), will be ignored" )
    void invalidPrimaryKeyJoinColumnAnnotation();

    @LogMessage( level = WARN )
    @Message( value = "Mixing inheritance strategy in a entity hierarchy is not allowed, ignoring sub strategy in: %s" )
    void invalidSubStrategy( String className );

    @LogMessage( level = WARN )
    @Message( value = "Illegal use of @Table in a subclass of a SINGLE_TABLE hierarchy: %s" )
    void invalidTableAnnotation( String className );

    @LogMessage( level = INFO )
    @Message( value = "JACC contextID: %s" )
    void jaccContextId( String contextId );

    @LogMessage( level = INFO )
    @Message( value = "java.sql.Types mapped the same code [%d] multiple times; was [%s]; now [%s]" )
    void JavaSqlTypesMappedSameCodeMultipleTimes( int code,
                                                  String old,
                                                  String name );

    @LogMessage( level = INFO )
    @Message( value = "JDBC3 getGeneratedKeys(): %s" )
    void jdbc3GeneratedKeys( String enabledDisabled );

    @LogMessage( level = WARN )
    @Message( value = "%s = false break the EJB3 specification" )
    void jdbcAutoCommitFalseBreaksEjb3Spec( String autocommit );

    @LogMessage( level = INFO )
    @Message( value = "JDBC batch size: %s" )
    void jdbcBatchSize( int batchSize );

    @LogMessage( level = INFO )
    @Message( value = "JDBC batch updates for versioned data: %s" )
    void jdbcBatchUpdates( String enabledDisabled );

    @Message( value = "JDBC begin failed" )
    String jdbcBeginFailed();

    @LogMessage( level = WARN )
    @Message( value = "no JDBC Driver class was specified by property %s" )
    void jdbcDriverNotSpecified( String driver );

    @LogMessage( level = INFO )
    @Message( value = "JDBC isolation level: %s" )
    void jdbcIsolationLevel( String isolationLevelToString );

    @LogMessage( level = INFO )
    @Message( value = "JDBC result set fetch size: %s" )
    void jdbcResultSetFetchSize( Integer statementFetchSize );

    @Message( value = "JDBC rollback failed" )
    String jdbcRollbackFailed();

    @Message( value = "JDBC URL was not specified by property %s" )
    String jdbcUrlNotSpecified( String url );

    @LogMessage( level = INFO )
    @Message( value = "JDBC version : %d.%d" )
    void jdbcVersion( int jdbcMajorVersion,
                      int jdbcMinorVersion );

    @LogMessage( level = INFO )
    @Message( value = "JNDI InitialContext properties:%s" )
    void jndiInitialContextProperties( Hashtable hash );

    @LogMessage( level = ERROR )
    @Message( value = "JNDI name %s does not handle a session factory reference" )
    void jndiNameDoesNotHandleSessionFactoryReference( String sfJNDIName,
                                                       @Cause ClassCastException e );

    @LogMessage( level = INFO )
    @Message( value = "JPA-QL strict compliance: %s" )
    void jpaQlStrictCompliance( String enabledDisabled );

    @LogMessage( level = INFO )
    @Message( value = "Lazy property fetching available for: %s" )
    void lazyPropertyFetchingAvailable( String name );

    @LogMessage( level = INFO )
    @Message( value = "JVM does not support LinkedHashMap, LinkedHashSet - ordered maps and sets disabled" )
    void linkedMapsAndSetsNotSupported();

    @LogMessage( level = WARN )
    @Message( value = "In CollectionLoadContext#endLoadingCollections, localLoadingCollectionKeys contained [%s], but no LoadingCollectionEntry was found in loadContexts" )
    void loadingCollectionKeyNotFound( CollectionKey collectionKey );

    @LogMessage( level = WARN )
    @Message( value = "On CollectionLoadContext#cleanup, localLoadingCollectionKeys contained [%d] entries" )
    void localLoadingCollectionKeysCount( int size );

    @LogMessage( level = INFO )
    @Message( value = "Logging statistics...." )
    void loggingStatistics();

    @LogMessage( level = INFO )
    @Message( value = "*** Logical connection closed ***" )
    void logicalConnectionClosed();

    @LogMessage( level = INFO )
    @Message( value = "Logical connection releasing its physical connection" )
    void logicalConnectionReleasingPhysicalConnection();

    @LogMessage( level = ERROR )
    @Message( value = "Malformed URL: %s" )
    void malformedUrl( URL jarUrl,
                       @Cause URISyntaxException e );

    @LogMessage( level = WARN )
    @Message( value = "Malformed URL: %s" )
    void malformedUrlWarning( URL jarUrl,
                              @Cause URISyntaxException e );

    @LogMessage( level = WARN )
    @Message( value = "You should set hibernate.transaction.manager_lookup_class if cache is enabled" )
    void managerLookupClassShouldBeSet();

    @LogMessage( level = INFO )
    @Message( value = "Mapping class: %s -> %s" )
    void mappingClass( String entityName,
                       String name );

    @LogMessage( level = INFO )
    @Message( value = "Mapping class join: %s -> %s" )
    void mappingClassJoin( String entityName,
                           String name );

    @LogMessage( level = INFO )
    @Message( value = "Mapping collection: %s -> %s" )
    void mappingCollection( String name1,
                            String name2 );

    @LogMessage( level = INFO )
    @Message( value = "Mapping joined-subclass: %s -> %s" )
    void mappingJoinedSubclass( String entityName,
                                String name );

    @LogMessage( level = INFO )
    @Message( value = "Mapping Package %s" )
    void mappingPackage( String packageName );

    @LogMessage( level = INFO )
    @Message( value = "Mapping subclass: %s -> %s" )
    void mappingSubclass( String entityName,
                          String name );

    @LogMessage( level = INFO )
    @Message( value = "Mapping union-subclass: %s -> %s" )
    void mappingUnionSubclass( String entityName,
                               String name );

    @LogMessage( level = INFO )
    @Message( value = "Maximum outer join fetch depth: %s" )
    void maxOuterJoinFetchDepth( Integer maxFetchDepth );

    @LogMessage( level = INFO )
    @Message( value = "Max query time: %ldms" )
    void maxQueryTime( long queryExecutionMaxTime );

    @LogMessage( level = WARN )
    @Message( value = "Function template anticipated %d arguments, but %d arguments encountered" )
    void missingArguments( int anticipatedNumberOfArguments,
                           int numberOfArguments );

    @LogMessage( level = WARN )
    @Message( value = "Class annotated @org.hibernate.annotations.Entity but not javax.persistence.Entity (most likely a user error): %s" )
    void missingEntityAnnotation( String className );

    @LogMessage( level = INFO )
    @Message( value = "Named query checking : %s" )
    void namedQueryChecking( String enabledDisabled );

    @LogMessage( level = ERROR )
    @Message( value = "Error in named query: %s" )
    void namedQueryError( String queryName,
                          @Cause HibernateException e );

    @LogMessage( level = WARN )
    @Message( value = "Naming exception occurred accessing factory: %s" )
    void namingExceptionAccessingFactory( NamingException exception );

    @LogMessage( level = WARN )
    @Message( value = "Narrowing proxy to %s - this operation breaks ==" )
    void narrowingProxy( Class concreteProxyClass );

    @LogMessage( level = WARN )
    @Message( value = "FirstResult/maxResults specified on polymorphic query; applying in memory!" )
    void needsLimit();

    @LogMessage( level = WARN )
    @Message( value = "No appropriate connection provider encountered, assuming application will be supplying connections" )
    void noAppropriateConnectionProvider();

    @LogMessage( level = INFO )
    @Message( value = "No default (no-argument) constructor for class: %s (class must be instantiated by Interceptor)" )
    void noDefaultConstructor( String name );

    @LogMessage( level = WARN )
    @Message( value = "no persistent classes found for query class: %s" )
    void noPersistentClassesFound( String query );

    @LogMessage( level = ERROR )
    @Message( value = "No session factory with JNDI name %s" )
    void noSessionFactoryWithJndiName( String sfJNDIName,
                                       @Cause NameNotFoundException e );

    @LogMessage( level = INFO )
    @Message( value = "Not binding factory to JNDI, no JNDI name configured" )
    void notBindingFactoryToJndi();

    @LogMessage( level = INFO )
    @Message( value = "Obtaining TransactionManager" )
    void obtainingTransactionManager();

    @LogMessage( level = INFO )
    @Message( value = "Optimistic lock failures: %ld" )
    void optimisticLockFailures( long optimisticFailureCount );

    @LogMessage( level = INFO )
    @Message( value = "Optimize cache for minimal puts: %s" )
    void optimizeCacheForMinimalInputs( String enabledDisabled );

    @LogMessage( level = WARN )
    @Message( value = "@OrderBy not allowed for an indexed collection, annotation ignored." )
    void orderByAnnotationIndexedCollection();

    @LogMessage( level = WARN )
    @Message( value = "Attribute \"order-by\" ignored in JDK1.3 or less" )
    void orderByAttributeIgnored();

    @LogMessage( level = INFO )
    @Message( value = "Order SQL inserts for batching: %s" )
    void orderSqlInsertsForBatching( String enabledDisabled );

    @LogMessage( level = INFO )
    @Message( value = "Order SQL updates by primary key: %s" )
    void orderSqlUpdatesByPrimaryKey( String enabledDisabled );

    @LogMessage( level = WARN )
    @Message( value = "Overriding %s is dangerous, this might break the EJB3 specification implementation" )
    void overridingTransactionStrategyDangerous( String transactionStrategy );

    @LogMessage( level = WARN )
    @Message( value = "Package not found or wo package-info.java: %s" )
    void packageNotFound( String packageName );

    @LogMessage( level = WARN )
    @Message( value = "Parameter position [%s] occurred as both JPA and Hibernate positional parameter" )
    void parameterPositionOccurredAsBothJpaAndHibernatePositionalParameter( Integer position );

    @LogMessage( level = ERROR )
    @Message( value = "Error parsing XML (%d) : %s" )
    void parsingXmlError( int lineNumber,
                          String message );

    @LogMessage( level = ERROR )
    @Message( value = "Error parsing XML: %s(%d) %s" )
    void parsingXmlErrorForFile( String file,
                                 int lineNumber,
                                 String message );

    @LogMessage( level = ERROR )
    @Message( value = "Warning parsing XML (%d) : %s" )
    void parsingXmlWarning( int lineNumber,
                            String message );

    @LogMessage( level = WARN )
    @Message( value = "Warning parsing XML: %s(%d) %s" )
    void parsingXmlWarningForFile( String file,
                                   int lineNumber,
                                   String message );

    @LogMessage( level = WARN )
    @Message( value = "Persistence provider caller does not implement the EJB3 spec correctly."
                      + "PersistenceUnitInfo.getNewTempClassLoader() is null." )
    void persistenceProviderCallerDoesNotImplementEjb3SpecCorrectly();

    @LogMessage( level = INFO )
    @Message( value = "Pooled optimizer source reported [%s] as the initial value; use of 1 or greater highly recommended" )
    void pooledOptimizerReportedInitialValue( IntegralDataTypeHolder value );

    @LogMessage( level = ERROR )
    @Message( value = "PreparedStatement was already in the batch, [%s]." )
    void preparedStatementAlreadyInBatch( String sql );

    @LogMessage( level = WARN )
    @Message( value = "processEqualityExpression() : No expression to process!" )
    void processEqualityExpression();

    @LogMessage( level = INFO )
    @Message( value = "Processing PersistenceUnitInfo [\n\tname: %s\n\t...]" )
    void processingPersistenceUnitInfoName( String persistenceUnitName );

    @LogMessage( level = INFO )
    @Message( value = "Loaded properties from resource hibernate.properties: %s" )
    void propertiesLoaded( Properties maskOut );

    @LogMessage( level = INFO )
    @Message( value = "hibernate.properties not found" )
    void propertiesNotFound();

    @LogMessage( level = WARN )
    @Message( value = "Property %s not found in class but described in <mapping-file/> (possible typo error)" )
    void propertyNotFound( String property );

    @LogMessage( level = WARN )
    @Message( value = "%s has been deprecated in favor of %s; that provider will be used instead." )
    void providerClassDeprecated( String providerClassName,
                                  String actualProviderClassName );

    @LogMessage( level = WARN )
    @Message( value = "proxool properties were encountered, but the %s provider class was not found on the classpath; these properties are going to be ignored." )
    void proxoolProviderClassNotFound( String proxoolProviderClassName );

    @LogMessage( level = INFO )
    @Message( value = "Queries executed to database: %ld" )
    void queriesExecuted( long queryExecutionCount );

    @LogMessage( level = INFO )
    @Message( value = "Query cache: %s" )
    void queryCache( String enabledDisabled );

    @LogMessage( level = INFO )
    @Message( value = "Query cache factory: %s" )
    void queryCacheFactory( String queryCacheFactoryClassName );

    @LogMessage( level = INFO )
    @Message( value = "Query cache hits: %ld" )
    void queryCacheHits( long queryCacheHitCount );

    @LogMessage( level = INFO )
    @Message( value = "Query cache misses: %ld" )
    void queryCacheMisses( long queryCacheMissCount );

    @LogMessage( level = INFO )
    @Message( value = "Query cache puts: %ld" )
    void queryCachePuts( long queryCachePutCount );

    @LogMessage( level = INFO )
    @Message( value = "Query language substitutions: %s" )
    void queryLanguageSubstitutions( Map querySubstitutions );

    @LogMessage( level = INFO )
    @Message( value = "Query translator: %s" )
    void queryTranslator( String className );

    @LogMessage( level = INFO )
    @Message( value = "RDMSOS2200Dialect version: 1.0" )
    void rdmsOs2200Dialect();

    @LogMessage( level = INFO )
    @Message( value = "Reading mappings from cache file: %s" )
    void readingCachedMappings( File cachedFile );

    @LogMessage( level = INFO )
    @Message( value = "Reading mappings from file: %s" )
    void readingMappingsFromFile( String path );

    @LogMessage( level = INFO )
    @Message( value = "Reading mappings from resource: %s" )
    void readingMappingsFromResource( String resourceName );

    @LogMessage( level = WARN )
    @Message( value = "read-only cache configured for mutable collection [%s]" )
    void readOnlyCacheConfiguredForMutableCollection( String name );

    @LogMessage( level = WARN )
    @Message( value = "Recognized obsolete hibernate namespace %s. Use namespace %s instead. Refer to Hibernate 3.6 Migration Guide!" )
    void recognizedObsoleteHibernateNamespace( String oldHibernateNamespace,
                                               String hibernateNamespace );

    @LogMessage( level = WARN )
    @Message( value = "Reconnecting the same connection that is already connected; should this connection have been disconnected?" )
    void reconnectingConnectedConnection();

    @LogMessage( level = WARN )
    @Message( value = "Property [%s] has been renamed to [%s]; update your properties appropriately" )
    void renamedProperty( Object propertyName,
                          Object newPropertyName );

    @LogMessage( level = INFO )
    @Message( value = "Required a different provider: %s" )
    void requiredDifferentProvider( String provider );

    @LogMessage( level = INFO )
    @Message( value = "Running hbm2ddl schema export" )
    void runningHbm2ddlSchemaExport();

    @LogMessage( level = INFO )
    @Message( value = "Running hbm2ddl schema update" )
    void runningHbm2ddlSchemaUpdate();

    @LogMessage( level = INFO )
    @Message( value = "Running schema validator" )
    void runningSchemaValidator();

    @LogMessage( level = INFO )
    @Message( value = "Schema export complete" )
    void schemaExportComplete();

    @LogMessage( level = ERROR )
    @Message( value = "Schema export unsuccessful" )
    void schemaExportUnsuccessful( @Cause Exception e );

    @LogMessage( level = INFO )
    @Message( value = "Schema update complete" )
    void schemaUpdateComplete();

    @LogMessage( level = WARN )
    @Message( value = "Scoping types to session factory %s after already scoped %s" )
    void scopingTypesToSessionFactoryAfterAlreadyScoped( SessionFactoryImplementor factory,
                                                         SessionFactoryImplementor factory2 );

    @LogMessage( level = INFO )
    @Message( value = "Scrollable result sets: %s" )
    void scrollabelResultSets( String enabledDisabled );

    @LogMessage( level = INFO )
    @Message( value = "Searching for mapping documents in jar: %s" )
    void searchingForMappingDocuments( String name );

    @LogMessage( level = INFO )
    @Message( value = "Second-level cache: %s" )
    void secondLevelCache( String enabledDisabled );

    @LogMessage( level = INFO )
    @Message( value = "Second level cache hits: %ld" )
    void secondLevelCacheHits( long secondLevelCacheHitCount );

    @LogMessage( level = INFO )
    @Message( value = "Second level cache misses: %ld" )
    void secondLevelCacheMisses( long secondLevelCacheMissCount );

    @LogMessage( level = INFO )
    @Message( value = "Second level cache puts: %ld" )
    void secondLevelCachePuts( long secondLevelCachePutCount );

    @LogMessage( level = INFO )
    @Message( value = "Service properties: %s" )
    void serviceProperties( Properties properties );

    @LogMessage( level = INFO )
    @Message( value = "Sessions closed: %ld" )
    void sessionsClosed( long sessionCloseCount );

    @LogMessage( level = INFO )
    @Message( value = "Sessions opened: %ld" )
    void sessionsOpened( long sessionOpenCount );

    @LogMessage( level = ERROR )
    @Message( value = "Setters of lazy classes cannot be final: %s.%s" )
    void settersOfLazyClassesCannotBeFinal( String entityName,
                                            String name );

    @LogMessage( level = WARN )
    @Message( value = "@Sort not allowed for an indexed collection, annotation ignored." )
    void sortAnnotationIndexedCollection();

    @LogMessage( level = WARN )
    @Message( value = "Manipulation query [%s] resulted in [%d] split queries" )
    void splitQueries( String sourceQuery,
                       int length );

    @LogMessage( level = WARN )
    @Message( value = "SQL Error: %d, SQLState: %s" )
    void sqlError( int errorCode,
                   String sqlState );

    @LogMessage( level = WARN )
    @Message( value = "SQL Error: %d, SQLState: %s" )
    void sqlException( int errorCode,
                       String sqlState );

    @LogMessage( level = ERROR )
    @Message( value = "SQLException escaped proxy" )
    void sqlExceptionEscapedProxy( @Cause SQLException e );

    @LogMessage( level = INFO )
    @Message( value = "Starting query cache at region: %s" )
    void startingQueryCache( String region );

    @LogMessage( level = INFO )
    @Message( value = "Starting service at JNDI name: %s" )
    void startingServiceAtJndiName( String boundName );

    @LogMessage( level = INFO )
    @Message( value = "Starting update timestamps cache at region: %s" )
    void startingUpdateTimestampsCache( String region );

    @LogMessage( level = INFO )
    @Message( value = "Start time: %s" )
    void startTime( long startTime );

    @LogMessage( level = INFO )
    @Message( value = "Statements closed: %ld" )
    void statementsClosed( long closeStatementCount );

    @LogMessage( level = INFO )
    @Message( value = "Statements prepared: %ld" )
    void statementsPrepared( long prepareStatementCount );

    @LogMessage( level = INFO )
    @Message( value = "Statistics: %s" )
    void statistics( String enabledDisabled );

    @LogMessage( level = INFO )
    @Message( value = "Stopping service" )
    void stoppingService();

    @LogMessage( level = INFO )
    @Message( value = "Structured second-level cache entries: %s" )
    void structuredSecondLevelCacheEntries( String enabledDisabled );

    @LogMessage( level = INFO )
    @Message( value = "sub-resolver threw unexpected exception, continuing to next : %s" )
    void subResolverException( String message );

    @LogMessage( level = INFO )
    @Message( value = "Successful transactions: %ld" )
    void successfulTransactions( long committedTransactionCount );

    @LogMessage( level = INFO )
    @Message( value = "Synchronization [%s] was already registered" )
    void synchronizationAlreadyRegistered( Synchronization synchronization );

    @LogMessage( level = ERROR )
    @Message( value = "Exception calling user Synchronization [%s] : %s" )
    void synchronizationFailed( Synchronization synchronization,
                                Throwable t );

    @LogMessage( level = INFO )
    @Message( value = "Table found: %s" )
    void tableFound( String string );

    @LogMessage( level = INFO )
    @Message( value = "Table not found: %s" )
    void tableNotFound( String name );

    @Message( value = "TransactionFactory class not found: %s" )
    String transactionFactoryClassNotFound( String strategyClassName );

    @LogMessage( level = INFO )
    @Message( value = "No TransactionManagerLookup configured (in JTA environment, use of read-write or transactional second-level cache is not recommended)" )
    void transactionManagerLookupNotConfigured();

    @LogMessage( level = WARN )
    @Message( value = "Transaction not available on beforeCompletion: assuming valid" )
    void transactionNotAvailableOnBeforeCompletion();

    @LogMessage( level = INFO )
    @Message( value = "Transactions: %ld" )
    void transactions( long transactionCount );

    @LogMessage( level = WARN )
    @Message( value = "Transaction started on non-root session" )
    void transactionStartedOnNonRootSession();

    @LogMessage( level = INFO )
    @Message( value = "Transaction strategy: %s" )
    void transactionStrategy( String strategyClassName );

    @LogMessage( level = WARN )
    @Message( value = "Type [%s] defined no registration keys; ignoring" )
    void typeDefinedNoRegistrationKeys( BasicType type );

    @LogMessage( level = INFO )
    @Message( value = "Type registration [%s] overrides previous : %s" )
    void typeRegistrationOverridesPrevious( String key,
                                            Type old );

    @Message( value = "Naming exception occurred accessing Ejb3Configuration" )
    Object unableToAccessEjb3Configuration();

    @LogMessage( level = ERROR )
    @Message( value = "Error while accessing session factory with JNDI name %s" )
    void unableToAccessSessionFactory( String sfJNDIName,
                                       @Cause NamingException e );

    @LogMessage( level = WARN )
    @Message( value = "Error accessing type info result set : %s" )
    void unableToAccessTypeInfoResultSet( String string );

    @Message( value = "Unable to apply constraints on DDL for %s" )
    Object unableToApplyConstraints( String className );

    @Message( value = "JTA transaction begin failed" )
    String unableToBeginJtaTransaction();

    @Message( value = "Could not bind Ejb3Configuration to JNDI" )
    Object unableToBindEjb3ConfigurationToJndi();

    @Message( value = "Could not bind factory to JNDI" )
    Object unableToBindFactoryToJndi();

    @LogMessage( level = INFO )
    @Message( value = "Could not bind value '%s' to parameter: %d; %s" )
    void unableToBindValueToParameter( String nullSafeToString,
                                       int index,
                                       String message );

    @LogMessage( level = ERROR )
    @Message( value = "Unable to build enhancement metamodel for %s" )
    void unableToBuildEnhancementMetamodel( String className );

    @LogMessage( level = INFO )
    @Message( value = "Could not build SessionFactory using the MBean classpath - will try again using client classpath: %s" )
    void unableToBuildSessionFactoryUsingMBeanClasspath( String message );

    @Message( value = "Unable to clean up callable statement" )
    Object unableToCleanUpCallableStatement();

    @Message( value = "Unable to clean up prepared statement" )
    Object unableToCleanUpPreparedStatement();

    @LogMessage( level = WARN )
    @Message( value = "Unable to cleanup temporary id table after use [%s]" )
    void unableToCleanupTemporaryIdTable( Throwable t );

    @Message( value = "Could not clear warnings" )
    Object unableToClearWarnings();

    @LogMessage( level = ERROR )
    @Message( value = "Error closing connection" )
    void unableToCloseConnection( @Cause Exception e );

    @LogMessage( level = INFO )
    @Message( value = "Error closing InitialContext [%s]" )
    void unableToCloseInitialContext( String string );

    @LogMessage( level = ERROR )
    @Message( value = "Error closing imput files: %s" )
    Object unableToCloseInputFiles( String name,
                                    @Cause IOException e );

    @Message( value = "Could not close input stream" )
    Object unableToCloseInputStream();

    @Message( value = "Could not close input stream for %s" )
    Object unableToCloseInputStreamForResource( String resourceName );

    @Message( value = "Unable to close iterator" )
    Object unableToCloseIterator();

    @LogMessage( level = ERROR )
    @Message( value = "Could not close jar: %s" )
    void unableToCloseJar( String message );

    @LogMessage( level = ERROR )
    @Message( value = "Error closing output file: %s" )
    void unableToCloseOutputFile( String outputFile,
                                  @Cause IOException e );

    @Message( value = "IOException occurred closing output stream" )
    Object unableToCloseOutputStream();

    @Message( value = "Problem closing pooled connection" )
    Object unableToClosePooledConnection();

    @LogMessage( level = ERROR )
    @Message( value = "Could not close session" )
    void unableToCloseSession( @Cause HibernateException e );

    @LogMessage( level = ERROR )
    @Message( value = "Could not close session during rollback" )
    void unableToCloseSessionDuringRollback( @Cause Exception e );

    @Message( value = "IOException occurred closing stream" )
    Object unableToCloseStream();

    @LogMessage( level = ERROR )
    @Message( value = "Could not close stream on hibernate.properties: %s" )
    void unableToCloseStreamError( IOException error );

    @Message( value = "JTA commit failed" )
    String unableToCommitJta();

    @LogMessage( level = ERROR )
    @Message( value = "Could not complete schema update" )
    void unableToCompleteSchemaUpdate( @Cause Exception e );

    @LogMessage( level = ERROR )
    @Message( value = "Could not complete schema validation" )
    void unableToCompleteSchemaValidation( @Cause SQLException e );

    @LogMessage( level = WARN )
    @Message( value = "Unable to configure SQLExceptionConverter : %s" )
    void unableToConfigureSqlExceptionConverter( HibernateException e );

    @LogMessage( level = ERROR )
    @Message( value = "Unable to construct current session context [%s]" )
    void unableToConstructCurrentSessionContext( String impl,
                                                 @Cause Throwable e );

    @LogMessage( level = WARN )
    @Message( value = "Unable to construct instance of specified SQLExceptionConverter : %s" )
    void unableToConstructSqlExceptionConverter( Throwable t );

    @LogMessage( level = WARN )
    @Message( value = "Could not copy system properties, system properties will be ignored" )
    void unableToCopySystemProperties();

    @Message( value = "Could not create proxy factory for:%s" )
    Object unableToCreateProxyFactory( String entityName );

    @LogMessage( level = ERROR )
    @Message( value = "Error creating schema " )
    void unableToCreateSchema( @Cause Exception e );

    @LogMessage( level = WARN )
    @Message( value = "Could not deserialize cache file: %s : %s" )
    void unableToDeserializeCache( String path,
                                   SerializationException error );

    @LogMessage( level = WARN )
    @Message( value = "Unable to destroy cache: %s" )
    void unableToDestroyCache( String message );

    @LogMessage( level = WARN )
    @Message( value = "Unable to destroy query cache: %s: %s" )
    void unableToDestroyQueryCache( String region,
                                    String message );

    @LogMessage( level = WARN )
    @Message( value = "Unable to destroy update timestamps cache: %s: %s" )
    void unableToDestroyUpdateTimestampsCache( String region,
                                               String message );

    @LogMessage( level = INFO )
    @Message( value = "Unable to determine lock mode value : %s -> %s" )
    void unableToDetermineLockModeValue( String hintName,
                                         Object value );

    @Message( value = "Could not determine transaction status" )
    String unableToDetermineTransactionStatus();

    @Message( value = "Could not determine transaction status after commit" )
    String unableToDetermineTransactionStatusAfterCommit();

    @LogMessage( level = WARN )
    @Message( value = "Unable to drop temporary id table after use [%s]" )
    void unableToDropTemporaryIdTable( String message );

    @LogMessage( level = ERROR )
    @Message( value = "Exception executing batch [%s]" )
    void unableToExecuteBatch( String message );

    @LogMessage( level = WARN )
    @Message( value = "Error executing resolver [%s] : %s" )
    void unableToExecuteResolver( AbstractDialectResolver abstractDialectResolver,
                                  String message );

    @Message( value = "Unable to find file (ignored): %s" )
    Object unableToFindFile( URL jarUrl );

    @LogMessage( level = INFO )
    @Message( value = "Unable to find %s on the classpath. Hibernate Search is not enabled." )
    void unableToFindListenerClass( String className );

    @LogMessage( level = INFO )
    @Message( value = "Could not find any META-INF/persistence.xml file in the classpath" )
    void unableToFindPersistenceXmlInClasspath();

    @LogMessage( level = ERROR )
    @Message( value = "Could not get database metadata" )
    void unableToGetDatabaseMetadata( @Cause SQLException e );

    @LogMessage( level = WARN )
    @Message( value = "Unable to instantiate configured schema name resolver [%s] %s" )
    void unableToInstantiateConfiguredSchemaNameResolver( String resolverClassName,
                                                          String message );

    @LogMessage( level = WARN )
    @Message( value = "Could not instantiate dialect resolver class : %s" )
    void unableToInstantiateDialectResolver( String message );

    @LogMessage( level = WARN )
    @Message( value = "Unable to instantiate specified optimizer [%s], falling back to noop" )
    void unableToInstantiateOptimizer( String type );

    @Message( value = "Failed to instantiate TransactionFactory" )
    String unableToInstantiateTransactionFactory();

    @Message( value = "Failed to instantiate TransactionManagerLookup '%s'" )
    String unableToInstantiateTransactionManagerLookup( String tmLookupClass );

    @LogMessage( level = WARN )
    @Message( value = "Unable to instantiate UUID generation strategy class : %s" )
    void unableToInstantiateUuidGenerationStrategy( Exception ignore );

    @LogMessage( level = WARN )
    @Message( value = "Cannot join transaction: do not override %s" )
    void unableToJoinTransaction( String transactionStrategy );

    @LogMessage( level = INFO )
    @Message( value = "Error performing load command : %s" )
    void unableToLoadCommand( HibernateException e );

    @LogMessage( level = WARN )
    @Message( value = "Unable to load/access derby driver class sysinfo to check versions : %s" )
    void unableToLoadDerbyDriver( String message );

    @LogMessage( level = ERROR )
    @Message( value = "Problem loading properties from hibernate.properties" )
    void unableToloadProperties();

    @Message( value = "Unable to locate config file: %s" )
    String unableToLocateConfigFile( String path );

    @LogMessage( level = WARN )
    @Message( value = "Unable to locate configured schema name resolver class [%s] %s" )
    void unableToLocateConfiguredSchemaNameResolver( String resolverClassName,
                                                     String message );

    @LogMessage( level = WARN )
    @Message( value = "Unable to locate MBeanServer on JMX service shutdown" )
    void unableToLocateMBeanServer();

    @LogMessage( level = INFO )
    @Message( value = "Could not locate 'java.sql.NClob' class; assuming JDBC 3" )
    void unableToLocateNClobClass();

    @LogMessage( level = ERROR )
    @Message( value = "Unable to locate static metamodel field : %s#%s" )
    void unableToLocateStaticMetamodelField( String name,
                                             String name2 );

    @LogMessage( level = WARN )
    @Message( value = "Unable to locate requested UUID generation strategy class : %s" )
    void unableToLocateUuidGenerationStrategy( String strategyClassName );

    @LogMessage( level = WARN )
    @Message( value = "Unable to log SQLWarnings : %s" )
    void unableToLogSqlWarnings( SQLException sqle );

    @Message( value = "Could not log warnings" )
    Object unableToLogWarnings();

    @LogMessage( level = ERROR )
    @Message( value = "Unable to mark for rollback on PersistenceException: " )
    void unableToMarkForRollbackOnPersistenceException( @Cause Exception e );

    @LogMessage( level = ERROR )
    @Message( value = "Unable to mark for rollback on TransientObjectException: " )
    void unableToMarkForRollbackOnTransientObjectException( @Cause Exception e );

    @LogMessage( level = WARN )
    @Message( value = "Could not obtain connection metadata: %s" )
    void unableToObjectConnectionMetadata( SQLException error );

    @LogMessage( level = WARN )
    @Message( value = "Could not obtain connection to query metadata: %s" )
    void unableToObjectConnectionToQueryMetadata( SQLException error );

    @LogMessage( level = WARN )
    @Message( value = "Could not obtain connection metadata : %s" )
    void unableToObtainConnectionMetadata( String message );

    @LogMessage( level = WARN )
    @Message( value = "Could not obtain connection to query metadata : %s" )
    void unableToObtainConnectionToQueryMetadata( String message );

    @LogMessage( level = ERROR )
    @Message( value = "Could not obtain initial context" )
    void unableToObtainInitialContext( @Cause NamingException e );

    @LogMessage( level = ERROR )
    @Message( value = "Could not parse the package-level metadata [%s]" )
    void unableToParseMetadata( String packageName );

    @Message( value = "JDBC commit failed" )
    String unableToPerformJdbcCommit();

    @LogMessage( level = ERROR )
    @Message( value = "Error during managed flush [%s]" )
    void unableToPerformManagedFlush( String message );

    @Message( value = "Unable to query java.sql.DatabaseMetaData" )
    String unableToQueryDatabaseMetadata();

    @LogMessage( level = ERROR )
    @Message( value = "Unable to read class: %s" )
    void unableToReadClass( String message );

    @LogMessage( level = INFO )
    @Message( value = "Could not read column value from result set: %s; %s" )
    void unableToReadColumnValueFromResultSet( String name,
                                               String message );

    @Message( value = "Could not read a hi value - you need to populate the table: %s" )
    String unableToReadHiValue( String tableName );

    @LogMessage( level = ERROR )
    @Message( value = "Could not read or init a hi value" )
    void unableToReadOrInitHiValue( @Cause SQLException e );

    @LogMessage( level = ERROR )
    @Message( value = "Unable to release batch statement..." )
    void unableToReleaseBatchStatement();

    @LogMessage( level = ERROR )
    @Message( value = "Could not release a cache lock : %s" )
    void unableToReleaseCacheLock( CacheException ce );

    @LogMessage( level = INFO )
    @Message( value = "Unable to release initial context: %s" )
    void unableToReleaseContext( String message );

    @LogMessage( level = WARN )
    @Message( value = "Unable to release created MBeanServer : %s" )
    void unableToReleaseCreatedMBeanServer( String string );

    @LogMessage( level = INFO )
    @Message( value = "Unable to release isolated connection [%s]" )
    void unableToReleaseIsolatedConnection( Throwable ignore );

    @LogMessage( level = WARN )
    @Message( value = "Unable to release type info result set" )
    void unableToReleaseTypeInfoResultSet();

    @LogMessage( level = WARN )
    @Message( value = "Unable to erase previously added bag join fetch" )
    void unableToRemoveBagJoinFetch();

    @LogMessage( level = TRACE )
    @Message( value = "Unable to reset connection back to auto-commit" )
    void unableToResetConnectionToAutoCommit();

    @LogMessage( level = INFO )
    @Message( value = "Could not resolve aggregate function {}; using standard definition" )
    void unableToResolveAggregateFunction( String name );

    @LogMessage( level = INFO )
    @Message( value = "Unable to resolve mapping file [%s]" )
    void unableToResolveMappingFile( String xmlFile );

    @LogMessage( level = INFO )
    @Message( value = "Unable to retreive cache from JNDI [%s]: %s" )
    void unableToRetrieveCache( String namespace,
                                String message );

    @LogMessage( level = WARN )
    @Message( value = "Unable to retrieve type info result set : %s" )
    void unableToRetrieveTypeInfoResultSet( String string );

    @LogMessage( level = INFO )
    @Message( value = "Unable to rollback connection on exception [%s]" )
    void unableToRollbackConnection( Exception ignore );

    @LogMessage( level = INFO )
    @Message( value = "Unable to rollback isolated transaction on error [%s] : [%s]" )
    void unableToRollbackIsolatedTransaction( Exception e,
                                              Exception ignore );

    @Message( value = "JTA rollback failed" )
    String unableToRollbackJta();

    @LogMessage( level = ERROR )
    @Message( value = "Error running schema update" )
    void unableToRunSchemaUpdate( @Cause Exception e );

    @LogMessage( level = ERROR )
    @Message( value = "Could not set transaction to rollback only" )
    void unableToSetTransactionToRollbackOnly( @Cause SystemException e );

    @Message( value = "Exception while stopping service" )
    Object unableToStopHibernateService();

    @LogMessage( level = INFO )
    @Message( value = "Error stopping service [%s] : %s" )
    void unableToStopService( Class class1,
                              String string );

    @LogMessage( level = ERROR )
    @Message( value = "Could not synchronize database state with session: %s" )
    void unableToSynchronizeDatabaseStateWithSession( HibernateException he );

    @LogMessage( level = ERROR )
    @Message( value = "Could not toggle autocommit" )
    void unableToToggleAutoCommit( @Cause Exception e );

    @LogMessage( level = ERROR )
    @Message( value = "Unable to transform class: %s" )
    void unableToTransformClass( String message );

    @Message( value = "Could not unbind factory from JNDI" )
    Object unableToUnbindFactoryFromJndi();

    @Message( value = "Could not update hi value in: %s" )
    Object unableToUpdateHiValue( String tableName );

    @LogMessage( level = ERROR )
    @Message( value = "Could not updateQuery hi value in: %s" )
    void unableToUpdateQueryHiValue( String tableName,
                                     @Cause SQLException error );

    @Message( value = "Error wrapping result set" )
    Object unableToWrapResultSet();

    @LogMessage( level = WARN )
    @Message( value = "I/O reported error writing cached file : %s: %s" )
    void unableToWriteCachedFile( String path,
                                  String message );

    @LogMessage( level = INFO )
    @Message( value = "Unbinding factory from JNDI name: %s" )
    void unbindingFactoryFromJndiName( String name );

    @LogMessage( level = WARN )
    @Message( value = "Unexpected literal token type [%d] passed for numeric processing" )
    void unexpectedLiteralTokenType( int type );

    @LogMessage( level = WARN )
    @Message( value = "JDBC driver did not return the expected number of row counts" )
    void unexpectedRowCounts();

    @LogMessage( level = WARN )
    @Message( value = "unrecognized bytecode provider [%s], using javassist by default" )
    void unknownBytecodeProvider( String providerName );

    @LogMessage( level = WARN )
    @Message( value = "Unknown Ingres major version [%d]; using Ingres 9.2 dialect" )
    void unknownIngresVersion( int databaseMajorVersion );

    @LogMessage( level = WARN )
    @Message( value = "Unknown Oracle major version [%d]" )
    void unknownOracleVersion( int databaseMajorVersion );

    @LogMessage( level = WARN )
    @Message( value = "Unknown Microsoft SQL Server major version [%d] using SQL Server 2000 dialect" )
    void unknownSqlServerVersion( int databaseMajorVersion );

    @LogMessage( level = WARN )
    @Message( value = "ResultSet had no statement associated with it, but was not yet registered" )
    void unregisteredResultSetWithoutStatement();

    @LogMessage( level = WARN )
    @Message( value = "ResultSet's statement was not registered" )
    void unregisteredStatement();

    @LogMessage( level = ERROR )
    @Message( value = "Unsuccessful: %s" )
    void unsuccessful( String sql );

    @LogMessage( level = ERROR )
    @Message( value = "Unsuccessful: %s" )
    void unsuccessfulCreate( String string );

    @LogMessage( level = WARN )
    @Message( value = "Overriding release mode as connection provider does not support 'after_statement'" )
    void unsupportedAfterStatement();

    @LogMessage( level = WARN )
    @Message( value = "Ingres 10 is not yet fully supported; using Ingres 9.3 dialect" )
    void unsupportedIngresVersion();

    @LogMessage( level = WARN )
    @Message( value = "Hibernate does not support SequenceGenerator.initialValue() unless '%s' set" )
    void unsupportedInitialValue( String propertyName );

    @LogMessage( level = WARN )
    @Message( value = "The %d.%d.%d version of H2 implements temporary table creation such that it commits current transaction; multi-table, bulk hql/jpaql will not work properly" )
    void unsupportedMultiTableBulkHqlJpaql( int majorVersion,
                                            int minorVersion,
                                            int buildId );

    @LogMessage( level = WARN )
    @Message( value = "Oracle 11g is not yet fully supported; using Oracle 10g dialect" )
    void unsupportedOracleVersion();

    @LogMessage( level = WARN )
    @Message( value = "Usage of obsolete property: %s no longer supported, use: %s" )
    void unsupportedProperty( Object propertyName,
                              Object newPropertyName );

    @LogMessage( level = INFO )
    @Message( value = "Updating schema" )
    void updatingSchema();

    @LogMessage( level = INFO )
    @Message( value = "Using ASTQueryTranslatorFactory" )
    void usingAstQueryTranslatorFactory();

    @LogMessage( level = INFO )
    @Message( value = "Explicit segment value for id generator [%s.%s] suggested; using default [%s]" )
    void usingDefaultIdGeneratorSegmentValue( String tableName,
                                              String segmentColumnName,
                                              String defaultToUse );

    @LogMessage( level = INFO )
    @Message( value = "Using default transaction strategy (direct JDBC transactions)" )
    void usingDefaultTransactionStrategy();

    @LogMessage( level = INFO )
    @Message( value = "Using dialect: %s" )
    void usingDialect( Dialect dialect );

    @LogMessage( level = INFO )
    @Message( value = "using driver [%s] at URL [%s]" )
    void usingDriver( String driverClassName,
                      String url );

    @LogMessage( level = INFO )
    @Message( value = "Using Hibernate built-in connection pool (not for production use!)" )
    void usingHibernateBuiltInConnectionPool();

    @LogMessage( level = INFO )
    @Message( value = "Using JDK 1.4 java.sql.Timestamp handling" )
    void usingJdk14TimestampHandling();

    @LogMessage( level = ERROR )
    @Message( value = "Don't use old DTDs, read the Hibernate 3.x Migration Guide!" )
    void usingOldDtd();

    @LogMessage( level = INFO )
    @Message( value = "Using pre JDK 1.4 java.sql.Timestamp handling" )
    void usingPreJdk14TimestampHandling();

    @LogMessage( level = INFO )
    @Message( value = "Using provided datasource" )
    void usingProvidedDataSource();

    @LogMessage( level = INFO )
    @Message( value = "Using bytecode reflection optimizer" )
    void usingReflectionOptimizer();

    @LogMessage( level = INFO )
    @Message( value = "Using java.io streams to persist binary types" )
    void usingStreams();

    @LogMessage( level = INFO )
    @Message( value = "Using workaround for JVM bug in java.sql.Timestamp" )
    void usingTimestampWorkaround();

    @LogMessage( level = WARN )
    @Message( value = "Using %s which does not generate IETF RFC 4122 compliant UUID values; consider using %s instead" )
    void usingUuidHexGenerator( String name,
                                String name2 );

    @LogMessage( level = INFO )
    @Message( value = "Hibernate Validator not found: ignoring" )
    void validatorNotFound();

    @LogMessage( level = WARN )
    @Message( value = "Value mapping mismatch as part of FK [table=%s, name=%s] while adding source column [%s]" )
    void valueMappingMismatch( String loggableString,
                               String name,
                               String loggableString2 );

    @LogMessage( level = INFO )
    @Message( value = "Hibernate %s" )
    void version( String versionString );

    @LogMessage( level = WARN )
    @Message( value = "Warnings creating temp table : %s" )
    void warningsCreatingTempTable( SQLWarning warning );

    @LogMessage( level = INFO )
    @Message( value = "Property hibernate.search.autoregister_listeners is set to false. No attempt will be made to register Hibernate Search event listeners." )
    void willNotRegisterListeners();

    @LogMessage( level = INFO )
    @Message( value = "Wrap result sets: %s" )
    void wrapResultSets( String enabledDisabled );

    @LogMessage( level = WARN )
    @Message( value = "Write locks via update not supported for non-versioned entities [%s]" )
    void writeLocksNotSupported( String entityName );

    @LogMessage( level = INFO )
    @Message( value = "Writing generated schema to file: %s" )
    void writingGeneratedSchemaToFile( String outputFile );
}
