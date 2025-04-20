/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.entity;

import org.hibernate.Internal;
import org.hibernate.internal.log.SubSystemLogging;
import org.hibernate.sql.results.LoadingLogger;
import org.hibernate.sql.results.graph.embeddable.EmbeddableLoadingLogger;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
@SubSystemLogging(
		name = EmbeddableLoadingLogger.LOGGER_NAME,
		description = "Logging related to entity loading"
)
@Internal
public interface EntityLoadingLogging {
	String LOGGER_NAME = LoadingLogger.LOGGER_NAME + ".entity";
	Logger ENTITY_LOADING_LOGGER = Logger.getLogger( LOGGER_NAME );
}
