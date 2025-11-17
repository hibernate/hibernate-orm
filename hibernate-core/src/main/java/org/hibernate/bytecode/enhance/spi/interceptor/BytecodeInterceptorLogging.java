/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.spi.interceptor;

import org.hibernate.Internal;
import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;

import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Logging related to bytecode enhancement interceptors
 */
@MessageLogger(projectCode = "HHH")
@ValidIdRange(min = 90005901, max = 90006000)
@SubSystemLogging(
		name = BytecodeInterceptorLogging.LOGGER_NAME,
		description = "Logging related to bytecode-based interception"
)
@Internal
public interface BytecodeInterceptorLogging extends BasicLogger {
	String LOGGER_NAME = SubSystemLogging.BASE + ".bytecode.interceptor";

	Logger LOGGER = Logger.getLogger( LOGGER_NAME );
	BytecodeInterceptorLogging BYTECODE_INTERCEPTOR_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), BytecodeInterceptorLogging.class, LOGGER_NAME );

	@LogMessage(level = WARN)
	@Message(
			id = 90005901,
			value = "Ignoring explicit lazy group '%s' specified for association '%s.%s'"
					+ " (a lazy group for a to-one association would lead to two separate SELECTs to initialize the association)"
	)
	void lazyGroupIgnoredForToOne(String requestedLazyGroup, String ownerName, String attributeName);

	@LogMessage(level = TRACE)
	@Message(id = 90005902, value = "Forcing initialization: %s.%s -> %s")
	void enhancementAsProxyLazinessForceInitialize(String entityName, Object identifier, String attributeName);

	@LogMessage(level = WARN)
	@Message(
			id = 90005903,
			value = "Unable to commit JDBC transaction on temporary session used to load lazy collection associated to no session"
	)
	void unableToCommitTransactionOnTemporarySession();

	@LogMessage(level = WARN)
	@Message(
			id = 90005904,
			value = "Unable to close temporary session used to load lazy collection associated to no session"
	)
	void unableToCloseTemporarySession();

	// DEBUG messages (type-safe)

	@LogMessage(level = DEBUG)
	@Message(
			id = 90005905,
			value = "To-one property '%s.%s' was mapped with LAZY + NO_PROXY but the class was not enhanced"
	)
	void toOneLazyNoProxyButNotEnhanced(String ownerName, String attributeName);

	@LogMessage(level = DEBUG)
	@Message(
			id = 90005906,
			value = "'%s.%s' was mapped with LAZY and explicit NO_PROXY but the associated entity ('%s') has subclasses"
	)
	void lazyNoProxyButAssociatedHasSubclasses(String ownerName, String attributeName, String associatedEntityName);

	@LogMessage(level = DEBUG)
	@Message(
			id = 90005907,
			value = "'%s.%s' specified NotFoundAction.IGNORE & LazyToOneOption.NO_PROXY;"
					+ " skipping foreign key selection to more efficiently handle NotFoundAction.IGNORE"
	)
	void notFoundIgnoreWithNoProxySkippingFkSelection(String ownerName, String attributeName);

	@LogMessage(level = DEBUG)
	@Message(id = 90005908, value = "Enhancement interception started temporary Session")
	void enhancementHelperStartedTemporarySession();

	@LogMessage(level = DEBUG)
	@Message(id = 90005909, value = "Enhancement interception starting transaction on temporary Session")
	void enhancementHelperStartingTransactionOnTemporarySession();

	@LogMessage(level = DEBUG)
	@Message(id = 90005910, value = "Enhancement interception committing transaction on temporary Session")
	void enhancementHelperCommittingTransactionOnTemporarySession();

	@LogMessage(level = DEBUG)
	@Message(id = 90005911, value = "Enhancement interception closing temporary Session")
	void enhancementHelperClosingTemporarySession();
}
