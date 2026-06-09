/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import org.hibernate.Internal;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;


/// todo : find the proper min/max id range and make message-logger
///
/// @since 9.0
/// @author Steve Ebersole
@Internal
public interface CategorizationLogging extends BasicLogger {
	String NAME = "org.hibernate.models.orm";

	Logger CATEGORIZATION_LOGGER = Logger.getLogger( NAME );
}
