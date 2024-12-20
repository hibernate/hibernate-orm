/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.Logger;

/**
 * Logging related to loading a {@linkplain org.hibernate.loader.ast.spi.Loadable loadable}
 * by multiple "keys".  The key can be primary, foreign or natural.
 *
 * @see org.hibernate.annotations.BatchSize
 * @see org.hibernate.Session#byMultipleIds
 * @see org.hibernate.Session#byMultipleNaturalId
 *
 * @author Steve Ebersole
 */
@SubSystemLogging(
		name = MultiKeyLoadLogging.LOGGER_NAME,
		description = "Logging related to multi-key loading of entity and collection references"
)
public interface MultiKeyLoadLogging {
	String LOGGER_NAME = SubSystemLogging.BASE + ".loader.multi";

	Logger MULTI_KEY_LOAD_LOGGER = Logger.getLogger( LOGGER_NAME );
}
