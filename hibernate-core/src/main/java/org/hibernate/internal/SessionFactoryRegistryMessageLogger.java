/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import org.hibernate.Internal;
import org.hibernate.engine.jndi.JndiException;
import org.hibernate.engine.jndi.JndiNameException;
import org.hibernate.internal.log.SubSystemLogging;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import javax.naming.NamingException;
import java.lang.invoke.MethodHandles;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;
import static org.jboss.logging.Logger.getMessageLogger;

@MessageLogger(projectCode = "HHH")
@ValidIdRange( min = 20100, max = 20400 )
@SubSystemLogging(
		name = SessionFactoryRegistryMessageLogger.LOGGER_NAME,
		description = "Logging related to session factory registry"
)
@Internal
public interface SessionFactoryRegistryMessageLogger extends BasicLogger  {
	String LOGGER_NAME = SubSystemLogging.BASE + ".factoryRegistry";

	SessionFactoryRegistryMessageLogger INSTANCE =
			getMessageLogger( MethodHandles.lookup(), SessionFactoryRegistryMessageLogger.class, LOGGER_NAME );

	@LogMessage(level = WARN)
	@Message(value = "Naming exception occurred accessing factory: %s", id = 20178)
	void namingExceptionAccessingFactory(NamingException exception);

	@LogMessage(level = INFO)
	@Message(value = "Bound factory to JNDI name: %s", id = 20194)
	void factoryBoundToJndiName(String name);

	@LogMessage(level = INFO)
	@Message(value = "A factory was renamed from [%s] to [%s] in JNDI", id = 20196)
	void factoryJndiRename(String oldName, String newName);

	@LogMessage(level = DEBUG)
	@Message(value = "Could not bind JNDI listener", id = 20127)
	void couldNotBindJndiListener();

	@LogMessage(level = ERROR)
	@Message(value = "Invalid JNDI name: %s", id = 20135)
	void invalidJndiName(String name, @Cause JndiNameException e);

	@LogMessage(level = WARN)
	@Message(value = "Could not bind factory to JNDI", id = 20277)
	void unableToBindFactoryToJndi(@Cause JndiException e);

	@LogMessage(level = INFO)
	@Message(value = "Unbound factory from JNDI name: %s", id = 20197)
	void factoryUnboundFromJndiName(String name);

	@LogMessage(level = INFO)
	@Message(value = "A factory was unbound from name: %s", id = 20198)
	void factoryUnboundFromName(String name);

	@LogMessage(level = WARN)
	@Message(value = "Could not unbind factory from JNDI", id = 20374)
	void unableToUnbindFactoryFromJndi(@Cause JndiException e);

	@LogMessage(level = DEBUG)
	@Message(value = "Registering SessionFactory: %s (%s)", id = 20384)
	void registeringSessionFactory(String uuid, String name);

	@LogMessage(level = DEBUG)
	@Message(value = "Attempting to bind SessionFactory [%s] to JNDI", id = 20280)
	void attemptingToBindFactoryToJndi(String name);

	@LogMessage(level = DEBUG)
	@Message(value = "Attempting to unbind SessionFactory [%s] from JNDI", id = 20281)
	void attemptingToUnbindFactoryFromJndi(String name);

	@LogMessage(level = DEBUG)
	@Message(value = "A SessionFactory was successfully bound to name: %s", id = 20282)
	void factoryBoundToJndi(String name);

	@LogMessage(level = DEBUG)
	@Message(value = "Not binding SessionFactory to JNDI, no JNDI name configured", id = 20385)
	void notBindingSessionFactory();
}
