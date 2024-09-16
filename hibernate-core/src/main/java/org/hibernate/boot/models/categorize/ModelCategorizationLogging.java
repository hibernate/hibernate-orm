/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize;

import org.hibernate.Internal;

import org.jboss.logging.Logger;

/**
 * todo : find the proper min/max id range
 *
 * @author Steve Ebersole
 */
@Internal
public interface ModelCategorizationLogging {
	String NAME = "org.hibernate.models.orm";

	Logger MODEL_CATEGORIZATION_LOGGER = Logger.getLogger( NAME );
}
