/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import org.hibernate.Internal;
import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Sub-system logging related to SessionFactory and its registry
 */
@SubSystemLogging(
		name = SessionFactoryLogging.NAME,
		description = "Logging related to SessionFactory lifecycle, serialization, and registry/JNDI operations"
)
@MessageLogger(projectCode = "HHH")
@ValidIdRange(min = 90006001, max = 90006100)
@Internal
public interface SessionFactoryLogging extends BasicLogger {
	String NAME = SubSystemLogging.BASE + ".factory";

	SessionFactoryLogging SESSION_FACTORY_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), SessionFactoryLogging.class, NAME );

	// ---- SessionFactoryImpl related ---------------------------------------------------------------

	@LogMessage(level = TRACE)
	@Message("Building session factory")
	void buildingSessionFactory();

	@LogMessage(level = DEBUG)
	@Message(value = "Instantiating factory [%s] with settings: %s", id = 90006001)
	void instantiatingFactory(String uuid, Map<String, Object> settings);

	@LogMessage(level = TRACE)
	@Message("Eating error closing factory after failed instantiation")
	void eatingErrorClosingFactoryAfterFailedInstantiation();

	@LogMessage(level = TRACE)
	@Message("Instantiated factory: %s")
	void instantiatedFactory(String uuid);

	@LogMessage(level = TRACE)
	@Message("Returning a Reference to the factory")
	void returningReferenceToFactory();

	@LogMessage(level = TRACE)
	@Message("Already closed")
	void alreadyClosed();

	@LogMessage(level = DEBUG)
	@Message(value = "Closing factory [%s]", id = 90006005)
	void closingFactory(String uuid);

	@LogMessage(level = DEBUG)
	@Message(value = "Serializing factory [%s]", id = 90006010)
	void serializingFactory(String uuid);

	@LogMessage(level = DEBUG)
	@Message(value = "Deserialized factory [%s]", id = 90006011)
	void deserializedFactory(String uuid);

	@LogMessage(level = TRACE)
	@Message("Serialized factory")
	void serializedFactory();

	@LogMessage(level = TRACE)
	@Message("Deserializing factory")
	void deserializingFactory();

	@LogMessage(level = TRACE)
	@Message("Resolving serialized factory")
	void resolvingSerializedFactory();

	@LogMessage(level = TRACE)
	@Message("Resolved factory by UUID: %s")
	void resolvedFactoryByUuid(String uuid);

	@LogMessage(level = TRACE)
	@Message("Resolved factory by name: %s")
	void resolvedFactoryByName(String name);

	@LogMessage(level = TRACE)
	@Message("Resolving factory from deserialized session")
	void resolvingFactoryFromDeserializedSession();

	@LogMessage(level = WARN)
	@Message(value = "Unable to construct current session context [%s]", id = 90006030)
	void unableToConstructCurrentSessionContext(String sessionContextType, @Cause Throwable throwable);

}
