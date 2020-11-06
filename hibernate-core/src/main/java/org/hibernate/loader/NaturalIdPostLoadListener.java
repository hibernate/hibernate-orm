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
 * Listener for post load-by-natural-id events
 */
@FunctionalInterface
public interface NaturalIdPostLoadListener {
	/**
	 * Singleton access for no listener
	 */
	NaturalIdPostLoadListener NO_OP = (loadingEntity, entity, session) -> {};

	/**
	 * Callback for a load-by-natural-id pre event
	 */
	void completedLoadByNaturalId(EntityMappingType entityMappingType, Object entity, SharedSessionContract session);
}
