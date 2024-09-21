/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader;

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
		description = "Logging related to loaders of domain model references (`org.hibernate.loader.ast.spi.Loader`); " +
				"see also `" + SubSystemLogging.BASE + ".results`"
)
public interface LoaderLogging {
	String LOGGER_NAME = SubSystemLogging.BASE + ".loader";

	Logger LOADER_LOGGER = Logger.getLogger( LOGGER_NAME );
}
