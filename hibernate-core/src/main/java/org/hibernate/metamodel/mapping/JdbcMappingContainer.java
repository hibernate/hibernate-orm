/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.internal.util.IndexedConsumer;

/**
 * Container for one-or-more JdbcMappings
 */
public interface JdbcMappingContainer {
	/**
	 * The number of JDBC mappings
	 */
	default int getJdbcTypeCount() {
		return forEachJdbcType( (index, jdbcMapping) -> {} );
	}

	JdbcMapping getJdbcMapping(int index);

	default JdbcMapping getSingleJdbcMapping() {
		assert getJdbcTypeCount() == 1;
		return getJdbcMapping( 0 );
	}

	/**
	 * Visit each of JdbcMapping
	 *
	 * @apiNote Same as {@link #forEachJdbcType(int, IndexedConsumer)} starting from `0`
	 */
	default int forEachJdbcType(IndexedConsumer<JdbcMapping> action) {
		return forEachJdbcType( 0, action );
	}

	/**
	 * Visit each JdbcMapping starting from the given offset
	 */
	int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action);
}
