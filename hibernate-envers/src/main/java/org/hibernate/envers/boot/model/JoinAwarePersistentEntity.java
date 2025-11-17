/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
