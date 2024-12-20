/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.Logger;

/**
 * Logging related to natural-id operations
 *
 * @author Steve Ebersole
 */
@SubSystemLogging(
		name = NaturalIdLogging.LOGGER_NAME,
		description = "Logging related to handling of natural-id mappings"
)
public interface NaturalIdLogging {
	String LOGGER_NAME = SubSystemLogging.BASE + ".mapping.natural_id";
	Logger NATURAL_ID_LOGGER = Logger.getLogger( LOGGER_NAME );
}
