/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.results.graph.entity;

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
public interface EntityLoadingLogging {
	String LOGGER_NAME = LoadingLogger.LOGGER_NAME + ".entity";
	Logger ENTITY_LOADING_LOGGER = Logger.getLogger( LOGGER_NAME );
}
