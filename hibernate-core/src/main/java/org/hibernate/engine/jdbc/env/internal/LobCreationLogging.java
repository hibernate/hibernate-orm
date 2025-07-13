/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.env.internal;

import org.hibernate.Internal;
import org.hibernate.boot.BootLogging;
import org.hibernate.engine.jdbc.JdbcLogging;
import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;

import static org.jboss.logging.Logger.Level.DEBUG;

/**
 * @author Steve Ebersole
 */
@SubSystemLogging(
		name = BootLogging.NAME,
		description = "Logging related to contextual LOB creation"
)
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 10010001, max = 10010050 )
@Internal
public interface LobCreationLogging extends BasicLogger {
	String NAME = JdbcLogging.NAME + ".lob";

	Logger LOB_LOGGER = Logger.getLogger( NAME );
	LobCreationLogging LOB_MESSAGE_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), LobCreationLogging.class, NAME );

	@LogMessage(level = DEBUG)
	@Message(value = "Disabling contextual LOB creation as %s is true", id = 10010001)
	void disablingContextualLOBCreation(String settingName);

	@LogMessage(level = DEBUG)
	@Message(value = "Disabling contextual LOB creation as connection was null", id = 10010002)
	void disablingContextualLOBCreationSinceConnectionNull();

	@LogMessage(level = DEBUG)
	@Message(value = "Disabling contextual LOB creation as JDBC driver reported JDBC version [%s] less than 4", id = 10010003)
	void nonContextualLobCreationJdbcVersion(int jdbcMajorVersion);

	@LogMessage(level = DEBUG)
	@Message(value = "Disabling contextual LOB creation as Dialect reported it is not supported", id = 10010004)
	void nonContextualLobCreationDialect();

	@LogMessage(level = DEBUG)
	@Message(value = "Disabling contextual LOB creation as createClob() method threw error : %s", id = 10010005)
	void contextualClobCreationFailed(Throwable t);

	@LogMessage(level = DEBUG)
	@Message(value = "Disabling contextual NCLOB creation as createNClob() method threw error : %s", id = 10010006)
	void contextualNClobCreationFailed(Throwable t);
}
