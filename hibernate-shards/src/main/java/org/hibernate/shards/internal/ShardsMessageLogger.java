package org.hibernate.shards.internal;

import org.hibernate.internal.CoreMessageLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.FATAL;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

// TODO: Identify which range will be used for Shards.

/**
 * The jboss-logging {@link MessageLogger} for the hibernate-envers module.  It reserves message ids ranging from
 * 75001 to 80000 inclusively.
 * <p/>
 * New messages must be added after the last message defined to ensure message codes are unique.
 *
 * @author Adriano Machado
 */
@MessageLogger( projectCode = "HHH" )
public interface ShardsMessageLogger extends CoreMessageLogger {

    @LogMessage( level = ERROR )
    @Message( value = "Exit Operation is not ready to use", id = 75001 )
    void exitOperationNotReadyToUse();

    @LogMessage( level = ERROR )
    @Message( value = "Result array is wrong size.  Expected 2 but found %s", id = 75002 )
    void resultArrayIsWrongSize(int length);

    @LogMessage( level = ERROR )
    @Message( value = "Distinct is not ready yet", id = 75003 )
    void distinctIsNotReadyYet();

    @LogMessage( level = ERROR )
    @Message( value = "Adding an unsupported Projection: %s", id = 75004 )
    void addingUnsupportedProjection(String projectionClass);

    @LogMessage( level = ERROR )
    @Message( value = "Use of unsupported aggregate: %s", id = 75005 )
    void useOfUnsupportedAggregate(String aggregateName);

    @LogMessage( level = ERROR )
    @Message( value = "Aggregation Projection is unsupported: %s", id = 75006 )
    void unsupportedAggregateProjection(String aggregate);

    @LogMessage( level = ERROR )
    @Message( value = "Waiting for threads to complete processing before proceeding.", id = 75007 )
    void waitingForThreadsToComplete();

    @LogMessage( level = ERROR )
    @Message( value = "Received unexpected exception while waiting for done signal.", id = 75008 )
    void receivedUnexpectedException();

    @LogMessage( level = DEBUG )
    @Message( value = "Compiling results.", id = 75010 )
    void compilingResults();

    @LogMessage( level = ERROR )
    @Message( value = "Cannot have more than one shard with shard id %d.", id = 75011 )
    void cannotHaveMoreThanOneShardWithSameId(int id);

    @LogMessage( level = WARN )
    @Message( value = "ShardedSessionFactoryImpl is being garbage collected but it was never properly closed.", id = 75012 )
    void shardedSessionFactoryImplIsBeingGCButWasNotClosedProperly();

    @LogMessage( level = WARN )
    @Message( value = "Caught exception when trying to close.", id = 75013 )
    void caughtExceptionWhenTryingToClose();

    @LogMessage( level = DEBUG )
    @Message( value = "Short-circuiting operation %s after execution against shard %s", id = 75014 )
    void shortCircuitingOperationAfterExecutionAgainstShard(String operationName, String shard);

    @LogMessage( level = ERROR )
    @Message( value = "Attempt to save object of type %s as a top-level object.", id = 75015 )
    void attemptToSaveObjectAsTopLevelObject(String name);

    @LogMessage( level = WARN )
    @Message( value = "ShardedSessionImpl is being garbage collected but it was never properly closed.", id = 75016 )
    void shardedSessionImplIsBeingGCButWasNotClosedProperly();

    @LogMessage( level = WARN )
    @Message( value = "Object of type %s is on shard %d but an associated object of type %s is on different shard.", id = 75017 )
    void objectOfTypeIsOnOneShardButAssociatedObjectIsOnADifferentShard(String classOfUpdatedObject,
                                                                        int expectedShardId,
                                                                        String classOfAssociatedObject,
                                                                        @Cause Exception e);

    @LogMessage( level = WARN )
    @Message( value = "Object of type %s is on shard %d but an associated object of type %s is on shard %d.", id = 75018 )
    void objectOfTypeIsOnOneShardButAssociatedObjectIsOnShard(String classOfUpdatedObject,
                                                              int expectedShardId,
                                                              String classOfAssociatedObject,
                                                              int localShardId);

    @LogMessage( level = DEBUG )
    @Message( value = "Task %d: Run invoked.", id = 75019 )
    void taskRunInvoked(int taskId);

    @LogMessage( level = DEBUG )
    @Message( value = "Task %d: Task will run.", id = 75020 )
    void taskWillNotRun(int taskId);

    @LogMessage( level = DEBUG )
    @Message( value = "Task %d: Task cancelled.", id = 75021 )
    void taskWasCancelled(int taskId);

    @LogMessage( level = FATAL )
    @Message( value = "Attempt to build a ShardedSessionFactory using a ShardConfiguration that has a null shard id.", id = 75022 )
    void attempToBuildShardedSessionFactoryWithNullShardId();

    @LogMessage( level = INFO )
    @Message( value = "Class %s does not support top-level saves.", id = 75023 )
    void classDoesNotSupportTopLevelSaves(String mappedClassName);
}

