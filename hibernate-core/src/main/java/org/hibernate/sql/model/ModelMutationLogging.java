/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.model;

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
public final class ModelMutationLogging {

	public static final String NAME = SubSystemLogging.BASE + ".jdbc.mutation";

	public static final Logger MODEL_MUTATION_LOGGER = Logger.getLogger( NAME );
}
