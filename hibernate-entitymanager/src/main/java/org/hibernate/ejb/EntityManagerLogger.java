/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.hibernate.ejb;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;
import java.net.URL;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * Interface defining messages that may be logged by the outer class
 */
@MessageLogger
public interface EntityManagerLogger extends BasicLogger {

    @LogMessage( level = INFO )
    @Message( value = "Bound Ejb3Configuration to JNDI name: %s" )
    void boundEjb3ConfigurationToJndiName( String name );

    @LogMessage( level = WARN )
    @Message( value = "Calling joinTransaction() on a non JTA EntityManager" )
    void callingJoinTransactionOnNonJtaEntityManager();

    @LogMessage( level = ERROR )
    @Message( value = "Container is providing a null PersistenceUnitRootUrl: discovery impossible" )
    void containerProvidingNullPersistenceUnitRootUrl();

    @LogMessage( level = WARN )
    @Message( value = "Defining %s=true ignored in HEM" )
    void definingFlushBeforeCompletionIgnoredInHem( String flushBeforeCompletion );

    @LogMessage( level = INFO )
    @Message( value = "Ejb3Configuration name: %s" )
    void ejb3ConfigurationName( String name );

    @LogMessage( level = INFO )
    @Message( value = "An Ejb3Configuration was renamed from name: %s" )
    void ejb3ConfigurationRenamedFromName( String name );

    @LogMessage( level = INFO )
    @Message( value = "An Ejb3Configuration was unbound from name: %s" )
    void ejb3ConfigurationUnboundFromName( String name );

    @LogMessage( level = WARN )
    @Message( value = "Entity Manager closed by someone else (%s must not be used)" )
    void entityManagerClosedBySomeoneElse( String autoCloseSession );

    @LogMessage( level = INFO )
    @Message( value = "Hibernate EntityManager %s" )
    void entityManagerVersion( String versionString );

    @LogMessage( level = INFO )
    @Message( value = "%s %s found" )
    void exceptionHeaderFound( String exceptionHeader,
                               String metaInfOrmXml );

    @LogMessage( level = INFO )
    @Message( value = "%s No %s found" )
    void exceptionHeaderNotFound( String exceptionHeader,
                                  String metaInfOrmXml );

    @LogMessage( level = WARN )
    @Message( value = "Exploded jar file does not exist (ignored): %s" )
    void explodedJarDoesNotExist( URL jarUrl );

    @LogMessage( level = WARN )
    @Message( value = "Exploded jar file not a directory (ignored): %s" )
    void explodedJarNotDirectory( URL jarUrl );

    @LogMessage( level = INFO )
    @Message( value = "Ignoring unrecognized query hint [%s]" )
    void ignoringUnrecognizedQueryHint( String hintName );

    @LogMessage( level = ERROR )
    @Message( value = "Illegal argument on static metamodel field injection : %s#%s; expected type :  %s; encountered type : %s" )
    void illegalArgumentOnStaticMetamodelFieldInjection( String name,
                                                         String name2,
                                                         String name3,
                                                         String name4 );

    @LogMessage( level = WARN )
    @Message( value = "InitialContext did not implement EventContext" )
    void initialContextDoesNotImplementEventContext();

    @Message( value = "Invalid JNDI name: %s" )
    Object invalidJndiName( String name );

    @LogMessage( level = WARN )
    @Message( value = "%s = false break the EJB3 specification" )
    void jdbcAutoCommitFalseBreaksEjb3Spec( String autocommit );

    @Message( value = "Malformed URL: %s" )
    Object malformedUrl( URL jarUrl );

    @LogMessage( level = WARN )
    @Message( value = "Overriding %s is dangerous, this might break the EJB3 specification implementation" )
    void overridingTransactionStrategyDangerous( String transactionStrategy );

    @LogMessage( level = WARN )
    @Message( value = "Parameter position [%s] occurred as both JPA and Hibernate positional parameter" )
    void parameterPositionOccurredAsBothJpaAndHibernatePositionalParameter( Integer position );

    @LogMessage( level = WARN )
    @Message( value = "Persistence provider caller does not implement the EJB3 spec correctly."
                      + "PersistenceUnitInfo.getNewTempClassLoader() is null." )
    void persistenceProviderCallerDoesNotImplementEjb3SpecCorrectly();

    @LogMessage( level = INFO )
    @Message( value = "Processing PersistenceUnitInfo [\n\tname: %s\n\t...]" )
    void processingPersistenceUnitInfoName( String persistenceUnitName );

    @LogMessage( level = INFO )
    @Message( value = "Required a different provider: %s" )
    void requiredDifferentProvider( String provider );

    @LogMessage( level = WARN )
    @Message( value = "Transaction not available on beforeCompletion: assuming valid" )
    void transactionNotAvailableOnBeforeCompletion();

    @Message( value = "Naming exception occurred accessing Ejb3Configuration" )
    Object unableToAccessEjb3Configuration();

    @Message( value = "Could not bind Ejb3Configuration to JNDI" )
    Object unableToBindEjb3ConfigurationToJndi();

    @Message( value = "Could not close input stream" )
    Object unableToCloseInputStream();

    @LogMessage( level = INFO )
    @Message( value = "Unable to determine lock mode value : %s -> %s" )
    void unableToDetermineLockModeValue( String hintName,
                                         Object value );

    @Message( value = "Unable to find file (ignored): %s" )
    Object unableToFindFile( URL jarUrl );

    @LogMessage( level = INFO )
    @Message( value = "Could not find any META-INF/persistence.xml file in the classpath" )
    void unableToFindPersistenceXmlInClasspath();

    @LogMessage( level = WARN )
    @Message( value = "Cannot join transaction: do not override %s" )
    void unableToJoinTransaction( String transactionStrategy );

    @LogMessage( level = ERROR )
    @Message( value = "Unable to locate static metamodel field : %s#%s" )
    void unableToLocateStaticMetamodelField( String name,
                                             String name2 );

    @Message( value = "Unable to mark for rollback on PersistenceException: " )
    Object unableToMarkForRollbackOnPersistenceException();

    @Message( value = "Unable to mark for rollback on TransientObjectException: " )
    Object unableToMarkForRollbackOnTransientObjectException();

    @LogMessage( level = INFO )
    @Message( value = "Unable to resolve mapping file [%s]" )
    void unableToResolveMappingFile( String xmlFile );

    @LogMessage( level = INFO )
    @Message( value = "Using provided datasource" )
    void usingProvidedDataSource();
}
