/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.query;

import org.hibernate.boot.BootLogging;
import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
@SubSystemLogging(
		name = BootQueryLogging.NAME,
		description = "Logging related to processing of named-queries"
)
public interface BootQueryLogging {
	String NAME = BootLogging.NAME + ".query";
	Logger BOOT_QUERY_LOGGER = Logger.getLogger( NAME );
}
