/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model;

import org.hibernate.Internal;
import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.Logger;

/**
 * Logging related to entity and collection mutations stemming from persistence-context events
 *
 * @author Steve Ebersole
 */
@SubSystemLogging(
		name = ModelMutationLogging.NAME,
		description = "Logging related to entity and collection mutations stemming from persistence-context events"
)
@Internal
public final class ModelMutationLogging {

	public static final String NAME = SubSystemLogging.BASE + ".jdbc.mutation";

	public static final Logger MODEL_MUTATION_LOGGER = Logger.getLogger( NAME );
}
