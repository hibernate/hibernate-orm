/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.enhanced;

import org.hibernate.Internal;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Logging related to Optimizer operations in identifier generation
 */
@SubSystemLogging(
		name = OptimizerLogger.NAME,
		description = "Logging related to identifier generator optimizers"
)
@MessageLogger(projectCode = "HHH")
@ValidIdRange(min = 90401, max = 90500)
@Internal
public interface OptimizerLogger extends BasicLogger {
	String NAME = SubSystemLogging.BASE + ".id.optimizer";

	OptimizerLogger OPTIMIZER_MESSAGE_LOGGER = Logger.getMessageLogger(
			MethodHandles.lookup(),
			OptimizerLogger.class,
			NAME
	);

	@LogMessage(level = TRACE)
	@Message(value = "Creating hilo optimizer with [incrementSize=%s, returnClass=%s]", id = 90401)
	void creatingHiLoOptimizer(int incrementSize, String returnClassName);

	@LogMessage(level = TRACE)
	@Message(value = "Creating hilo optimizer (legacy) with [incrementSize=%s, returnClass=%s]", id = 90402)
	void creatingLegacyHiLoOptimizer(int incrementSize, String returnClassName);

	@LogMessage(level = TRACE)
	@Message(value = "Creating pooled optimizer with [incrementSize=%s, returnClass=%s]", id = 90403)
	void creatingPooledOptimizer(int incrementSize, String returnClassName);

	@LogMessage(level = DEBUG)
	@Message(value = "Creating pooled optimizer (lo) with [incrementSize=%s, returnClass=%s]", id = 90404)
	void creatingPooledLoOptimizer(int incrementSize, String returnClassName);

	@LogMessage(level = INFO)
	@Message(value = "Pooled optimizer source reported [%s] as the initial value; use of 1 or greater highly recommended", id = 90405)
	void pooledOptimizerReportedInitialValue(IntegralDataTypeHolder value);

	@LogMessage(level = WARN)
	@Message(value = "Unable to interpret specified optimizer [%s], falling back to noop optimizer", id = 90406)
	void unableToLocateCustomOptimizerClass(String type);

	@LogMessage(level = WARN)
	@Message(value = "Unable to instantiate specified optimizer [%s], falling back to noop optimizer", id = 90407)
	void unableToInstantiateOptimizer(String type);
}
