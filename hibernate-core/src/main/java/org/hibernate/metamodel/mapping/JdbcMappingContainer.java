/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
