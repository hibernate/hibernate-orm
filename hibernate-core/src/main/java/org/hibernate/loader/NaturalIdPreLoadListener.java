/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader;

import org.hibernate.SharedSessionContract;
import org.hibernate.metamodel.mapping.EntityMappingType;

/**
 * Listener for pre load-by-natural-id events
 */
@FunctionalInterface
public interface NaturalIdPreLoadListener {
	/**
	 * Singleton access for no listener
	 */
	NaturalIdPreLoadListener NO_OP = (loadingEntity, naturalId, session) -> {};

	/**
	 * Callback for a load-by-natural-id pre event
	 */
	void startingLoadByNaturalId(EntityMappingType loadingEntity, Object naturalId, SharedSessionContract session);
}
