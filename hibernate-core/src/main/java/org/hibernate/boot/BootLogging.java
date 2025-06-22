/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot;

import org.hibernate.Internal;
import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.Logger;


/**
 * Logging related to Hibernate bootstrapping
 */
@SubSystemLogging(
		name = BootLogging.NAME,
		description = "Logging related to bootstrapping of a SessionFactory / EntityManagerFactory"
)
@Internal
public interface BootLogging {
	String NAME = SubSystemLogging.BASE + ".boot";
	Logger BOOT_LOGGER = Logger.getLogger( NAME );
}
