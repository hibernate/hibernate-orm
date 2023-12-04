/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind;

import org.hibernate.Internal;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;

/**
 * todo : find the proper min/max id range
 *
 * @author Steve Ebersole
 */
@Internal
public interface ModelBindingLogging extends BasicLogger {
	String NAME = "org.hibernate.models.orm";

	Logger MODEL_BINDING_LOGGER = Logger.getLogger( NAME );
}
