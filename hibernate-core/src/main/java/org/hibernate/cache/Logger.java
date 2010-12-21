/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.hibernate.cache;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;
import java.io.Serializable;
import java.util.Set;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * Interface defining messages that may be logged by the outer class
 */
@MessageLogger
interface Logger extends BasicLogger {

    @LogMessage( level = DEBUG )
    @Message( value = "Cached: %s" )
    void cached( Object key );

    @LogMessage( level = DEBUG )
    @Message( value = "Caching: %s" )
    void caching( Object key );

    @LogMessage( level = DEBUG )
    @Message( value = "Caching after insert: %s" )
    void cachingAfterInsert( Object key );

    @LogMessage( level = DEBUG )
    @Message( value = "Caching query results in region: %s; timestamp=%s" )
    void cachingQueryResults( String region,
                              Long ts );

    @LogMessage( level = DEBUG )
    @Message( value = "Checking cached query results in region: %s" )
    void checkingQueryResults( String region );

    @LogMessage( level = DEBUG )
    @Message( value = "Checking query spaces are up-to-date: %s" )
    void checkingQuerySpacesUpToDate( Set<Serializable> spaces );

    @LogMessage( level = DEBUG )
    @Message( value = "Clearing" )
    void clearing();

    @LogMessage( level = DEBUG )
    @Message( value = "Item already cached: %s" )
    void exists( Object key );

    @LogMessage( level = WARN )
    @Message( value = "An item was expired by the cache while it was locked (increase your cache timeout): %s" )
    void expired( Object key );

    @LogMessage( level = DEBUG )
    @Message( value = "Cache hit: %s" )
    void hit( Object key );

    @LogMessage( level = DEBUG )
    @Message( value = "Inserted: %s" )
    void inserted( Object key );

    @LogMessage( level = DEBUG )
    @Message( value = "Inserting: %s" )
    void inserting( Object key );

    @LogMessage( level = DEBUG )
    @Message( value = "Invalidating: %s" )
    void invalidating( Object key );

    @LogMessage( level = DEBUG )
    @Message( value = "Invalidating space [%s], timestamp: %s" )
    void invalidatingSpace( Serializable space,
                            Long ts );

    @LogMessage( level = ERROR )
    @Message( value = "Application attempted to edit read only item: %s" )
    void invalidEditOfReadOnlyItem( Object key );

    @LogMessage( level = TRACE )
    @Message( value = "key.hashCode=%s" )
    void key( int hashCode );

    @LogMessage( level = DEBUG )
    @Message( value = "Cached item was locked: %s" )
    void locked( Object key );

    @LogMessage( level = DEBUG )
    @Message( value = "Cache lookup: %s" )
    void lookup( Object key );

    @LogMessage( level = DEBUG )
    @Message( value = "Cache miss: %s" )
    void miss( Object key );

    @LogMessage( level = TRACE )
    @Message( value = " tuple is null; returnTypes is %s" )
    void nullTuple( String returnTypesState );

    @LogMessage( level = DEBUG )
    @Message( value = "Pre-invalidating space [%s]" )
    void preInvalidatingSpace( Serializable space );

    @LogMessage( level = DEBUG )
    @Message( value = "Query results were not found in cache" )
    void queryResultsNotFound();

    @LogMessage( level = DEBUG )
    @Message( value = "Cached query results were not up-to-date" )
    void queryResultsNotUpToDate();

    @LogMessage( level = TRACE )
    @Message( value = "querySpaces=%s" )
    void querySpaces( Set<Serializable> spaces );

    @LogMessage( level = DEBUG )
    @Message( value = "Releasing: %s" )
    void releasing( Object key );

    @LogMessage( level = DEBUG )
    @Message( value = "Removing: %s" )
    void removing( Object key );

    @LogMessage( level = DEBUG )
    @Message( value = "Returning cached query results" )
    void returningQueryResults();

    @LogMessage( level = TRACE )
    @Message( value = "unexpected returnTypes is %s! result%s" )
    void returnTypeInfo( String returnTypeInfo );

    @LogMessage( level = DEBUG )
    @Message( value = "[%s] last update timestamp: %d, result set timestamp: %d" )
    void spaceLastUpdated( Serializable space,
                           long lastUpdate,
                           long timestamp );

    @LogMessage( level = INFO )
    @Message( value = "Starting query cache at region: %s" )
    void startingQueryCache( String region );

    @LogMessage( level = INFO )
    @Message( value = "Starting update timestamps cache at region: %s" )
    void startingUpdateTimestampsCache( String region );

    @LogMessage( level = TRACE )
    @Message( value = " tuple is Object[%d]; returnTypes is Type[%d]" )
    void tupleAndReturnTypes( int tupleCount,
                              int returnTypeCount );

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

    @LogMessage( level = DEBUG )
    @Message( value = "Unable to reassemble cached result set" )
    void unableToReassembleResultSet();

    @LogMessage( level = INFO )
    @Message( value = "Unable to release initial context: %s" )
    void unableToReleaseContext( String message );

    @LogMessage( level = INFO )
    @Message( value = "Unable to retreive cache from JNDI [%s]: %s" )
    void unableToRetrieveCache( String namespace,
                                String message );

    @LogMessage( level = TRACE )
    @Message( value = "Unexpected result tuple! tuple is null; returnTypes is %s" )
    void unexpectedNonNullTupleResult( String returnTypesState );

    @LogMessage( level = TRACE )
    @Message( value = "Unexpected result tuple! tuple is null; should be Object[%d]!" )
    void unexpectedNullTupleResult( int returnTypeCount );

    @LogMessage( level = TRACE )
    @Message( value = "Unexpected returnTypes is %s! result%s" )
    void unexpectedReturnTypes( String returnTypesState,
                                String resultState );

    @LogMessage( level = TRACE )
    @Message( value = "Unexpected tuple length! transformer= expected=%d got=%d" )
    void unexpectedTupleCount( int returntypesCount,
                               int tupleCount );

    @LogMessage( level = TRACE )
    @Message( value = "Unexpected tuple value type! transformer= expected=%s got=%s" )
    void unexpectedTupleValueType( String returnTypeClassName,
                                   String tupleClassName );

    @LogMessage( level = DEBUG )
    @Message( value = "Updated: %s" )
    void updated( Object key );

    @LogMessage( level = DEBUG )
    @Message( value = "Updating: %s" )
    void updating( Object key );
}
