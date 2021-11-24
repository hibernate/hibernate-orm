/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.model;

import java.util.List;

/**
 * Contract for entity mappings that support secondary table joins.
 *
 * @author Chris Cranford
 */
public interface JoinAwarePersistentEntity {
	/**
	 * Get an unmodifiable list of joins associated with entity mapping.
	 * @return list of joins
	 */
	List<Join> getJoins();

	/**
	 * Add a secondary table join to the entity mapping.
	 * @param join the secondary table join, should never be {@code null}
	 */
	void addJoin(Join join);
}
