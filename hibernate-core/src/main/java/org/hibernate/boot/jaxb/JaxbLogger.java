/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb;

import org.hibernate.Internal;
import org.hibernate.boot.BootLogging;
import org.hibernate.internal.log.SubSystemLogging;

import org.hibernate.type.SerializationException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.invoke.MethodHandles;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * @author Steve Ebersole
 */
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 90005501, max = 90005600 )
@SubSystemLogging(
		name = JaxbLogger.LOGGER_NAME,
		description = "Logging related to JAXB processing"
)
@Internal
public interface JaxbLogger extends BasicLogger {
	String LOGGER_NAME = BootLogging.NAME + ".jaxb";
	JaxbLogger JAXB_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), JaxbLogger.class, LOGGER_NAME );

	@LogMessage(level = DEBUG)
	@Message(id = 90005501, value = "Unable to close StAX reader")
	void unableToCloseStaxReader(@Cause Throwable e);

	@LogMessage(level = TRACE)
	@Message(id = 90005502, value = "Performing JAXB binding of hbm.xml document: %s")
	void performingJaxbBindingOfHbmXmlDocument(String origin);

	@LogMessage(level = TRACE)
	@Message(id = 90005503, value = "Performing JAXB binding of orm.xml document: %s")
	void performingJaxbBindingOfOrmXmlDocument(String origin);

	@LogMessage(level = INFO)
	@Message(id = 90005504, value = "Reading mappings from file: %s")
	void readingMappingsFromFile(String path);

	@LogMessage(level = INFO)
	@Message(id = 90005505, value = "Reading mappings from cache file: %s")
	void readingCachedMappings(File cachedFile);

	@LogMessage(level = WARN)
	@Message(id = 90005506, value = "Could not deserialize cache file [%s]: %s")
	void unableToDeserializeCache(String path, SerializationException error);

	@LogMessage(level = WARN)
	@Message(id = 90005507, value = "I/O reported error writing cached file: [%s]: %s")
	void unableToWriteCachedFile(String path, String message);

	@LogMessage(level = WARN)
	@Message(id = 90005508, value = "Could not update cached file timestamp: [%s]")
	@SuppressWarnings("unused")
	void unableToUpdateCachedFileTimestamp(String path);

	@LogMessage(level = WARN)
	@Message(id = 90005509, value = "I/O reported cached file could not be found: [%s]: %s")
	void cachedFileNotFound(String path, FileNotFoundException error);

	@LogMessage(level = INFO)
	@Message(id = 90005510, value = "Omitting cached file [%s] as the mapping file is newer")
	void cachedFileObsolete(File cachedFile);

	@LogMessage(level = TRACE)
	@Message(id = 90005511, value = "Writing cache file for: %s to: %s")
	void writingCacheFile(String xmlPath, String serPath);

	@LogMessage(level = DEBUG)
	@Message(id = 90005512, value = "Problem closing schema stream [%s]")
	void problemClosingSchemaStream(String details);

	@LogMessage(level = TRACE)
	@Message(id = 90005513, value = "In resolveEntity(%s, %s, %s, %s)")
	void resolveEntityInvocation(String publicID, String systemID, String baseURI, String namespace);

	@LogMessage(level = TRACE)
	@Message(id = 90005514, value = "Interpreting namespace: %s")
	void interpretingNamespace(String namespace);

	@LogMessage(level = TRACE)
	@Message(id = 90005515, value = "Checking public/system identifiers `%s`/`%s` as DTD references")
	void checkingDtdReferences(String publicID, String systemID);

	@LogMessage(level = TRACE)
	@Message(id = 90005516, value = "Recognized 'classpath:' identifier; attempting to resolve on classpath [%s]")
	void recognizedClasspathIdentifierAttemptingToResolve(String systemID);

	@LogMessage(level = TRACE)
	@Message(id = 90005517, value = "Unable to resolve [%s] on classpath")
	void unableToResolveOnClasspath(String systemID);

	@LogMessage(level = TRACE)
	@Message(id = 90005518, value = "Resolved [%s] on classpath")
	void resolvedOnClasspath(String systemID);
}
