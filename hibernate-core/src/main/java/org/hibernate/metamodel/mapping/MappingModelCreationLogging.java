/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.Internal;
import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;

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
			Logger.getMessageLogger( MethodHandles.lookup(), MappingModelCreationLogging.class, LOGGER_NAME );
}
