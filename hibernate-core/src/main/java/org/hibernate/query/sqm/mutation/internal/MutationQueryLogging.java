/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.internal;

import org.hibernate.Internal;
import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
@SubSystemLogging(
		name = "MutationQueryLogging.MUTATION_QUERY_LOGGER_NAME",
		description = "Logging for multi-table mutation queries"
)
@Internal
public interface MutationQueryLogging {
	String MUTATION_QUERY_LOGGER_NAME = SubSystemLogging.BASE + ".query.mutation";

	Logger MUTATION_QUERY_LOGGER = Logger.getLogger( MUTATION_QUERY_LOGGER_NAME );
}
