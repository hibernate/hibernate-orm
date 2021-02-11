/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.bytecode;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import static org.jboss.logging.Logger.Level.WARN;

/**
 * @author Steve Ebersole
 */
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 90005901, max = 90006000 )
public interface BytecodeLogging extends BasicLogger {
	String NAME = "org.hibernate.orm.bytecode";

	Logger LOGGER = Logger.getLogger( NAME );
	BytecodeLogging MESSAGE_LOGGER = Logger.getMessageLogger( BytecodeLogging.class, NAME );

	boolean TRACE_ENABLED = LOGGER.isTraceEnabled();
	boolean DEBUG_ENABLED = LOGGER.isDebugEnabled();

	@LogMessage( level = WARN )
	@Message(
			id = 90005901,
			value = "`%s#%s` was mapped with explicit lazy-group (`%s`).  Hibernate will ignore the lazy-group - this is generally " +
					"not a good idea for to-one associations as it would lead to 2 separate SQL selects to initialize the association.  " +
					"This is expected to be improved in future versions of Hibernate"
	)
	void lazyGroupIgnoredForToOne(String ownerName, String attributeName, String requestedLazyGroup);
}
