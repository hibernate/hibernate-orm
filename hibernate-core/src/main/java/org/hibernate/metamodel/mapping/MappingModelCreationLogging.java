/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.Internal;
import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;
import java.util.Locale;

import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Logger used during mapping-model creation
 *
 * @author Steve Ebersole
 */
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 90005701, max = 90005800 )
@SubSystemLogging(
		name = MappingModelCreationLogging.LOGGER_NAME,
		description = "Logging related to building of Hibernate's runtime metamodel descriptors of the domain model"
)
@Internal
public interface MappingModelCreationLogging extends BasicLogger {
	String LOGGER_NAME = SubSystemLogging.BASE + ".model.mapping.creation";

	Logger MAPPING_MODEL_CREATION_LOGGER = Logger.getLogger( LOGGER_NAME );
	MappingModelCreationLogging MAPPING_MODEL_CREATION_MESSAGE_LOGGER =
			Logger.getMessageLogger( MethodHandles.lookup(), MappingModelCreationLogging.class, LOGGER_NAME, Locale.ROOT );

	@LogMessage(level = TRACE)
	@Message(id = 90005701, value = "Wrapping up metadata context...")
	void wrappingUpMetadataContext();

	@LogMessage(level = TRACE)
	@Message(id = 90005702, value = "Starting entity [%s]")
	void startingEntity(String entityName);

	@LogMessage(level = TRACE)
	@Message(id = 90005703, value = "Completed entity [%s]")
	void completedEntity(String entityName);

	@LogMessage(level = TRACE)
	@Message(id = 90005704, value = "Starting mapped superclass [%s]")
	void startingMappedSuperclass(String name);

	@LogMessage(level = TRACE)
	@Message(id = 90005705, value = "Completed mapped superclass [%s]")
	void completedMappedSuperclass(String name);

	@LogMessage(level = TRACE)
	@Message(id = 90005706, value = "Building old-school composite identifier [%s]")
	void buildingOldSchoolCompositeIdentifier(String name);

	@LogMessage(level = WARN)
	@Message(id = 90005707, value = "Unable to locate static metamodel field: %s.%s")
	void unableToLocateStaticMetamodelField(String className, String fieldName);
}
