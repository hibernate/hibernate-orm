/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader;

import org.hibernate.Internal;
import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.Logger;

/**
 * Logging for loaders
 *
 * @see org.hibernate.loader.ast.spi.Loader
 *
 * @author Steve Ebersole
 */
@SubSystemLogging(
		name = LoaderLogging.LOGGER_NAME,
		description = "Logging related to loaders of domain model references"
)
@Internal
public interface LoaderLogging {
	String LOGGER_NAME = SubSystemLogging.BASE + ".loader";

	Logger LOADER_LOGGER = Logger.getLogger( LOGGER_NAME );
}
