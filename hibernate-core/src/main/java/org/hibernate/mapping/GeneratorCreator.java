/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.mapping;

import org.hibernate.Internal;
import org.hibernate.generator.Assigned;
import org.hibernate.generator.Generator;
import org.hibernate.generator.GeneratorCreationContext;

/**
 * Instantiates a {@link Generator}.
 *
 * @since 6.2
 *
 * @author Gavin King
 */
@Internal
@FunctionalInterface
public interface GeneratorCreator {
	/**
	 * Create the generator.
	 */
	Generator createGenerator(GeneratorCreationContext context);

	/**
	 * Does this object create instances of {@link Assigned}?
	 *
	 * @since 7.0
	 */
	default boolean isAssigned() {
		return false;
	}
}
