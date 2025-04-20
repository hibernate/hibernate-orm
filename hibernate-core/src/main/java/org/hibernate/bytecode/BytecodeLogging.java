/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode;

import org.hibernate.Internal;
import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
@SubSystemLogging(
		name = BytecodeLogging.LOGGER_NAME,
		description = "Logging related to bytecode handling"
)
@Internal
public interface BytecodeLogging {
	String LOGGER_NAME = SubSystemLogging.BASE + "bytecode";
	Logger LOGGER = Logger.getLogger( LOGGER_NAME );
}
