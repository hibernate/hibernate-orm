/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.enhanced;

import org.hibernate.Internal;
import org.hibernate.internal.log.SubSystemLogging;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Logging related to {@link TableGenerator} operations
 *
 * @author Gavin King
 */
@SubSystemLogging(
		name = SequenceGeneratorLogger.NAME,
		description = "Logging related to sequence-based identifier generation"
)
@MessageLogger(projectCode = "HHH")
@ValidIdRange(min = 90201, max = 90300)
@Internal
public interface SequenceGeneratorLogger extends BasicLogger {
	String NAME = SubSystemLogging.BASE + ".id.table";

	SequenceGeneratorLogger SEQUENCE_GENERATOR_MESSAGE_LOGGER = Logger.getMessageLogger(
			MethodHandles.lookup(),
			SequenceGeneratorLogger.class,
			NAME
	);

	@LogMessage(level = INFO)
	@Message(value = "Forcing table use for sequence-style generator due to pooled optimizer selection where db does not support pooled sequences",
			id = 90201)
	void forcingTableUse();

	@LogMessage(level = WARN)
	@Message(value = "The increment size of the sequence '%s' is set to [%d] in the entity mapping but the mapped database sequence increment size is [%d]",
			id = 90202)
	void sequenceIncrementSizeMismatch(String sequenceName, int incrementSize, int databaseIncrementSize);

	@LogMessage(level = TRACE)
	@Message(value = "The increment size of the sequence '%s' is set to [%d] in the entity mapping but the mapped database sequence increment size is [%d]"
					+ " - the database sequence increment size will take precedence to avoid identifier allocation conflicts.",
			id = 90203)
	void sequenceIncrementSizeMismatchFixed(String sequenceName, int incrementSize, int databaseIncrementSize);

	@LogMessage(level = WARN)
	@Message(value = "Sequence-style generator configuration specified explicit optimizer [%s], but [%s=%s]; using optimizer [%s] increment default of [%s]",
			id = 90205)
	void honoringOptimizerSetting(
			String none,
			String incrementParam,
			int incrementSize,
			String positiveOrNegative,
			int defaultIncrementSize);

}
