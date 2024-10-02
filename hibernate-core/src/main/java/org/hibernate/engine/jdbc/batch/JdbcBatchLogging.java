/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.engine.jdbc.batch;

import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Sub-system logging related to JDBC batch execution
 *
 * @author Steve Ebersole
 */
@SubSystemLogging(
		name = JdbcBatchLogging.NAME,
		description = "Logging related to JDBC batch execution"
)
@MessageLogger(projectCode = "HHH")
@ValidIdRange(min = 100501, max = 101000)
public interface JdbcBatchLogging extends BasicLogger {
	String NAME = "org.hibernate.orm.jdbc.batch";

	Logger BATCH_LOGGER = Logger.getLogger( NAME );
	JdbcBatchLogging BATCH_MESSAGE_LOGGER = Logger.getMessageLogger( JdbcBatchLogging.class, NAME );

	@LogMessage(level = WARN)
	@Message(id = 100502, value = "Unable to release batch statement...")
	void unableToReleaseBatchStatement();

	@LogMessage(level = INFO)
	@Message(id=100503, value = "On release of batch it still contained JDBC statements")
	void batchContainedStatementsOnRelease();
}
