/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.query;

import org.hibernate.Internal;
import org.hibernate.boot.BootLogging;
import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
@SubSystemLogging(
		name = BootQueryLogging.NAME,
		description = "Logging related to processing of named queries"
)
@Internal
public interface BootQueryLogging {
	String NAME = BootLogging.NAME + ".query";
	Logger BOOT_QUERY_LOGGER = Logger.getLogger( NAME );
}
