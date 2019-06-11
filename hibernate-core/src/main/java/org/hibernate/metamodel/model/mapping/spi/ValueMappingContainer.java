/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.mapping.spi;

import java.util.function.Consumer;

/**
 * Container for ValueMappings
 *
 * @author Steve Ebersole
 */
public interface ValueMappingContainer {
	/**
	 * Find a sub-ValueMapping by name
	 */
	ValueMapping findValueMapping(String name);

	/**
	 * Visit all of this container's sub-ValueMappings
	 */
	void visitValueMappings(Consumer<ValueMapping> consumer);
}
