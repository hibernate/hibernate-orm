/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.results.spi;

import org.hibernate.sql.exec.results.internal.EntitySqlSelectionMappings;

/**
 * Various utility methods related to generation of various components needed
 * for {@link Initializer} implementations.
 *
 * @author Steve Ebersole
 */
public class InitializerHelper {

	/**
	 * Resolve the `EntitySqlSelectionMappings` pertaining to a specific entity reference
	 *
	 * @param entityReference The entity reference - a specific "usage" of an entity in a
	 * given query to account for roots, fetches et.al.
	 *
	 * @param creationContext Access to information needed as we resolve the mappings.
	 *
	 * @return The aggregated resolution of
	 */
	public static EntitySqlSelectionMappings resolveSqlSelectionMappings(
			EntityReference entityReference,
			QueryResultCreationContext creationContext) {
		final EntitySqlSelectionMappingBuildingVisitationStrategy strategy = new EntitySqlSelectionMappingBuildingVisitationStrategy(
				entityReference, creationContext
		);
		entityReference.getEntityDescriptor().visitNavigables( strategy );
		return strategy.generateMappings();
	}
}
