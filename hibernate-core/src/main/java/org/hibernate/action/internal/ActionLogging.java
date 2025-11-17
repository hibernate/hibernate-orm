/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.internal;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.hibernate.Internal;
import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Logging related to the action queue.
 */
@MessageLogger(projectCode = "HHH")
@ValidIdRange(min = 90032001, max = 90033000)
@SubSystemLogging(
		name = ActionLogging.NAME,
		description = "Logging related to the action queue"
)
@Internal
public interface ActionLogging extends BasicLogger {
	String NAME = SubSystemLogging.BASE + ".action";

	ActionLogging ACTION_LOGGER = Logger.getMessageLogger(
			MethodHandles.lookup(), ActionLogging.class, NAME
	);

	int NAMESPACE = 90032000;

	@LogMessage(level = TRACE)
	@Message(
			value = "Adding insert with non-nullable, transient entities; insert=[%s], dependencies=[%s]",
			id = NAMESPACE + 1
	)
	void addingInsertWithNonNullableTransientEntities(Object insert, String dependenciesLoggableString);

	@LogMessage(level = TRACE)
	@Message(
			value = "No entity insert actions have non-nullable, transient entity dependencies",
			id = NAMESPACE + 2
	)
	void noEntityInsertActionsHaveNonNullableTransientDependencies();

	@LogMessage(level = WARN)
	@Message(
			id = NAMESPACE + 3,
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


	@LogMessage(level = TRACE)
	@Message(
			value = "No unresolved entity inserts that depended on [%s]",
			id = NAMESPACE + 4
	)
	void noUnresolvedEntityInsertsThatDependedOn(String entityInfoString);

	@LogMessage(level = TRACE)
	@Message(
			value = "Unresolved inserts before resolving [%s]: [%s]",
			id = NAMESPACE + 5
	)
	void unresolvedInsertsBeforeResolving(String entityInfoString, String unresolvedState);

	@LogMessage(level = TRACE)
	@Message(
			value = "Resolving insert [%s] dependency on [%s]",
			id = NAMESPACE + 6
	)
	void resolvingInsertDependencyOn(String dependentInfo, String entityInfo);

	@LogMessage(level = TRACE)
	@Message(
			value = "Resolving insert [%s] (only depended on [%s])",
			id = NAMESPACE + 7
	)
	void resolvingInsertOnlyDependedOn(Object dependentAction, String entityInfo);

	@LogMessage(level = TRACE)
	@Message(
			value = "Unresolved inserts after resolving [%s]: [%s]",
			id = NAMESPACE + 8
	)
	void unresolvedInsertsAfterResolving(String entityInfo, String unresolvedState);

	@LogMessage(level = TRACE)
	@Message(
			value = "Starting serialization of [%s] unresolved insert entries",
			id = NAMESPACE + 9
	)
	void serializingUnresolvedInsertEntries(int queueSize);

	@LogMessage(level = TRACE)
	@Message(
			value = "Starting deserialization of [%s] unresolved insert entries",
			id = NAMESPACE + 10
	)
	void deserializingUnresolvedInsertEntries(int queueSize);

	// ActionQueue
	@LogMessage(level = TRACE)
	@Message(
			value = "Adding an EntityInsertAction for entity of type [%s]",
			id = NAMESPACE + 11
	)
	void addingEntityInsertAction(String entityName);

	@LogMessage(level = TRACE)
	@Message(
			value = "Executing inserts before finding non-nullable transient entities for early insert: [%s]",
			id = NAMESPACE + 12
	)
	void executingInsertsBeforeFindingNonNullableTransientEntitiesForEarlyInsert(Object insertAction);

	@LogMessage(level = TRACE)
	@Message(
			value = "Adding insert with no non-nullable, transient entities: [%s]",
			id = NAMESPACE + 13
	)
	void addingInsertWithNoNonNullableTransientEntities(Object insertAction);

	@LogMessage(level = TRACE)
	@Message(
			value = "Executing insertions before resolved early insert",
			id = NAMESPACE + 14
	)
	void executingInsertionsBeforeResolvedEarlyInsert();

	@LogMessage(level = TRACE)
	@Message(
			value = "Executing identity insert immediately",
			id = NAMESPACE + 15
	)
	void executingIdentityInsertImmediately();

	@LogMessage(level = TRACE)
	@Message(
			value = "Adding resolved non-early insert action.",
			id = NAMESPACE + 16
	)
	void addingResolvedNonEarlyInsertAction();

	@LogMessage(level = TRACE)
	@Message(
			value = "Adding an EntityIdentityInsertAction for entity of type [%s]",
			id = NAMESPACE + 17
	)
	void addingEntityIdentityInsertAction(String entityName);

	@LogMessage(level = TRACE)
	@Message(
			value = "Changes must be flushed to space: %s",
			id = NAMESPACE + 18
	)
	void changesMustBeFlushedToSpace(Object space);

	@LogMessage(level = TRACE)
	@Message(
			value = "Serializing action queue",
			id = NAMESPACE + 19
	)
	void serializingActionQueue();

	@LogMessage(level = TRACE)
	@Message(
			value = "Deserializing action queue",
			id = NAMESPACE + 20
	)
	void deserializingActionQueue();

	@LogMessage(level = TRACE)
	@Message(
			value = "Deserialized [%s] entries",
			id = NAMESPACE + 21
	)
	void deserializedEntries(int count);

	@LogMessage(level = WARN)
	@Message(
			value = "Batch containing %s statements could not be sorted (might indicate a circular entity relationship)",
			id = NAMESPACE + 22
	)
	void batchCouldNotBeSorted(int count);
}
